<template>
    <div class="flex h-full w-full relative">
        <!-- 环境光晕层 -->
        <div class="pointer-events-none fixed inset-0 -z-10 overflow-hidden" aria-hidden="true">
            <div class="absolute -top-24 -right-20 w-[460px] h-[460px] rounded-full opacity-40 blur-3xl" style="background: radial-gradient(circle, #818cf8, transparent 70%);"></div>
            <div class="absolute top-1/3 -left-32 w-[420px] h-[420px] rounded-full opacity-30 blur-3xl" style="background: radial-gradient(circle, #c084fc, transparent 70%);"></div>
            <div class="absolute -bottom-24 left-1/3 w-[420px] h-[420px] rounded-full opacity-30 blur-3xl" style="background: radial-gradient(circle, #2dd4bf, transparent 70%);"></div>
        </div>

        <transition name="fade">
            <div v-if="isLoading" class="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/25 backdrop-blur-sm">
                <div class="loader"></div>
            </div>
        </transition>

        <ToastStack />

        <AuthShell v-if="!currentUser" />

        <template v-else>
            <Sidebar />
            <main class="flex-1 flex flex-col overflow-hidden relative">
                <TopHeader />
                <div v-if="loadError" class="mx-6 mt-4 flex items-center justify-between gap-4 rounded-2xl border border-rose-200 bg-rose-50/80 px-4 py-3 text-sm text-rose-600 backdrop-blur">
                    <span class="flex items-center gap-2 min-w-0">
                        <ion-icon name="alert-circle-outline" class="text-lg flex-shrink-0"></ion-icon>
                        <span class="truncate">{{ $t('toast.loadFailed') }}：{{ loadError }}</span>
                    </span>
                    <button @click="fetchAllData()" class="flex-shrink-0 px-3.5 py-1.5 rounded-full bg-rose-500 text-white text-xs font-semibold hover:brightness-110 transition">{{ $t('toast.retry') }}</button>
                </div>
                <div class="flex-1 overflow-x-hidden overflow-y-auto p-6">
                    <div class="max-w-6xl mx-auto pb-12">
                        <RouterView v-slot="{ Component }">
                            <transition name="fade" mode="out-in">
                                <component :is="Component" :key="currentPage" />
                            </transition>
                        </RouterView>
                    </div>
                </div>
            </main>
        </template>

        <Modal />
    </div>
</template>

<script setup lang="ts">
import { watch, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute } from 'vue-router'
import { useLms } from './lms.js'

import AuthShell from './components/AuthShell.vue'
import Sidebar from './components/Sidebar.vue'
import TopHeader from './components/TopHeader.vue'
import ToastStack from './components/ToastStack.vue'
import Modal from './components/Modal.vue'

const store = useLms()
const { currentUser, currentPage, isLoading, loadError } = storeToRefs(store)
// 动作可安全解构（Pinia setup store 的动作已绑定 store 实例）
const { fetchAllData } = store

// 路由 meta.page → 业务页 currentPage（保持 store 内部 watch(currentPage) 逻辑一致）
const route = useRoute()
watch(
    () => route.meta.page,
    (page) => {
        const pid = typeof page === 'string' ? page : ''
        if (pid && store.currentPage !== pid) store.currentPage = pid
    },
    { immediate: true }
)

// 会话持久化恢复：刷新/重开页面后如 currentUser 已由 Pinia 恢复，则重新拉取业务数据
onMounted(() => {
    if (store.currentUser) {
        store.fetchAllData()
    }
})
</script>

