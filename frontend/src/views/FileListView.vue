<template>
  <div class="file-list-container">
    <div class="file-list-card">
      <div class="card-header">
        <h2 class="title">{{ showShared ? '共享给我' : '我上传的' }}</h2>
        <div class="header-actions">
          <label class="switch-label">
            <input type="checkbox" v-model="showShared" @change="loadFileList" class="switch" />
            <span>{{ showShared ? '共享给我' : '我上传' }}</span>
          </label>
          <button @click="$router.push('/file-upload')" class="btn-primary">上传文件</button>
        </div>
      </div>
      
      <div class="table-container" v-loading="loading">
        <table class="custom-table">
          <thead>
            <tr>
              <th>文件名</th>
              <th>文件大小</th>
              <th>状态</th>
              <th>上传时间</th>
              <th>下载次数</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in fileList" :key="row.fileId">
              <td class="file-name">{{ row.fileName }}</td>
              <td>{{ formatFileSize(row.fileSize) }}</td>
              <td>
                <span :class="['status-tag', getStatusClass(row.status)]">
                  {{ row.status }}
                </span>
              </td>
              <td>{{ formatDate(row.createdAt) }}</td>
              <td class="text-center">{{ row.downloadCount }}</td>
              <td class="actions">
                <button 
                  @click="handleDownload(row)" 
                  :disabled="row.status !== 'COMPLETE'"
                  class="btn-action btn-download"
                >
                  下载
                </button>
                <button @click="handleDelete(row)" class="btn-action btn-delete">删除</button>
              </td>
            </tr>
            <tr v-if="fileList.length === 0">
              <td colspan="6" class="empty-state">暂无数据</td>
            </tr>
          </tbody>
        </table>
      </div>
      
      <div v-if="total > 0" class="pagination">
        <button 
          @click="handlePageChange(currentPage - 1)" 
          :disabled="currentPage === 1"
          class="page-btn"
        >
          上一页
        </button>
        <span class="page-info">第 {{ currentPage }} / {{ Math.ceil(total / pageSize) }} 页</span>
        <button 
          @click="handlePageChange(currentPage + 1)" 
          :disabled="currentPage >= Math.ceil(total / pageSize)"
          class="page-btn"
        >
          下一页
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http, getErrorMessage } from '../utils/request'
import { loadPrivateKeyLocal, importPrivateKey, unwrapAESKeyWithRSA, decryptChunk } from '../utils/crypto'
import { useAuthStore } from '../stores/auth'

const fileList = ref<any[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const showShared = ref(false)
const auth = useAuthStore()

const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

const formatDate = (s: string) => {
  if (!s) return '-'
  const str = String(s)
  if (str.includes('T')) return str.replace('T', ' ').substring(0, 19)
  return str.substring(0, 19)
}

const getStatusClass = (status: string) => {
  switch (status) {
    case 'COMPLETE': return 'status-success'
    case 'EXPIRED': return 'status-danger'
    case 'LIMIT_REACHED': return 'status-warning'
    case 'DELETED': return 'status-info'
    case 'UPLOADING': return 'status-warning'
    default: return 'status-info'
  }
}

const loadFileList = async () => {
  loading.value = true
  try {
    const url = showShared.value ? '/api/v1/file/shared/page' : '/api/v1/file/page'
    const resp = await http.get(url, { params: { current: currentPage.value, size: pageSize.value } })
    if (resp.data?.code === 200) {
      const pr = resp.data.data
      fileList.value = Array.isArray(pr?.list) ? pr.list : []
      total.value = Number(pr?.total ?? 0)
    } else {
      ElMessage.error(resp.data?.message || '加载文件列表失败')
    }
  } catch (e: any) {
    ElMessage.error(getErrorMessage(e))
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  loadFileList()
}

const handleDownload = async (row: any) => {
  let extractCode = ''
  try {
    const r = await ElMessageBox.prompt('如设置了提取码，请输入（留空表示未设置）', '下载校验', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: '1~6位数字或字母，留空表示未设置',
      inputType: 'text',
      inputValidator: (val: string) => {
        const v = (val || '').trim()
        if (!v) return true
        return /^[A-Za-z0-9]{1,6}$/.test(v) || '格式不正确'
      },
      distinguishCancelAndClose: true,
    })
    extractCode = (r.value || '').trim()
  } catch (e) {
    extractCode = ''
  }
  downloadFile(row, extractCode).catch((e: any) => ElMessage.error(getErrorMessage(e)))
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确认删除文件「${row.fileName}」？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    const resp = await http.post('/api/v1/file/delete', null, { params: { fileId: row.fileId } })
    if (resp.data?.code === 200) {
      ElMessage.success('已删除')
      await loadFileList()
    } else {
      ElMessage.error(resp.data?.message || '删除失败')
    }
  } catch {
    // 用户取消，不提示
  }
}

async function downloadFile(row: any, extractCode: string) {
  const username = auth.username || 'current'
  const privatePem = loadPrivateKeyLocal(username)
  if (!privatePem) throw new Error('未检测到本地私钥，请先在密钥管理生成并保存')
  
  const resp = await http.post('/api/v1/file/download/apply', { 
    fileId: row.fileId, 
    extractCode: extractCode || undefined 
  })
  
  if (resp.data?.code !== 200) {
    if (resp.data?.code === 20004) {
      await ElMessageBox.alert(
        '当前文件的接收者列表为空或未包含你本人。请先在"密钥管理"上传公钥，然后重新上传该文件并勾选"包含自己"。',
        '无法下载（缺少接收者密钥）',
        { type: 'warning' }
      )
      throw new Error('接收者未配置或未上传公钥')
    }
    throw new Error(resp.data?.message || '申请下载失败')
  }
  
  const vo = resp.data.data
  const priv = await importPrivateKey(privatePem)
  const aesKey = await unwrapAESKeyWithRSA(vo.wrappedAesKey, priv)
  
  const chunks: Uint8Array[] = []
  for (const meta of vo.chunkMetaList) {
    const r = await http.get('/api/v1/file/download/chunk', { 
      params: { sessionId: vo.downloadSessionId, chunkIndex: meta.chunkIndex }, 
      responseType: 'arraybuffer' 
    })
    const buf = await decryptChunk(r.data, aesKey)
    chunks.push(new Uint8Array(buf))
  }
  
  const totalLen = chunks.reduce((s, c) => s + c.byteLength, 0)
  const merged = new Uint8Array(totalLen)
  let offset = 0
  for (const c of chunks) {
    merged.set(c, offset)
    offset += c.byteLength
  }
  
  const blob = new Blob([merged], { type: row.contentType || 'application/octet-stream' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = row.fileName || 'download'
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
  
  await http.post('/api/v1/file/download/finish', null, { params: { sessionId: vo.downloadSessionId } })
  ElMessage.success('下载完成')
  await loadFileList()
}

onMounted(() => {
  loadFileList()
})
</script>

<style scoped>
.file-list-container {
  padding: 24px;
  min-height: calc(100vh - 60px);
}

.file-list-card {
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(0, 191, 255, 0.2);
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 0 30px rgba(0, 191, 255, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  flex-wrap: wrap;
  gap: 16px;
}

.title {
  font-size: 28px;
  font-weight: 700;
  color: #00bfff;
  text-shadow: 0 0 20px rgba(0, 191, 255, 0.5);
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 16px;
  align-items: center;
}

.switch-label {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #e2e8f0;
  cursor: pointer;
}

.switch {
  width: 48px;
  height: 24px;
  cursor: pointer;
}

.btn-primary {
  padding: 10px 24px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  border: none;
  background: linear-gradient(135deg, #00bfff, #00ff9d);
  color: #0a0e1a;
  box-shadow: 0 0 20px rgba(0, 191, 255, 0.3);
}

.btn-primary:hover {
  box-shadow: 0 0 30px rgba(0, 191, 255, 0.5);
  transform: translateY(-2px);
}

.table-container {
  overflow-x: auto;
  margin-bottom: 24px;
}

.custom-table {
  width: 100%;
  border-collapse: collapse;
}

.custom-table thead tr {
  background: rgba(0, 191, 255, 0.1);
  border-bottom: 2px solid rgba(0, 191, 255, 0.3);
}

.custom-table th {
  padding: 16px;
  text-align: left;
  color: #00bfff;
  font-weight: 600;
  text-shadow: 0 0 10px rgba(0, 191, 255, 0.3);
}

.custom-table tbody tr {
  border-bottom: 1px solid rgba(100, 116, 139, 0.2);
  transition: all 0.2s;
}

.custom-table tbody tr:hover {
  background: rgba(0, 191, 255, 0.05);
}

.custom-table td {
  padding: 16px;
  color: #e2e8f0;
}

.file-name {
  font-weight: 500;
  color: #00ff9d;
}

.text-center {
  text-align: center;
}

.status-tag {
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
}

.status-success {
  background: rgba(16, 185, 129, 0.2);
  color: #10b981;
  border: 1px solid rgba(16, 185, 129, 0.4);
}

.status-danger {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
  border: 1px solid rgba(239, 68, 68, 0.4);
}

.status-warning {
  background: rgba(245, 158, 11, 0.2);
  color: #f59e0b;
  border: 1px solid rgba(245, 158, 11, 0.4);
}

.status-info {
  background: rgba(100, 116, 139, 0.2);
  color: #94a3b8;
  border: 1px solid rgba(100, 116, 139, 0.4);
}

.actions {
  display: flex;
  gap: 8px;
}

.btn-action {
  padding: 6px 16px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.btn-download {
  background: rgba(0, 255, 157, 0.2);
  color: #00ff9d;
  border: 1px solid rgba(0, 255, 157, 0.4);
}

.btn-download:hover:not(:disabled) {
  background: rgba(0, 255, 157, 0.3);
  box-shadow: 0 0 15px rgba(0, 255, 157, 0.3);
}

.btn-download:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.btn-delete {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
  border: 1px solid rgba(239, 68, 68, 0.4);
}

.btn-delete:hover {
  background: rgba(239, 68, 68, 0.3);
  box-shadow: 0 0 15px rgba(239, 68, 68, 0.3);
}

.empty-state {
  text-align: center;
  color: #64748b;
  padding: 48px !important;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
}

.page-btn {
  padding: 8px 20px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  background: rgba(0, 191, 255, 0.1);
  color: #00bfff;
  border: 1px solid rgba(0, 191, 255, 0.3);
}

.page-btn:hover:not(:disabled) {
  background: rgba(0, 191, 255, 0.2);
  box-shadow: 0 0 15px rgba(0, 191, 255, 0.2);
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-info {
  color: #94a3b8;
  font-size: 14px;
}
</style>
