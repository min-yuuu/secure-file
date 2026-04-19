<template>
  <div class="upload-container">
    <div class="upload-card">
      <div class="card-header">
        <h2 class="title">文件上传</h2>
      </div>
      
      <div class="upload-area" @dragover.prevent @drop.prevent="handleDrop">
        <input 
          ref="fileInput" 
          type="file" 
          style="display: none" 
          @change="handleFileSelect"
        />
        <div class="upload-icon" @click="$refs.fileInput.click()">
          <svg viewBox="0 0 24 24" width="64" height="64">
            <path fill="currentColor" d="M9 16h6v-6h4l-7-7-7 7h4zm-4 2h14v2H5z"/>
          </svg>
        </div>
        <div class="upload-text">
          拖拽文件到此处或 <span class="link" @click="$refs.fileInput.click()">点击上传</span>
        </div>
        <div class="upload-tip">支持上传最大100MB的文件</div>
        
        <div v-if="fileList.length > 0" class="file-list">
          <div v-for="(file, idx) in fileList" :key="idx" class="file-item">
            <span>{{ file.name }}</span>
            <button @click="removeFile(idx)" class="remove-btn">×</button>
          </div>
        </div>
      </div>
      
      <div class="form-section">
        <div class="form-row">
          <div class="form-item">
            <label>提取码（1~6位）</label>
            <input v-model="extractCode" maxlength="6" placeholder="可选，留空表示不设置" class="input" />
          </div>
          <div class="form-item">
            <label>有效期</label>
            <select v-model="expiryHours" class="select">
              <option :value="24">1天</option>
              <option :value="168">7天</option>
              <option :value="-1">永久</option>
            </select>
          </div>
          <div class="form-item">
            <label>下载次数限制</label>
            <input v-model.number="downloadLimit" type="number" min="-1" max="999" class="input" />
            <div class="hint">-1 表示不限制</div>
          </div>
        </div>
        
        <div class="form-item full">
          <label>接收者（按用户名搜索）</label>
          <div class="recipients-input">
            <input 
              v-model="searchQuery" 
              @input="onSearchRecipients" 
              placeholder="搜索用户名并选择接收者" 
              class="input"
            />
            <div v-if="recipientsOptions.length > 0" class="dropdown">
              <div 
                v-for="opt in recipientsOptions" 
                :key="opt.value" 
                @click="addRecipient(opt)"
                class="dropdown-item"
              >
                {{ opt.label }}
              </div>
            </div>
          </div>
          <div class="selected-recipients">
            <span 
              v-for="id in recipientsIds" 
              :key="id" 
              class="recipient-tag"
            >
              {{ getRecipientLabel(id) }}
              <button @click="removeRecipient(id)" class="tag-remove">×</button>
            </span>
          </div>
          <div class="hint">提示：接收者必须先在"密钥管理"上传公钥，否则无法封装 AES 密钥。</div>
        </div>
        
        <div class="form-item full">
          <label class="checkbox-label">
            <input type="checkbox" v-model="includeSelf" />
            <span>同时包含自己（文件所有者）为可下载接收者</span>
          </label>
        </div>
      </div>
      
      <div class="upload-actions" v-if="fileList.length > 0">
        <button @click="handleUpload" :disabled="uploading" class="btn-primary">
          {{ uploading ? '上传中...' : '开始上传' }}
        </button>
        <button @click="handleClear" class="btn-secondary">清空列表</button>
      </div>
      
      <div v-if="uploading" class="progress-bar">
        <div class="progress-fill" :style="{ width: uploadProgress + '%' }"></div>
        <span class="progress-text">{{ uploadProgress }}%</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { http, getErrorMessage } from '../utils/request'
import { encryptChunk, sha256Hex, generateAESKey, importPublicKey, wrapAESKeyWithRSA } from '../utils/crypto'
import { useAuthStore } from '../stores/auth'

const fileList = ref<any[]>([])
const uploading = ref(false)
const uploadProgress = ref(0)
const fileId = ref('')
const recipientsIds = ref<string[]>([])
const recipientsOptions = ref<any[]>([])
const searchQuery = ref('')
const includeSelf = ref(true)
const auth = useAuthStore()
const extractCode = ref('')
const expiryHours = ref(24)
const downloadLimit = ref(-1)
const burnAfterReading = ref(false)
const fileInput = ref<HTMLInputElement>()

const handleFileSelect = (e: Event) => {
  const target = e.target as HTMLInputElement
  if (target.files && target.files.length > 0) {
    fileList.value = Array.from(target.files).map(f => ({ name: f.name, raw: f }))
  }
}

const handleDrop = (e: DragEvent) => {
  if (e.dataTransfer?.files && e.dataTransfer.files.length > 0) {
    fileList.value = Array.from(e.dataTransfer.files).map(f => ({ name: f.name, raw: f }))
  }
}

const removeFile = (idx: number) => {
  fileList.value.splice(idx, 1)
}

const addRecipient = (opt: any) => {
  if (!recipientsIds.value.includes(opt.value)) {
    recipientsIds.value.push(opt.value)
  }
  searchQuery.value = ''
  recipientsOptions.value = []
}

const removeRecipient = (id: string) => {
  recipientsIds.value = recipientsIds.value.filter(x => x !== id)
}

const getRecipientLabel = (id: string) => {
  const opt = recipientsOptions.value.find(o => o.value === id)
  return opt ? opt.label : id
}

const handleUpload = async () => {
  if (fileList.value.length === 0) {
    ElMessage.warning('请先选择文件')
    return
  }
  
  // 上传前验证：检查是否有接收者或勾选了"包含自己"
  if (recipientsIds.value.length === 0 && !includeSelf.value) {
    ElMessage.error('请至少选择一个接收者或勾选"包含自己"')
    return
  }
  
  // 如果勾选了"包含自己"，提前检查当前用户是否有公钥
  if (includeSelf.value) {
    try {
      const selfResp = await http.get('/api/v1/key/my')
      if (selfResp.data?.code !== 200 || !selfResp.data.data?.publicKey) {
        ElMessage.error('当前用户未上传公钥，无法将自己加入接收者。请先在"密钥管理"上传公钥')
        return
      }
    } catch (e: any) {
      ElMessage.error('检查公钥失败：' + getErrorMessage(e))
      return
    }
  }
  
  uploading.value = true
  uploadProgress.value = 0
  try {
    const file = fileList.value[0].raw
    const fileName = file.name
    const fileSize = file.size
    const contentType = file.type || 'application/octet-stream'
    const chunkCount = 1
    const plainBuf = await file.arrayBuffer()
    const aesKey = await generateAESKey()
    const encrypted = await encryptChunk(plainBuf, aesKey)
    const cipherHash = await sha256Hex(encrypted)
    const plainHash = await sha256Hex(plainBuf)
    
    const initResp = await http.post('/api/v1/file/upload/init', {
      fileName,
      fileSize,
      contentType,
      chunkCount,
      plainSha256: plainHash,
      extractCode: (extractCode.value || '').trim() || undefined,
      expiryHours: expiryHours.value,
      downloadLimit: downloadLimit.value,
      burnAfterReading: burnAfterReading.value
    })
    
    if (initResp.data?.code !== 200) throw new Error(initResp.data?.message || '初始化失败')
    fileId.value = initResp.data.data.fileId
    
    const form = new FormData()
    form.append('fileId', fileId.value)
    form.append('chunkIndex', String(0))
    form.append('cipherSha256', cipherHash)
    form.append('blob', new Blob([encrypted], { type: 'application/octet-stream' }))
    await http.post('/api/v1/file/upload/chunk', form)
    uploadProgress.value = 80
    
    const recipientsWrappedKeys: any[] = []
    const ids = recipientsIds.value.map((x) => Number(x))
    for (const id of ids) {
      const resp = await http.get(`/api/v1/key/user/${id}`)
      if (resp.data?.code === 200 && resp.data.data?.publicKey) {
        const pubKey = await importPublicKey(resp.data.data.publicKey)
        const wrapped = await wrapAESKeyWithRSA(aesKey, pubKey)
        recipientsWrappedKeys.push({ recipientUserId: id, wrappedAesKey: wrapped })
      }
    }
    
    if (includeSelf.value) {
      const selfResp = await http.get('/api/v1/key/my')
      if (selfResp.data?.code === 200 && selfResp.data.data?.publicKey) {
        const uid = Number(selfResp.data.data.userId)
        const selfPub = await importPublicKey(selfResp.data.data.publicKey)
        const selfWrapped = await wrapAESKeyWithRSA(aesKey, selfPub)
        if (!recipientsWrappedKeys.some(r => r.recipientUserId === uid)) {
          recipientsWrappedKeys.push({ recipientUserId: uid, wrappedAesKey: selfWrapped })
        }
      }
    }
    
    if (recipientsWrappedKeys.length === 0) {
      ElMessage.error('接收者列表为空：所有选中的接收者都没有上传公钥')
      uploading.value = false
      return
    }
    
    await http.post('/api/v1/file/upload/complete', {
      fileId: fileId.value,
      recipientsWrappedKeys
    })
    
    uploadProgress.value = 100
    ElMessage.success('上传成功')
    fileList.value = []
    recipientsIds.value = []
  } catch (e: any) {
    ElMessage.error(getErrorMessage(e))
  } finally {
    uploading.value = false
  }
}

const handleClear = () => {
  fileList.value = []
  uploadProgress.value = 0
}

async function onSearchRecipients() {
  const q = searchQuery.value.trim()
  if (!q) {
    recipientsOptions.value = []
    return
  }
  try {
    const resp = await http.get('/api/v1/user/search', { params: { keyword: q, limit: 10 } })
    if (resp.data?.code === 200 && Array.isArray(resp.data.data)) {
      recipientsOptions.value = resp.data.data.map((u: any) => {
        let label = u.username
        if (u.createTime) {
          const date = String(u.createTime).replace('T', ' ').substring(0, 10)
          label += ` (注册于 ${date})`
        }
        return {
          value: String(u.userId),
          label: label,
          publicKey: u.publicKey,
        }
      })
    } else {
      recipientsOptions.value = []
    }
  } catch {
    recipientsOptions.value = []
  }
}
</script>

<style scoped>
.upload-container {
  padding: 24px;
  min-height: calc(100vh - 60px);
}

.upload-card {
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(0, 191, 255, 0.2);
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 0 30px rgba(0, 191, 255, 0.1);
}

.card-header {
  margin-bottom: 32px;
}

.title {
  font-size: 28px;
  font-weight: 700;
  color: #00bfff;
  text-shadow: 0 0 20px rgba(0, 191, 255, 0.5);
  margin: 0;
}

.upload-area {
  background: rgba(10, 14, 26, 0.8);
  border: 2px dashed rgba(0, 191, 255, 0.3);
  border-radius: 12px;
  padding: 48px 24px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  margin-bottom: 32px;
}

.upload-area:hover {
  border-color: #00bfff;
  background: rgba(10, 14, 26, 0.95);
}

.upload-icon {
  color: #00bfff;
  margin-bottom: 16px;
  display: inline-block;
}

.upload-text {
  color: #94a3b8;
  font-size: 16px;
  margin-bottom: 8px;
}

.link {
  color: #00ff9d;
  cursor: pointer;
  text-decoration: underline;
}

.upload-tip {
  color: #64748b;
  font-size: 14px;
}

.file-list {
  margin-top: 24px;
  text-align: left;
}

.file-item {
  background: rgba(0, 191, 255, 0.1);
  border: 1px solid rgba(0, 191, 255, 0.3);
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #e2e8f0;
}

.remove-btn {
  background: transparent;
  border: none;
  color: #ef4444;
  font-size: 24px;
  cursor: pointer;
  padding: 0 8px;
}

.form-section {
  margin-bottom: 32px;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.form-item {
  display: flex;
  flex-direction: column;
}

.form-item.full {
  grid-column: 1 / -1;
}

.form-item label {
  color: #00bfff;
  font-size: 14px;
  margin-bottom: 8px;
  text-shadow: 0 0 10px rgba(0, 191, 255, 0.3);
}

.input, .select {
  background: rgba(10, 14, 26, 0.8);
  border: 1px solid rgba(0, 191, 255, 0.3);
  border-radius: 8px;
  padding: 12px 16px;
  color: #e2e8f0;
  font-size: 14px;
  transition: all 0.3s;
}

.input:focus, .select:focus {
  outline: none;
  border-color: #00bfff;
  box-shadow: 0 0 15px rgba(0, 191, 255, 0.2);
}

.hint {
  color: #64748b;
  font-size: 12px;
  margin-top: 6px;
}

.recipients-input {
  position: relative;
}

.dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: rgba(10, 14, 26, 0.95);
  border: 1px solid rgba(0, 191, 255, 0.3);
  border-radius: 8px;
  margin-top: 4px;
  max-height: 200px;
  overflow-y: auto;
  z-index: 10;
}

.dropdown-item {
  padding: 12px 16px;
  color: #e2e8f0;
  cursor: pointer;
  transition: all 0.2s;
}

.dropdown-item:hover {
  background: rgba(0, 191, 255, 0.1);
  color: #00bfff;
}

.selected-recipients {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.recipient-tag {
  background: rgba(0, 255, 157, 0.2);
  border: 1px solid rgba(0, 255, 157, 0.4);
  border-radius: 6px;
  padding: 6px 12px;
  color: #00ff9d;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.tag-remove {
  background: transparent;
  border: none;
  color: #00ff9d;
  font-size: 18px;
  cursor: pointer;
  padding: 0;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #e2e8f0;
  cursor: pointer;
}

.checkbox-label input[type="checkbox"] {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

.upload-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
  margin-bottom: 24px;
}

.btn-primary, .btn-secondary {
  padding: 12px 32px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  border: none;
}

.btn-primary {
  background: linear-gradient(135deg, #00bfff, #00ff9d);
  color: #0a0e1a;
  box-shadow: 0 0 20px rgba(0, 191, 255, 0.3);
}

.btn-primary:hover:not(:disabled) {
  box-shadow: 0 0 30px rgba(0, 191, 255, 0.5);
  transform: translateY(-2px);
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-secondary {
  background: rgba(100, 116, 139, 0.2);
  color: #94a3b8;
  border: 1px solid rgba(100, 116, 139, 0.3);
}

.btn-secondary:hover {
  background: rgba(100, 116, 139, 0.3);
  border-color: rgba(100, 116, 139, 0.5);
}

.progress-bar {
  position: relative;
  height: 32px;
  background: rgba(10, 14, 26, 0.8);
  border: 1px solid rgba(0, 191, 255, 0.3);
  border-radius: 16px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #00bfff, #00ff9d);
  transition: width 0.3s;
  box-shadow: 0 0 20px rgba(0, 191, 255, 0.5);
}

.progress-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #e2e8f0;
  font-weight: 600;
  text-shadow: 0 0 10px rgba(0, 0, 0, 0.8);
}
</style>
