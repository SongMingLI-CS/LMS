// @vitest-environment happy-dom
// 登录集成测试：挂载完整 App + 真实 router/Pinia/i18n，mock API，走通 登录→主界面→登出
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import App from '../App.vue'
import router from '../router'
import i18n from '../i18n'
import { useLms } from '../lms'
import { api } from '../api/http.js'

vi.mock('../api/http.js', () => ({
    api: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
    SESSION_KEY: 'lms-session'
}))

const USER = { id: 1, username: 'stu001', name: '张三', email: 'stu001@mail.nwpu.edu.cn', role: 'ROLE_USER' }

async function mountApp() {
    const pinia = createPinia()
    setActivePinia(pinia)
    await router.push('/')
    const store = useLms()
    const wrapper = mount(App, { global: { plugins: [pinia, router, i18n] } })
    await flushPromises()
    return { wrapper, store }
}

describe('App 登录集成', () => {
    beforeEach(() => {
        vi.mocked(api.get).mockReset()
        vi.mocked(api.post).mockReset()
        vi.mocked(api.put).mockReset()
        vi.mocked(api.delete).mockReset()
        vi.mocked(api.get).mockResolvedValue({ data: [] })
        vi.mocked(api.post).mockImplementation((url) => {
            if (url === '/login') {
                return Promise.resolve({ data: { token: 'tk-1', refreshToken: 'rt-1', user: { ...USER } } })
            }
            return Promise.resolve({ data: {} })
        })
    })

    it('未登录渲染登录页；提交表单后进入主界面并展示用户信息', async () => {
        const { wrapper, store } = await mountApp()
        expect(wrapper.find('h2').text()).toBe('欢迎回来')

        await wrapper.find('input[type="text"]').setValue('stu001')
        await wrapper.find('input[type="password"]').setValue('pw123')
        await wrapper.find('form').trigger('submit')
        await flushPromises()
        await flushPromises()

        expect(store.currentUser.role).toBe('user')
        expect(api.post).toHaveBeenCalledWith('/login', { username: 'stu001', password: 'pw123' })
        // 已进入主界面：侧边导航 / 欢迎语 / 顶栏标题均可见
        expect(wrapper.text()).toContain('图书资源')
        expect(wrapper.text()).toContain('借阅记录')
        expect(wrapper.text()).toContain('你好，张三')
    })

    it('登录后可点击退出登录回到登录页', async () => {
        const { wrapper, store } = await mountApp()
        await wrapper.find('input[type="text"]').setValue('stu001')
        await wrapper.find('input[type="password"]').setValue('pw123')
        await wrapper.find('form').trigger('submit')
        await flushPromises()

        const logout = wrapper.findAll('button').find((w) => w.text().includes('退出登录'))
        expect(logout).toBeTruthy()
        await logout.trigger('click')
        await flushPromises()
        expect(store.currentUser).toBeNull()
        expect(api.post).toHaveBeenCalledWith('/api/auth/logout', { refreshToken: 'rt-1' })
        expect(wrapper.find('h2').text()).toBe('欢迎回来')
    })
})
