// @vitest-environment happy-dom
import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import AuthShell from '../components/AuthShell.vue'
import i18n, { setLocale, loadLocale, STORAGE_KEY } from '../i18n'

describe('i18n 基础设施', () => {
    beforeEach(() => {
        localStorage.removeItem(STORAGE_KEY)
        setLocale('zh-CN')
    })

    it('默认语言为中文且保留既有文案', () => {
        expect(loadLocale()).toBe('zh-CN')
        expect(i18n.global.t('auth.loginTitle')).toBe('欢迎回来')
        expect(i18n.global.t('status.borrowed')).toBe('借阅中')
        expect(i18n.global.t('role.superadmin')).toBe('超级管理员')
    })

    it('切换语言后返回英文并持久化到 localStorage', () => {
        setLocale('en')
        expect(i18n.global.t('auth.loginTitle')).toBe('Welcome back')
        expect(i18n.global.t('status.borrowed')).toBe('Borrowed')
        expect(i18n.global.t('role.superadmin')).toBe('Super Admin')
        expect(localStorage.getItem(STORAGE_KEY)).toBe('en')
        expect(loadLocale()).toBe('en')
    })

    it('非法语言值回退为中文', () => {
        setLocale('fr')
        expect(loadLocale()).toBe('zh-CN')
    })

    it('切到英文后 AuthShell 渲染英文文案', () => {
        setLocale('en')
        const pinia = createPinia()
        setActivePinia(pinia)
        const wrapper = mount(AuthShell, { global: { plugins: [pinia, i18n] } })
        expect(wrapper.find('h2').text()).toBe('Welcome back')
        expect(wrapper.text()).toContain('Sign In')
        expect(wrapper.find('input[type="text"]').attributes('placeholder')).toBe('Enter student / staff ID')
    })
})
