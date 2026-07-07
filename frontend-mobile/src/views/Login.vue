<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { login as loginApi } from '../api/auth'
import { showToast } from 'vant'
import LiquidBackground from '../components/LiquidBackground.vue'

const router = useRouter()
const auth = useAuthStore()
const phone = ref('')
const password = ref('')
const rememberMe = ref(false)
const loading = ref(false)
const errorMsg = ref('')

async function handleLogin() {
  errorMsg.value = ''
  if (!phone.value) { errorMsg.value = '请输入手机号'; return }
  if (!/^1[3-9]\d{9}$/.test(phone.value)) { errorMsg.value = '手机号格式不正确'; return }
  if (!password.value) { errorMsg.value = '请输入密码'; return }
  loading.value = true
  try {
    const res: any = await loginApi(phone.value, password.value)
    auth.setAuth(res, rememberMe.value)
    showToast({ message: '登录成功', icon: 'success' })
    router.push(res.role === 'booster' ? '/booster/pool' : '/boss/home')
  } catch (e: any) { errorMsg.value = e?.response?.data?.message || '登录失败，请重试' }
  finally { loading.value = false }
}
function guestMode() { auth.setGuest(); router.push('/boss/home') }
</script>

<template>
  <LiquidBackground>
    <div class="login-page">
      <!-- Logo -->
      <div class="logo-area">
        <div class="logo-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="1.5" class="logo-svg">
            <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/>
          </svg>
        </div>
        <h1 class="logo-title">沧月电竞</h1>
        <p class="logo-sub">CangYue Esports</p>
      </div>

      <!-- 表单 -->
      <div class="card">
        <input v-model="phone" type="tel" maxlength="11" placeholder="手机号" class="input" />
        <input v-model="password" type="password" placeholder="密码" class="input" @keyup.enter="handleLogin" />

        <div v-if="errorMsg" class="error">{{ errorMsg }}</div>

        <van-checkbox v-model="rememberMe" checked-color="#fff" class="check" icon-size="16px">
          <span class="check-label">记住密码</span>
        </van-checkbox>

        <button class="btn" :disabled="loading" @click="handleLogin">
          <span v-if="loading">登录中...</span>
          <span v-else>登 录</span>
        </button>
      </div>

      <div class="links">
        <router-link to="/register" class="link">立即注册</router-link>
        <span class="divider">|</span>
        <span class="link guest" @click="guestMode">游客体验</span>
      </div>
    </div>
  </LiquidBackground>
</template>

<style scoped>
.login-page { min-height: 100vh; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 0 28px; }

.logo-area { text-align: center; margin-bottom: 40px; }
.logo-icon { width: 72px; height: 72px; border-radius: 22px; background: rgba(255,255,255,.15); backdrop-filter: blur(12px); display: flex; align-items: center; justify-content: center; margin: 0 auto 16px; border: 2px solid rgba(255,255,255,.2); }
.logo-svg { width: 36px; height: 36px; }
.logo-title { font-size: 28px; font-weight: 900; color: #fff; letter-spacing: 3px; margin: 0; }
.logo-sub { font-size: 12px; color: rgba(255,255,255,.5); letter-spacing: 6px; margin-top: 4px; }

.card { width: 100%; max-width: 360px; background: rgba(255,255,255,.1); backdrop-filter: blur(20px); border-radius: 20px; padding: 24px 20px; border: 1px solid rgba(255,255,255,.15); }
.input { width: 100%; padding: 14px 16px; border-radius: 12px; background: rgba(255,255,255,.12); border: 1px solid rgba(255,255,255,.1); color: #fff; font-size: 15px; outline: none; margin-bottom: 14px; box-sizing: border-box; }
.input::placeholder { color: rgba(255,255,255,.4); }
.input:focus { border-color: rgba(255,255,255,.3); background: rgba(255,255,255,.18); }

.error { background: rgba(239,68,68,.2); border: 1px solid rgba(239,68,68,.4); border-radius: 10px; padding: 10px 14px; color: #fecaca; font-size: 13px; margin-bottom: 14px; }

.check { margin-bottom: 16px; }
.check-label { color: rgba(255,255,255,.6); font-size: 13px; }

.btn { width: 100%; padding: 14px; border: none; border-radius: 14px; background: #fff; color: #6366f1; font-size: 16px; font-weight: 700; cursor: pointer; letter-spacing: 4px; transition: all .2s; }
.btn:hover { transform: translateY(-1px); box-shadow: 0 8px 24px rgba(0,0,0,.15); }
.btn:disabled { opacity: .5; cursor: not-allowed; }

.links { display: flex; gap: 16px; margin-top: 28px; font-size: 13px; }
.link { color: rgba(255,255,255,.7); text-decoration: none; font-weight: 500; }
.divider { color: rgba(255,255,255,.3); }
.guest { cursor: pointer; opacity: .5; }
</style>
