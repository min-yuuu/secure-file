<template>
  <div class="wrap">
    <div class="card">
      <div class="header">
        <h2 class="title">注册</h2>
        <button @click="router.push('/login')" class="link-btn">去登录</button>
      </div>

      <form @submit.prevent="onSubmit" class="register-form">
        <div class="form-item">
          <label>用户名 *</label>
          <input 
            v-model="form.username" 
            autocomplete="username" 
            placeholder="请输入用户名"
            class="input"
            required
          />
        </div>

        <div class="form-item">
          <label>密码 *</label>
          <input 
            v-model="form.password" 
            type="password" 
            autocomplete="new-password" 
            placeholder="请输入密码"
            class="input"
            required
          />
        </div>

        <div class="strength">
          <div class="strength-row">
            <span class="label">强度</span>
            <span :class="['strength-tag', strengthTagClass]">{{ strengthLabel }}</span>
          </div>
          <div class="progress-bar">
            <div 
              class="progress-fill" 
              :class="strengthStatusClass"
              :style="{ width: strengthPercent + '%' }"
            ></div>
          </div>
        </div>

        <div class="form-item">
          <label>确认密码 *</label>
          <input 
            v-model="form.confirmPassword" 
            type="password" 
            autocomplete="new-password" 
            placeholder="请再次输入密码"
            class="input"
            required
          />
        </div>

        <button 
          type="submit"
          :disabled="loading || strengthPercent < 60"
          class="btn-submit"
        >
          {{ loading ? '注册中...' : '注册并登录' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuth } from '../hooks/useAuth'
import { getErrorMessage } from '../utils/request'

const router = useRouter()
const { loading, register, login } = useAuth()

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
})

function calcStrength(pw) {
  let score = 0
  if (pw.length >= 8) score += 1
  if (/[A-Z]/.test(pw)) score += 1
  if (/[a-z]/.test(pw)) score += 1
  if (/[0-9]/.test(pw)) score += 1
  if (/[^A-Za-z0-9]/.test(pw)) score += 1
  if (pw.length >= 12) score += 1
  return score
}

const strengthScore = computed(() => calcStrength(form.password))
const strengthPercent = computed(() => Math.min(100, Math.round((strengthScore.value / 6) * 100)))
const strengthLabel = computed(() => {
  if (strengthPercent.value < 40) return '弱'
  if (strengthPercent.value < 70) return '中'
  return '强'
})
const strengthTagClass = computed(() => {
  if (strengthPercent.value < 40) return 'tag-danger'
  if (strengthPercent.value < 70) return 'tag-warning'
  return 'tag-success'
})
const strengthStatusClass = computed(() => {
  if (strengthPercent.value < 40) return 'fill-danger'
  if (strengthPercent.value < 70) return 'fill-warning'
  return 'fill-success'
})

async function onSubmit() {
  if (!form.username.trim()) {
    ElMessage.warning('请输入用户名')
    return
  }
  if (strengthPercent.value < 60) {
    ElMessage.warning('密码强度过低')
    return
  }
  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  
  try {
    await register({ username: form.username.trim(), password: form.password })
    await login({ username: form.username.trim(), password: form.password })
    ElMessage.success('注册成功，已登录')
    router.replace('/key-manage')
  } catch (e) {
    ElMessage.error(getErrorMessage(e))
  }
}
</script>

<style scoped>
.wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: #0a0e1a;
  position: relative;
  overflow: hidden;
}

.wrap::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: 
    radial-gradient(circle at 30% 20%, rgba(0, 191, 255, 0.15), transparent 40%),
    radial-gradient(circle at 80% 70%, rgba(0, 255, 157, 0.12), transparent 40%);
  animation: rotate 20s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.card {
  width: min(480px, 100%);
  background: rgba(15, 23, 42, 0.8);
  border: 1px solid rgba(0, 191, 255, 0.3);
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 0 40px rgba(0, 191, 255, 0.2);
  position: relative;
  z-index: 1;
  backdrop-filter: blur(10px);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}

.title {
  font-size: 32px;
  font-weight: 700;
  color: #00bfff;
  text-shadow: 0 0 20px rgba(0, 191, 255, 0.5);
  margin: 0;
}

.link-btn {
  background: transparent;
  border: none;
  color: #00ff9d;
  font-size: 14px;
  cursor: pointer;
  text-decoration: underline;
}

.link-btn:hover {
  color: #00bfff;
}

.register-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-item {
  display: flex;
  flex-direction: column;
}

.form-item label {
  color: #00bfff;
  font-size: 14px;
  margin-bottom: 8px;
  text-shadow: 0 0 10px rgba(0, 191, 255, 0.3);
}

.input {
  background: rgba(10, 14, 26, 0.8);
  border: 1px solid rgba(0, 191, 255, 0.3);
  border-radius: 8px;
  padding: 12px 16px;
  color: #e2e8f0;
  font-size: 14px;
  transition: all 0.3s;
}

.input:focus {
  outline: none;
  border-color: #00bfff;
  box-shadow: 0 0 15px rgba(0, 191, 255, 0.2);
}

.strength {
  padding: 16px;
  border: 1px solid rgba(0, 191, 255, 0.2);
  border-radius: 8px;
  background: rgba(10, 14, 26, 0.6);
}

.strength-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.label {
  color: #94a3b8;
  font-size: 14px;
}

.strength-tag {
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
}

.tag-danger {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
  border: 1px solid rgba(239, 68, 68, 0.4);
}

.tag-warning {
  background: rgba(245, 158, 11, 0.2);
  color: #f59e0b;
  border: 1px solid rgba(245, 158, 11, 0.4);
}

.tag-success {
  background: rgba(16, 185, 129, 0.2);
  color: #10b981;
  border: 1px solid rgba(16, 185, 129, 0.4);
}

.progress-bar {
  height: 8px;
  background: rgba(10, 14, 26, 0.8);
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  transition: width 0.3s, background 0.3s;
}

.fill-danger {
  background: linear-gradient(90deg, #ef4444, #dc2626);
}

.fill-warning {
  background: linear-gradient(90deg, #f59e0b, #d97706);
}

.fill-success {
  background: linear-gradient(90deg, #10b981, #059669);
}

.btn-submit {
  width: 100%;
  padding: 14px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  border: none;
  background: linear-gradient(135deg, #00bfff, #00ff9d);
  color: #0a0e1a;
  box-shadow: 0 0 20px rgba(0, 191, 255, 0.3);
  margin-top: 8px;
}

.btn-submit:hover:not(:disabled) {
  box-shadow: 0 0 30px rgba(0, 191, 255, 0.5);
  transform: translateY(-2px);
}

.btn-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
