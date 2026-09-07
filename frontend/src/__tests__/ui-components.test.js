// @vitest-environment happy-dom
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import StatCard from '../components/ui/StatCard.vue'
import CardHeader from '../components/ui/CardHeader.vue'
import EmptyState from '../components/ui/EmptyState.vue'

describe('StatCard', () => {
    it('渲染 label、value 与图标', () => {
        const wrapper = mount(StatCard, {
            props: { label: '我的借阅', value: 5, icon: 'book-outline' }
        })
        expect(wrapper.text()).toContain('我的借阅')
        expect(wrapper.text()).toContain('5')
        expect(wrapper.find('ion-icon').attributes('name')).toBe('book-outline')
    })

    it('支持自定义数值颜色 class', () => {
        const wrapper = mount(StatCard, {
            props: { label: '库存预警', value: 3, valueClass: 'text-rose-500' }
        })
        const value = wrapper.find('.text-3xl')
        expect(value.classes()).toContain('text-rose-500')
    })
})

describe('CardHeader', () => {
    it('渲染标题并暴露 actions 插槽', () => {
        const wrapper = mount(CardHeader, {
            props: { title: '热门图书 Top 5' },
            slots: { actions: '<button class="export">导出</button>' }
        })
        expect(wrapper.text()).toContain('热门图书 Top 5')
        expect(wrapper.find('.export').exists()).toBe(true)
    })
})

describe('EmptyState', () => {
    it('显示默认文案，并支持自定义', () => {
        expect(mount(EmptyState).text()).toContain('暂无数据')
        const custom = mount(EmptyState, { props: { text: '空空如也', icon: 'sad-outline' } })
        expect(custom.text()).toContain('空空如也')
        expect(custom.find('ion-icon').attributes('name')).toBe('sad-outline')
    })
})
