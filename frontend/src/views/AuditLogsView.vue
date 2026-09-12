<template>
<section v-if="currentPage === 'auditLogs'">
                            <div class="glass-card rounded-2xl overflow-hidden">
                                <div class="px-6 py-4 border-b border-slate-200/80 flex items-center justify-between">
                                    <h3 class="font-bold text-slate-900 flex items-center gap-2"><span class="w-1 h-4 rounded bg-gradient-to-b from-indigo-500 to-fuchsia-500 inline-block"></span>{{ $t('audit.title') }}</h3>
                                    <span class="inline-flex items-center gap-1 text-xs px-2.5 py-1 rounded-full bg-violet-50 text-violet-600 font-semibold"><ion-icon name="shield-checkmark-outline"></ion-icon>{{ $t('audit.onlySuper') }}</span>
                                </div>
                                <div class="overflow-x-auto">
                                <table class="w-full text-left text-sm"><thead class="bg-slate-100/70 text-slate-500"><tr><th class="px-6 py-3.5">{{ $t('audit.colTime') }}</th><th class="px-6 py-3.5">{{ $t('audit.colUser') }}</th><th class="px-6 py-3.5">{{ $t('audit.colAction') }}</th><th class="px-6 py-3.5">{{ $t('audit.colDetail') }}</th></tr></thead><tbody class="divide-y divide-slate-100"><tr v-for="l in auditLogs" :key="l.id" class="hover:bg-indigo-50/40 transition-colors"><td class="px-6 py-3.5 text-slate-500">{{ new Date(l.timestamp).toLocaleString() }}</td><td class="px-6 py-3.5 font-semibold text-slate-800">{{ l.username }}</td><td class="px-6 py-3.5"><span class="bg-indigo-50 text-indigo-600 px-2.5 py-1 rounded-full text-xs font-semibold">{{ l.action }}</span></td><td class="px-6 py-3.5 text-slate-500">{{ l.details }}</td></tr><tr v-if="auditLogs.length===0"><td colspan="4" class="px-6 py-14 text-center text-slate-400 text-sm">{{ $t('audit.empty') }}</td></tr></tbody></table>
                                </div>
                                <div class="px-6 py-4 border-t border-slate-200/80 flex items-center justify-between bg-white/40">
                                    <button @click="fetchAuditLogs(auditLogPage-1)" :disabled="auditLogPage<=0" class="px-4 py-1.5 rounded-full text-xs font-semibold bg-white border border-slate-200 text-slate-600 hover:border-indigo-300 hover:text-indigo-600 transition-colors disabled:opacity-40 disabled:pointer-events-none">{{ $t('audit.prev') }}</button>
                                    <span class="text-xs text-slate-500 font-medium">{{ $t('common.pageLabel', { page: auditLogPage+1, total: auditLogTotalPages }) }}</span>
                                    <button @click="fetchAuditLogs(auditLogPage+1)" :disabled="auditLogPage>=auditLogTotalPages-1" class="px-4 py-1.5 rounded-full text-xs font-semibold bg-white border border-slate-200 text-slate-600 hover:border-indigo-300 hover:text-indigo-600 transition-colors disabled:opacity-40 disabled:pointer-events-none">{{ $t('audit.next') }}</button>
                                </div>
                            </div>
                        </section>
</template>

<script lang="ts">
import { useLms } from '../lms'
import { bindStore } from '../utils/store-bindings'
export default {
  name: 'AuditLogsView',
  setup() {
    const s = useLms()
    return { ...bindStore(s) }
  }
}
</script>
