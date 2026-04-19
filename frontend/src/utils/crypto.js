// src/utils/crypto.js

// ArrayBuffer转Base64
function arrayBufferToBase64(buf) {
    const bytes = new Uint8Array(buf)
    let binary = ''
    for (let i = 0; i < bytes.byteLength; i++) binary += String.fromCharCode(bytes[i] ?? 0)
    return btoa(binary)
}

// Base64转ArrayBuffer
function base64ToArrayBuffer(b64) {
    const binary = atob(b64)
    const bytes = new Uint8Array(binary.length)
    for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i)
    return bytes.buffer
}

function chunk64(s) {
    return s.match(/.{1,64}/g)?.join('\n') ?? s
}

function toPem(base64, label) {
    return `-----BEGIN ${label}-----\n${chunk64(base64)}\n-----END ${label}-----`
}

function pemToBase64(pem) {
    return pem
        .replace(/-----BEGIN [A-Z ]+-----/, '')
        .replace(/-----END [A-Z ]+-----/, '')
        .replace(/\s/g, '')
}

// 生成RSA密钥对
export async function generateRSAKeyPair() {
    const keyPair = await crypto.subtle.generateKey(
        {
            name: 'RSA-OAEP',
            modulusLength: 2048,
            publicExponent: new Uint8Array([1, 0, 1]),
            hash: 'SHA-256',
        },
        true,
        ['encrypt', 'decrypt'],
    )

    const publicSpki = await crypto.subtle.exportKey('spki', keyPair.publicKey)
    const privatePkcs8 = await crypto.subtle.exportKey('pkcs8', keyPair.privateKey)

    const publicKeyPem = toPem(arrayBufferToBase64(publicSpki), 'PUBLIC KEY')
    const privateKeyPem = toPem(arrayBufferToBase64(privatePkcs8), 'PRIVATE KEY')
    return { publicKeyPem, privateKeyPem }
}

// 导入公钥
export async function importPublicKey(pem) {
    const b64 = pemToBase64(pem)
    const der = base64ToArrayBuffer(b64)
    return crypto.subtle.importKey('spki', der, { name: 'RSA-OAEP', hash: 'SHA-256' }, false, ['encrypt'])
}

// 导入私钥
export async function importPrivateKey(pem) {
    const b64 = pemToBase64(pem)
    const der = base64ToArrayBuffer(b64)
    return crypto.subtle.importKey('pkcs8', der, { name: 'RSA-OAEP', hash: 'SHA-256' }, false, ['decrypt'])
}

// 生成随机的AES-256-GCM密钥
export async function generateAESKey() {
    return crypto.subtle.generateKey({ name: 'AES-GCM', length: 256 }, true, ['encrypt', 'decrypt'])
}

// 将AES密钥导出为原始字节（32字节）
export async function exportAESKey(key) {
    return crypto.subtle.exportKey('raw', key)
}

// 从原始字节导入AES密钥
export async function importAESKey(raw) {
    return crypto.subtle.importKey('raw', raw, { name: 'AES-GCM', length: 256 }, true, ['encrypt', 'decrypt'])
}

// 使用RSA公钥加密AES密钥
export async function wrapAESKeyWithRSA(aesKey, rsaPublicKey) {
    const rawAes = await exportAESKey(aesKey)
    const encrypted = await crypto.subtle.encrypt({ name: 'RSA-OAEP' }, rsaPublicKey, rawAes)
    return arrayBufferToBase64(new Uint8Array(encrypted))
}

// 使用RSA私钥解密AES密钥
export async function unwrapAESKeyWithRSA(wrappedB64, rsaPrivateKey) {
    try {
        console.log('开始解密 AES 密钥')
        console.log('封装密钥长度:', wrappedB64?.length)
        console.log('封装密钥格式检查:', /^[A-Za-z0-9+/=]+$/.test(wrappedB64))
        
        const encrypted = base64ToArrayBuffer(wrappedB64)
        console.log('解码后的密文长度:', encrypted.byteLength)
        
        const rawAes = await crypto.subtle.decrypt({ name: 'RSA-OAEP' }, rsaPrivateKey, encrypted)
        console.log('AES 密钥解密成功，长度:', rawAes.byteLength)
        
        return importAESKey(rawAes)
    } catch (error) {
        console.error('解密 AES 密钥失败:', error)
        console.error('错误类型:', error.name)
        console.error('错误信息:', error.message)
        throw error
    }
}

// 使用AES-GCM加密数据块
export async function encryptChunk(data, aesKey) {
    const iv = crypto.getRandomValues(new Uint8Array(12))
    const ciphertext = await crypto.subtle.encrypt({ name: 'AES-GCM', iv }, aesKey, data)
    const result = new Uint8Array(iv.byteLength + ciphertext.byteLength)
    result.set(iv, 0)
    result.set(new Uint8Array(ciphertext), iv.byteLength)
    return result
}

// 解密由encryptChunk加密的数据块
export async function decryptChunk(encrypted, aesKey) {
    const data = new Uint8Array(encrypted)
    const iv = data.slice(0, 12)
    const ciphertext = data.slice(12)
    return crypto.subtle.decrypt({ name: 'AES-GCM', iv }, aesKey, ciphertext)
}

/**
 * 计算数据的SHA-256哈希值，返回十六进制字符串
 * @param {ArrayBuffer|Uint8Array|Blob|File} data - 要计算哈希的数据
 * @returns {Promise<string>} 十六进制哈希字符串
 */
export async function sha256Hex(data) {
    // 处理不同类型的输入
    let buffer;

    if (data instanceof File || data instanceof Blob) {
        buffer = await data.arrayBuffer();
    } else if (data instanceof ArrayBuffer) {
        buffer = data;
    } else if (data instanceof Uint8Array) {
        buffer = data.buffer;
    } else if (data.buffer instanceof ArrayBuffer) {
        buffer = data.buffer;
    } else {
        throw new TypeError('Unsupported data type for sha256Hex');
    }

    const hash = await crypto.subtle.digest('SHA-256', buffer);
    return Array.from(new Uint8Array(hash))
        .map((b) => b.toString(16).padStart(2, '0'))
        .join('');
}

export async function sha256File(file) {
    if (!(file instanceof File || file instanceof Blob)) {
        throw new TypeError('Expected a File or Blob object');
    }
    return sha256Hex(file);
}

export function savePrivateKeyLocal(username, privateKeyPem) {
    const key = `securefile.privateKey.${username}`
    const encoded = btoa(unescape(encodeURIComponent(privateKeyPem)))
    localStorage.setItem(key, encoded)
}

export function loadPrivateKeyLocal(username) {
    const key = `securefile.privateKey.${username}`
    const encoded = localStorage.getItem(key)
    if (!encoded) return null
    try {
        return decodeURIComponent(escape(atob(encoded)))
    } catch {
        return null
    }
}

export const calculateSHA256 = sha256File;