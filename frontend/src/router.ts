import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import type { Component } from 'vue'
import { useLms } from './lms.js'

import DashboardView from './views/DashboardView.vue'
import AnalysisView from './views/AnalysisView.vue'
import BooksView from './views/BooksView.vue'
import RecordsView from './views/RecordsView.vue'
import AuditLogsView from './views/AuditLogsView.vue'
import BorrowManageView from './views/BorrowManageView.vue'
import ProfileView from './views/ProfileView.vue'
import UsersView from './views/UsersView.vue'

export const PAGE_PATHS: Record<string, string> = {
    dashboard: '/',
    analysis: '/analysis',
    manageBooks: '/books',
    records: '/records',
    auditLogs: '/audit',
    manageBorrow: '/borrow',
    profile: '/profile',
    manageUsers: '/users'
}

const routes: RouteRecordRaw[] = [
    { path: '/', name: 'dashboard', meta: { page: 'dashboard', roles: ['user', 'admin', 'superadmin'] }, component: DashboardView as Component },
    { path: '/analysis', name: 'analysis', meta: { page: 'analysis', roles: ['admin', 'superadmin'] }, component: AnalysisView as Component },
    { path: '/books', name: 'books', meta: { page: 'manageBooks', roles: ['user', 'admin', 'superadmin'] }, component: BooksView as Component },
    { path: '/records', name: 'records', meta: { page: 'records', roles: ['user', 'admin', 'superadmin'] }, component: RecordsView as Component },
    { path: '/audit', name: 'audit', meta: { page: 'auditLogs', roles: ['superadmin'] }, component: AuditLogsView as Component },
    { path: '/borrow', name: 'borrow', meta: { page: 'manageBorrow', roles: ['admin', 'superadmin'] }, component: BorrowManageView as Component },
    { path: '/profile', name: 'profile', meta: { page: 'profile', roles: ['user', 'admin', 'superadmin'] }, component: ProfileView as Component },
    { path: '/users', name: 'users', meta: { page: 'manageUsers', roles: ['superadmin'] }, component: UsersView as Component },
    { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 全局鉴权 + 角色守卫（页面元信息 roles 来自服务端角色体系）
// '/' 即登录/首页：未登录用户访问 '/' 时放行（由 AuthShell 渲染登录墙），访问其他受保护页时回首页
router.beforeEach((to) => {
    const lms = useLms()
    const roles = to.meta?.roles as string[] | undefined
    if (!roles) return true
    if (!lms.currentUser) {
        return to.path === '/' ? true : { path: '/' }
    }
    const role = lms.getCleanRole(lms.currentUser.role)
    if (!roles.includes(role)) return { path: '/' }
    return true
})

export default router
