import { createRouter, createWebHistory } from 'vue-router'

import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import MainLayout from '../layouts/MainLayout.vue'
import HomeView from '../views/HomeView.vue'
import FileDownloadView from '../views/FileDownloadView.vue'
import MyFilesView from '../views/MyFilesView.vue'
import AuditView from '../views/AuditView.vue'
// 取消文件上传/列表的导入注释
import FileListView from '../views/FileListView.vue'
import FileUploadView from '../views/FileUploadView.vue'
import KeyManageView from '../views/KeyManageView.vue'
// 暂时不需要的保持注释
// import AuditView from '../views/AuditView.vue'
import ProfileView from '../views/ProfileView.vue'

const routes = [
    { path: '/login', component: LoginView },
    { path: '/register', component: RegisterView },
    {
        path: '/',
        component: MainLayout,
        children: [
            { path: '', component: HomeView }, // 根路径 / 显示首页
            { path: 'home', component: HomeView }, // /home 显示首页
            { path: 'files', component: FileListView },      // 上传管理列表
            { path: 'file-upload', component: FileUploadView }, // 文件上传页面
            { path: 'file-download', component: FileDownloadView }, // 文件下载页面（确保这一行存在）
            { path: 'key-manage', component: KeyManageView }, // 密钥管理
            { path: 'audit', component: AuditView }, // 审计日志
            { path: 'my-files', component: MyFilesView},
            { path: 'profile', component: ProfileView }
        ]
    }
]

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes
})

export default router