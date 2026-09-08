import { ref, reactive, computed, watch } from 'vue'
import { defineStore } from 'pinia'
import { api, SESSION_KEY } from './api/http.js'
import { getCleanRole, getRoleName, isOverdue, statusLabel } from './utils/helpers.js'
import { t as translate } from './i18n'

export const useLms = defineStore('lms', () => {
            // 1. 状态定义
            const currentUser = ref(null);
            const loginForm = reactive({ username: '', password: '' });
            const loginError = ref('');
            const currentPage = ref('dashboard');
            const isSidebarOpen = ref(true);
            const toasts = ref([]);
            const isLoading = ref(false);

            // 2. 业务数据
            const books = ref([]);
            const users = ref([]);
            const borrowRecords = ref([]);
            const popularBooks = ref([]);
            const activeUsers = ref([]);
            const categoryStats = ref([]);
            const peakTimeStats = ref([]);
            const overdueUsers = ref([]);
            const stagnantBooks = ref([]);
            const lowStockBooks = ref([]);
            const auditLogs = ref([]);
            const auditLogPage = ref(0);
            const auditLogTotalPages = ref(1);

            // 3. UI 控制
            const isModalOpen = ref(false);
            const modalType = ref('');
            const bookSearchQuery = ref('');
            const userSearchQuery = ref('');
            const returnBookId = ref('');
            const returnUserId = ref('');

            // 4. 表单数据
            const defaultBook = { id: null, title: '', author: '', isbn: '', stock: 1, available: 1, cover: '', category: '', publisher: '', price: 0, introduction: '' };
            const currentBook = reactive({ ...defaultBook });
            const currentUserForm = reactive({ id: null, username: '', name: '', email: '', role: 'user', password: '' });
            const registerForm = reactive({ username: '', name: '', password: '', confirmPassword: '', email: '' });
            const resetForm = reactive({ email: '', code: '', newPassword: '', codeSent: false });
            const profileForm = reactive({ name: '' });
            const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' });

            // 5. 临时流程状态
            const isRegistering = ref(false);
            const isVerificationStep = ref(false);
            const isForgotPassword = ref(false);
            const verificationCode = ref('');
            const registerError = ref('');
            const registerSuccess = ref('');
            const usernameToVerify = ref('');
            const selectedFile = ref(null);
            const importResults = reactive({ success: 0, failed: 0, errors: [] });
            const chartColors = ['#3b82f6', '#10b981', '#f59e0b', '#ec4899', '#8b5cf6', '#6b7280'];

            // 6. API 实例与 401 静默刷新拦截（抽离至 api/http.js，模块级单例）

            // 7. 辅助函数
            let toastId = 0;
            const showMessage = (title, text, type = 'success') => {
                const id = toastId++;
                toasts.value.push({ id, title, text, type });
                setTimeout(() => toasts.value = toasts.value.filter(t => t.id !== id), 3000);
            };
            const clearErrors = () => { loginError.value=''; registerError.value=''; };
            // 角色/状态/逾期等纯函数已抽离至 utils/helpers.js（见文件顶部 import）
            const closeModal = () => { isModalOpen.value=false; selectedFile.value=null; };
            const openBookModal = (b) => { Object.assign(currentBook, b || defaultBook); if(!b) currentBook.id=null; modalType.value='book'; isModalOpen.value=true; };
            const openUserModal = (u) => { Object.assign(currentUserForm, u || {role:'user'}); if(!u) currentUserForm.id=null; modalType.value='user'; isModalOpen.value=true; };
            const handleFileSelect = (e) => selectedFile.value = e.target.files[0];
            const clearBookSearch = () => bookSearchQuery.value = '';

            const getBookById = (id) => books.value.find(b => b.id === id);
            const getUserById = (id) => users.value.find(u => u.id === id);
            const isBookBorrowedByUser = (b) => borrowRecords.value.some(r => r.bookId === b.id && r.userId === currentUser.value.id && r.status === 'borrowed');
            const isBookReservedByUser = (b) => borrowRecords.value.some(r => r.bookId === b.id && r.userId === currentUser.value.id && (r.status === 'reserved' || r.status === 'awaiting_pickup'));

            // 8. 原生CSS图表逻辑 (绝对稳定)
            const pieChartStyle = computed(() => {
                if (!categoryStats.value.length) return 'gray';
                const total = categoryStats.value.reduce((acc, cur) => acc + cur.count, 0);
                let currentAngle = 0;
                const segments = categoryStats.value.map((cat, i) => {
                    const start = currentAngle;
                    const end = currentAngle + (cat.count / total) * 360;
                    currentAngle = end;
                    return `${chartColors[i % chartColors.length]} ${start}deg ${end}deg`;
                });
                return `conic-gradient(${segments.join(', ')})`;
            });

            // 9. 数据获取
            const fetchAllData = async () => {
                isLoading.value = true;
                try {
                    await Promise.all([
                        api.get('/api/books').then(r => books.value = r.data),
                        api.get('/api/records').then(r => borrowRecords.value = r.data),
                        getCleanRole(currentUser.value?.role)!=='user' ? api.get('/api/users').then(r => users.value = r.data) : null,
                        getCleanRole(currentUser.value?.role)!=='user' ? api.get('/api/books/low-stock').then(r => lowStockBooks.value = r.data) : null,
                        api.get('/api/records/analysis/popular-books').then(r => popularBooks.value = r.data).catch(()=>{}),
                        api.get('/api/records/analysis/active-users').then(r => activeUsers.value = r.data).catch(()=>{}),
                        api.get('/api/books/analysis/categories').then(r => categoryStats.value = r.data).catch(()=>{}),
                        api.get('/api/records/analysis/peak-times').then(r => peakTimeStats.value = r.data).catch(()=>{}),
                        api.get('/api/records/analysis/overdue-users').then(r => overdueUsers.value = r.data).catch(()=>{}),
                        api.get('/api/books/analysis/stagnant-books').then(r => stagnantBooks.value = r.data).catch(()=>{})
                    ]);
                } catch(e) { console.error(e); }
                finally { isLoading.value = false; }
            };

            const fetchAuditLogs = async (page=0) => {
                if(getCleanRole(currentUser.value?.role)!=='superadmin') return;
                isLoading.value = true;
                try {
                    const res = await api.get(`/api/audit-logs?page=${page}&size=20`);
                    auditLogs.value = res.data.content;
                    auditLogPage.value = res.data.number;
                    auditLogTotalPages.value = res.data.totalPages;
                } catch(e){} finally { isLoading.value = false; }
            };
            watch(currentPage, (v) => { if(v==='auditLogs') fetchAuditLogs(0); });

            // 10. 业务操作
            const handleLogin = async () => {
                isLoading.value = true; loginError.value = '';
                try {
                    const res = await api.post('/login', loginForm);
                    localStorage.setItem('token', res.data.token);
                    if (res.data.refreshToken) localStorage.setItem('refreshToken', res.data.refreshToken);
                    let user = res.data.user; user.role = getCleanRole(user.role);
                    currentUser.value = user; profileForm.name = user.name;
                    await fetchAllData(); showMessage(translate('toast.welcome'), user.name);
                } catch(e) {
                    if(e.response?.data?.message?.includes('激活')) { loginError.value=translate('auth.notActivated'); usernameToVerify.value=loginForm.username; isRegistering.value=true; isVerificationStep.value=true; }
                    else loginError.value = translate('auth.badCredentials');
                } finally { isLoading.value = false; }
            };

            const handleRegister = async () => {
                registerError.value=''; if(registerForm.password!==registerForm.confirmPassword) return registerError.value=translate('auth.pwdMismatch');
                isLoading.value = true;
                try { await api.post('/register', registerForm); usernameToVerify.value=registerForm.username; isVerificationStep.value=true; }
                catch(e) { registerError.value=translate('auth.registerFailed'); } finally { isLoading.value=false; }
            };
            const handleVerification = async () => { isLoading.value=true; try { await api.post('/register/verify', {username:usernameToVerify.value, code:verificationCode.value}); isRegistering.value=false; isVerificationStep.value=false; showMessage(translate('toast.activateOk')); } catch(e){ registerError.value=translate('auth.wrongCode'); } finally { isLoading.value=false; } };

            const handleLogout = async () => {
                try { await api.post('/api/auth/logout', { refreshToken: localStorage.getItem('refreshToken') }); } catch(e) {}
                currentUser.value=null; localStorage.removeItem('token'); localStorage.removeItem('refreshToken');
            };

            // 操作封装
            const action = async (fn, msgKey) => { try { await fn(); showMessage(translate('toast.success'), translate(msgKey)); await fetchAllData(); } catch(e) { showMessage(translate('toast.failed'), e.response?.data?.message||translate('toast.operateFailed'), 'error'); } };

            const handleBorrow = (b) => action(() => api.post('/api/records/borrow', {bookId: b.id}), 'toast.borrow');
            const handleReserve = (b) => action(() => api.post('/api/records/reserve', {bookId: b.id}), 'toast.reserve');
            const handleRenew = (r) => action(() => api.put(`/api/records/renew/${r.id}`), 'toast.renew');
            const handleReturn = () => action(() => api.post('/api/records/return', {bookIdentifier: returnBookId.value, userId: returnUserId.value||null}), 'toast.return');
            const handleProcessReservation = (r) => action(() => api.post(`/api/records/process-reservation/${r.id}`), 'toast.approve');
            const handleDeleteBook = (b) => confirm(translate('common.deleteConfirm')) && action(() => api.delete(`/api/books/${b.id}`), 'toast.deleted');
            const handleDeleteUser = (u) => confirm(translate('common.deleteConfirm')) && action(() => api.delete(`/api/users/${u.id}`), 'toast.deleted');

            const handleSaveBook = async () => { try { if(currentBook.id) await api.put(`/api/books/${currentBook.id}`, currentBook); else await api.post('/api/books', currentBook); closeModal(); showMessage(translate('toast.saved')); await fetchAllData(); } catch(e){} };
            const handleSaveUser = async () => { try { let u={...currentUserForm}; if(!u.password) delete u.password; if(u.id) await api.put(`/api/users/${u.id}`, u); else await api.post('/api/users', u); closeModal(); showMessage(translate('toast.saved')); await fetchAllData(); } catch(e){} };

            const handleUploadBooks = async () => {
                if(!selectedFile.value) return;
                let fd = new FormData(); fd.append('file', selectedFile.value);
                try { const res = await api.post('/api/books/upload', fd, {headers:{'Content-Type':'multipart/form-data'}}); Object.assign(importResults, res.data); await fetchAllData(); if(!importResults.failed) closeModal(); } catch(e){}
            };

            // 通用下载函数
            const handleDownload = async (url, filename) => {
                isLoading.value = true; // 添加 loading 状态
                try {
                    const res = await api.get(url, { responseType: 'blob' });
                    const link = document.createElement('a');
                    link.href = window.URL.createObjectURL(new Blob([res.data]));
                    link.download = filename;
                    document.body.appendChild(link);
                    link.click();
                    document.body.removeChild(link);
                    showMessage(translate('toast.success'), translate('toast.downloadStarted'));
                } catch (e) {
                    console.error("Download error:", e);
                    showMessage(translate('toast.failed'), translate('toast.exportFailed'), 'error');
                } finally {
                    isLoading.value = false;
                }
            };

            const handleExportExcel = () => handleDownload('/api/records/export/excel', 'records.xlsx');
            const handleExportAnalysis = (part, name) => handleDownload(`/api/analysis/export/${part}`, name);

            const handleUpdateProfile = async () => { try { await api.put('/api/profile/me', profileForm); currentUser.value.name=profileForm.name; showMessage(translate('toast.updated')); } catch(e){} };
            const handleChangePassword = async () => { try { await api.put('/api/profile/change-password', {oldPassword:passwordForm.oldPassword, newPassword:passwordForm.newPassword}); showMessage(translate('toast.pwdChanged')); closeModal(); } catch(e){} };
            const handleForgotPassword = async () => { try { await api.post('/forgot-password', {email:resetForm.email}); resetForm.codeSent=true; } catch(e){} };
            const handleResetPassword = async () => { try { await api.post('/reset-password', resetForm); isForgotPassword.value=false; showMessage(translate('toast.pwdResetOk')); } catch(e){} };

            // 计算属性
            const visiblePages = computed(() => {
                const role = getCleanRole(currentUser.value?.role);
                const all = [
                    {id:'dashboard',titleKey:'page.dashboard',icon:'grid-outline',roles:['user','admin','superadmin']},
                    {id:'manageBooks',titleKey:'page.manageBooks',icon:'library-outline',roles:['user','admin','superadmin']},
                    {id:'records',titleKey:'page.records',icon:'reader-outline',roles:['user','admin','superadmin']},
                    {id:'manageBorrow',titleKey:'page.manageBorrow',icon:'swap-horizontal-outline',roles:['admin','superadmin']},
                    {id:'analysis',titleKey:'page.analysis',icon:'stats-chart-outline',roles:['admin','superadmin']},
                    {id:'manageUsers',titleKey:'page.manageUsers',icon:'people-outline',roles:['superadmin']},
                    {id:'auditLogs',titleKey:'page.auditLogs',icon:'shield-checkmark-outline',roles:['superadmin']},
                    {id:'profile',titleKey:'page.profile',icon:'person-outline',roles:['user','admin','superadmin']}
                ];
                return all.filter(p => p.roles.includes(role));
            });
            const currentPageTitle = computed(() => visiblePages.value.find(p=>p.id===currentPage.value)?.titleKey || 'app.title');

            const filteredBooks = computed(() => books.value.filter(b => b.title.includes(bookSearchQuery.value) || b.isbn.includes(bookSearchQuery.value)));
            const filteredRecords = computed(() => getCleanRole(currentUser.value?.role)==='user' ? borrowRecords.value.filter(r=>r.userId===currentUser.value.id) : borrowRecords.value);
            const filteredUsers = computed(() => users.value.filter(u => u.username.includes(userSearchQuery.value)));

            const myBorrowCount = computed(() => borrowRecords.value.filter(r => r.userId===currentUser.value.id && r.status==='borrowed').length);
            const myReservationCount = computed(() => borrowRecords.value.filter(r => r.userId===currentUser.value.id && r.status==='reserved').length);
            const totalBookCount = computed(() => books.value.length);
            const pendingReservations = computed(() => borrowRecords.value.filter(r => r.status==='reserved'));

            return {
                currentUser, loginForm, loginError, currentPage, isSidebarOpen, isLoading, toasts,
                isRegistering, isVerificationStep, isForgotPassword, registerForm, verificationCode, registerError,
                resetForm, profileForm, passwordForm,
                books, users, borrowRecords, popularBooks, activeUsers, categoryStats, peakTimeStats, overdueUsers, stagnantBooks, lowStockBooks, auditLogs, auditLogPage, auditLogTotalPages,
                isModalOpen, modalType, currentBook, currentUserForm, bookSearchQuery, userSearchQuery, returnBookId, returnUserId, selectedFile, importResults,
                visiblePages, currentPageTitle, filteredBooks, filteredRecords, filteredUsers, myBorrowCount, myReservationCount, totalBookCount, pendingReservations,
                pieChartStyle, chartColors,
                handleLogin, handleRegister, handleVerification, handleForgotPassword, handleResetPassword, handleLogout,
                handleBorrow, handleReserve, handleRenew, handleReturn, handleProcessReservation,
                handleSaveBook, handleDeleteBook, handleSaveUser, handleDeleteUser, handleUploadBooks, handleExportExcel, handleExportAnalysis,
                handleUpdateProfile, handleChangePassword, fetchAllData, fetchAuditLogs,
                getCleanRole, getRoleName, isOverdue, statusLabel, isBookBorrowedByUser, isBookReservedByUser, getBookById, getUserById,
                closeModal, openBookModal, openUserModal, handleFileSelect, clearBookSearch, clearErrors
            };
}, {
    persist: {
        key: SESSION_KEY,
        pick: ['currentUser']
    }
})
