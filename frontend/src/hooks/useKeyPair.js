import { computed, ref } from 'vue'
import { http } from '../utils/request'
import { generateRSAKeyPair, savePrivateKeyLocal, loadPrivateKeyLocal } from '../utils/crypto'
import { isMockMode } from '../utils/mock'

export function useKeyPair() {
    const publicKeyPem = ref('')
    const privateKeyPem = ref('')
    const hasLocalPrivateKey = computed(() => !!privateKeyPem.value)

    const generating = ref(false)
    const uploading = ref(false)

    async function generate(username) {
        generating.value = true
        try {
            const { publicKeyPem: pub, privateKeyPem: pri } = await generateRSAKeyPair()
            publicKeyPem.value = pub
            privateKeyPem.value = pri
            savePrivateKeyLocal(username, pri)
            // 同时保存公钥到本地，方便刷新后恢复
            localStorage.setItem(`securefile.publicKey.${username}`, pub)
            return { publicKeyPem: pub, privateKeyPem: pri }
        } finally {
            generating.value = false
        }
    }

    function loadLocal(username) {
        const pri = loadPrivateKeyLocal(username)
        if (pri) privateKeyPem.value = pri
        // 恢复公钥
        const pub = localStorage.getItem(`securefile.publicKey.${username}`)
        if (pub) publicKeyPem.value = pub
        return pri
    }

    async function uploadPublicKey(publicKey) {
        uploading.value = true
        try {
            if (isMockMode()) {
                return true
            }

            const resp = await http.post('/api/v1/key/my', { publicKey })
            if (resp.data.code !== 200) throw new Error(resp.data.message)
            return resp.data.data
        } finally {
            uploading.value = false
        }
    }

    return {
        publicKeyPem,
        privateKeyPem,
        hasLocalPrivateKey,
        generating,
        uploading,
        generate,
        loadLocal,
        uploadPublicKey,
    }
}