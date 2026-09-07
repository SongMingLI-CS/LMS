<template>
<header class="h-16 flex items-center justify-between px-6 sticky top-0 z-40">
                <div class="flex items-center">
                    <button @click="isSidebarOpen = !isSidebarOpen" class="p-2.5 rounded-xl hover:bg-slate-100/80 text-slate-500 transition-colors"><ion-icon name="menu-outline" class="text-2xl"></ion-icon></button>
                    <div class="ml-4 flex items-center gap-2.5">
                        <span class="w-2 h-2 rounded-full bg-gradient-to-r from-indigo-500 to-fuchsia-500 inline-block"></span>
                        <h2 class="text-lg font-bold text-slate-800">{{ currentPageTitle }}</h2>
                    </div>
                </div>
                <div class="flex items-center gap-3">
                    <div class="hidden sm:flex items-center gap-2 text-xs font-medium text-slate-500 bg-white/70 border border-slate-200/70 rounded-full pl-3 pr-4 py-1.5 backdrop-blur">
                        <ion-icon name="calendar-clear-outline" class="text-slate-400"></ion-icon>{{ new Date().toLocaleDateString('zh-CN', { year:'numeric', month:'long', day:'numeric', weekday:'long' }) }}
                    </div>
                    <div class="w-9 h-9 rounded-full bg-gradient-to-br from-indigo-500 to-fuchsia-500 text-white flex items-center justify-center text-xs font-bold shadow-glow ring-2 ring-white/60">{{ currentUser.name.charAt(0) }}</div>
                </div>
            </header>
</template>

<script>
import { storeToRefs } from 'pinia'
import { useLms } from '../lms'
export default {
  name: 'TopHeader',
  setup() {
    const s = useLms()
    return { ...s, ...storeToRefs(s) }
  }
}
</script>
