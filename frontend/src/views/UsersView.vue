<template>
<section v-if="currentPage === 'manageUsers'">
                            <div class="glass-card rounded-2xl p-4 mb-5 flex flex-col md:flex-row gap-4 md:items-center justify-between">
                                <div class="relative flex-1 max-w-md">
                                    <ion-icon name="search" class="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400"></ion-icon>
                                    <input v-model="userSearchQuery" placeholder="搜索用户名 / 姓名..." class="w-full pl-10 pr-4 py-2.5 bg-white border border-slate-200 rounded-xl outline-none">
                                </div>
                                <button @click="openUserModal(null)" class="btn-primary px-4 py-2.5 rounded-xl text-sm font-semibold flex items-center gap-1.5 self-start md:self-auto"><ion-icon name="person-add-outline"></ion-icon>添加用户</button>
                            </div>
                            <div class="glass-card rounded-2xl overflow-hidden">
                                <div class="overflow-x-auto"><table class="w-full text-left text-sm"><thead class="bg-slate-100/70 text-slate-500 uppercase text-xs"><tr><th class="px-6 py-3.5">账号</th><th class="px-6 py-3.5">姓名</th><th class="px-6 py-3.5">角色</th><th class="px-6 py-3.5 text-right">操作</th></tr></thead><tbody class="divide-y divide-slate-100"><tr v-for="u in filteredUsers" :key="u.id" class="hover:bg-indigo-50/40 transition-colors"><td class="px-6 py-3.5 font-mono text-slate-700">{{ u.username }}</td><td class="px-6 py-3.5"><span class="inline-flex items-center gap-2.5"><span class="w-8 h-8 rounded-lg bg-gradient-to-br from-indigo-400 to-fuchsia-500 text-white text-xs font-bold flex items-center justify-center">{{ (u.name||'?').charAt(0) }}</span><span class="font-semibold text-slate-800">{{ u.name }}</span></span></td><td class="px-6 py-3.5"><span class="px-2.5 py-1 rounded-full text-xs font-semibold" :class="u.role==='superadmin' ? 'bg-violet-50 text-violet-600' : u.role==='admin' ? 'bg-indigo-50 text-indigo-600' : 'bg-slate-100 text-slate-600'">{{ getRoleName(u.role) }}</span></td><td class="px-6 py-3.5 text-right whitespace-nowrap"><button @click="openUserModal(u)" class="px-3 py-1.5 rounded-full bg-indigo-50 text-indigo-600 text-xs font-semibold hover:bg-indigo-100 transition-colors mr-2">编辑</button><button @click="handleDeleteUser(u)" class="px-3 py-1.5 rounded-full bg-rose-50 text-rose-500 text-xs font-semibold hover:bg-rose-100 transition-colors">删除</button></td></tr><tr v-if="filteredUsers.length===0"><td colspan="4" class="px-6 py-14 text-center text-slate-400 text-sm">未找到匹配的用户</td></tr></tbody></table></div>
                            </div>
                        </section>
</template>

<script lang="ts">
import { storeToRefs } from 'pinia'
import { useLms } from '../lms'
export default {
  name: 'UsersView',
  setup() {
    const s = useLms()
    return { ...s, ...storeToRefs(s) }
  }
}
</script>
