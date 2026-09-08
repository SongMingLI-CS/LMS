<template>
    <button
        type="button"
        class="inline-flex items-center justify-center h-8 px-3 rounded-full text-xs font-bold border border-slate-200/70 bg-white/60 backdrop-blur text-slate-500 hover:text-indigo-600 hover:border-indigo-300 transition-colors flex-shrink-0"
        :title="$t('lang.switchTo')"
        @click="toggle"
    >
        {{ label }}
    </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { setLocale } from '../../i18n'

const { locale } = useI18n()

const label = computed(() => (locale.value === 'zh-CN' ? 'EN' : '中'))

function toggle() {
    setLocale(locale.value === 'zh-CN' ? 'en' : 'zh-CN')
}
</script>
