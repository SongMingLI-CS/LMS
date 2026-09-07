<template>
    <div class="glass-card rounded-2xl p-5 relative overflow-hidden group">
        <div class="absolute -right-8 -top-8 w-28 h-28 rounded-full opacity-25 blur-2xl group-hover:opacity-40 transition-opacity" :class="glow"></div>
        <div class="relative flex items-center justify-between">
            <div class="min-w-0">
                <p class="text-xs font-medium text-slate-500">{{ label }}</p>
                <p class="text-3xl font-extrabold mt-1.5 truncate" :class="valueClass">{{ value }}</p>
            </div>
            <div class="w-11 h-11 rounded-xl text-white flex items-center justify-center text-[22px] shadow-glow flex-shrink-0 ml-3" :class="iconClass">
                <ion-icon :name="icon"></ion-icon>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
withDefaults(
    defineProps<{
        label: string
        value: string | number
        icon?: string
        iconClass?: string
        glow?: string
        valueClass?: string
    }>(),
    {
        icon: 'ellipse-outline',
        iconClass: 'bg-gradient-to-br from-indigo-500 to-violet-500',
        glow: 'bg-gradient-to-br from-indigo-500 to-violet-500',
        valueClass: 'text-slate-900'
    }
)
</script>
