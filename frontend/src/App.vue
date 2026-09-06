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
                <div class="flex-1 overflow-x-hidden overflow-y-auto p-6">
                    <transition name="fade" mode="out-in">
                        <div :key="currentPage" class="max-w-6xl mx-auto pb-12">
                            <component :is="currentViewComponent" />
                        </div>
                    </transition>
                </div>
            </main>
        </template>

        <Modal />
    </div>
</template>

<script setup>
import { provide, computed } from 'vue'
import { createLmsStore } from './lms.js'

import AuthShell from './components/AuthShell.vue'
import Sidebar from './components/Sidebar.vue'
import TopHeader from './components/TopHeader.vue'
import ToastStack from './components/ToastStack.vue'
import Modal from './components/Modal.vue'

import DashboardView from './views/DashboardView.vue'
import AnalysisView from './views/AnalysisView.vue'
import BooksView from './views/BooksView.vue'
import RecordsView from './views/RecordsView.vue'
import AuditLogsView from './views/AuditLogsView.vue'
import BorrowManageView from './views/BorrowManageView.vue'
import ProfileView from './views/ProfileView.vue'
import UsersView from './views/UsersView.vue'

const store = createLmsStore()
provide('lms', store)

const currentUser = store.currentUser
const currentPage = store.currentPage
const isLoading = store.isLoading

const viewMap = {
    dashboard: DashboardView,
    analysis: AnalysisView,
    manageBooks: BooksView,
    records: RecordsView,
    auditLogs: AuditLogsView,
    manageBorrow: BorrowManageView,
    profile: ProfileView,
    manageUsers: UsersView
}
const currentViewComponent = computed(() => viewMap[currentPage.value] || DashboardView)
</script>

