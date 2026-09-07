<template>
    <div class="text-center py-8 text-slate-400">
        <ion-icon :name="icon" class="text-4xl text-slate-300 mb-2"></ion-icon>
        <p class="text-sm">{{ text }}</p>
        <slot></slot>
    </div>
</template>

<script setup>
defineProps({
    icon: { type: String, default: 'file-tray-outline' },
    text: { type: String, default: '暂无数据' }
})
</script>
