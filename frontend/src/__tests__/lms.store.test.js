import { describe, it, expect, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useLms } from '../lms'

describe('lms store helpers', () => {
    beforeEach(() => {
        setActivePinia(createPinia())
    })

    it('规范化角色并映射中文名', () => {
        const s = useLms()
        expect(s.getCleanRole('ROLE_SUPERADMIN')).toBe('superadmin')
        expect(s.getCleanRole('ROLE_USER')).toBe('user')
        expect(s.getRoleName('admin')).toBe('管理员')
        expect(s.getRoleName('superadmin')).toBe('超级管理员')
        expect(s.getRoleName('ROLE_ADMIN')).toBe('管理员')
    })

    it('借阅状态映射为中文', () => {
        const s = useLms()
        expect(s.statusLabel('borrowed')).toBe('借阅中')
        expect(s.statusLabel('returned')).toBe('已归还')
        expect(s.statusLabel('overdue')).toBe('已逾期')
        expect(s.statusLabel('reserved')).toBe('已预约')
        expect(s.statusLabel('awaiting_pickup')).toBe('待取书')
        expect(s.statusLabel('unknown-status')).toBe('unknown-status')
    })

    it('按日期判断是否逾期', () => {
        const s = useLms()
        expect(s.isOverdue('2099-01-01')).toBe(false)
        expect(s.isOverdue('1999-01-01')).toBe(true)
    })

    it('初始状态：未登录、数据为空', () => {
        const s = useLms()
        expect(s.currentUser).toBeNull()
        expect(s.books).toEqual([])
        expect(s.borrowRecords).toEqual([])
        expect(s.currentPage).toBe('dashboard')
    })
})
