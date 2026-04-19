import { computed, ref } from 'vue'
import { http } from '../utils/request'
import { useAuthStore } from '../stores/auth'
import { isMockMode, isMockToken, mockToken } from '../utils/mock'


export function useAuth() {
    const auth = useAuthStore()
    const loading = ref(false)

    const isAuthed = computed(() => auth.isAuthed)

    async function login(dto) {
        loading.value = true
        try {
            if (isMockMode() || dto.password === 'mock') {
                const username = dto.username.trim() || 'demo'
                auth.setUser(username)
                auth.setToken(mockToken(username))
                return auth.token
            }

            const resp = await http.post('/api/v1/auth/login', dto)
            if (resp.data.code !== 200) throw new Error(resp.data.message)
            auth.setUser(dto.username.trim())
            auth.setToken(resp.data.data)
            return resp.data.data
        } finally {
            loading.value = false
        }
    }

    async function register(dto) {
        loading.value = true
        try {
            if (isMockMode()) {
                return '注册成功'
            }

            const resp = await http.post('/api/v1/auth/register', dto)
            if (resp.data.code !== 200) throw new Error(resp.data.message)
            return resp.data.data
        } finally {
            loading.value = false
        }
    }

    function logout() {
        auth.clear()
    }

    function ensureUsernameFallback() {
        if (!auth.username && auth.token && !isMockToken(auth.token)) {
            auth.setUser('current')
        }
    }

    return {
        loading,
        isAuthed,
        auth,
        login,
        register,
        logout
    }
}