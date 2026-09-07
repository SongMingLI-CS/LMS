<template>
<section v-if="currentPage === 'manageBorrow'">
                            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                                <div class="glass-card rounded-2xl p-6">
                                    <h3 class="font-bold text-slate-900 mb-5 flex items-center gap-3"><span class="w-9 h-9 rounded-xl bg-gradient-to-br from-emerald-400 to-teal-500 text-white flex items-center justify-center text-lg shadow-glow"><ion-icon name="arrow-undo-outline"></ion-icon></span>图书归还</h3>
                                    <div class="space-y-3">
                                        <input v-model="returnBookId" placeholder="ISBN / 图书 ID" class="w-full px-4 py-2.5 border rounded-xl" required>
                                        <input v-model="returnUserId" placeholder="用户 ID（选填）" class="w-full px-4 py-2.5 border rounded-xl">
                                        <button @click="handleReturn" class="btn-primary w-full py-2.5 rounded-xl font-semibold flex items-center justify-center gap-2"><ion-icon name="checkmark-circle-outline"></ion-icon>确认归还</button>
                                    </div>
                                    <p class="text-xs text-slate-400 mt-4 leading-relaxed">支持按 ISBN 或图书 ID 归还；管理员可将书籍归还到指定读者账户。</p>
                                </div>
                                <div class="glass-card rounded-2xl p-6">
                                    <h3 class="font-bold text-slate-900 mb-5 flex items-center gap-3"><span class="w-9 h-9 rounded-xl bg-gradient-to-br from-amber-400 to-orange-500 text-white flex items-center justify-center text-lg shadow-glow"><ion-icon name="bookmark-outline"></ion-icon></span>预约处理</h3>
                                    <ul class="space-y-2.5">
                                        <li v-for="res in pendingReservations" :key="res.id" class="flex justify-between items-center p-3 bg-white/60 rounded-xl border border-slate-200/70">
                                            <div class="min-w-0 mr-3">
                                                <p class="text-sm font-semibold text-slate-800 truncate">{{ getBookById(res.bookId)?.title || '未知图书' }}</p>
                                                <p class="text-xs text-slate-500 mt-0.5 truncate">预约人：{{ getUserById(res.userId)?.name || ('用户 #' + res.userId) }}</p>
                                            </div>
                                            <button @click="handleProcessReservation(res)" class="px-3.5 py-1.5 rounded-full bg-gradient-to-r from-emerald-500 to-teal-500 text-white text-xs font-semibold hover:brightness-110 transition flex-shrink-0">批准</button>
                                        </li>
                                        <li v-if="pendingReservations.length===0" class="text-slate-400 text-center text-sm py-6 bg-white/40 rounded-xl">暂无待办预约 🎉</li>
                                    </ul>
                                </div>
                            </div>
                        </section>
</template>

<script>
import { storeToRefs } from 'pinia'
import { useLms } from '../lms'
export default {
  name: 'BorrowManageView',
  setup() {
    const s = useLms()
    return { ...s, ...storeToRefs(s) }
  }
}
</script>
