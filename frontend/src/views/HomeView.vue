<template>
  <div class="home-container">
    <!-- 代码雨背景 -->
    <canvas ref="matrixCanvas" class="matrix-bg"></canvas>
    
    <!-- 主内容 -->
    <div class="content-wrapper">
      <!-- 主标题区域 -->
      <div class="hero-section">
        <h1 class="main-title">
          <span class="title-line">FILE SECURE</span>
          <span class="title-glow">加密文件保险箱</span>
        </h1>
        <p class="slogan">
          基于密钥认证的文件存储与审计系统 | 专注信息安全
        </p>
        <div class="tech-badge">
          <span class="badge-item">🔐 RSA-2048</span>
          <span class="badge-item">🛡️ AES-256-GCM</span>
          <span class="badge-item">📊 SHA-256</span>
        </div>
      </div>

      <!-- 功能速查卡片 -->
      <div class="quick-actions">
        <div class="action-card" @click="router.push('/file-upload')">
          <div class="card-icon">⬆️</div>
          <div class="card-title">上传</div>
          <div class="card-desc">加密上传文件</div>
          <div class="card-glow"></div>
        </div>

        <div class="action-card" @click="router.push('/file-download')">
          <div class="card-icon">⬇️</div>
          <div class="card-title">下载</div>
          <div class="card-desc">解密下载文件</div>
          <div class="card-glow"></div>
        </div>

        <div class="action-card" @click="router.push('/files')">
          <div class="card-icon">📁</div>
          <div class="card-title">管理</div>
          <div class="card-desc">文件管理中心</div>
          <div class="card-glow"></div>
        </div>

        <div class="action-card" @click="router.push('/audit')">
          <div class="card-icon">📊</div>
          <div class="card-title">审计</div>
          <div class="card-desc">安全审计日志</div>
          <div class="card-glow"></div>
        </div>
      </div>

      <!-- 系统状态 -->
      <div class="system-status">
        <div class="status-item">
          <span class="status-label">系统状态</span>
          <span class="status-value online">● ONLINE</span>
        </div>
        <div class="status-item">
          <span class="status-label">加密协议</span>
          <span class="status-value">RSA-OAEP + AES-GCM</span>
        </div>
        <div class="status-item">
          <span class="status-label">密钥状态</span>
          <span class="status-value" :class="hasKey ? 'online' : 'offline'">
            {{ hasKey ? '● 已配置' : '○ 未配置' }}
          </span>
        </div>
      </div>

      <!-- 首次使用提示 -->
      <div v-if="!hasKey" class="warning-banner">
        <div class="warning-icon">⚠️</div>
        <div class="warning-content">
          <div class="warning-title">首次使用提示</div>
          <div class="warning-text">
            请先前往【密钥管理】生成并上传 RSA 公钥，否则无法加密/解密文件
          </div>
        </div>
        <button class="warning-btn" @click="router.push('/key-manage')">
          立即配置 →
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { loadPrivateKeyLocal } from '../utils/crypto'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const matrixCanvas = ref(null)
const hasKey = ref(false)

let animationId = null

// 代码雨效果
const initMatrix = () => {
  const canvas = matrixCanvas.value
  if (!canvas) return

  const ctx = canvas.getContext('2d')
  canvas.width = window.innerWidth
  canvas.height = window.innerHeight

  const chars = '01アイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワヲン'
  const fontSize = 14
  const columns = canvas.width / fontSize
  const drops = Array(Math.floor(columns)).fill(1)

  const draw = () => {
    ctx.fillStyle = 'rgba(10, 14, 26, 0.05)'
    ctx.fillRect(0, 0, canvas.width, canvas.height)

    ctx.fillStyle = '#00bfff'
    ctx.font = `${fontSize}px monospace`

    for (let i = 0; i < drops.length; i++) {
      const text = chars[Math.floor(Math.random() * chars.length)]
      ctx.fillText(text, i * fontSize, drops[i] * fontSize)

      if (drops[i] * fontSize > canvas.height && Math.random() > 0.975) {
        drops[i] = 0
      }
      drops[i]++
    }
  }

  const animate = () => {
    draw()
    animationId = requestAnimationFrame(animate)
  }

  animate()
}

onMounted(() => {
  const username = auth.username || 'current'
  hasKey.value = !!loadPrivateKeyLocal(username)
  
  initMatrix()

  window.addEventListener('resize', () => {
    if (matrixCanvas.value) {
      matrixCanvas.value.width = window.innerWidth
      matrixCanvas.value.height = window.innerHeight
    }
  })
})

onUnmounted(() => {
  if (animationId) {
    cancelAnimationFrame(animationId)
  }
})
</script>

<style scoped>
.home-container {
  position: relative;
  min-height: calc(100vh - 64px);
  overflow: hidden;
}

/* 代码雨背景 */
.matrix-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  opacity: 0.3;
}

/* 内容包装器 */
.content-wrapper {
  position: relative;
  z-index: 1;
  max-width: 1200px;
  margin: 0 auto;
  padding: 60px 20px;
}

/* 主标题区域 */
.hero-section {
  text-align: center;
  margin-bottom: 60px;
}

.main-title {
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.title-line {
  font-size: 72px;
  font-weight: 900;
  color: #00bfff;
  text-shadow: 
    0 0 10px rgba(0, 191, 255, 0.8),
    0 0 20px rgba(0, 191, 255, 0.6),
    0 0 30px rgba(0, 191, 255, 0.4),
    0 0 40px rgba(0, 191, 255, 0.2);
  letter-spacing: 8px;
  font-family: 'Courier New', monospace;
  animation: glow 2s ease-in-out infinite alternate;
}

.title-glow {
  font-size: 32px;
  font-weight: 700;
  color: #00ff9d;
  text-shadow: 
    0 0 10px rgba(0, 255, 157, 0.8),
    0 0 20px rgba(0, 255, 157, 0.4);
  letter-spacing: 4px;
}

@keyframes glow {
  from {
    text-shadow: 
      0 0 10px rgba(0, 191, 255, 0.8),
      0 0 20px rgba(0, 191, 255, 0.6),
      0 0 30px rgba(0, 191, 255, 0.4);
  }
  to {
    text-shadow: 
      0 0 20px rgba(0, 191, 255, 1),
      0 0 30px rgba(0, 191, 255, 0.8),
      0 0 40px rgba(0, 191, 255, 0.6),
      0 0 50px rgba(0, 191, 255, 0.4);
  }
}

.slogan {
  font-size: 18px;
  color: rgba(0, 255, 157, 0.9);
  margin: 24px 0;
  letter-spacing: 2px;
  text-shadow: 0 0 10px rgba(0, 255, 157, 0.5);
}

.tech-badge {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 32px;
}

.badge-item {
  padding: 8px 20px;
  background: rgba(0, 191, 255, 0.1);
  border: 1px solid rgba(0, 191, 255, 0.3);
  border-radius: 20px;
  color: #00bfff;
  font-size: 14px;
  font-weight: 600;
  box-shadow: 0 0 15px rgba(0, 191, 255, 0.2);
  font-family: 'Courier New', monospace;
}

/* 功能速查卡片 */
.quick-actions {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
  margin-bottom: 48px;
}

.action-card {
  position: relative;
  padding: 32px 24px;
  background: linear-gradient(135deg, rgba(15, 20, 25, 0.9), rgba(10, 14, 26, 0.9));
  border: 1px solid rgba(0, 191, 255, 0.3);
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.3s;
  overflow: hidden;
  text-align: center;
}

.action-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, #00bfff, transparent);
  opacity: 0;
  transition: opacity 0.3s;
}

.action-card:hover {
  transform: translateY(-8px);
  border-color: rgba(0, 191, 255, 0.6);
  box-shadow: 
    0 0 30px rgba(0, 191, 255, 0.3),
    0 8px 32px rgba(0, 0, 0, 0.5);
}

.action-card:hover::before {
  opacity: 1;
}

.card-icon {
  font-size: 48px;
  margin-bottom: 16px;
  filter: drop-shadow(0 0 10px rgba(0, 191, 255, 0.5));
}

.card-title {
  font-size: 24px;
  font-weight: 700;
  color: #00bfff;
  margin-bottom: 8px;
  text-shadow: 0 0 10px rgba(0, 191, 255, 0.5);
}

.card-desc {
  font-size: 14px;
  color: rgba(0, 255, 157, 0.7);
}

.card-glow {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, transparent, #00bfff, transparent);
  opacity: 0;
  transition: opacity 0.3s;
}

.action-card:hover .card-glow {
  opacity: 1;
}

/* 系统状态 */
.system-status {
  display: flex;
  justify-content: space-around;
  padding: 24px;
  background: rgba(15, 20, 25, 0.6);
  border: 1px solid rgba(0, 191, 255, 0.2);
  border-radius: 12px;
  margin-bottom: 32px;
}

.status-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  text-align: center;
}

.status-label {
  font-size: 12px;
  color: rgba(0, 255, 157, 0.6);
  text-transform: uppercase;
  letter-spacing: 1px;
}

.status-value {
  font-size: 16px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.9);
  font-family: 'Courier New', monospace;
}

.status-value.online {
  color: #00bfff;
  text-shadow: 0 0 10px rgba(0, 191, 255, 0.5);
}

.status-value.offline {
  color: #ff6b6b;
}

/* 警告横幅 */
.warning-banner {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 24px;
  background: linear-gradient(135deg, rgba(255, 107, 107, 0.1), rgba(255, 59, 92, 0.1));
  border: 1px solid rgba(255, 107, 107, 0.3);
  border-radius: 12px;
  box-shadow: 0 0 20px rgba(255, 107, 107, 0.2);
}

.warning-icon {
  font-size: 32px;
  filter: drop-shadow(0 0 10px rgba(255, 107, 107, 0.5));
}

.warning-content {
  flex: 1;
}

.warning-title {
  font-size: 16px;
  font-weight: 700;
  color: #ff6b6b;
  margin-bottom: 8px;
}

.warning-text {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

.warning-btn {
  padding: 12px 24px;
  background: linear-gradient(135deg, rgba(255, 107, 107, 0.3), rgba(255, 59, 92, 0.3));
  border: 1px solid rgba(255, 107, 107, 0.5);
  border-radius: 8px;
  color: #ff6b6b;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.warning-btn:hover {
  background: linear-gradient(135deg, rgba(255, 107, 107, 0.4), rgba(255, 59, 92, 0.4));
  box-shadow: 0 0 20px rgba(255, 107, 107, 0.4);
  transform: translateY(-2px);
}

/* 响应式 */
@media (max-width: 1024px) {
  .quick-actions {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .title-line {
    font-size: 48px;
  }
}

@media (max-width: 768px) {
  .quick-actions {
    grid-template-columns: 1fr;
  }
  
  .title-line {
    font-size: 36px;
  }
  
  .system-status {
    flex-direction: column;
    gap: 16px;
  }
}
</style>