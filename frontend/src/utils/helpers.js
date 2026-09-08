// 纯工具函数：角色/状态/逾期 判定与文案映射（文案经 i18n 本地化，默认中文）
import { t as translate, has as hasMessage } from '../i18n'

export function getCleanRole(r) {
    return (r || 'ROLE_USER').replace(/ROLE_/gi, '').toLowerCase()
}

export function getRoleName(r) {
    const key = getCleanRole(r)
    return hasMessage(`role.${key}`) ? translate(`role.${key}`) : translate('role.unknown')
}

export function isOverdue(d) {
    return new Date(d) < new Date(new Date().toISOString().split('T')[0])
}

export function statusLabel(s) {
    return hasMessage(`status.${s}`) ? translate(`status.${s}`) : s
}

