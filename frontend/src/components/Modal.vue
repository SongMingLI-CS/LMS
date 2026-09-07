<template>
<transition name='fade'>
<div v-if="isModalOpen" class="fixed inset-0 z-[70] flex items-center justify-center bg-[#0b1024]/55 backdrop-blur-md" @click.self="closeModal">
            <div class="glass-modal rounded-2xl w-full max-w-lg p-6 sm:p-7 m-4 max-h-[90vh] overflow-y-auto relative">
                <button @click="closeModal" class="absolute top-4 right-4 w-8 h-8 inline-flex items-center justify-center rounded-full bg-slate-100 text-slate-500 hover:bg-rose-50 hover:text-rose-500 transition-colors text-lg"><ion-icon name="close"></ion-icon></button>

                <div v-if="modalType === 'book'">
                    <div class="flex items-center gap-3 mb-5">
                        <span class="w-10 h-10 rounded-xl bg-gradient-to-br from-indigo-500 to-fuchsia-500 text-white flex items-center justify-center text-xl shadow-glow flex-shrink-0"><ion-icon :name="currentBook.id ? 'create-outline' : 'add-circle-outline'"></ion-icon></span>
                        <div>
                            <h3 class="text-lg font-extrabold text-slate-900">{{ currentBook.id ? '编辑图书' : '新书入库' }}</h3>
                            <p class="text-xs text-slate-400 mt-0.5">{{ currentBook.id ? '更新馆藏图书基本信息' : '录入新书，系统将自动生成馆藏单册' }}</p>
                        </div>
                    </div>
                    <form @submit.prevent="handleSaveBook" class="space-y-4">
                        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                            <div class="sm:col-span-2">
                                <label class="block text-xs font-semibold text-slate-500 mb-1.5">书名 <span class="text-rose-400">*</span></label>
                                <input v-model="currentBook.title" placeholder="输入图书完整名称" class="w-full px-3.5 py-2.5 border rounded-xl" required>
                            </div>
                            <div>
                                <label class="block text-xs font-semibold text-slate-500 mb-1.5">作者 <span class="text-rose-400">*</span></label>
                                <input v-model="currentBook.author" placeholder="作者姓名" class="w-full px-3.5 py-2.5 border rounded-xl" required>
                            </div>
                            <div>
                                <label class="block text-xs font-semibold text-slate-500 mb-1.5">ISBN <span class="text-rose-400">*</span></label>
                                <input v-model="currentBook.isbn" placeholder="如 978-7-…" class="w-full px-3.5 py-2.5 border rounded-xl font-mono" required>
                            </div>
                            <div>
                                <label class="block text-xs font-semibold text-slate-500 mb-1.5">库存</label>
                                <input type="number" v-model.number="currentBook.stock" min="0" class="w-full px-3.5 py-2.5 border rounded-xl" :disabled="!!currentBook.id">
                            </div>
                            <div>
                                <label class="block text-xs font-semibold text-slate-500 mb-1.5">可借数量</label>
                                <input type="number" v-model.number="currentBook.available" min="0" class="w-full px-3.5 py-2.5 border rounded-xl" :disabled="!!currentBook.id">
                            </div>
                            <div class="sm:col-span-2">
                                <label class="block text-xs font-semibold text-slate-500 mb-1.5">封面图片 URL</label>
                                <input v-model="currentBook.cover" placeholder="https://…（选填）" class="w-full px-3.5 py-2.5 border rounded-xl">
                            </div>
                        </div>
                        <button class="btn-primary w-full mt-2 py-3 rounded-xl font-semibold flex items-center justify-center gap-2"><ion-icon name="checkmark-circle-outline"></ion-icon>保存</button>
                    </form>
                </div>

                <div v-if="modalType === 'user'">
                    <div class="flex items-center gap-3 mb-5">
                        <span class="w-10 h-10 rounded-xl bg-gradient-to-br from-emerald-400 to-teal-500 text-white flex items-center justify-center text-xl shadow-glow flex-shrink-0"><ion-icon :name="currentUserForm.id ? 'create-outline' : 'person-add-outline'"></ion-icon></span>
                        <div>
                            <h3 class="text-lg font-extrabold text-slate-900">{{ currentUserForm.id ? '编辑用户' : '添加用户' }}</h3>
                            <p class="text-xs text-slate-400 mt-0.5">管理用户账号与系统角色</p>
                        </div>
                    </div>
                    <form @submit.prevent="handleSaveUser" class="space-y-4">
                        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                            <div>
                                <label class="block text-xs font-semibold text-slate-500 mb-1.5">用户名 <span class="text-rose-400">*</span></label>
                                <input v-model="currentUserForm.username" placeholder="登录账号" class="w-full px-3.5 py-2.5 border rounded-xl" required>
                            </div>
                            <div>
                                <label class="block text-xs font-semibold text-slate-500 mb-1.5">姓名 <span class="text-rose-400">*</span></label>
                                <input v-model="currentUserForm.name" placeholder="真实姓名" class="w-full px-3.5 py-2.5 border rounded-xl" required>
                            </div>
                            <div class="sm:col-span-2">
                                <label class="block text-xs font-semibold text-slate-500 mb-1.5">邮箱 <span class="text-rose-400">*</span></label>
                                <input type="email" v-model="currentUserForm.email" placeholder="user@example.com" class="w-full px-3.5 py-2.5 border rounded-xl" required>
                            </div>
                            <div>
                                <label class="block text-xs font-semibold text-slate-500 mb-1.5">初始密码</label>
                                <input type="password" v-model="currentUserForm.password" placeholder="留空则不修改" class="w-full px-3.5 py-2.5 border rounded-xl">
                            </div>
                            <div>
                                <label class="block text-xs font-semibold text-slate-500 mb-1.5">角色</label>
                                <select v-model="currentUserForm.role" class="w-full px-3.5 py-2.5 border rounded-xl bg-white"><option value="user">用户</option><option value="admin">管理员</option><option value="superadmin">超级管理员</option></select>
                            </div>
                        </div>
                        <button class="btn-primary w-full mt-2 py-3 rounded-xl font-semibold flex items-center justify-center gap-2"><ion-icon name="checkmark-circle-outline"></ion-icon>保存</button>
                    </form>
                </div>

                <div v-if="modalType === 'importBooks'">
                    <div class="flex items-center gap-3 mb-5">
                        <span class="w-10 h-10 rounded-xl bg-gradient-to-br from-sky-400 to-indigo-500 text-white flex items-center justify-center text-xl shadow-glow flex-shrink-0"><ion-icon name="cloud-upload-outline"></ion-icon></span>
                        <div>
                            <h3 class="text-lg font-extrabold text-slate-900">批量导入</h3>
                            <p class="text-xs text-slate-400 mt-0.5">上传 .xlsx 模板批量新增图书</p>
                        </div>
                    </div>
                    <div v-if="importResults.success || importResults.failed" class="mb-4 p-4 bg-slate-50 border border-slate-200 rounded-xl text-sm">
                        <div class="flex items-center gap-3">
                            <span class="inline-flex items-center gap-1 text-emerald-600 font-bold"><span class="w-2 h-2 rounded-full bg-emerald-500"></span>成功 {{ importResults.success }}</span>
                            <span class="inline-flex items-center gap-1 text-rose-500 font-bold"><span class="w-2 h-2 rounded-full bg-rose-500"></span>失败 {{ importResults.failed }}</span>
                        </div>
                        <ul v-if="importResults.errors.length" class="mt-2.5 space-y-1 list-disc pl-4 text-rose-500 text-xs"><li v-for="(e, idx) in importResults.errors" :key="idx">{{ e }}</li></ul>
                    </div>
                    <div class="rounded-2xl border-2 border-dashed border-slate-200 bg-slate-50/60 p-6 text-center hover:border-indigo-300 transition-colors">
                        <ion-icon name="document-attach-outline" class="text-3xl text-slate-300 mb-2"></ion-icon>
                        <input type="file" @change="handleFileSelect" accept=".xlsx" class="block w-full text-sm text-slate-500 file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-indigo-50 file:text-indigo-600 hover:file:bg-indigo-100"/>
                        <p class="text-[11px] text-slate-400 mt-2">支持 .xlsx 格式的图书模板文件</p>
                    </div>
                    <button @click="handleUploadBooks" :disabled="!selectedFile" class="btn-primary w-full mt-4 py-3 rounded-xl font-semibold flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"><ion-icon name="cloud-upload-outline"></ion-icon>开始上传</button>
                </div>

                <div v-if="modalType === 'password'">
                    <div class="flex items-center gap-3 mb-5">
                        <span class="w-10 h-10 rounded-xl bg-gradient-to-br from-rose-400 to-pink-500 text-white flex items-center justify-center text-xl shadow-glow flex-shrink-0"><ion-icon name="key-outline"></ion-icon></span>
                        <div>
                            <h3 class="text-lg font-extrabold text-slate-900">修改密码</h3>
                            <p class="text-xs text-slate-400 mt-0.5">建议定期更换密码以保护账户安全</p>
                        </div>
                    </div>
                    <form @submit.prevent="handleChangePassword" class="space-y-4">
                        <input type="password" v-model="passwordForm.oldPassword" placeholder="当前密码" class="w-full px-3.5 py-2.5 border rounded-xl">
                        <input type="password" v-model="passwordForm.newPassword" placeholder="新密码" class="w-full px-3.5 py-2.5 border rounded-xl">
                        <input type="password" v-model="passwordForm.confirmPassword" placeholder="确认新密码" class="w-full px-3.5 py-2.5 border rounded-xl">
                        <button class="btn-primary w-full py-3 rounded-xl font-semibold flex items-center justify-center gap-2"><ion-icon name="shield-checkmark-outline"></ion-icon>确认修改</button>
                    </form>
                </div>
            </div>
        </div>
</transition>
</template>

<script lang="ts">
import { storeToRefs } from 'pinia'
import { useLms } from '../lms'
export default {
  name: 'Modal',
  setup() {
    const s = useLms()
    return { ...s, ...storeToRefs(s) }
  }
}
</script>
