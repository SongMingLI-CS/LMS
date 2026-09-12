<template>
<div v-if="!currentUser" class="relative w-full h-full overflow-hidden bg-[#080b1c]">
        <LangSwitch class="absolute top-4 right-4 z-50" />
        <!-- 背景氛围层 -->
        <div class="absolute inset-0" aria-hidden="true">
            <div class="absolute -top-32 -left-24 w-[560px] h-[560px] rounded-full opacity-60 blur-[100px]" style="background: radial-gradient(circle, #312e81, #6d28d9 55%, transparent 75%);"></div>
            <div class="absolute top-1/4 -right-24 w-[540px] h-[540px] rounded-full opacity-40 blur-[110px]" style="background: radial-gradient(circle, #4338ca, transparent 72%);"></div>
            <div class="absolute -bottom-24 left-1/4 w-[560px] h-[420px] rounded-full opacity-35 blur-[120px]" style="background: radial-gradient(circle, #a21caf, transparent 70%);"></div>
            <div class="absolute inset-0 opacity-[0.05]" style="background-image: linear-gradient(rgba(255,255,255,.55) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,.55) 1px, transparent 1px); background-size: 46px 46px;"></div>
        </div>

        <div class="relative z-10 w-full h-full flex items-center justify-center p-5 overflow-y-auto">
            <div class="w-full max-w-md my-8">
                <div class="text-center mb-8">
                    <div class="w-16 h-16 mx-auto rounded-2xl bg-gradient-to-br from-indigo-500 via-violet-500 to-fuchsia-500 flex items-center justify-center text-3xl text-white shadow-[0_18px_44px_-10px_rgba(139,92,246,0.7)] ring-1 ring-white/20 mb-5" style="animation: floaty 6s ease-in-out infinite;">
                        <ion-icon name="library"></ion-icon>
                    </div>
                    <h1 class="text-3xl font-extrabold tracking-tight text-white">{{ $t('app.title') }}</h1>
                    <p class="text-slate-400 text-sm mt-2">{{ $t('app.org') }}</p>
                </div>

                <div class="rounded-3xl border border-white/10 bg-white/[0.06] backdrop-blur-2xl p-7 sm:p-8 shadow-[0_30px_90px_-24px_rgba(0,0,0,0.75)]">
                    <transition name="fade" mode="out-in">
                <div v-if="!isRegistering && !isVerificationStep && !isForgotPassword" key="login">
                    <div class="mb-5">
                        <h2 class="text-white text-xl font-bold">{{ $t('auth.loginTitle') }}</h2>
                        <p class="text-slate-400 text-xs mt-1">{{ $t('auth.loginSub') }}</p>
                    </div>
                    <form @submit.prevent="handleLogin" class="space-y-4">
                        <div>
                            <label class="block text-xs font-medium text-slate-300 mb-1.5">{{ $t('auth.account') }}</label>
                            <input type="text" v-model="loginForm.username" :placeholder="$t('auth.accountPh')" class="auth-input" required>
                        </div>
                        <div>
                            <div class="flex justify-between items-center mb-1.5">
                                <label class="text-xs font-medium text-slate-300">{{ $t('auth.password') }}</label>
                                <a href="#" @click.prevent="isForgotPassword=true; clearErrors()" class="text-xs text-indigo-300 hover:text-white transition-colors">{{ $t('auth.forgot') }}</a>
                            </div>
                            <input type="password" v-model="loginForm.password" :placeholder="$t('auth.passwordPh')" class="auth-input" required>
                        </div>
                        <button type="submit" :disabled="isLoading" class="btn-primary w-full py-3 rounded-xl font-semibold text-[15px]">{{ $t('auth.login') }}</button>
                        <div v-if="loginError" class="text-rose-300 text-sm text-center bg-rose-500/10 border border-rose-400/20 rounded-xl py-2">{{ loginError }}</div>
                    </form>
                    <div class="mt-6 pt-5 border-t border-white/10 text-center text-sm text-slate-400">
                        {{ $t('auth.hasNoAccount') }} <a href="#" @click.prevent="isRegistering=true; clearErrors()" class="text-indigo-300 font-semibold hover:text-white transition-colors">{{ $t('auth.signup') }}</a>
                    </div>
                </div>

                <div v-else-if="isRegistering && !isVerificationStep" key="register">
                    <div class="mb-5">
                        <h2 class="text-white text-xl font-bold">{{ $t('auth.registerTitle') }}</h2>
                        <p class="text-slate-400 text-xs mt-1">{{ $t('auth.registerSub') }}</p>
                    </div>
                    <form @submit.prevent="handleRegister" class="space-y-3.5">
                        <div class="grid grid-cols-2 gap-3.5">
                            <input type="text" v-model="registerForm.username" :placeholder="$t('auth.studentNo')" class="auth-input" required>
                            <input type="text" v-model="registerForm.name" :placeholder="$t('auth.name')" class="auth-input" required>
                        </div>
                        <input type="email" v-model="registerForm.email" :placeholder="$t('auth.schoolEmail')" class="auth-input" required>
                        <input type="password" v-model="registerForm.password" :placeholder="$t('auth.setPassword')" class="auth-input" required>
                        <input type="password" v-model="registerForm.confirmPassword" :placeholder="$t('auth.confirmPassword')" class="auth-input" required>
                        <button type="submit" class="btn-primary w-full py-3 rounded-xl font-semibold">{{ $t('auth.register') }}</button>
                        <div v-if="registerError" class="text-rose-300 text-sm text-center bg-rose-500/10 border border-rose-400/20 rounded-xl py-2">{{ registerError }}</div>
                    </form>
                    <button @click="isRegistering=false; clearErrors()" class="w-full mt-4 text-sm text-slate-400 hover:text-white transition-colors">{{ $t('auth.backLogin') }}</button>
                </div>

                <div v-else-if="isVerificationStep" key="verify" class="text-center">
                    <div class="w-12 h-12 mx-auto mb-4 rounded-xl bg-gradient-to-br from-emerald-400 to-teal-500 flex items-center justify-center text-2xl text-white shadow-[0_10px_30px_-8px_rgba(16,185,129,0.6)]">
                        <ion-icon name="mail-open-outline"></ion-icon>
                    </div>
                    <h2 class="text-white text-xl font-bold mb-1.5">{{ $t('auth.verifyTitle') }}</h2>
                    <p class="text-sm text-slate-400 mb-6">{{ $t('auth.verifySent', { email: registerForm.email }) }}</p>
                    <form @submit.prevent="handleVerification" class="space-y-4">
                        <input type="text" v-model="verificationCode" maxlength="6" :placeholder="$t('auth.verifyPh')" class="w-full text-center text-2xl tracking-[0.4em] font-mono py-3.5 rounded-xl bg-white/5 border border-white/10 text-white focus:border-indigo-400/70 outline-none transition" required>
                        <button type="submit" class="btn-primary w-full py-3 rounded-xl font-semibold">{{ $t('auth.activate') }}</button>
                        <div v-if="registerError" class="text-rose-300 text-sm bg-rose-500/10 border border-rose-400/20 rounded-xl py-2">{{ registerError }}</div>
                    </form>
                    <button @click="isVerificationStep=false" class="mt-5 text-sm text-slate-400 hover:text-white transition-colors">{{ $t('auth.prevStep') }}</button>
                </div>

                <div v-else-if="isForgotPassword" key="forgot">
                    <div class="mb-5">
                        <h2 class="text-white text-xl font-bold">{{ $t('auth.resetTitle') }}</h2>
                        <p class="text-slate-400 text-xs mt-1">{{ $t('auth.resetSub') }}</p>
                    </div>
                    <div v-if="!resetForm.codeSent">
                        <form @submit.prevent="handleForgotPassword" class="space-y-4">
                            <input type="email" v-model="resetForm.email" :placeholder="$t('auth.emailPh')" class="auth-input" required>
                            <button class="btn-primary w-full py-3 rounded-xl font-semibold">{{ $t('auth.sendCode') }}</button>
                        </form>
                    </div>
                    <div v-else>
                        <form @submit.prevent="handleResetPassword" class="space-y-4">
                            <input type="text" v-model="resetForm.code" :placeholder="$t('auth.emailCode')" class="auth-input" required>
                            <input type="password" v-model="resetForm.newPassword" :placeholder="$t('auth.newPassword')" class="auth-input" required>
                            <button class="btn-primary w-full py-3 rounded-xl font-semibold">{{ $t('auth.doReset') }}</button>
                        </form>
                    </div>
                    <div v-if="registerError" class="text-rose-300 text-sm text-center mt-4 bg-rose-500/10 border border-rose-400/20 rounded-xl py-2">{{ registerError }}</div>
                    <button @click="isForgotPassword=false; clearErrors()" class="w-full mt-4 text-sm text-slate-400 hover:text-white transition-colors">{{ $t('auth.cancelReset') }}</button>
                </div>
            </transition>
                </div>

                <p class="text-center text-[11px] text-slate-600 mt-7 tracking-wide">{{ $t('app.footer') }}</p>
            </div>
        </div>
    </div>
</template>

<script lang="ts">
import { useLms } from '../lms'
import LangSwitch from './ui/LangSwitch.vue'
import { bindStore } from '../utils/store-bindings'
export default {
  name: 'AuthShell',
  components: { LangSwitch },
  setup() {
    const s = useLms()
    return { ...bindStore(s) }
  }
}
</script>
