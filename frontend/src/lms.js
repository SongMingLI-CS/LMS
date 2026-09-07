import { ref, reactive, computed, watch } from 'vue'
import { defineStore } from 'pinia'
import axios from 'axios'

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

            // 6. API 初始化
            const api = axios.create({ baseURL: '/', timeout: 8000 });
            api.interceptors.request.use(config => {
                const token = localStorage.getItem('token');
                if (token) config.headers.Authorization = `Bearer ${token}`;
                return config;
            });

            // P0: 401 自动刷新访问令牌（共享单次刷新请求，避免并发重复刷新）
            let refreshPromise = null;
            api.interceptors.response.use(
                (response) => response,
                async (error) => {
                    const original = error.config;
                    const status = error.response?.status;
                    const refreshToken = localStorage.getItem('refreshToken');
                    if (status === 401 && original && !original._retry && refreshToken) {
                        original._retry = true;
                        if (!refreshPromise) {
                            refreshPromise = axios.post('/api/auth/refresh', { refreshToken }, { baseURL: '/', timeout: 8000 })
                                .then((res) => {
                                    localStorage.setItem('token', res.data.token);
                                    if (res.data.refreshToken) localStorage.setItem('refreshToken', res.data.refreshToken);
                                    return res.data.token;
                                })
                                .finally(() => { refreshPromise = null; });
                        }
                        try {
                            const newToken = await refreshPromise;
                            original.headers = original.headers || {};
                            original.headers.Authorization = `Bearer ${newToken}`;
                            return api(original);
                        } catch (e) {
                            localStorage.removeItem('token');
                            localStorage.removeItem('refreshToken');
                            window.location.reload();
                            return Promise.reject(e);
                        }
                    }
                    return Promise.reject(error);
                }
            );

            // 7. 辅助函数
            let toastId = 0;
            const showMessage = (title, text, type = 'success') => {
                const id = toastId++;
                toasts.value.push({ id, title, text, type });
                setTimeout(() => toasts.value = toasts.value.filter(t => t.id !== id), 3000);
            };
            const clearErrors = () => { loginError.value=''; registerError.value=''; };
            const getCleanRole = (r) => (r || 'ROLE_USER').replace(/ROLE_/gi, '').toLowerCase();
            const getRoleName = (r) => ({user:'用户',admin:'管理员',superadmin:'超级管理员'}[getCleanRole(r)] || '未知');
            const isOverdue = (d) => new Date(d) < new Date(new Date().toISOString().split('T')[0]);
            const statusLabel = (s) => ({ borrowed: '借阅中', returned: '已归还', reserved: '已预约', awaiting_pickup: '待取书', overdue: '已逾期' }[s] || s);
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
                    await fetchAllData(); showMessage('欢迎', user.name);
                } catch(e) {
                    if(e.response?.data?.message?.includes('激活')) { loginError.value='未激活'; usernameToVerify.value=loginForm.username; isRegistering.value=true; isVerificationStep.value=true; }
                    else loginError.value = '账号或密码错误';
                } finally { isLoading.value = false; }
            };

            const handleRegister = async () => {
                registerError.value=''; if(registerForm.password!==registerForm.confirmPassword) return registerError.value='密码不一致';
                isLoading.value = true;
                try { await api.post('/register', registerForm); usernameToVerify.value=registerForm.username; isVerificationStep.value=true; }
                catch(e) { registerError.value='注册失败'; } finally { isLoading.value=false; }
            };
            const handleVerification = async () => { isLoading.value=true; try { await api.post('/register/verify', {username:usernameToVerify.value, code:verificationCode.value}); isRegistering.value=false; isVerificationStep.value=false; showMessage('激活成功'); } catch(e){ registerError.value='验证码错误'; } finally { isLoading.value=false; } };

            const handleLogout = async () => {
                try { await api.post('/api/auth/logout', { refreshToken: localStorage.getItem('refreshToken') }); } catch(e) {}
                currentUser.value=null; localStorage.removeItem('token'); localStorage.removeItem('refreshToken');
            };

            // 操作封装
            const action = async (fn, msg) => { try { await fn(); showMessage('成功', msg); await fetchAllData(); } catch(e) { showMessage('失败', e.response?.data?.message||'操作失败', 'error'); } };

            const handleBorrow = (b) => action(() => api.post('/api/records/borrow', {bookId: b.id}), '借阅成功');
            const handleReserve = (b) => action(() => api.post('/api/records/reserve', {bookId: b.id}), '预约成功');
            const handleRenew = (r) => action(() => api.put(`/api/records/renew/${r.id}`), '续借成功');
            const handleReturn = () => action(() => api.post('/api/records/return', {bookIdentifier: returnBookId.value, userId: returnUserId.value||null}), '归还成功');
            const handleProcessReservation = (r) => action(() => api.post(`/api/records/process-reservation/${r.id}`), '批准成功');
            const handleDeleteBook = (b) => confirm('删除?') && action(() => api.delete(`/api/books/${b.id}`), '已删除');
            const handleDeleteUser = (u) => confirm('删除?') && action(() => api.delete(`/api/users/${u.id}`), '已删除');

            const handleSaveBook = async () => { try { if(currentBook.id) await api.put(`/api/books/${currentBook.id}`, currentBook); else await api.post('/api/books', currentBook); closeModal(); showMessage('已保存'); await fetchAllData(); } catch(e){} };
            const handleSaveUser = async () => { try { let u={...currentUserForm}; if(!u.password) delete u.password; if(u.id) await api.put(`/api/users/${u.id}`, u); else await api.post('/api/users', u); closeModal(); showMessage('已保存'); await fetchAllData(); } catch(e){} };

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
                    showMessage('成功', '下载已开始');
                } catch (e) {
                    console.error("Download error:", e);
                    showMessage('失败', '导出失败，请检查网络或权限', 'error');
                } finally {
                    isLoading.value = false;
                }
            };

            const handleExportExcel = () => handleDownload('/api/records/export/excel', 'records.xlsx');
            const handleExportAnalysis = (part, name) => handleDownload(`/api/analysis/export/${part}`, name);

            const handleUpdateProfile = async () => { try { await api.put('/api/profile/me', profileForm); currentUser.value.name=profileForm.name; showMessage('已更新'); } catch(e){} };
            const handleChangePassword = async () => { try { await api.put('/api/profile/change-password', {oldPassword:passwordForm.oldPassword, newPassword:passwordForm.newPassword}); showMessage('密码已改'); closeModal(); } catch(e){} };
            const handleForgotPassword = async () => { try { await api.post('/forgot-password', {email:resetForm.email}); resetForm.codeSent=true; } catch(e){} };
            const handleResetPassword = async () => { try { await api.post('/reset-password', resetForm); isForgotPassword.value=false; showMessage('密码重置成功'); } catch(e){} };

            // 计算属性
            const visiblePages = computed(() => {
                const role = getCleanRole(currentUser.value?.role);
                const all = [
                    {id:'dashboard',title:'仪表盘',icon:'grid-outline',roles:['user','admin','superadmin']},
                    {id:'manageBooks',title:'图书资源',icon:'library-outline',roles:['user','admin','superadmin']},
                    {id:'records',title:'借阅记录',icon:'reader-outline',roles:['user','admin','superadmin']},
                    {id:'manageBorrow',title:'借还管理',icon:'swap-horizontal-outline',roles:['admin','superadmin']},
                    {id:'analysis',title:'数据分析',icon:'stats-chart-outline',roles:['admin','superadmin']},
                    {id:'manageUsers',title:'用户管理',icon:'people-outline',roles:['superadmin']},
                    {id:'auditLogs',title:'审计日志',icon:'shield-checkmark-outline',roles:['superadmin']},
                    {id:'profile',title:'个人中心',icon:'person-outline',roles:['user','admin','superadmin']}
                ];
                return all.filter(p => p.roles.includes(role));
            });
            const currentPageTitle = computed(() => visiblePages.value.find(p=>p.id===currentPage.value)?.title || 'LMS');

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
                handleUpdateProfile, handleChangePassword, fetchAuditLogs,
                getRoleName, isOverdue, statusLabel, isBookBorrowedByUser, isBookReservedByUser, getBookById, getUserById,
                closeModal, openBookModal, openUserModal, handleFileSelect, clearBookSearch, clearErrors
            };
})
