<template>
  <div class="profile-container">
    <div class="profile-card" v-loading="loading">
      <h2 class="title">个人中心</h2>
      
      <div class="info-grid">
        <div class="info-section">
          <div class="section-title">用户信息</div>
          <div class="info-table">
            <div class="info-row">
              <span class="info-label">用户名</span>
              <span class="info-value">{{ profile?.username ?? auth.username ?? '-' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">登录状态</span>
              <span :class="['info-value', auth.isAuthed ? 'status-success' : 'status-warning']">
                {{ auth.isAuthed ? '已登录' : '未登录' }}
              </span>
            </div>
            <div class="info-row">
              <span class="info-label">注册时间</span>
              <span class="info-value">{{ formatDate(profile?.createTime) }}</span>
            </div>
          </div>
        </div>

        <div class="info-section">
          <div class="section-title">密钥状态</div>
          <div class="info-table">
            <div class="info-row">
              <span class="info-label">本地私钥</span>
              <span :class="['status-tag', hasLocalPrivateKey ? 'tag-success' : 'tag-warning']">
                {{ hasLocalPrivateKey ? '已保存' : '未检测到' }}
              </span>
            </div>
            <div class="info-row">
              <span class="info-label">公钥上传</span>
              <span v-if="profile" :class="['status-tag', profile.hasPublicKey ? 'tag-success' : 'tag-danger']">
                {{ profile.hasPublicKey ? '已上传' : '未上传' }}
              </span>
              <span v-else class="info-value">-</span>
            </div>
          </div>
        </div>
      </div>

      <div class="divider"></div>

      <div class="action-section">
        <button @click="regenerate" :disabled="generating" class="btn-warning">
          {{ generating ? '生成中...' : '重新生成密钥对（需确认）' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuth } from '../hooks/useAuth'
import { useKeyPair } from '../hooks/useKeyPair'
import { http, getErrorMessage } from '../utils/request'

const router = useRouter()
const { auth } = useAuth()
const { generating, generate, privateKeyPem, loadLocal } = useKeyPair()

const hasLocalPrivateKey = computed(() => !!privateKeyPem.value)

const loading = ref(false)
const profile = ref<any>(null)

onMounted(async () => {
  if (auth.username) {
    loadLocal(auth.username)
  }

  loading.value = true
  try {
    const resp = await http.get('/api/v1/auth/me')
    if (resp.data.code === 200) {
      profile.value = resp.data.data
    }
  } catch {
  } finally {
    loading.value = false
  }
})

function formatDate(s: string) {
  if (!s) return '-'
  const str = String(s)
  if (str.includes('T')) return str.replace('T', ' ').substring(0, 19)
  return str.substring(0, 19)
}

async function regenerate() {
  try {
    await ElMessageBox.confirm('将生成新的RSA密钥对并覆盖本地私钥，旧私钥若未备份将无法恢复。是否继续？', '确认操作', {
      type: 'warning',
      confirmButtonText: '继续',
    })
    await generate(auth.username || 'current')
    ElMessage.success('已重新生成（私钥已保存到本地）')
    router.push('/key-manage')
  } catch (e: any) {
    if (typeof e === 'string' && (e === 'cancel' || e === 'close')) return
    ElMessage.error(getErrorMessage(e))
  }
}
</script>

<style scoped>
.profile-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px;
  min-height: calc(100vh - 60px);
}

.profile-card {
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(0, 191, 255, 0.2);
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 0 30px rgba(0, 191, 255, 0.1);
}

.title {
  font-size: 28px;
  font-weight: 700;
  color: #00bfff;
  text-shadow: 0 0 20px rgba(0, 191, 255, 0.5);
  margin: 0 0 32px 0;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
  margin-bottom: 32px;
}

@media (min-width: 980px) {
  .info-grid {
    grid-template-columns: 1fr 1fr;
  }
}

.info-section {
  background: rgba(10, 14, 26, 0.6);
  border: 1px solid rgba(0, 191, 255, 0.2);
  border-radius: 8px;
  padding: 20px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #00ff9d;
  text-shadow: 0 0 15px rgba(0, 255, 157, 0.4);
  margin-bottom: 16px;
}

.info-table {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid rgba(100, 116, 139, 0.2);
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  color: #94a3b8;
  font-size: 14px;
}

.info-value {
  color: #e2e8f0;
  font-size: 14px;
  font-weight: 500;
}

.status-success {
  color: #10b981;
}

.status-warning {
  color: #f59e0b;
}

.status-tag {
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
}

.tag-success {
  background: rgba(16, 185, 129, 0.2);
  color: #10b981;
  border: 1px solid rgba(16, 185, 129, 0.4);
}

.tag-warning {
  background: rgba(245, 158, 11, 0.2);
  color: #f59e0b;
  border: 1px solid rgba(245, 158, 11, 0.4);
}

.tag-danger {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
  border: 1px solid rgba(239, 68, 68, 0.4);
}

.divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(0, 191, 255, 0.3), transparent);
  margin: 32px 0;
}

.action-section {
  display: flex;
  justify-content: center;
}

.btn-warning {
  padding: 12px 32px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  border: none;
  background: rgba(245, 158, 11, 0.2);
  color: #f59e0b;
  border: 1px solid rgba(245, 158, 11, 0.4);
}

.btn-warning:hover:not(:disabled) {
  background: rgba(245, 158, 11, 0.3);
  box-shadow: 0 0 20px rgba(245, 158, 11, 0.3);
  transform: translateY(-2px);
}

.btn-warning:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
</style>
