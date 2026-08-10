<template>
  <view class="auth-page">
    <view class="brand-panel">
      <view class="brand-logo">沧</view>
      <view class="brand-copy">
        <text class="brand-title">沧月电竞</text>
        <text class="brand-desc">找陪玩、下订单、跟进服务进度</text>
      </view>
    </view>

    <view class="auth-card">
      <view class="auth-heading">
        <view>
          <text class="auth-kicker">WELCOME BACK</text>
          <text class="auth-title">欢迎回来</text>
        </view>
        <navigator url="/pages/auth/register" class="auth-link">注册账号</navigator>
      </view>

      <view class="field">
        <text class="field-label">手机号</text>
        <input v-model="phone" type="number" maxlength="11" placeholder="请输入手机号" placeholder-class="input-placeholder" class="field-input" />
      </view>

      <view class="field">
        <text class="field-label">密码</text>
        <input v-model="password" type="password" placeholder="请输入密码" placeholder-class="input-placeholder" class="field-input" @confirm="handleLogin" />
      </view>

      <view class="form-row">
        <label class="remember-row" @tap="rememberMe = !rememberMe">
          <view :class="['check-box', { checked: rememberMe }]">{{ rememberMe ? '✓' : '' }}</view>
          <text>记住登录</text>
        </label>
        <text class="guest-btn" @tap="guestMode">先看看</text>
      </view>

      <view v-if="errorMsg" class="form-error">{{ errorMsg }}</view>

      <button class="login-btn" :disabled="loading" @tap="handleLogin">
        {{ loading ? '登录中…' : '登录' }}
      </button>

      <!-- #ifdef MP-WEIXIN -->
      <button class="wx-btn" @tap="wxLogin">
        <text class="wx-dot">微</text>
        <text>微信一键登录</text>
      </button>
      <!-- #endif -->

      <text class="agreement">登录即表示同意平台服务规则与隐私说明</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue"
import { login as loginApi, wxLogin as wxLoginApi } from "@/api/auth"
import { useAuthStore } from "@/store/auth"

const auth = useAuthStore()
const phone = ref("")
const password = ref("")
const rememberMe = ref(true)
const loading = ref(false)
const errorMsg = ref("")

async function handleLogin() {
  errorMsg.value = ""
  if (!/^1[3-9]\d{9}$/.test(phone.value)) {
    errorMsg.value = "请输入正确的手机号"
    return
  }
  if (!password.value) {
    errorMsg.value = "请输入密码"
    return
  }
  loading.value = true
  try {
    const result: any = await loginApi(phone.value, password.value)
    auth.setAuth(result, rememberMe.value)
    uni.showToast({ title: "登录成功", icon: "success" })
    uni.switchTab({ url: "/pages/home/home" })
  } catch (error: any) {
    errorMsg.value = error?.data?.message || error?.message || "登录失败，请稍后重试"
  } finally {
    loading.value = false
  }
}

function guestMode() {
  auth.setGuest()
  uni.switchTab({ url: "/pages/home/home" })
}

// #ifdef MP-WEIXIN
async function wxLogin() {
  uni.showLoading({ title: "微信登录中", mask: true })
  try {
    const { code } = await new Promise<{ code: string }>((resolve, reject) => {
      uni.login({ provider: "weixin", success: (res) => resolve(res as any), fail: reject })
    })
    const result: any = await wxLoginApi(code)
    auth.setAuth(result, true)
    uni.hideLoading()
    uni.showToast({ title: "登录成功", icon: "success" })
    uni.switchTab({ url: "/pages/home/home" })
  } catch (error: any) {
    uni.hideLoading()
    uni.showToast({ title: error?.data?.message || "微信登录失败，请使用手机号登录", icon: "none" })
  }
}
// #endif
</script>

<style scoped>
.auth-page {
  width: 100%; min-height: 100vh; box-sizing: border-box;
  padding: calc(54px + env(safe-area-inset-top)) 22px calc(30px + env(safe-area-inset-bottom));
  background: radial-gradient(circle at 86% 8%, rgba(224,167,91,.22), transparent 28%), linear-gradient(180deg, #edf5ee 0%, #f8f5ec 100%);
}
.brand-panel { display: flex; align-items: center; gap: 13px; margin: 8px 4px 28px; }
.brand-logo { width: 50px; height: 50px; flex-shrink: 0; border-radius: 17px; background: linear-gradient(145deg, #255f41, #65a878); color: #fff; font-size: 21px; font-weight: 900; line-height: 50px; text-align: center; box-shadow: 0 10px 24px rgba(44,112,77,.2); }
.brand-copy { min-width: 0; }
.brand-title { display: block; color: #173e2a; font-size: 22px; font-weight: 950; }
.brand-desc { display: block; margin-top: 4px; color: #6d7f72; font-size: 11px; }
.auth-card { width: 100%; box-sizing: border-box; padding: 24px 20px 22px; border: 1px solid rgba(215,228,216,.95); border-radius: 26px; background: rgba(255,255,255,.94); box-shadow: 0 20px 48px rgba(43,77,55,.1); }
.auth-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 12px; margin-bottom: 23px; }
.auth-kicker { display: block; color: #76a282; font-size: 9px; font-weight: 800; letter-spacing: 2px; }
.auth-title { display: block; margin-top: 5px; color: #183c29; font-size: 25px; font-weight: 950; }
.auth-link { flex-shrink: 0; padding-bottom: 3px; color: #2c704d; font-size: 12px; font-weight: 850; }
.field { width: 100%; box-sizing: border-box; margin-bottom: 12px; padding: 11px 14px 12px; border: 1px solid #e1e9e1; border-radius: 16px; background: #f7f9f6; }
.field-label { display: block; color: #6b7a70; font-size: 10px; font-weight: 750; }
.field-input { width: 100%; height: 32px; margin-top: 2px; color: #173e2a; font-size: 15px; }
.input-placeholder { color: #a9b3ac; }
.form-row { display: flex; align-items: center; justify-content: space-between; margin: 2px 1px 17px; color: #697a6e; font-size: 11px; }
.remember-row { display: flex; align-items: center; gap: 7px; }
.check-box { width: 17px; height: 17px; border: 1px solid #becbc1; border-radius: 5px; color: #fff; font-size: 11px; line-height: 17px; text-align: center; }
.check-box.checked { border-color: #3d7b57; background: #3d7b57; }
.guest-btn { color: #2c704d; font-weight: 850; }
.form-error { margin-bottom: 12px; padding: 9px 11px; border-radius: 11px; background: #fff0ed; color: #bd4e45; font-size: 11px; }
.login-btn, .wx-btn { width: 100%; box-sizing: border-box; margin: 0; border-radius: 999px; font-size: 14px; font-weight: 850; }
.login-btn { border: 0; padding: 12px; background: linear-gradient(135deg, #285f42, #65a878); color: #fff; box-shadow: 0 9px 20px rgba(44,112,77,.2); }
.login-btn[disabled] { opacity: .58; }
.wx-btn { display: flex; align-items: center; justify-content: center; gap: 8px; margin-top: 11px; padding: 11px; border: 1px solid #64b67b; background: #fff; color: #2e8b4c; }
.wx-dot { width: 22px; height: 22px; border-radius: 50%; background: #43b764; color: #fff; font-size: 10px; line-height: 22px; text-align: center; }
.agreement { display: block; margin-top: 15px; color: #a1aaa4; font-size: 9px; text-align: center; }
</style>
