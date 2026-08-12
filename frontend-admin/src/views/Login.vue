<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../api/request'
import { ElMessage } from 'element-plus'

const router = useRouter()
const phone = ref('')
const password = ref('')
const loading = ref(false)

async function handleLogin() {
  loading.value = true
  try {
    const res: any = await request.post('/auth/login', { phone: phone.value, password: password.value })
    localStorage.setItem('adminToken', res.accessToken)
    localStorage.setItem('adminUser', JSON.stringify(res))
    ElMessage.success(`欢迎，${res.nickname}`)
    router.push('/dashboard')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '登录失败')
  } finally { loading.value = false }
}
</script>

<template>
  <div class="login-bg">
    <div class="blob blob-1" />
    <div class="blob blob-2" />
    <div class="blob blob-3" />
    <div class="overlay" />

    <div class="login-card">
      <div class="logo-area">
        <div class="logo-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="1.5" class="logo-svg">
            <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/>
          </svg>
        </div>
        <h1>沧月电竞</h1>
        <p>客服管理后台</p>
      </div>

      <div class="input-wrap">
        <input v-model="phone" type="tel" placeholder="手机号" @keyup.enter="handleLogin" />
        <input v-model="password" type="password" placeholder="密码" @keyup.enter="handleLogin" />
        <button :disabled="loading" @click="handleLogin">
          {{ loading ? '登录中...' : '登 录' }}
        </button>
      </div>

      <p class="footer">仅限授权客服人员登录</p>
    </div>
  </div>
</template>

<style scoped>
.login-bg {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 40%, #a78bfa 70%, #c084fc 100%);
  position: relative; overflow: hidden;
}
.overlay { position: absolute; inset: 0; background: rgba(0,0,0,.12); }
.blob { position: absolute; border-radius: 50%; filter: blur(80px); animation: morph 8s ease-in-out infinite alternate; }
.blob-1 { width: 500px; height: 500px; background: rgba(255,255,255,.12); top: -15%; right: -20%; }
.blob-2 { width: 400px; height: 400px; background: rgba(255,255,255,.08); bottom: -10%; left: -15%; animation-delay: -3s; animation-duration: 10s; }
.blob-3 { width: 300px; height: 300px; background: rgba(255,255,255,.1); top: 30%; left: 40%; animation-delay: -6s; animation-duration: 12s; }
@keyframes morph {
  0% { border-radius: 50%; transform: translate(0,0) scale(1); }
  50% { border-radius: 40% 60% 60% 40%; transform: translate(30px,-20px) scale(1.1); }
  100% { border-radius: 55% 45% 45% 55%; transform: translate(-20px,20px) scale(.9); }
}

.login-card {
  position: relative; z-index: 2; width: 400px; max-width: 90vw;
  background: rgba(255,255,255,.08); backdrop-filter: blur(24px);
  border-radius: 24px; padding: 40px 36px; border: 1px solid rgba(255,255,255,.12);
}
.logo-area { text-align: center; margin-bottom: 32px; }
.logo-icon { width: 56px; height: 56px; border-radius: 16px; background: rgba(255,255,255,.15); display: flex; align-items: center; justify-content: center; margin: 0 auto 12px; border: 2px solid rgba(255,255,255,.15); }
.logo-svg { width: 28px; height: 28px; }
.logo-area h1 { color: #fff; font-size: 22px; font-weight: 800; letter-spacing: 3px; margin: 0; }
.logo-area p { color: rgba(255,255,255,.4); font-size: 12px; margin: 4px 0 0; letter-spacing: 2px; }

.input-wrap { display: flex; flex-direction: column; gap: 14px; }
.input-wrap input {
  width: 100%; padding: 14px 16px; border-radius: 12px;
  background: rgba(255,255,255,.1); border: 1px solid rgba(255,255,255,.1);
  color: #fff; font-size: 15px; outline: none; box-sizing: border-box;
}
.input-wrap input::placeholder { color: rgba(255,255,255,.35); }
.input-wrap input:focus { border-color: rgba(255,255,255,.3); background: rgba(255,255,255,.16); }

.input-wrap button {
  width: 100%; padding: 14px; border: none; border-radius: 12px;
  background: #fff; color: #6366f1; font-size: 16px; font-weight: 700;
  cursor: pointer; letter-spacing: 4px; margin-top: 4px;
}
.input-wrap button:disabled { opacity: .5; cursor: not-allowed; }

.footer { text-align: center; color: rgba(255,255,255,.25); font-size: 12px; margin-top: 20px; }
</style>
