// 纯工具函数：角色/状态/逾期 判定与文案 key 映射（不依赖 Vue/Pinia/i18n）
// 展示文案统一在模板经 $t() 渲染：getRoleName/statusLabel 保留中文默认值（兼容旧引用/测试），
// roleLabelKey/statusLabelKey 返回 i18n key 供模板随语言切换。

export function getCleanRole(r) {
    return (r || 'ROLE_USER').replace(/ROLE_/gi, '').toLowerCase()
}

const ROLE_NAME = { user: '用户', admin: '管理员', superadmin: '超级管理员' }
const STATUS_LABEL = { borrowed: '借阅中', returned: '已归还', reserved: '已预约', awaiting_pickup: '待取书', overdue: '已逾期' }

export function getRoleName(r) {
    return ROLE_NAME[getCleanRole(r)] || '未知'
}

export function roleLabelKey(r) {
    return `role.${getCleanRole(r)}`
}

export function statusLabel(s) {
    return STATUS_LABEL[s] || s
}

export function statusLabelKey(s) {
    return `status.${s}`
}

export function isOverdue(d) {
    return new Date(d) < new Date(new Date().toISOString().split('T')[0])
}


