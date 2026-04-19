import { createApp } from 'vue'
import { createPinia } from 'pinia' // 状态管理
import ElementPlus from 'element-plus' // UI 组件库
import 'element-plus/dist/index.css' // Element Plus 样式
import * as ElementPlusIconsVue from '@element-plus/icons-vue' // Element Plus 图标

import App from './App.vue'
import router from './router' // 路由配置
import { useAuthStore } from './stores/auth'
import { initPublicIp } from './utils/request'

const app = createApp(App)

const pinia = createPinia()
app.use(pinia) // 挂载 Pinia 状态管理

// 初始化认证状态（从本地存储恢复）
const auth = useAuthStore()
auth.initFromStorage()

// 初始化客户端公网 IP（用于审计日志）
initPublicIp()

app.use(router) // 挂载路由
app.use(ElementPlus) // 挂载 Element Plus

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

app.mount('#app')
