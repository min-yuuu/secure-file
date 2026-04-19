<template>
  <el-container class="app-shell">
    <el-aside class="aside" width="240px">
      <div class="brand">
        <div class="logo">
          <span class="logo-icon">🔐</span>
        </div>
        <div class="brand-text">
          <div class="name">FILE SECURE</div>
          <div class="tagline">加密保险箱</div>
        </div>
      </div>

      <el-menu :default-active="active" router class="menu">
        <el-menu-item index="/home">
          <span class="menu-icon">🏠</span>
          <span>首页</span>
        </el-menu-item>
        <el-menu-item index="/file-upload">
          <span class="menu-icon">⬆️</span>
          <span>文件上传</span>
        </el-menu-item>
        <el-menu-item index="/files">
          <span class="menu-icon">📁</span>
          <span>文件管理</span>
        </el-menu-item>
        <el-menu-item index="/file-download">
          <span class="menu-icon">⬇️</span>
          <span>文件下载</span>
        </el-menu-item>
        <el-menu-item index="/audit">
          <span class="menu-icon">📊</span>
          <span>审计日志</span>
        </el-menu-item>
        <el-menu-item index="/key-manage">
          <span class="menu-icon">🔑</span>
          <span>密钥管理</span>
        </el-menu-item>
        <el-menu-item index="/profile">
          <span class="menu-icon">👤</span>
          <span>个人中心</span>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-footer">
        <div class="status-indicator">
          <span class="status-dot"></span>
          <span class="status-text">系统运行中</span>
        </div>
      </div>
    </el-aside>

    <el-container>
      <el-header class="header" height="64px">
        <div class="header-left">
          <div class="page-title">{{ pageTitle }}</div>
          <div class="page-subtitle">{{ pageSubtitle }}</div>
        </div>
        <div class="header-right">
          <div class="user-info">
            <span class="user-icon">👨‍💻</span>
            <span class="username">{{ auth.username }}</span>
          </div>
          <el-button class="logout-btn" @click="onLogout">
            <span>退出</span>
          </el-button>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
    <DownloadManager />
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuth } from '../hooks/useAuth'
import DownloadManager from '../components/DownloadManager.vue'

const route = useRoute()
const router = useRouter()
const { auth, logout } = useAuth()

const active = computed(() => route.path)

const pageTitle = computed(() => {
  switch (route.path) {
    case '/home': return '控制中心'
    case '/file-upload': return '文件上传'
    case '/files': return '文件管理'
    case '/file-download': return '文件下载'
    case '/key-manage': return '密钥管理'
    case '/audit': return '审计日志'
    case '/profile': return '个人中心'
    default: return 'FILE SECURE'
  }
})

const pageSubtitle = computed(() => {
  switch (route.path) {
    case '/home': return 'Dashboard'
    case '/file-upload': return 'Upload Encrypted Files'
    case '/files': return 'Manage Your Files'
    case '/file-download': return 'Download & Decrypt'
    case '/key-manage': return 'RSA Key Management'
    case '/audit': return 'Security Audit Logs'
    case '/profile': return 'User Profile'
    default: return 'Secure File Transfer System'
  }
})

function onLogout() {
  logout()
  ElMessage.success('已安全退出')
  router.push('/login')
}
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: #0a0e1a;
}

/* 侧边栏 */
.aside {
  background: linear-gradient(180deg, #0f1419 0%, #0a0e1a 100%);
  border-right: 1px solid rgba(0, 191, 255, 0.1);
  box-shadow: 4px 0 20px rgba(0, 191, 255, 0.05);
  position: relative;
  overflow: hidden;
}

.aside::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(0, 191, 255, 0.5), transparent);
}

/* 品牌区域 */
.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 24px 20px;
  border-bottom: 1px solid rgba(0, 191, 255, 0.1);
}

.logo {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, rgba(0, 191, 255, 0.2), rgba(0, 255, 157, 0.2));
  border: 1px solid rgba(0, 191, 255, 0.3);
  box-shadow: 0 0 20px rgba(0, 191, 255, 0.3), inset 0 0 20px rgba(0, 191, 255, 0.1);
}

.logo-icon {
  font-size: 24px;
  filter: drop-shadow(0 0 8px rgba(0, 191, 255, 0.8));
}

.brand-text {
  flex: 1;
}

.name {
  font-weight: 800;
  font-size: 16px;
  color: #00bfff;
  letter-spacing: 2px;
  text-shadow: 0 0 10px rgba(0, 191, 255, 0.5);
  font-family: 'Courier New', monospace;
}

.tagline {
  font-size: 11px;
  color: rgba(0, 191, 255, 0.6);
  margin-top: 2px;
  letter-spacing: 1px;
}

/* 菜单 */
.menu {
  border-right: none;
  background: transparent;
  padding: 12px 8px;
}

.menu :deep(.el-menu-item) {
  margin: 4px 0;
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.7);
  transition: all 0.3s;
  border: 1px solid transparent;
  display: flex;
  align-items: center;
  gap: 12px;
}

.menu :deep(.el-menu-item:hover) {
  background: rgba(0, 191, 255, 0.1);
  color: #00bfff;
  border-color: rgba(0, 191, 255, 0.3);
  box-shadow: 0 0 15px rgba(0, 191, 255, 0.2);
}

.menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(0, 191, 255, 0.2), rgba(0, 255, 157, 0.1));
  color: #00bfff;
  border-color: rgba(0, 191, 255, 0.5);
  box-shadow: 0 0 20px rgba(0, 191, 255, 0.3);
  font-weight: 600;
}

.menu-icon {
  font-size: 18px;
  filter: drop-shadow(0 0 4px currentColor);
}

/* 侧边栏底部 */
.sidebar-footer {
  position: absolute;
  bottom: 20px;
  left: 20px;
  right: 20px;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: rgba(0, 191, 255, 0.05);
  border: 1px solid rgba(0, 191, 255, 0.2);
  border-radius: 8px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #00bfff;
  box-shadow: 0 0 10px rgba(0, 191, 255, 0.8);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.status-text {
  font-size: 12px;
  color: rgba(0, 191, 255, 0.8);
}

/* 头部 */
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(90deg, #0f1419 0%, #0a0e1a 100%);
  border-bottom: 1px solid rgba(0, 191, 255, 0.2);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5);
  padding: 0 32px;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-title {
  font-weight: 800;
  font-size: 24px;
  color: #00bfff;
  text-shadow: 0 0 15px rgba(0, 191, 255, 0.6);
  letter-spacing: 1px;
}

.page-subtitle {
  font-size: 12px;
  color: rgba(0, 255, 157, 0.7);
  letter-spacing: 2px;
  text-transform: uppercase;
  font-family: 'Courier New', monospace;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: rgba(0, 191, 255, 0.1);
  border: 1px solid rgba(0, 191, 255, 0.3);
  border-radius: 20px;
}

.user-icon {
  font-size: 18px;
}

.username {
  color: #00bfff;
  font-weight: 600;
  font-size: 14px;
}

.logout-btn {
  background: linear-gradient(135deg, rgba(255, 59, 92, 0.2), rgba(255, 107, 107, 0.2));
  border: 1px solid rgba(255, 59, 92, 0.5);
  color: #ff6b6b;
  font-weight: 600;
  transition: all 0.3s;
}

.logout-btn:hover {
  background: linear-gradient(135deg, rgba(255, 59, 92, 0.3), rgba(255, 107, 107, 0.3));
  box-shadow: 0 0 20px rgba(255, 59, 92, 0.4);
  transform: translateY(-2px);
}

/* 主内容区 */
.main {
  background: #0a0e1a;
  padding: 24px;
  min-height: calc(100vh - 64px);
}
</style>