import axios from 'axios'

// 会话持久化存储键（与 pinia-plugin-persistedstate 中 useLms 的 persist 配置保持一致）
export const SESSION_KEY = 'lms-session'

// 单一 axios 实例：请求自动附加 Bearer Token，401 静默刷新访问令牌
export const api = axios.create({ baseURL: '/', timeout: 8000 })

api.interceptors.request.use((config) => {
    const token = localStorage.getItem('token')
    if (token) config.headers.Authorization = `Bearer ${token}`
    return config
})

// P0: 401 自动刷新访问令牌（共享单次刷新请求，避免并发重复刷新）
let refreshPromise = null
api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const original = error.config
        const status = error.response?.status
        const refreshToken = localStorage.getItem('refreshToken')
        if (status === 401 && original && !original._retry && refreshToken) {
            original._retry = true
            if (!refreshPromise) {
                refreshPromise = axios
                    .post('/api/auth/refresh', { refreshToken }, { baseURL: '/', timeout: 8000 })
                    .then((res) => {
                        localStorage.setItem('token', res.data.token)
                        if (res.data.refreshToken) localStorage.setItem('refreshToken', res.data.refreshToken)
                        return res.data.token
                    })
                    .finally(() => {
                        refreshPromise = null
                    })
            }
            try {
                const newToken = await refreshPromise
                original.headers = original.headers || {}
                original.headers.Authorization = `Bearer ${newToken}`
                return api(original)
            } catch (e) {
                localStorage.removeItem('token')
                localStorage.removeItem('refreshToken')
                localStorage.removeItem(SESSION_KEY) // 会话失效时同时清除持久化的登录态，避免刷新循环
                window.location.reload()
                return Promise.reject(e)
            }
        }
        return Promise.reject(error)
    }
)
