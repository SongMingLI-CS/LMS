// @vitest-environment happy-dom
// 路由守卫集成测试：真实 router + 真实 Pinia store，验证登录态与角色鉴权
import { describe, it, expect, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import router from '../router'
import { useLms } from '../lms'

function loginAs(user) {
    const pinia = createPinia()
    setActivePinia(pinia)
    const store = useLms()
    if (user) store.currentUser = user
    return store
}

const USER = { id: 1, username: 'stu001', name: '张三', role: 'user' }
const ADMIN = { id: 2, username: 'admin', name: '管理员', role: 'admin' }
const SUPER = { id: 3, username: 'sroot', name: '超级管理员', role: 'superadmin' }

describe('router 鉴权守卫', () => {
    beforeEach(async () => {
        loginAs(null)
        await router.push('/')
    })

    it('未登录访问任意受保护页均重定向回首页', async () => {
        await router.push('/books')
        expect(router.currentRoute.value.path).toBe('/')
        await router.push('/users')
        expect(router.currentRoute.value.path).toBe('/')
    })

    it('普通用户可访问公开页面', async () => {
        loginAs(USER)
        await router.push('/books')
        expect(router.currentRoute.value.path).toBe('/books')
        await router.push('/records')
        expect(router.currentRoute.value.path).toBe('/records')
    })

    it('普通用户不可访问管理页并回退首页', async () => {
        loginAs(USER)
        await router.push('/users')
        expect(router.currentRoute.value.path).toBe('/')
        await router.push('/borrow')
        expect(router.currentRoute.value.path).toBe('/')
        await router.push('/analysis')
        expect(router.currentRoute.value.path).toBe('/')
    })

    it('管理员可访问借还与数据分析，不可访问用户管理', async () => {
        loginAs(ADMIN)
        await router.push('/borrow')
        expect(router.currentRoute.value.path).toBe('/borrow')
        await router.push('/analysis')
        expect(router.currentRoute.value.path).toBe('/analysis')
        await router.push('/users')
        expect(router.currentRoute.value.path).toBe('/')
    })

    it('超级管理员可访问全部页面（含用户管理与审计）', async () => {
        loginAs(SUPER)
        await router.push('/users')
        expect(router.currentRoute.value.path).toBe('/users')
        await router.push('/audit')
        expect(router.currentRoute.value.path).toBe('/audit')
    })
})
