import { defineStore } from 'pinia'

// 存储键名常量（统一管理）
const TOKEN_KEY = 'securefile.jwt'
const REMEMBER_KEY = 'securefile.remember'
const USERNAME_KEY = 'securefile.username'

export const useAuthStore = defineStore('auth', {
    state: () => ({
        token: '',       // 登录token
        remember: false, // 是否记住登录状态
        username: '',    // 用户名
    }),
    getters: {
        // 判断是否已认证（有有效token）
        isAuthed: (s) => !!s.token,
    },
    actions: {
        /**
         * 从本地存储初始化认证状态（页面刷新/首次加载时调用）
         * 核心：根据remember状态，从localStorage/sessionStorage恢复token和用户名
         */
        initFromStorage() {
            // 恢复"记住我"状态
            const remember = localStorage.getItem(REMEMBER_KEY)
            this.remember = remember === '1'

            // 选择存储介质（记住我→localStorage，否则→sessionStorage）
            const storage = this.remember ? localStorage : sessionStorage

            // 恢复token和用户名
            this.token = storage.getItem(TOKEN_KEY) || ''
            this.username = storage.getItem(USERNAME_KEY) || ''
        },

        /**
         * 设置"记住我"状态，并同步迁移token存储位置
         * @param {boolean} remember - 是否记住登录
         */
        setRemember(remember) {
            this.remember = remember
            localStorage.setItem(REMEMBER_KEY, remember ? '1' : '0')

            // 先清空所有存储的token和用户名
            const token = this.token
            const username = this.username
            localStorage.removeItem(TOKEN_KEY)
            localStorage.removeItem(USERNAME_KEY)
            sessionStorage.removeItem(TOKEN_KEY)
            sessionStorage.removeItem(USERNAME_KEY)

            // 重新存储（根据新的remember状态选择介质）
            if (token) {
                const storage = remember ? localStorage : sessionStorage
                storage.setItem(TOKEN_KEY, token)
                if (username) storage.setItem(USERNAME_KEY, username)
            }
        },

        /**
         * 设置用户名并持久化
         * @param {string} username - 用户名
         */
        setUser(username) {
            this.username = username
            const storage = this.remember ? localStorage : sessionStorage
            if (username) storage.setItem(USERNAME_KEY, username)
            else storage.removeItem(USERNAME_KEY)
        },

        /**
         * 设置token并持久化
         * @param {string} token - 登录token
         */
        setToken(token) {
            this.token = token
            const storage = this.remember ? localStorage : sessionStorage
            storage.setItem(TOKEN_KEY, token)
        },

        /**
         * 清空所有认证状态（登出/401时调用）
         */
        clear() {
            // 清空内存状态
            this.token = ''
            this.username = ''
            // 清空本地存储
            localStorage.removeItem(TOKEN_KEY)
            localStorage.removeItem(USERNAME_KEY)
            sessionStorage.removeItem(TOKEN_KEY)
            sessionStorage.removeItem(USERNAME_KEY)
        },

        /**
         * 登录成功后统一设置认证信息（简化调用）
         * @param {string} token - 登录token
         * @param {string} username - 用户名
         * @param {boolean} remember - 是否记住登录
         */
        loginSuccess(token, username, remember = false) {
            this.setRemember(remember) // 先设置记住状态
            this.setToken(token)       // 再存储token
            this.setUser(username)     // 最后存储用户名
        }
    },
})