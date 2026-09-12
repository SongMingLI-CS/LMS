<template>
<section v-if="currentPage === 'manageUsers'">
                            <div class="glass-card rounded-2xl p-4 mb-5 flex flex-col md:flex-row gap-4 md:items-center justify-between">
                                <div class="relative flex-1 max-w-md">
                                    <ion-icon name="search" class="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400"></ion-icon>
                                    <input v-model="userSearchQuery" :placeholder="$t('users.searchPh')" class="w-full pl-10 pr-4 py-2.5 bg-white border border-slate-200 rounded-xl outline-none">
                                </div>
                                <button @click="openUserModal(null)" class="btn-primary px-4 py-2.5 rounded-xl text-sm font-semibold flex items-center gap-1.5 self-start md:self-auto"><ion-icon name="person-add-outline"></ion-icon>{{ $t('users.add') }}</button>
                            </div>
                            <div class="glass-card rounded-2xl overflow-hidden">
                                <div class="overflow-x-auto"><table class="w-full text-left text-sm"><thead class="bg-slate-100/70 text-slate-500 uppercase text-xs"><tr><th class="px-6 py-3.5">{{ $t('users.colUsername') }}</th><th class="px-6 py-3.5">{{ $t('users.colName') }}</th><th class="px-6 py-3.5">{{ $t('users.colRole') }}</th><th class="px-6 py-3.5 text-right">{{ $t('users.colActions') }}</th></tr></thead><tbody class="divide-y divide-slate-100"><tr v-for="u in filteredUsers" :key="u.id" class="hover:bg-indigo-50/40 transition-colors"><td class="px-6 py-3.5 font-mono text-slate-700">{{ u.username }}</td><td class="px-6 py-3.5"><span class="inline-flex items-center gap-2.5"><span class="w-8 h-8 rounded-lg bg-gradient-to-br from-indigo-400 to-fuchsia-500 text-white text-xs font-bold flex items-center justify-center">{{ (u.name||'?').charAt(0) }}</span><span class="font-semibold text-slate-800">{{ u.name }}</span></span></td><td class="px-6 py-3.5"><span class="px-2.5 py-1 rounded-full text-xs font-semibold" :class="u.role==='superadmin' ? 'bg-violet-50 text-violet-600' : u.role==='admin' ? 'bg-indigo-50 text-indigo-600' : 'bg-slate-100 text-slate-600'">{{ $t(roleLabelKey(u.role)) }}</span></td><td class="px-6 py-3.5 text-right whitespace-nowrap"><button @click="openUserModal(u)" class="px-3 py-1.5 rounded-full bg-indigo-50 text-indigo-600 text-xs font-semibold hover:bg-indigo-100 transition-colors mr-2">{{ $t('users.edit') }}</button><button @click="handleDeleteUser(u)" class="px-3 py-1.5 rounded-full bg-rose-50 text-rose-500 text-xs font-semibold hover:bg-rose-100 transition-colors">{{ $t('users.delete') }}</button></td></tr><tr v-if="filteredUsers.length===0"><td colspan="4" class="px-6 py-14 text-center text-slate-400 text-sm">{{ $t('users.empty') }}</td></tr></tbody></table></div>
                                <div class="px-6 py-4 border-t border-slate-200/80 flex items-center justify-between bg-white/40">
                                    <button @click="loadUsers(userPage-1)" :disabled="userPage<=0 || isLoading" class="px-4 py-1.5 rounded-full text-xs font-semibold bg-white border border-slate-200 text-slate-600 hover:border-indigo-300 hover:text-indigo-600 transition-colors disabled:opacity-40 disabled:pointer-events-none">{{ $t('audit.prev') }}</button>
                                    <span class="text-xs text-slate-500 font-medium">{{ $t('common.pageLabel', { page: userPage+1, total: userTotalPages }) }} · {{ $t('common.unitRecords', { n: userTotalElements }) }}</span>
                                    <button @click="loadUsers(userPage+1)" :disabled="userPage>=userTotalPages-1 || isLoading" class="px-4 py-1.5 rounded-full text-xs font-semibold bg-white border border-slate-200 text-slate-600 hover:border-indigo-300 hover:text-indigo-600 transition-colors disabled:opacity-40 disabled:pointer-events-none">{{ $t('audit.next') }}</button>
                                </div>
                            </div>
                        </section>
</template>

<script lang="ts">
import { useLms } from '../lms'
import { bindStore } from '../utils/store-bindings'
import { roleLabelKey } from '../utils/helpers'
export default {
  name: 'UsersView',
  setup() {
    const s = useLms()
    return { ...bindStore(s), roleLabelKey }
  }
}
</script>
