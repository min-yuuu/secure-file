<template>
  <div class="wrap">
    <!-- 背景动画 -->
    <div class="bg-animation"></div>
    
    <el-card class="card" shadow="never">
      <template #header>
        <div class="header">
          <div class="title-section">
            <div class="logo">🔐</div>
            <div>
              <div class="title">FILE SECURE</div>
              <div class="subtitle">加密文件保险箱</div>
            </div>
          </div>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
        <el-form-item label="用户名" prop="username">
          <el-input 
            v-model="form.username" 
            autocomplete="username" 
            clearable 
            placeholder="请输入用户名"
            size="large"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input 
            v-model="form.password" 
            type="password" 
            autocomplete="current-password" 
            show-password 
            placeholder="请输入密码"
            size="large"
          />
        </el-form-item>

        <el-form-item>
          <el-checkbox v-model="remember">记住登录状态</el-checkbox>
        </el-form-item>

        <el-form-item>
          <el-button 
            type="primary" 
            style="width: 100%" 
            :loading="loading" 
            @click="onSubmit"
            size="large"
            class="login-btn"
          >
            登录系统
          </el-button>
        </el-form-item>

        <div class="footer-links">
          <el-button link @click="router.push('/register')" class="register-link">
            还没有账号？立即注册 →
          </el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuth } from '../hooks/useAuth'
import { getErrorMessage } from '../utils/request'

const router = useRouter()
const route = useRoute()
const { loading, login, auth } = useAuth()

const formRef = ref()
const form = reactive({
  username: '',
  password: '',
})

const remember = ref(auth.remember)
watch(remember, (v) => auth.setRemember(v))

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function onSubmit() {
  await formRef.value?.validate(async (ok) => {
    if (!ok) return
    try {
      await login({ username: form.username.trim(), password: form.password, remember: remember.value })
      ElMessage.success('登录成功')
      const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/home'
      router.replace(redirect)
    } catch (e) {
      ElMessage.error(getErrorMessage(e))
    }
  })
}
</script>

<style scoped>
.wrap {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background: #0a0e1a;
  position: relative;
  overflow: hidden;
}

/* 背景动画 */
.bg-animation {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    radial-gradient(circle at 20% 30%, rgba(0, 191, 255, 0.1) 0%, transparent 50%),
    radial-gradient(circle at 80% 70%, rgba(0, 255, 157, 0.1) 0%, transparent 50%);
  animation: pulse 8s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 0.5; }
  50% { opacity: 1; }
}

.card {
  width: min(460px, 100%);
  background: linear-gradient(135deg, rgba(15, 20, 25, 0.95), rgba(10, 14, 26, 0.95));
  border: 1px solid rgba(0, 191, 255, 0.3);
  box-shadow: 
    0 0 40px rgba(0, 191, 255, 0.2),
    0 20px 60px rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(10px);
  position: relative;
  z-index: 1;
}

.card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, #00bfff, transparent);
}

.card :deep(.el-card__header) {
  background: transparent;
  border-bottom: 1px solid rgba(0, 191, 255, 0.2);
  padding: 24px;
}

.card :deep(.el-card__body) {
  padding: 32px 24px;
}

.header {
  display: flex;
  justify-content: center;
}

.title-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.logo {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, rgba(0, 191, 255, 0.2), rgba(0, 255, 157, 0.2));
  border: 1px solid rgba(0, 191, 255, 0.3);
  box-shadow: 0 0 20px rgba(0, 191, 255, 0.3);
  font-size: 28px;
}

.title {
  font-weight: 800;
  font-size: 24px;
  color: #00bfff;
  letter-spacing: 2px;
  text-shadow: 0 0 10px rgba(0, 191, 255, 0.5);
  font-family: 'Courier New', monospace;
}

.subtitle {
  font-size: 12px;
  color: rgba(0, 255, 157, 0.7);
  margin-top: 4px;
  letter-spacing: 1px;
}

/* 表单样式 */
.card :deep(.el-form-item__label) {
  color: rgba(0, 191, 255, 0.8);
  font-weight: 600;
}

.card :deep(.el-input__wrapper) {
  background: rgba(15, 20, 25, 0.6);
  border: 1px solid rgba(0, 191, 255, 0.3);
  box-shadow: none;
  transition: all 0.3s;
}

.card :deep(.el-input__wrapper:hover) {
  border-color: rgba(0, 191, 255, 0.5);
}

.card :deep(.el-input__wrapper.is-focus) {
  border-color: #00bfff;
  box-shadow: 0 0 15px rgba(0, 191, 255, 0.3);
}

.card :deep(.el-input__inner) {
  color: rgba(255, 255, 255, 0.9);
}

.card :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.3);
}

.card :deep(.el-checkbox__label) {
  color: rgba(255, 255, 255, 0.7);
}

.card :deep(.el-checkbox__inner) {
  background: rgba(15, 20, 25, 0.6);
  border-color: rgba(0, 191, 255, 0.3);
}

.card :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background: #00bfff;
  border-color: #00bfff;
}

.login-btn {
  background: linear-gradient(135deg, rgba(0, 191, 255, 0.3), rgba(0, 255, 157, 0.3));
  border: 1px solid rgba(0, 191, 255, 0.5);
  color: #00bfff;
  font-weight: 700;
  font-size: 16px;
  letter-spacing: 2px;
  transition: all 0.3s;
}

.login-btn:hover {
  background: linear-gradient(135deg, rgba(0, 191, 255, 0.4), rgba(0, 255, 157, 0.4));
  box-shadow: 0 0 30px rgba(0, 191, 255, 0.4);
  transform: translateY(-2px);
}

.footer-links {
  text-align: center;
  margin-top: 16px;
}

.register-link {
  color: rgba(0, 255, 157, 0.8);
  font-size: 14px;
  transition: all 0.3s;
}

.register-link:hover {
  color: #00ff9d;
  text-shadow: 0 0 10px rgba(0, 255, 157, 0.5);
}
</style>
