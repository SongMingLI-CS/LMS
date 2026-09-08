// @vitest-environment happy-dom
// AuthShell 登录/注册表单组件测试：挂载真实组件 + 真实 Pinia store，仅 mock API 层
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import AuthShell from '../components/AuthShell.vue'
import { useLms } from '../lms'
import { api } from '../api/http.js'
import i18n from '../i18n'

vi.mock('../api/http.js', () => ({
    api: { get: vi.fn(), post: vi.fn() },
    SESSION_KEY: 'lms-session'
}))

async function mountAuth() {
    const pinia = createPinia()
    setActivePinia(pinia)
    const store = useLms()
    const wrapper = mount(AuthShell, { global: { plugins: [pinia, i18n] } })
    await flushPromises()
    return { wrapper, store }
}

async function findLinkByText(wrapper, text) {
    const link = wrapper.findAll('a').find((w) => w.text().includes(text))
    if (!link) throw new Error(`未找到链接：${text}`)
    return link
}

describe('AuthShell 登录表单', () => {
    beforeEach(() => {
        vi.mocked(api.get).mockReset()
        vi.mocked(api.post).mockReset()
        vi.mocked(api.get).mockResolvedValue({ data: [] })
    })

    it('未登录时默认渲染登录表单（账号/密码/登录按钮）', async () => {
        const { wrapper } = await mountAuth()
        expect(wrapper.find('h2').text()).toBe('欢迎回来')
        expect(wrapper.find('input[type="text"]').attributes('placeholder')).toBe('请输入学号 / 工号')
        expect(wrapper.find('input[type="password"]').exists()).toBe(true)
        expect(wrapper.text()).toContain('登 录')
    })

    it('输入账号密码时实时同步到 store.loginForm', async () => {
        const { wrapper, store } = await mountAuth()
        await wrapper.find('input[type="text"]').setValue('stu001')
        await wrapper.find('input[type="password"]').setValue('secret123')
        expect(store.loginForm.username).toBe('stu001')
        expect(store.loginForm.password).toBe('secret123')
    })

    it('提交登录：调用 /login、写入令牌并恢复登录态', async () => {
        vi.mocked(api.post).mockResolvedValue({
            data: {
                token: 'tk-1',
                refreshToken: 'rt-1',
                user: { id: 1, username: 'stu001', name: '张三', email: 'stu001@mail.nwpu.edu.cn', role: 'ROLE_USER' }
            }
        })
        const { wrapper, store } = await mountAuth()
        await wrapper.find('input[type="text"]').setValue('stu001')
        await wrapper.find('input[type="password"]').setValue('pw123')
        await wrapper.find('form').trigger('submit')
        await flushPromises()
        expect(api.post).toHaveBeenCalledWith('/login', { username: 'stu001', password: 'pw123' })
        expect(store.currentUser).toEqual({
            id: 1,
            username: 'stu001',
            name: '张三',
            email: 'stu001@mail.nwpu.edu.cn',
            role: 'user'
        })
        expect(store.isLoading).toBe(false)
        expect(localStorage.getItem('token')).toBe('tk-1')
        expect(localStorage.getItem('refreshToken')).toBe('rt-1')
    })

    it('登录失败时展示错误提示', async () => {
        vi.mocked(api.post).mockRejectedValue(new Error('Network Error'))
        const { wrapper, store } = await mountAuth()
        await wrapper.find('input[type="text"]').setValue('stu001')
        await wrapper.find('input[type="password"]').setValue('wrong-pass')
        await wrapper.find('form').trigger('submit')
        await flushPromises()
        expect(store.loginError).toBe('账号或密码错误')
        expect(store.currentUser).toBeNull()
        expect(wrapper.text()).toContain('账号或密码错误')
    })

    it('账号未激活时跳转至邮箱验证步骤', async () => {
        vi.mocked(api.post).mockRejectedValue({ response: { data: { message: '账户尚未激活，请完成邮箱验证后再登录' } } })
        const { wrapper, store } = await mountAuth()
        await wrapper.find('input[type="text"]').setValue('stu001')
        await wrapper.find('input[type="password"]').setValue('pw123')
        await wrapper.find('form').trigger('submit')
        await flushPromises()
        expect(store.loginError).toBe('未激活')
        expect(store.isVerificationStep).toBe(true)
        expect(wrapper.text()).toContain('激活账户')
        expect(wrapper.find('input[maxlength="6"]').exists()).toBe(true)
    })

    it('注册表单两次密码不一致时给出提示且不发请求', async () => {
        const { wrapper, store } = await mountAuth()
        await (await findLinkByText(wrapper, '立即注册')).trigger('click')
        await flushPromises()
        expect(store.isRegistering).toBe(true)
        expect(wrapper.text()).toContain('创建账户')

        await wrapper.find('input[placeholder="学号"]').setValue('stu001')
        await wrapper.find('input[placeholder="姓名"]').setValue('张三')
        await wrapper.find('input[placeholder="学校邮箱"]').setValue('stu001@mail.nwpu.edu.cn')
        await wrapper.find('input[placeholder="设置密码"]').setValue('pw123456')
        await wrapper.find('input[placeholder="确认密码"]').setValue('pw123457')
        await wrapper.find('form').trigger('submit')
        await flushPromises()
        expect(store.registerError).toBe('密码不一致')
        expect(wrapper.text()).toContain('密码不一致')
        expect(api.post).not.toHaveBeenCalled()
    })

    it('注册成功后进入邮箱验证步骤', async () => {
        vi.mocked(api.post).mockResolvedValue({ data: {} })
        const { wrapper, store } = await mountAuth()
        await (await findLinkByText(wrapper, '立即注册')).trigger('click')
        await flushPromises()

        await wrapper.find('input[placeholder="学号"]').setValue('stu001')
        await wrapper.find('input[placeholder="姓名"]').setValue('张三')
        await wrapper.find('input[placeholder="学校邮箱"]').setValue('stu001@mail.nwpu.edu.cn')
        await wrapper.find('input[placeholder="设置密码"]').setValue('pw123456')
        await wrapper.find('input[placeholder="确认密码"]').setValue('pw123456')
        await wrapper.find('form').trigger('submit')
        await flushPromises()
        expect(api.post).toHaveBeenCalledWith('/register', {
            username: 'stu001',
            name: '张三',
            email: 'stu001@mail.nwpu.edu.cn',
            password: 'pw123456',
            confirmPassword: 'pw123456'
        })
        expect(store.isVerificationStep).toBe(true)
        expect(store.registerForm.username).toBe('stu001')
        expect(wrapper.text()).toContain('激活账户')
        expect(wrapper.text()).toContain('stu001@mail.nwpu.edu.cn')
    })

    it('“忘记密码”与“取消”可在登录/重置视图间切换', async () => {
        const { wrapper, store } = await mountAuth()
        await (await findLinkByText(wrapper, '忘记密码')).trigger('click')
        await flushPromises()
        expect(store.isForgotPassword).toBe(true)
        expect(wrapper.text()).toContain('重置密码')
        expect(wrapper.find('input[placeholder="注册邮箱"]').exists()).toBe(true)

        const cancel = wrapper.findAll('button').find((w) => w.text().includes('取消'))
        await cancel.trigger('click')
        await flushPromises()
        expect(store.isForgotPassword).toBe(false)
        expect(wrapper.find('h2').text()).toBe('欢迎回来')
    })
})
