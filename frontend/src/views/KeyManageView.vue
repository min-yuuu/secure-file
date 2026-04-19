<template>
  <div class="key-manage-container">
    <div class="key-manage-card">
      <div class="alert-warning">
        <svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor">
          <path d="M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z"/>
        </svg>
        <div>
          <div class="alert-title">重要提示</div>
          <div class="alert-desc">私钥仅保存在本地浏览器（localStorage）。请立即备份，丢失无法恢复。</div>
        </div>
      </div>

      <div class="section">
        <div class="action-row">
          <button @click="onGenerate()" :disabled="generating" class="btn-primary">
            {{ generating ? '生成中...' : '生成密钥对' }}
          </button>
          <button @click="onUpload()" :disabled="!publicKeyPem || uploading" class="btn-success">
            {{ uploading ? '上传中...' : '上传公钥' }}
          </button>
          <button @click="copy(publicKeyPem)" :disabled="!publicKeyPem" class="btn-secondary">
            复制公钥
          </button>
        </div>

        <div class="key-grid">
          <div class="key-item">
            <div class="key-label">公钥（PEM）</div>
            <textarea v-model="publicKeyPem" readonly class="key-textarea" rows="10"></textarea>
          </div>
          <div class="key-item">
            <div class="key-label">私钥（PEM）</div>
            <textarea 
              v-model="privatePreview" 
              readonly 
              class="key-textarea" 
              rows="10"
              placeholder="私钥生成后已保存到本地；此处仅展示开头/结尾用于确认。"
            ></textarea>
          </div>
        </div>
      </div>

      <div class="divider"></div>

      <div v-if="!hasLocalPrivateKey" class="result-box result-warning">
        <svg viewBox="0 0 24 24" width="48" height="48" fill="currentColor">
          <path d="M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z"/>
        </svg>
        <div class="result-title">尚未在本地检测到私钥</div>
        <div class="result-subtitle">建议点击【生成密钥对】完成首次引导，然后上传公钥。</div>
      </div>
      <div v-else class="result-box result-success">
        <svg viewBox="0 0 24 24" width="48" height="48" fill="currentColor">
          <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
        </svg>
        <div class="result-title">本地私钥已保存</div>
        <div class="result-subtitle">请确认已备份，建议妥善保管。</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useKeyPair } from '../hooks/useKeyPair'
import { useAuthStore } from '../stores/auth'
import { getErrorMessage } from '../utils/request'

const auth = useAuthStore()
const localKeyUsername = auth.username || 'current'

const { publicKeyPem, privateKeyPem, hasLocalPrivateKey, generating, uploading, generate, loadLocal, uploadPublicKey } =
    useKeyPair()

const privatePreview = computed(() => {
  if (!privateKeyPem.value) return ''
  const s = privateKeyPem.value
  if (s.length <= 120) return s
  return `${s.slice(0, 80)}\n...\n${s.slice(-80)}`
})

onMounted(async () => {
  loadLocal(localKeyUsername)

  if (!privateKeyPem.value || !publicKeyPem.value) {
    try {
      await onGenerate(true)
      await onUpload(true)
    } catch {
    }
  }
})

async function onGenerate(silent = false) {
  if (!silent) {
    await ElMessageBox.confirm(
        '将生成新的RSA密钥对并覆盖本地保存的私钥。请确保已备份旧私钥。',
        '确认生成',
        { type: 'warning' },
    )
  }
  try {
    await generate(localKeyUsername)
    if (!silent) ElMessage.success('已生成密钥对（私钥已保存到本地）')
  } catch (e) {
    if (!silent) ElMessage.error(getErrorMessage(e))
    throw e
  }
}

async function onUpload(silent = false) {
  if (!publicKeyPem.value) {
    if (!silent) ElMessage.warning('请先生成密钥对')
    return
  }
  try {
    await uploadPublicKey(publicKeyPem.value)
    if (!silent) ElMessage.success('公钥上传成功')
  } catch (e) {
    if (!silent) ElMessage.error(getErrorMessage(e))
    throw e
  }
}

async function copy(text) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制')
  } catch {
    ElMessage.error('复制失败')
  }
}
</script>

<style scoped>
.key-manage-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  min-height: calc(100vh - 60px);
}

.key-manage-card {
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(0, 191, 255, 0.2);
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 0 30px rgba(0, 191, 255, 0.1);
}

.alert-warning {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 20px;
  border-radius: 8px;
  background: rgba(245, 158, 11, 0.1);
  border: 1px solid rgba(245, 158, 11, 0.3);
  color: #f59e0b;
  margin-bottom: 32px;
}

.alert-title {
  font-weight: 700;
  font-size: 16px;
  margin-bottom: 4px;
}

.alert-desc {
  font-size: 14px;
  opacity: 0.9;
}

.section {
  margin-bottom: 32px;
}

.action-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 24px;
}

.btn-primary, .btn-success, .btn-secondary {
  padding: 10px 24px;
  border-radius: 8px;
  font-size: 14px;
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

.btn-success {
  background: rgba(16, 185, 129, 0.2);
  color: #10b981;
  border: 1px solid rgba(16, 185, 129, 0.4);
}

.btn-success:hover:not(:disabled) {
  background: rgba(16, 185, 129, 0.3);
  box-shadow: 0 0 20px rgba(16, 185, 129, 0.3);
}

.btn-secondary {
  background: rgba(100, 116, 139, 0.2);
  color: #94a3b8;
  border: 1px solid rgba(100, 116, 139, 0.3);
}

.btn-secondary:hover:not(:disabled) {
  background: rgba(100, 116, 139, 0.3);
  border-color: rgba(100, 116, 139, 0.5);
}

.btn-primary:disabled, .btn-success:disabled, .btn-secondary:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.key-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 20px;
}

@media (min-width: 980px) {
  .key-grid {
    grid-template-columns: 1fr 1fr;
  }
}

.key-item {
  display: flex;
  flex-direction: column;
}

.key-label {
  color: #00bfff;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
  text-shadow: 0 0 10px rgba(0, 191, 255, 0.3);
}

.key-textarea {
  background: rgba(10, 14, 26, 0.8);
  border: 1px solid rgba(0, 191, 255, 0.3);
  border-radius: 8px;
  padding: 12px;
  color: #e2e8f0;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  resize: vertical;
  transition: all 0.3s;
}

.key-textarea:focus {
  outline: none;
  border-color: #00bfff;
  box-shadow: 0 0 15px rgba(0, 191, 255, 0.2);
}

.divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(0, 191, 255, 0.3), transparent);
  margin: 32px 0;
}

.result-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32px;
  border-radius: 8px;
  text-align: center;
}

.result-warning {
  background: rgba(245, 158, 11, 0.1);
  border: 1px solid rgba(245, 158, 11, 0.3);
  color: #f59e0b;
}

.result-success {
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.3);
  color: #10b981;
}

.result-title {
  font-size: 20px;
  font-weight: 700;
  margin: 16px 0 8px;
}

.result-subtitle {
  font-size: 14px;
  opacity: 0.8;
}
</style>
