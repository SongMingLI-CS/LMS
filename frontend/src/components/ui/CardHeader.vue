<template>
    <div class="flex justify-between items-center mb-5">
        <h3 class="font-bold text-slate-900 flex items-center gap-2 min-w-0">
            <span class="w-1 h-4 rounded bg-gradient-to-b inline-block flex-shrink-0" :class="barClass"></span>
            <span class="truncate">{{ title }}</span>
        </h3>
        <div class="flex items-center gap-2 flex-shrink-0">
            <slot name="actions"></slot>
        </div>
    </div>
</template>

<script setup lang="ts">
withDefaults(
    defineProps<{
        title: string
        barClass?: string
    }>(),
    { barClass: 'from-indigo-500 to-fuchsia-500' }
)
</script>
