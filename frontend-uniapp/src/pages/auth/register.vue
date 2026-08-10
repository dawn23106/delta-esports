<template>
  <view class="auth-page">
    <view class="top-row">
      <view><text class="top-kicker">CREATE ACCOUNT</text><text class="top-title">加入沧月</text><text class="top-desc">选择身份，进入对应的小程序工作台</text></view>
      <navigator url="/pages/auth/login" class="back-link">去登录</navigator>
    </view>

    <view class="auth-card">
      <view class="field"><text class="field-label">手机号</text><input v-model="phone" type="number" maxlength="11" placeholder="请输入手机号" placeholder-class="input-placeholder" class="field-input" /></view>
      <view class="field"><text class="field-label">昵称</text><input v-model="nickname" maxlength="20" placeholder="请输入昵称" placeholder-class="input-placeholder" class="field-input" /></view>
      <view class="field"><text class="field-label">密码</text><input v-model="password" type="password" placeholder="至少 6 位" placeholder-class="input-placeholder" class="field-input" @confirm="handleRegister" /></view>

      <text class="role-title">选择身份</text>
      <view class="role-grid">
        <view v-for="option in roleOptions" :key="option.value" :class="['role-card', { active: role === option.value }]" @tap="role = option.value">
          <text class="role-icon">{{ option.icon }}</text><text class="role-label">{{ option.label }}</text><text class="role-desc">{{ option.desc }}</text>
        </view>
      </view>

      <view v-if="errorMsg" class="form-error">{{ errorMsg }}</view>
      <button class="register-btn" :disabled="loading" @tap="handleRegister">{{ loading ? '注册中…' : '注册并进入' }}</button>
      <text class="agreement">注册即表示同意平台服务规则与隐私说明</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue"
import { register as registerApi } from "@/api/auth"
import { useAuthStore } from "@/store/auth"

const auth = useAuthStore()
const phone = ref("")
const password = ref("")
const nickname = ref("")
const role = ref<"boss" | "booster">("boss")
const loading = ref(false)
const errorMsg = ref("")
const roleOptions = [
  { value: "boss" as const, label: "我是玩家", desc: "下单找陪玩", icon: "玩" },
  { value: "booster" as const, label: "我是打手", desc: "接单做服务", icon: "接" },
]

async function handleRegister() {
  errorMsg.value = ""
  if (!/^1[3-9]\d{9}$/.test(phone.value)) return void (errorMsg.value = "请输入正确的手机号")
  if (!nickname.value.trim()) return void (errorMsg.value = "请输入昵称")
  if (password.value.length < 6) return void (errorMsg.value = "密码至少需要 6 位")
  loading.value = true
  try {
    const result: any = await registerApi(phone.value, password.value, nickname.value.trim(), role.value)
    auth.setAuth(result, true)
    uni.showToast({ title: "注册成功", icon: "success" })
    uni.switchTab({ url: "/pages/home/home" })
  } catch (error: any) {
    errorMsg.value = error?.data?.message || error?.message || "注册失败，请稍后重试"
  } finally { loading.value = false }
}
</script>

<style scoped>
.auth-page { width: 100%; min-height: 100vh; box-sizing: border-box; padding: calc(58px + env(safe-area-inset-top)) 22px calc(30px + env(safe-area-inset-bottom)); background: radial-gradient(circle at 12% 6%, rgba(101,168,120,.2), transparent 28%), linear-gradient(180deg, #f7f4ea, #edf5ee); }
.top-row { display: flex; align-items: flex-end; justify-content: space-between; gap: 14px; margin: 4px 4px 24px; }
.top-kicker { display: block; color: #6e987a; font-size: 9px; font-weight: 850; letter-spacing: 2px; }
.top-title { display: block; margin-top: 5px; color: #173e2a; font-size: 27px; font-weight: 950; }
.top-desc { display: block; margin-top: 5px; color: #708077; font-size: 11px; }
.back-link { flex-shrink: 0; padding-bottom: 3px; color: #2c704d; font-size: 12px; font-weight: 850; }
.auth-card { width: 100%; box-sizing: border-box; padding: 21px 19px; border: 1px solid #dae6dc; border-radius: 25px; background: rgba(255,255,255,.95); box-shadow: 0 20px 46px rgba(43,77,55,.09); }
.field { width: 100%; box-sizing: border-box; margin-bottom: 11px; padding: 10px 13px 11px; border: 1px solid #e1e9e1; border-radius: 15px; background: #f7f9f6; }
.field-label { display: block; color: #6b7a70; font-size: 10px; font-weight: 750; }
.field-input { width: 100%; height: 31px; margin-top: 2px; color: #173e2a; font-size: 15px; }
.input-placeholder { color: #a9b3ac; }
.role-title { display: block; margin: 17px 1px 9px; color: #405a48; font-size: 12px; font-weight: 850; }
.role-grid { display: flex; gap: 10px; }
.role-card { min-width: 0; flex: 1; padding: 13px 8px; border: 1px solid #dfe7df; border-radius: 16px; background: #fff; text-align: center; }
.role-card.active { border-color: #5d9b70; background: #edf7ef; box-shadow: inset 0 0 0 1px rgba(93,155,112,.12); }
.role-icon { display: block; width: 32px; height: 32px; margin: 0 auto 7px; border-radius: 11px; background: #e8eee9; color: #557361; font-size: 12px; font-weight: 900; line-height: 32px; }
.role-card.active .role-icon { background: #3d7b57; color: #fff; }
.role-label { display: block; color: #173e2a; font-size: 13px; font-weight: 900; }
.role-desc { display: block; margin-top: 3px; color: #819087; font-size: 9px; }
.form-error { margin-top: 12px; padding: 9px 11px; border-radius: 11px; background: #fff0ed; color: #bd4e45; font-size: 11px; }
.register-btn { width: 100%; box-sizing: border-box; margin: 16px 0 0; padding: 12px; border: 0; border-radius: 999px; background: linear-gradient(135deg, #285f42, #65a878); color: #fff; font-size: 14px; font-weight: 850; box-shadow: 0 9px 20px rgba(44,112,77,.2); }
.register-btn[disabled] { opacity: .58; }
.agreement { display: block; margin-top: 14px; color: #a1aaa4; font-size: 9px; text-align: center; }
</style>
