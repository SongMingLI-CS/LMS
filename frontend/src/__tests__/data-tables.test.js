// @vitest-environment happy-dom
// 数据表格视图组件测试：挂载真实视图 + 真实 Pinia store，预置业务数据后断言表格渲染
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useLms } from '../lms'
import BooksView from '../views/BooksView.vue'
import RecordsView from '../views/RecordsView.vue'
import UsersView from '../views/UsersView.vue'
import BorrowManageView from '../views/BorrowManageView.vue'

function boot({ page, user, books = [], users = [], records = [] }) {
    const pinia = createPinia()
    setActivePinia(pinia)
    const store = useLms()
    store.currentUser = user
    store.currentPage = page
    store.books = books
    store.users = users
    store.borrowRecords = records
    return { pinia, store }
}

function mountView(Component, pinia) {
    return mount(Component, { global: { plugins: [pinia] } })
}

const ADMIN = { id: 1, username: 'admin', name: '管理员', role: 'admin' }

describe('BooksView 图书表格', () => {
    it('渲染图书行：书名/作者/ISBN 与库存状态徽章', () => {
        const { pinia } = boot({
            page: 'manageBooks',
            user: ADMIN,
            books: [
                { id: 1, title: '三体', author: '刘慈欣', isbn: '978-7-5366-9293-0', category: '科幻', cover: '', available: 2, stock: 3 },
                { id: 2, title: '活着', author: '余华', isbn: '978-7-5063-6543-7', cover: '', available: 0, stock: 2 }
            ]
        })
        const wrapper = mountView(BooksView, pinia)
        const rows = wrapper.findAll('tbody tr')
        expect(rows).toHaveLength(2)
        expect(rows[0].text()).toContain('三体')
        expect(rows[0].text()).toContain('刘慈欣')
        expect(rows[0].text()).toContain('剩余 2 本')
        expect(rows[1].text()).toContain('活着')
        expect(rows[1].text()).toContain('暂无库存')
    })

    it('按关键词过滤图书行', async () => {
        const { pinia } = boot({
            page: 'manageBooks',
            user: ADMIN,
            books: [
                { id: 1, title: '三体', author: '刘慈欣', isbn: 'A1', cover: '', available: 1 },
                { id: 2, title: '活着', author: '余华', isbn: 'B2', cover: '', available: 1 }
            ]
        })
        const wrapper = mountView(BooksView, pinia)
        const search = wrapper.find('input')
        await search.setValue('活着')
        let rows = wrapper.findAll('tbody tr')
        expect(rows).toHaveLength(1)
        expect(rows[0].text()).toContain('活着')
        await search.setValue('不存在的书')
        rows = wrapper.findAll('tbody tr')
        expect(rows).toHaveLength(1)
        expect(rows[0].text()).toContain('未找到匹配的图书')
    })

    it('无图书数据时渲染空态行', () => {
        const { pinia } = boot({ page: 'manageBooks', user: ADMIN, books: [] })
        const wrapper = mountView(BooksView, pinia)
        expect(wrapper.text()).toContain('未找到匹配的图书')
    })

    it('操作入口随角色/库存/借阅状态显隐', () => {
        const USER = { id: 5, username: 'stu001', name: '张三', role: 'user' }
        const { pinia } = boot({
            page: 'manageBooks',
            user: USER,
            books: [
                { id: 1, title: '已被我借阅', cover: '', available: 2 },
                { id: 2, title: '可借阅', cover: '', available: 2 },
                { id: 3, title: '无库存可预约', cover: '', available: 0 }
            ],
            records: [{ id: 1, bookId: 1, userId: 5, status: 'borrowed' }]
        })
        const wrapper = mountView(BooksView, pinia)
        const rows = wrapper.findAll('tbody tr')
        expect(rows[0].text()).toContain('已借阅')
        expect(rows[1].find('button').text()).toBe('借阅')
        expect(rows[2].find('button').text()).toBe('预约')
        expect(wrapper.text()).not.toContain('新书入库')
        expect(wrapper.text()).not.toContain('批量导入')
    })
})

describe('RecordsView 借阅记录表格', () => {
    it('按状态渲染中文标签', () => {
        const { pinia } = boot({
            page: 'records',
            user: ADMIN,
            records: [
                { id: 1, bookTitle: '三体', borrowDate: '2026-08-01', dueDate: '2099-01-01', status: 'borrowed' },
                { id: 2, bookTitle: '活着', borrowDate: '2026-07-01', dueDate: '2026-07-15', status: 'returned' },
                { id: 3, bookTitle: '百年孤独', borrowDate: '2026-08-05', dueDate: '2026-09-01', status: 'reserved' }
            ]
        })
        const wrapper = mountView(RecordsView, pinia)
        const rows = wrapper.findAll('tbody tr')
        expect(rows).toHaveLength(3)
        expect(wrapper.text()).toContain('3 条')
        expect(rows[0].text()).toContain('三体')
        expect(rows[0].text()).toContain('借阅中')
        expect(rows[1].text()).toContain('已归还')
        expect(rows[2].text()).toContain('已预约')
        expect(wrapper.text()).not.toContain('续借')
    })

    it('普通用户仅显示本人记录且出现续借入口', () => {
        const USER = { id: 7, username: 'stu001', name: '张三', role: 'user' }
        const { pinia } = boot({
            page: 'records',
            user: USER,
            records: [
                { id: 1, bookTitle: '数据结构', borrowDate: '2026-08-01', dueDate: '2099-01-01', status: 'borrowed', userId: 7 },
                { id: 2, bookTitle: '别人的记录', borrowDate: '2026-07-01', dueDate: '2026-07-15', status: 'returned', userId: 8 }
            ]
        })
        const wrapper = mountView(RecordsView, pinia)
        const rows = wrapper.findAll('tbody tr')
        expect(rows).toHaveLength(1)
        expect(rows[0].text()).toContain('数据结构')
        expect(rows[0].text()).toContain('借阅中')
        expect(rows[0].text()).toContain('续借')
        expect(wrapper.text()).not.toContain('别人的记录')
    })
})

describe('UsersView 用户表格', () => {
    it('渲染用户行与角色中文及行操作', () => {
        const { pinia } = boot({
            page: 'manageUsers',
            user: { id: 1, username: 'sroot', name: '系统管理员', role: 'superadmin' },
            users: [
                { id: 1, username: 'sroot', name: '系统管理员', role: 'superadmin' },
                { id: 2, username: 'libadmin', name: '图书管理员', role: 'admin' },
                { id: 3, username: 'stu001', name: '张三', role: 'user' }
            ]
        })
        const wrapper = mountView(UsersView, pinia)
        const rows = wrapper.findAll('tbody tr')
        expect(rows).toHaveLength(3)
        expect(rows[0].text()).toContain('系统管理员')
        expect(rows[0].text()).toContain('超级管理员')
        expect(rows[1].text()).toContain('管理员')
        expect(rows[2].text()).toContain('张三')
        expect(rows[2].text()).toContain('编辑')
        expect(rows[2].text()).toContain('删除')
    })

    it('按用户名关键词过滤用户行', async () => {
        const { pinia } = boot({
            page: 'manageUsers',
            user: { id: 1, username: 'sroot', name: '系统管理员', role: 'superadmin' },
            users: [
                { id: 1, username: 'sroot', name: '系统管理员', role: 'superadmin' },
                { id: 2, username: 'stu001', name: '张三', role: 'user' }
            ]
        })
        const wrapper = mountView(UsersView, pinia)
        await wrapper.find('input').setValue('stu001')
        const rows = wrapper.findAll('tbody tr')
        expect(rows).toHaveLength(1)
        expect(rows[0].text()).toContain('张三')
        expect(wrapper.text()).not.toContain('系统管理员')
    })
})

describe('BorrowManageView 借还/预约面板', () => {
    it('展示待处理预约的图书与预约人并可批准', () => {
        const { pinia } = boot({
            page: 'manageBorrow',
            user: ADMIN,
            books: [{ id: 1, title: '三体', author: '刘慈欣', cover: '', available: 1 }],
            users: [{ id: 10, username: 'stu010', name: '李四', role: 'user' }],
            records: [{ id: 1, bookId: 1, userId: 10, status: 'reserved' }]
        })
        const wrapper = mountView(BorrowManageView, pinia)
        expect(wrapper.text()).toContain('图书归还')
        expect(wrapper.find('input[placeholder="ISBN / 图书 ID"]').exists()).toBe(true)
        expect(wrapper.text()).toContain('三体')
        expect(wrapper.text()).toContain('李四')
        const approve = wrapper.findAll('button').find((w) => w.text().includes('批准'))
        expect(approve).toBeTruthy()
    })

    it('无待处理预约时展示空状态', () => {
        const { pinia } = boot({ page: 'manageBorrow', user: ADMIN })
        const wrapper = mountView(BorrowManageView, pinia)
        expect(wrapper.text()).toContain('确认归还')
        expect(wrapper.text()).toContain('暂无待办预约')
    })
})

