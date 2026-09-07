// 纯工具函数：角色/状态/逾期 判定与文案映射（不依赖 Vue / Pinia）

export function getCleanRole(r) {
    return (r || 'ROLE_USER').replace(/ROLE_/gi, '').toLowerCase()
}

export function getRoleName(r) {
    return { user: '用户', admin: '管理员', superadmin: '超级管理员' }[getCleanRole(r)] || '未知'
}

export function isOverdue(d) {
    return new Date(d) < new Date(new Date().toISOString().split('T')[0])
}

export function statusLabel(s) {
    return (
        {
            borrowed: '借阅中',
            returned: '已归还',
            reserved: '已预约',
            awaiting_pickup: '待取书',
            overdue: '已逾期'
        }[s] || s
    )
}
