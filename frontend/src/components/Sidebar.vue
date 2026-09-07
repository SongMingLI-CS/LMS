<template>
<aside :class="['sidebar flex flex-col transition-all duration-300 z-40 border-r border-white/5 overflow-hidden', isSidebarOpen ? 'w-72' : 'w-20']">
            <div class="h-16 flex items-center px-5 border-b border-white/10 flex-shrink-0">
                <div class="w-9 h-9 rounded-xl bg-gradient-to-br from-indigo-500 via-violet-500 to-fuchsia-500 text-white flex-shrink-0 flex items-center justify-center text-lg shadow-glow">
                    <ion-icon name="library"></ion-icon>
                </div>
                <div v-if="isSidebarOpen" class="ml-3 whitespace-nowrap overflow-hidden">
                    <p class="font-extrabold text-white tracking-tight text-[15px] leading-tight">LMS 智慧图书馆</p>
                    <p class="text-[10px] text-slate-500 tracking-[0.2em] uppercase">Library Mgmt</p>
                </div>
            </div>

            <nav class="flex-1 py-4 overflow-y-auto flex flex-col">
                <p v-if="isSidebarOpen" class="px-5 mb-1.5 text-[10px] font-semibold tracking-[0.2em] text-slate-500 uppercase">主导航</p>
                <a v-for="page in visiblePages" :key="page.id" href="#" @click.prevent="goPage(page.id)"
                   :class="['nav-link', currentPage === page.id ? 'active' : '', isSidebarOpen ? '' : 'justify-center']">
                    <ion-icon :name="page.icon" class="text-[20px] flex-shrink-0" :class="[isSidebarOpen ? 'mr-3' : '']"></ion-icon>
                    <span v-if="isSidebarOpen" class="text-sm whitespace-nowrap">{{ page.title }}</span>
                </a>
                <p v-if="isSidebarOpen" class="px-5 mt-3 mb-1.5 text-[10px] font-semibold tracking-[0.2em] text-slate-500 uppercase">数据服务</p>
                <a href="#" @click.prevent="handleExportExcel" :class="['nav-link', isSidebarOpen ? '' : 'justify-center']">
                    <ion-icon name="download-outline" class="text-[20px] flex-shrink-0" :class="[isSidebarOpen ? 'mr-3' : '']"></ion-icon>
                    <span v-if="isSidebarOpen" class="text-sm whitespace-nowrap">导出借阅记录</span>
                </a>
            </nav>

            <div class="p-4 border-t border-white/10 flex-shrink-0">
                <div class="flex items-center" :class="[isSidebarOpen ? '' : 'justify-center']">
                    <div class="w-9 h-9 rounded-xl bg-gradient-to-br from-indigo-500 to-fuchsia-500 text-white flex items-center justify-center text-sm font-bold shadow-glow ring-1 ring-white/20">{{ currentUser.name.charAt(0) }}</div>
                    <div v-if="isSidebarOpen" class="ml-3 overflow-hidden">
                        <p class="text-sm font-semibold text-white truncate">{{ currentUser.name }}</p>
                        <p class="text-[11px] text-slate-400 truncate flex items-center gap-1.5"><span class="inline-block w-1.5 h-1.5 rounded-full bg-emerald-400"></span>{{ getRoleName(currentUser.role) }}</p>
                    </div>
                </div>
                <button @click="handleLogout" :class="['mt-3 w-full flex items-center py-2 rounded-xl text-slate-400 hover:text-rose-300 hover:bg-white/5 transition-colors', isSidebarOpen ? 'justify-start pl-4' : 'justify-center']">
                    <ion-icon name="log-out-outline" :class="['text-lg', isSidebarOpen ? 'mr-2' : '']"></ion-icon>
                    <span v-if="isSidebarOpen" class="text-xs">退出登录</span>
                </button>
            </div>
        </aside>
</template>

<script lang="ts">
import { storeToRefs } from 'pinia'
import { useLms } from '../lms'
import { useRouter } from 'vue-router'
import { PAGE_PATHS } from '../router'

export default {
  name: 'Sidebar',
  setup() {
    const s = useLms()
    const router = useRouter()
    function goPage(id) {
      const target = PAGE_PATHS[id] || '/'
      if (router.currentRoute.value.path === target) return
      router.push(target)
    }
    return { ...s, ...storeToRefs(s), goPage }
  }
}
</script>
