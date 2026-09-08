import { createI18n } from 'vue-i18n'
import zhCN from './locales/zh-CN.js'
import en from './locales/en.js'

export const LOCALES = ['zh-CN', 'en']
export const STORAGE_KEY = 'lms-lang'

export function loadLocale() {
    try {
        const saved = localStorage.getItem(STORAGE_KEY)
        if (saved && LOCALES.includes(saved)) return saved
    } catch {
        /* 非浏览器环境（如 vitest node 环境）下无 localStorage */
    }
    return 'zh-CN'
}

const i18n = createI18n({
    legacy: false,
    globalInjection: true,
    locale: loadLocale(),
    fallbackLocale: 'zh-CN',
    messages: { 'zh-CN': zhCN, en }
})

export function setLocale(locale) {
    if (!LOCALES.includes(locale)) locale = 'zh-CN'
    i18n.global.locale.value = locale
    try {
        localStorage.setItem(STORAGE_KEY, locale)
        if (typeof document !== 'undefined') document.documentElement.lang = locale
    } catch {
        /* ignore */
    }
}

// 供纯函数/非组件逻辑使用（读取 locale ref，具备响应式）
export const t = (key, params) => i18n.global.t(key, params)
export const has = (key) => i18n.global.te(key)

export default i18n
