<template>
<section v-if="currentPage === 'records'">
                            <div class="glass-card rounded-2xl overflow-hidden">
                                <div class="px-6 py-4 border-b border-slate-200/80 flex items-center justify-between">
                                    <h3 class="font-bold text-slate-900 flex items-center gap-2"><span class="w-1 h-4 rounded bg-gradient-to-b from-indigo-500 to-fuchsia-500 inline-block"></span>{{ $t('records.title') }}</h3>
                                    <span class="text-xs px-2.5 py-1 rounded-full bg-indigo-50 text-indigo-600 font-semibold">{{ $t('common.unitRecords', { n: filteredRecords.length }) }}</span>
                                </div>
                                <div class="overflow-x-auto">
                                <table class="w-full text-left text-sm">
                                    <thead class="bg-slate-100/70 text-slate-500 uppercase text-xs"><tr><th class="px-6 py-3.5">{{ $t('records.colBook') }}</th><th class="px-6 py-3.5">{{ $t('records.colBorrowed') }}</th><th class="px-6 py-3.5">{{ $t('records.colDue') }}</th><th class="px-6 py-3.5">{{ $t('records.colStatus') }}</th><th class="px-6 py-3.5 text-right">{{ $t('records.colActions') }}</th></tr></thead>
                                    <tbody class="divide-y divide-slate-100">
                                    <tr v-for="r in filteredRecords" :key="r.id" class="hover:bg-indigo-50/40 transition-colors">
                                        <td class="px-6 py-3.5 font-semibold text-slate-800">{{ r.bookTitle }}</td>
                                        <td class="px-6 py-3.5 text-slate-500">{{ r.borrowDate }}</td>
                                        <td class="px-6 py-3.5 text-slate-500">{{ r.dueDate }}</td>
                                        <td class="px-6 py-3.5"><span :class="['px-2.5 py-1 rounded-full text-xs font-semibold', r.status==='borrowed' ? (isOverdue(r.dueDate) ? 'bg-rose-50 text-rose-500' : 'bg-indigo-50 text-indigo-600') : r.status==='returned' ? 'bg-emerald-50 text-emerald-600' : r.status==='reserved' ? 'bg-amber-50 text-amber-600' : r.status==='awaiting_pickup' ? 'bg-violet-50 text-violet-600' : 'bg-slate-100 text-slate-600']">{{ statusLabel(r.status) }}</span></td>
                                        <td class="px-6 py-3.5 text-right"><button v-if="r.status==='borrowed' && currentUser.role==='user'" @click="handleRenew(r)" class="px-3 py-1.5 rounded-full bg-indigo-50 text-indigo-600 text-xs font-semibold hover:bg-indigo-100 transition-colors">{{ $t('records.renew') }}</button></td>
                                    </tr>
                                    <tr v-if="filteredRecords.length===0"><td colspan="5" class="px-6 py-14 text-center text-slate-400 text-sm">{{ $t('records.empty') }}</td></tr>
                                    </tbody>
                                </table>
                                </div>
                            </div>
                        </section>
</template>

<script lang="ts">
import { storeToRefs } from 'pinia'
import { useLms } from '../lms'
export default {
  name: 'RecordsView',
  setup() {
    const s = useLms()
    return { ...s, ...storeToRefs(s) }
  }
}
</script>
