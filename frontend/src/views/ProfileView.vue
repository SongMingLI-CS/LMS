<template>
<section v-if="currentPage === 'profile'">
                            <div class="max-w-2xl mx-auto glass-card rounded-2xl overflow-hidden">
                                <div class="p-7 sm:p-8 bg-gradient-to-r from-indigo-500/10 via-violet-500/10 to-fuchsia-500/10 border-b border-white/60 flex items-center gap-5">
                                    <div class="w-16 h-16 rounded-2xl bg-gradient-to-br from-indigo-500 via-violet-500 to-fuchsia-500 text-white flex items-center justify-center text-2xl font-bold shadow-glow ring-2 ring-white/60">{{ currentUser.name.charAt(0) }}</div>
                                    <div>
                                        <h3 class="text-lg font-extrabold text-slate-900">账户设置</h3>
                                        <p class="text-sm text-slate-500 mt-0.5">{{ currentUser.username }} · {{ getRoleName(currentUser.role) }}</p>
                                    </div>
                                </div>
                                <div class="p-7 sm:p-8 space-y-8">
                                    <form @submit.prevent="handleUpdateProfile">
                                        <label class="block text-xs font-semibold text-slate-500 uppercase tracking-wider mb-2">显示名称</label>
                                        <div class="flex gap-3">
                                            <input v-model="profileForm.name" class="flex-1 px-4 py-2.5 border rounded-xl" placeholder="输入你的显示名称">
                                            <button class="btn-primary px-5 py-2.5 rounded-xl text-sm font-semibold flex items-center gap-1.5"><ion-icon name="save-outline"></ion-icon>保存</button>
                                        </div>
                                    </form>
                                    <div class="pt-6 border-t border-slate-200/70">
                                        <h4 class="font-semibold text-slate-800 mb-1 flex items-center gap-2"><ion-icon name="shield-checkmark-outline" class="text-emerald-500"></ion-icon>安全</h4>
                                        <p class="text-xs text-slate-400 mb-3">定期更换密码可更好地保护你的账户。</p>
                                        <button @click="modalType='password'; isModalOpen=true" class="px-4 py-2 rounded-xl text-sm font-semibold bg-white border border-slate-200 text-slate-600 hover:border-indigo-300 hover:text-indigo-600 transition-colors inline-flex items-center gap-1.5"><ion-icon name="key-outline"></ion-icon>修改登录密码</button>
                                    </div>
                                </div>
                            </div>
                        </section>
</template>

<script>
import { inject } from 'vue'
export default {
  name: 'ProfileView',
  setup() {
    return { ...inject('lms') }
  }
}
</script>