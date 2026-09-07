<template>
    <div class="text-center py-8 text-slate-400">
        <ion-icon :name="icon" class="text-4xl text-slate-300 mb-2"></ion-icon>
        <p class="text-sm">{{ text }}</p>
        <slot></slot>
    </div>
</template>

<script setup lang="ts">
withDefaults(
    defineProps<{
        icon?: string
        text?: string
    }>(),
    { icon: 'file-tray-outline', text: '暂无数据' }
)
</script>
