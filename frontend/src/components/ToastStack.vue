<template>
<div class="fixed top-5 right-5 z-[110] flex flex-col gap-3 pointer-events-none w-[340px] max-w-[calc(100vw-2.5rem)]">
        <transition-group name="toast">
            <div v-for="toast in toasts" :key="toast.id"
                 class="pointer-events-auto relative glass-card rounded-2xl py-3.5 pl-4 pr-5 flex items-start overflow-hidden shadow-glow">
                <span class="absolute inset-y-0 left-0 w-1.5" :class="toast.type === 'error' ? 'bg-gradient-to-b from-rose-500 to-pink-500' : 'bg-gradient-to-b from-emerald-400 to-teal-500'"></span>
                <div :class="['relative w-9 h-9 mr-3.5 rounded-xl flex items-center justify-center text-lg text-white flex-shrink-0', toast.type === 'error' ? 'bg-gradient-to-br from-rose-500 to-pink-500' : 'bg-gradient-to-br from-emerald-400 to-teal-500']">
                    <ion-icon :name="toast.type === 'error' ? 'alert' : 'checkmark'"></ion-icon>
                </div>
                <div class="relative min-w-0">
                    <h4 class="text-sm font-bold text-slate-900">{{ toast.title }}</h4>
                    <p class="text-xs text-slate-500 mt-0.5 break-words">{{ toast.text }}</p>
                </div>
            </div>
        </transition-group>
    </div>
</template>

<script lang="ts">
import { useLms } from '../lms'
import { bindStore } from '../utils/store-bindings'
export default {
  name: 'ToastStack',
  setup() {
    const s = useLms()
    return { ...bindStore(s) }
  }
}
</script>
