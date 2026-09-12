import { ref, reactive, computed, watch } from 'vue'
import { defineStore } from 'pinia'
import { api, SESSION_KEY } from './api/http.js'
import { getCleanRole, getRoleName, isOverdue, statusLabel } from './utils/helpers.js'
import { t as translate } from './i18n'

// 服务端分页页大小（后端 BookService.MAX_PAGE_SIZE = 100 为上限）
const PAGE_SIZE = 20
// 搜索输入防抖（毫秒）：避免每次按键都打后端
const SEARCH_DEBOUNCE_MS = 300

export const useLms = defineStore('lms', () => {
    // 1. 状态定义
    const currentUser = ref(null);
    const loginForm = reactive({ username: '', password: '' });
    const loginError = ref('');
    const currentPage = ref('dashboard');
    const isSidebarOpen = ref(true);
    const toasts = ref([]);
    const isLoading = ref(false);
    // P1: 页面级数据加载失败状态（用于错误提示 + 重试按钮）
    const loadError = ref('');

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

    // 2.1 服务端分页状态（图书 / 用户列表）
    const bookPage = ref(0);
    const bookTotalPages = ref(1);
    const bookTotalElements = ref(0);
    const userPage = ref(0);
    const userTotalPages = ref(1);
    const userTotalElements = ref(0);

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
    const usernameToVerify = ref('');
    const selectedFile = ref(null);
    const importResults = reactive({ success: 0, failed: 0, errors: [] });
    const chartColors = ['#3b82f6', '#10b981', '#f59e0b', '#ec4899', '#8b5cf6', '#6b7280'];

    // 6. 辅助函数
    let toastId = 0;
    const showMessage = (title, text, type = 'success') => {
        const id = toastId++;
        toasts.value.push({ id, title, text, type });
        setTimeout(() => toasts.value = toasts.value.filter(t => t.id !== id), 3000);
    };

    /**
     * 从异常中提取用户可读消息。
     * 优先使用服务端返回的 message（后端 GlobalExceptionHandler 保证该字段存在），
     * 否则按 HTTP 状态码给出本地化文案；绝不静默吞掉错误。
     */
    const apiErrorMessage = (e) => {
        const serverMsg = e?.response?.data?.message
        if (typeof serverMsg === 'string' && serverMsg.trim()) return serverMsg
        const status = e?.response?.status
        if (status === 400) return translate('toast.badRequest')
        if (status === 401) return translate('toast.unauthorized')
        if (status === 403) return translate('toast.forbidden')
        if (status === 404) return translate('toast.notFound')
        if (status === 409) return translate('toast.conflict')
        if (status === 429) return translate('toast.tooManyRequests')
        if (status >= 500) return translate('toast.serverError')
        return translate('toast.networkError')
    };

    const showError = (e) => showMessage(translate('toast.failed'), apiErrorMessage(e), 'error');
    const clearErrors = () => { loginError.value=''; registerError.value=''; loadError.value=''; };
    // 角色/状态/逾期等纯函数已抽离至 utils/helpers.js（见文件顶部 import）
    const closeModal = () => { isModalOpen.value=false; selectedFile.value=null; };
    const openBookModal = (b) => { Object.assign(currentBook, b || defaultBook); if(!b) currentBook.id=null; modalType.value='book'; isModalOpen.value=true; };
    const openUserModal = (u) => { Object.assign(currentUserForm, u || {role:'user'}); if(!u) currentUserForm.id=null; modalType.value='user'; isModalOpen.value=true; };
    const handleFileSelect = (e) => selectedFile.value = e.target.files[0];

    const getBookById = (id) => books.value.find(b => b.id === id);
    const getUserById = (id) => users.value.find(u => u.id === id);
    const isBookBorrowedByUser = (b) => borrowRecords.value.some(r => r.bookId === b.id && r.userId === currentUser.value?.id && r.status === 'borrowed');
    const isBookReservedByUser = (b) => borrowRecords.value.some(r => r.bookId === b.id && r.userId === currentUser.value?.id && (r.status === 'reserved' || r.status === 'awaiting_pickup'));

    const isAdmin = computed(() => getCleanRole(currentUser.value?.role) !== 'user');

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

    // 9. 数据获取（全部走后端分页/检索接口，不在前端做全表过滤）

    /** 图书列表：服务端检索 + 分页 */
    const loadBooks = async (page = 0) => {
        const res = await api.get('/api/books/search', {
            params: { q: bookSearchQuery.value || undefined, page, size: PAGE_SIZE, sort: 'id,asc' }
        });
        const data = res.data || {};
        books.value = data.content || [];
        bookPage.value = data.number ?? 0;
        bookTotalPages.value = Math.max(data.totalPages ?? 1, 1);
        bookTotalElements.value = data.totalElements ?? books.value.length;
    };

    /** 用户列表：服务端检索 + 分页（仅管理员可用） */
    const loadUsers = async (page = 0) => {
        if (!isAdmin.value) return;
        const res = await api.get('/api/users/search', {
            params: { q: userSearchQuery.value || undefined, page, size: PAGE_SIZE, sort: 'id,asc' }
        });
        const data = res.data || {};
        users.value = data.content || [];
        userPage.value = data.number ?? 0;
        userTotalPages.value = Math.max(data.totalPages ?? 1, 1);
        userTotalElements.value = data.totalElements ?? users.value.length;
    };

    const fetchAllData = async () => {
        isLoading.value = true;
        loadError.value = '';
        try {
            const tasks = [
                loadBooks(bookPage.value),
                api.get('/api/records').then(r => borrowRecords.value = r.data)
            ];
            if (isAdmin.value) {
                tasks.push(loadUsers(userPage.value));
                tasks.push(api.get('/api/books/low-stock').then(r => lowStockBooks.value = r.data));
                // 分析类接口：仅管理员可用（后端同样已限制），失败不阻断主流程
                tasks.push(
                    api.get('/api/records/analysis/popular-books').then(r => popularBooks.value = r.data),
                    api.get('/api/records/analysis/active-users').then(r => activeUsers.value = r.data),
                    api.get('/api/books/analysis/categories').then(r => categoryStats.value = r.data),
                    api.get('/api/records/analysis/peak-times').then(r => peakTimeStats.value = r.data),
                    api.get('/api/records/analysis/overdue-users').then(r => overdueUsers.value = r.data),
                    api.get('/api/books/analysis/stagnant-books').then(r => stagnantBooks.value = r.data)
                );
            }
            await Promise.all(tasks);
        } catch (e) {
            loadError.value = apiErrorMessage(e);
            showError(e);
        } finally {
            isLoading.value = false;
        }
    };

    const fetchAuditLogs = async (page=0) => {
        if(getCleanRole(currentUser.value?.role)!=='superadmin') return;
        isLoading.value = true;
        try {
            const res = await api.get(`/api/audit-logs?page=${page}&size=20`);
            auditLogs.value = res.data.content;
            auditLogPage.value = res.data.number;
            auditLogTotalPages.value = res.data.totalPages;
        } catch(e){ showError(e); } finally { isLoading.value = false; }
    };
    watch(currentPage, (v) => { if(v==='auditLogs') fetchAuditLogs(0); });

    // 搜索防抖：输入停顿后才请求后端（服务端检索，避免下载全表后前端过滤）
    let bookSearchTimer = null;
    watch(bookSearchQuery, () => {
        clearTimeout(bookSearchTimer);
        bookSearchTimer = setTimeout(() => { loadBooks(0).catch(showError); }, SEARCH_DEBOUNCE_MS);
    });
    let userSearchTimer = null;
    watch(userSearchQuery, () => {
        if (!isAdmin.value) return;
        clearTimeout(userSearchTimer);
        userSearchTimer = setTimeout(() => { loadUsers(0).catch(showError); }, SEARCH_DEBOUNCE_MS);
    });
    const clearBookSearch = () => { bookSearchQuery.value = ''; loadBooks(0).catch(showError); };



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
            const msg = e.response?.data?.message || '';
            if(msg.includes('激活')) { loginError.value=translate('auth.notActivated'); usernameToVerify.value=loginForm.username; isRegistering.value=true; isVerificationStep.value=true; }
            else if (msg.includes('锁定')) loginError.value = translate('auth.locked');
            else if (msg.includes('频繁')) loginError.value = translate('auth.tooManyAttempts');
            else loginError.value = translate('auth.badCredentials');
        } finally { isLoading.value = false; }
    };

    const handleRegister = async () => {
        registerError.value=''; if(registerForm.password!==registerForm.confirmPassword) return registerError.value=translate('auth.pwdMismatch');
        isLoading.value = true;
        try { await api.post('/register', registerForm); usernameToVerify.value=registerForm.username; isVerificationStep.value=true; }
        catch(e) { registerError.value = e.response?.data?.message || translate('auth.registerFailed'); } finally { isLoading.value=false; }
    };

    const handleVerification = async () => {
        isLoading.value=true;
        try {
            await api.post('/register/verify', {username:usernameToVerify.value, code:verificationCode.value});
            isRegistering.value=false; isVerificationStep.value=false; showMessage(translate('toast.activateOk'));
        } catch(e){ registerError.value = e.response?.data?.message || translate('auth.wrongCode'); } finally { isLoading.value=false; }
    };

    const handleLogout = async () => {
        try { await api.post('/api/auth/logout', { refreshToken: localStorage.getItem('refreshToken') }); } catch(e) { /* 本地登出不受服务端影响 */ }
        currentUser.value=null; localStorage.removeItem('token'); localStorage.removeItem('refreshToken');
    };

    // 操作封装：成功提示 + 刷新；失败时展示服务端消息（决不静默失败）
    const action = async (fn, msgKey) => { try { await fn(); showMessage(translate('toast.success'), translate(msgKey)); await fetchAllData(); } catch(e) { showError(e); } };

    const handleBorrow = (b) => action(() => api.post('/api/records/borrow', {bookId: b.id}), 'toast.borrow');
    const handleReserve = (b) => action(() => api.post('/api/records/reserve', {bookId: b.id}), 'toast.reserve');
    const handleRenew = (r) => action(() => api.put(`/api/records/renew/${r.id}`), 'toast.renew');
    const handleReturn = () => action(() => api.post('/api/records/return', {bookIdentifier: returnBookId.value, userId: returnUserId.value||null}), 'toast.return');
    const handleProcessReservation = (r) => action(() => api.post(`/api/records/process-reservation/${r.id}`), 'toast.approve');
    const handleDeleteBook = (b) => confirm(translate('common.deleteConfirm')) && action(() => api.delete(`/api/books/${b.id}`), 'toast.deleted');
    const handleDeleteUser = (u) => confirm(translate('common.deleteConfirm')) && action(() => api.delete(`/api/users/${u.id}`), 'toast.deleted');


    /**
     * 保存图书（新增/编辑）。
     * P1 修复：原实现 catch(e){} 静默吞异常——保存失败时用户看不到任何反馈。
     */
    const handleSaveBook = async () => {
        isLoading.value = true;
        try {
            if(currentBook.id) await api.put(`/api/books/${currentBook.id}`, currentBook);
            else await api.post('/api/books', currentBook);
            closeModal();
            showMessage(translate('toast.success'), translate('toast.saved'));
            await fetchAllData();
        } catch(e) {
            showError(e);
        } finally { isLoading.value = false; }
    };

    /** 保存用户（新增/编辑）。P1 修复：错误必须可见。 */
    const handleSaveUser = async () => {
        isLoading.value = true;
        try {
            let u={...currentUserForm};
            if(!u.password) delete u.password;
            if(u.id) await api.put(`/api/users/${u.id}`, u);
            else await api.post('/api/users', u);
            closeModal();
            showMessage(translate('toast.success'), translate('toast.saved'));
            await fetchAllData();
        } catch(e) {
            showError(e);
        } finally { isLoading.value = false; }
    };

    const handleUploadBooks = async () => {
        if(!selectedFile.value) return;
        isLoading.value = true;
        let fd = new FormData(); fd.append('file', selectedFile.value);
        try {
            const res = await api.post('/api/books/upload', fd, {headers:{'Content-Type':'multipart/form-data'}});
            Object.assign(importResults, res.data);
            await fetchAllData();
            if(!importResults.failed) closeModal();
        } catch(e){
            showError(e);
        } finally { isLoading.value = false; }
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

    /** 更新个人资料。P1 修复：失败需可见。 */
    const handleUpdateProfile = async () => {
        isLoading.value = true;
        try {
            const res = await api.put('/api/profile/me', { name: profileForm.name });
            const newName = res?.data?.name ?? profileForm.name;
            profileForm.name = newName;
            currentUser.value = { ...currentUser.value, name: newName };
            showMessage(translate('toast.success'), translate('toast.updated'));
        } catch(e) { showError(e); } finally { isLoading.value = false; }
    };

    /** 修改密码。P1 修复：校验两次输入一致 + 失败可见。 */
    const handleChangePassword = async () => {
        if (passwordForm.newPassword !== passwordForm.confirmPassword) {
            showMessage(translate('toast.failed'), translate('modal.password.mismatch'), 'error');
            return;
        }
        if (!passwordForm.oldPassword || !passwordForm.newPassword) {
            showMessage(translate('toast.failed'), translate('modal.password.required'), 'error');
            return;
        }
        isLoading.value = true;
        try {
            await api.put('/api/profile/change-password', {oldPassword:passwordForm.oldPassword, newPassword:passwordForm.newPassword});
            Object.assign(passwordForm, { oldPassword:'', newPassword:'', confirmPassword:'' });
            showMessage(translate('toast.success'), translate('toast.pwdChanged'));
            closeModal();
            // P0：服务端改密后已吊销全部令牌，前端同步清理本地会话并回登录页
            currentUser.value = null;
            localStorage.removeItem('token');
            localStorage.removeItem('refreshToken');
        } catch(e) { showError(e); } finally { isLoading.value = false; }
    };

    const handleForgotPassword = async () => {
        isLoading.value = true;
        try {
            await api.post('/forgot-password', {email:resetForm.email});
            resetForm.codeSent=true;
            showMessage(translate('toast.success'), translate('auth.resetCodeSent'));
        } catch(e) { showError(e); } finally { isLoading.value = false; }
    };

    const handleResetPassword = async () => {
        if (!resetForm.code || !resetForm.newPassword) {
            showMessage(translate('toast.failed'), translate('auth.codeAndPwdRequired'), 'error');
            return;
        }
        isLoading.value = true;
        try {
            await api.post('/reset-password', resetForm);
            isForgotPassword.value=false; showMessage(translate('toast.success'), translate('toast.pwdResetOk'));
        } catch(e) { showError(e); } finally { isLoading.value = false; }
    };


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

    // 图书/用户列表已在服务端检索 + 分页，此处不再做“下载全表再前端过滤”
    const filteredBooks = computed(() => books.value);
    const filteredRecords = computed(() => getCleanRole(currentUser.value?.role)==='user' ? borrowRecords.value.filter(r=>r.userId===currentUser.value?.id) : borrowRecords.value);
    const filteredUsers = computed(() => users.value);

    const myBorrowCount = computed(() => borrowRecords.value.filter(r => r.userId===currentUser.value?.id && r.status==='borrowed').length);
    const myReservationCount = computed(() => borrowRecords.value.filter(r => r.userId===currentUser.value?.id && r.status==='reserved').length);
    // P2: 馆藏总数改为服务端 totalElements，避免仅统计当前分页
    const totalBookCount = computed(() => bookTotalElements.value);
    const pendingReservations = computed(() => borrowRecords.value.filter(r => r.status==='reserved'));

    return {
        currentUser, loginForm, loginError, currentPage, isSidebarOpen, isLoading, toasts, loadError,
        isRegistering, isVerificationStep, isForgotPassword, registerForm, verificationCode, registerError,
        resetForm, profileForm, passwordForm,
        books, users, borrowRecords, popularBooks, activeUsers, categoryStats, peakTimeStats, overdueUsers, stagnantBooks, lowStockBooks, auditLogs, auditLogPage, auditLogTotalPages,
        bookPage, bookTotalPages, bookTotalElements, userPage, userTotalPages, userTotalElements,
        isModalOpen, modalType, currentBook, currentUserForm, bookSearchQuery, userSearchQuery, returnBookId, returnUserId, selectedFile, importResults,
        visiblePages, currentPageTitle, filteredBooks, filteredRecords, filteredUsers, myBorrowCount, myReservationCount, totalBookCount, pendingReservations,
        pieChartStyle, chartColors, isAdmin,
        handleLogin, handleRegister, handleVerification, handleForgotPassword, handleResetPassword, handleLogout,
        handleBorrow, handleReserve, handleRenew, handleReturn, handleProcessReservation,
        handleSaveBook, handleDeleteBook, handleSaveUser, handleDeleteUser, handleUploadBooks, handleExportExcel, handleExportAnalysis,
        handleUpdateProfile, handleChangePassword, fetchAllData, fetchAuditLogs, loadBooks, loadUsers,
        getCleanRole, getRoleName, isOverdue, statusLabel, isBookBorrowedByUser, isBookReservedByUser, getBookById, getUserById,
        closeModal, openBookModal, openUserModal, handleFileSelect, clearBookSearch, clearErrors
    };
}, {
    persist: {
        key: SESSION_KEY,
        pick: ['currentUser']
    }
})

