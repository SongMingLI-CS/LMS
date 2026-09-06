<template>
<section v-if="currentPage === 'dashboard'">
                            <div class="glass-card rounded-3xl p-6 sm:p-8 mb-7 relative overflow-hidden">
                                <div class="absolute -right-16 -top-20 w-72 h-72 rounded-full opacity-30 blur-3xl pointer-events-none" style="background: radial-gradient(circle, #818cf8, transparent 70%);"></div>
                                <div class="absolute right-24 -bottom-24 w-56 h-56 rounded-full opacity-25 blur-3xl pointer-events-none" style="background: radial-gradient(circle, #e879f9, transparent 70%);"></div>
                                <div class="relative flex flex-col md:flex-row md:items-center justify-between gap-5">
                                    <div class="flex items-start gap-4">
                                        <div class="w-14 h-14 rounded-2xl bg-gradient-to-br from-indigo-500 via-violet-500 to-fuchsia-500 text-white flex items-center justify-center text-2xl shadow-glow flex-shrink-0">
                                            <ion-icon name="sparkles-outline"></ion-icon>
                                        </div>
                                        <div>
                                            <h2 class="text-xl font-extrabold text-slate-900">你好，{{ currentUser.name }}</h2>
                                            <p class="text-sm text-slate-500 mt-1">欢迎回到 LMS 智慧图书馆 · 祝您阅读愉快</p>
                                            <div class="mt-2.5 flex flex-wrap items-center gap-2">
                                                <span class="inline-flex items-center gap-1.5 text-xs font-medium px-2.5 py-1 rounded-full bg-indigo-50 text-indigo-600 border border-indigo-100">
                                                    <ion-icon name="shield-checkmark-outline"></ion-icon>{{ getRoleName(currentUser.role) }}
                                                </span>
                                                <span class="inline-flex items-center gap-1.5 text-xs font-medium px-2.5 py-1 rounded-full bg-emerald-50 text-emerald-600 border border-emerald-100">
                                                    <span class="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>系统运行正常
                                                </span>
                                            </div>
                                        </div>
                                    </div>
                                    <div class="hidden lg:flex flex-col items-end text-right">
                                        <p class="text-[11px] uppercase tracking-widest text-slate-400 font-semibold">Today</p>
                                        <p class="text-sm font-bold text-slate-700">{{ new Date().toLocaleDateString('zh-CN', { year:'numeric', month:'long', day:'numeric', weekday:'long' }) }}</p>
                                    </div>
                                </div>
                            </div>
                            <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-5 mb-7">
                                <div class="glass-card rounded-2xl p-5 relative overflow-hidden group">
                                    <div class="absolute -right-8 -top-8 w-28 h-28 rounded-full opacity-25 bg-gradient-to-br from-indigo-500 to-violet-500 blur-2xl group-hover:opacity-40 transition-opacity"></div>
                                    <div class="relative flex items-center justify-between">
                                        <div>
                                            <p class="text-xs font-medium text-slate-500">我的借阅</p>
                                            <p class="text-3xl font-extrabold text-slate-900 mt-1.5">{{ myBorrowCount }}</p>
                                        </div>
                                        <div class="w-11 h-11 rounded-xl bg-gradient-to-br from-indigo-500 to-violet-500 text-white flex items-center justify-center text-[22px] shadow-glow"><ion-icon name="book-outline"></ion-icon></div>
                                    </div>
                                </div>
                                <div class="glass-card rounded-2xl p-5 relative overflow-hidden group">
                                    <div class="absolute -right-8 -top-8 w-28 h-28 rounded-full opacity-25 bg-gradient-to-br from-amber-400 to-orange-500 blur-2xl group-hover:opacity-40 transition-opacity"></div>
                                    <div class="relative flex items-center justify-between">
                                        <div>
                                            <p class="text-xs font-medium text-slate-500">我的预约</p>
                                            <p class="text-3xl font-extrabold text-slate-900 mt-1.5">{{ myReservationCount }}</p>
                                        </div>
                                        <div class="w-11 h-11 rounded-xl bg-gradient-to-br from-amber-400 to-orange-500 text-white flex items-center justify-center text-[22px] shadow-glow"><ion-icon name="bookmark-outline"></ion-icon></div>
                                    </div>
                                </div>
                                <template v-if="currentUser.role !== 'user'">
                                    <div class="glass-card rounded-2xl p-5 relative overflow-hidden group">
                                        <div class="absolute -right-8 -top-8 w-28 h-28 rounded-full opacity-25 bg-gradient-to-br from-emerald-400 to-teal-500 blur-2xl group-hover:opacity-40 transition-opacity"></div>
                                        <div class="relative flex items-center justify-between">
                                            <div>
                                                <p class="text-xs font-medium text-slate-500">馆藏总数</p>
                                                <p class="text-3xl font-extrabold text-slate-900 mt-1.5">{{ totalBookCount }}</p>
                                            </div>
                                            <div class="w-11 h-11 rounded-xl bg-gradient-to-br from-emerald-400 to-teal-500 text-white flex items-center justify-center text-[22px] shadow-glow"><ion-icon name="library-outline"></ion-icon></div>
                                        </div>
                                    </div>
                                    <div class="glass-card rounded-2xl p-5 relative overflow-hidden group">
                                        <div class="absolute -right-8 -top-8 w-28 h-28 rounded-full opacity-25 bg-gradient-to-br from-rose-500 to-pink-500 blur-2xl group-hover:opacity-40 transition-opacity"></div>
                                        <div class="relative flex items-center justify-between">
                                            <div>
                                                <p class="text-xs font-medium text-slate-500">库存预警</p>
                                                <p class="text-3xl font-extrabold mt-1.5" :class="lowStockBooks.length>0 ? 'text-rose-500' : 'text-slate-900'">{{ lowStockBooks.length }}</p>
                                            </div>
                                            <div class="w-11 h-11 rounded-xl bg-gradient-to-br from-rose-500 to-pink-500 text-white flex items-center justify-center text-[22px] shadow-glow"><ion-icon name="alert-circle-outline"></ion-icon></div>
                                        </div>
                                    </div>
                                </template>
                            </div>
                            <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
                                <div class="lg:col-span-2 glass-card rounded-2xl p-6">
                                    <h3 class="text-base font-bold text-slate-900 mb-4 flex items-center gap-2"><span class="w-1 h-4 rounded bg-gradient-to-b from-indigo-500 to-fuchsia-500 inline-block"></span>系统公告</h3>
                                    <div class="text-sm text-slate-600 leading-relaxed p-5 bg-white/60 rounded-2xl border border-slate-200/70">
                                        欢迎回来，<strong>{{ currentUser.name }}</strong>。<br>
                                        您当前身份为 <span class="px-2 py-0.5 bg-indigo-50 text-indigo-600 border border-indigo-100 rounded-full text-xs font-semibold">{{ getRoleName(currentUser.role) }}</span>。<br>
                                        系统各项服务运行正常。如需帮助，请联系管理员。
                                    </div>
                                </div>
                                <div v-if="currentUser.role !== 'user' && lowStockBooks.length > 0" class="glass-card rounded-2xl p-6">
                                    <h3 class="text-base font-bold text-slate-900 mb-4 flex items-center gap-2"><span class="w-1 h-4 rounded bg-gradient-to-b from-rose-500 to-orange-400 inline-block"></span>补货提醒 <span class="ml-1 text-xs bg-rose-100 text-rose-600 px-2 py-0.5 rounded-full font-bold">{{ lowStockBooks.length }}</span></h3>
                                    <ul class="space-y-2.5">
                                        <li v-for="book in lowStockBooks.slice(0,5)" :key="book.id" class="flex justify-between items-center text-sm py-1.5 px-2 -mx-2 rounded-lg hover:bg-white/70 transition-colors">
                                            <span class="text-slate-600 truncate w-2/3">{{ book.title }}</span>
                                            <span class="font-mono text-rose-500 font-bold text-xs bg-rose-50 px-2 py-0.5 rounded-full">{{ book.available }} 本</span>
                                        </li>
                                    </ul>
                                </div>
                            </div>
                        </section>
</template>

<script>
import { inject } from 'vue'
export default {
  name: 'DashboardView',
  setup() {
    return { ...inject('lms') }
  }
}
</script>