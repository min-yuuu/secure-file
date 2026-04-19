// src/utils/request.ts
import axios from 'axios'
import { useAuthStore } from '../stores/auth'

// 缓存客户端公网 IP
let cachedPublicIp = ''

/**
 * 初始化获取客户端公网 IP
 * 用于后端审计日志记录真实客户端 IP
 */
export async function initPublicIp() {
    try {
        const r = await fetch('https://api.ipify.org?format=json')
        const j = await r.json()
        if (typeof j?.ip === 'string') {
            cachedPublicIp = j.ip
            console.log('客户端公网 IP:', cachedPublicIp)
        }
    } catch (error) {
        console.warn('获取公网 IP 失败:', error)
    }
}

export const http = axios.create({
    baseURL: '',
    timeout: 20000,
})

// 请求拦截器 - 添加认证信息和客户端 IP
http.interceptors.request.use((config) => {
    console.log('=== 请求拦截器开始 ===');
    console.log('请求URL:', config.url);
    console.log('请求方法:', config.method);

    // 直接从 sessionStorage 获取 token
    const sessionToken = sessionStorage.getItem('securefile.jwt');
    console.log('sessionStorage token:', sessionToken);

    const localToken = localStorage.getItem('securefile.jwt');
    console.log('localStorage token:', localToken);

    // 优先使用 sessionStorage 的 token
    const token = sessionToken || localToken;

    if (token) {
        config.headers = config.headers || {}
        config.headers.Authorization = `Bearer ${token}`
        console.log('设置 Authorization:', `Bearer ${token.substring(0, 20)}...`);
    } else {
        console.log('❌ 没有找到 token！');
        // 尝试从 auth store 获取
        try {
            const auth = useAuthStore()
            console.log('auth store token:', auth.token);
            if (auth.token) {
                config.headers.Authorization = `Bearer ${auth.token}`;
            }
        } catch (e) {
            console.log('获取 auth store 失败:', e);
        }
    }

    // 添加客户端公网 IP 到请求头（用于审计日志）
    if (cachedPublicIp) {
        config.headers = config.headers || {}
        config.headers['X-Client-Public-IP'] = cachedPublicIp
        console.log('设置客户端公网 IP:', cachedPublicIp);
    }

    console.log('最终请求头:', config.headers);
    console.log('=== 请求拦截器结束 ===');
    return config
})

// 响应拦截器
http.interceptors.response.use(
    (resp) => {
        console.log('响应成功:', resp.config.url, resp.status);
        return resp;
    },
    (err) => {
        console.error('响应错误:', err.response?.status, err.config?.url);
        console.error('错误详情:', err.response?.data);

        if (err.response?.status === 401) {
            const auth = useAuthStore()
            auth.clear()
        }
        return Promise.reject(err)
    },
)

export function getErrorMessage(e) {
    if (axios.isAxiosError(e)) {
        if (e.response?.status === 401) return '未登录或登录已过期，请重新登录'
        const data = e.response?.data
        if (data && typeof data.message === 'string' && data.message) return data.message
        if (typeof e.message === 'string' && e.message) return e.message
        return '网络错误'
    }
    if (e instanceof Error) return e.message
    return '未知错误'
}

export const request = http
export default http
