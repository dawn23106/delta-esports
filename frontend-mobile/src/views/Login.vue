<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { login as loginApi } from '../api/auth'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const auth = useAuthStore()
const phone = ref('')
const password = ref('')
const rememberMe = ref(true)
const loading = ref(false)
const errorMsg = ref('')

async function handleLogin() {
  errorMsg.value = ''
  if (!/^1[3-9]\d{9}$/.test(phone.value)) {
    errorMsg.value = '请输入正确的手机号'
    return
  }
  if (!password.value) {
    errorMsg.value = '请输入密码'
    return
  }

  loading.value = true
  try {
    const result: any = await loginApi(phone.value, password.value)
    auth.setAuth(result, rememberMe.value)
    showToast({ message: '登录成功', icon: 'success' })
    router.push(result.role === 'booster' ? '/booster/pool' : '/boss/home')
  } catch (error: any) {
    errorMsg.value = error?.response?.data?.message || '登录失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function guestMode() {
  auth.setGuest()
  router.push('/boss/home')
}
</script>

<template>
  <main class="auth-page">
    <section class="brand-panel">
      <div class="brand-mark">
        <van-icon name="fire-o" />
      </div>
      <h1>沧月电竞</h1>
      <p>快速找陪玩、下单、跟进订单进度。</p>
    </section>

    <section class="auth-card">
      <div class="auth-title">
        <span>欢迎回来</span>
        <router-link to="/register">注册</router-link>
      </div>

      <van-field v-model="phone" type="tel" maxlength="11" label="手机号" placeholder="请输入手机号" clearable />
      <van-field v-model="password" type="password" label="密码" placeholder="请输入密码" clearable @keyup.enter="handleLogin" />

      <div class="form-row">
        <van-checkbox v-model="rememberMe" icon-size="16px" checked-color="#3157ff">记住登录</van-checkbox>
        <button type="button" @click="guestMode">游客体验</button>
      </div>

      <div v-if="errorMsg" class="form-error">{{ errorMsg }}</div>

      <van-button block round type="primary" size="large" :loading="loading" color="linear-gradient(135deg, #3157ff, #08b6d8)" @click="handleLogin">
        登录
      </van-button>
    </section>
  </main>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  padding: 34px 18px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 20px;
  background:
    linear-gradient(135deg, rgba(16,19,35,.72), rgba(49,87,255,.48)),
    url('https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1000&h=1400&fit=crop');
  background-size: cover;
  background-position: center;
}

.brand-panel {
  color: #fff;
}

.brand-mark {
  width: 60px;
  height: 60px;
  border-radius: 20px;
  display: grid;
  place-items: center;
  background: rgba(255,255,255,.16);
  border: 1px solid rgba(255,255,255,.22);
  backdrop-filter: blur(18px);
  font-size: 30px;
}

.brand-panel h1 {
  margin: 18px 0 8px;
  font-size: 34px;
  font-weight: 950;
  letter-spacing: 0;
}

.brand-panel p {
  max-width: 260px;
  margin: 0;
  color: rgba(255,255,255,.76);
  font-size: 14px;
  line-height: 1.6;
}

.auth-card {
  border-radius: 24px;
  background: rgba(255,255,255,.92);
  backdrop-filter: blur(20px);
  padding: 18px;
  display: grid;
  gap: 12px;
  box-shadow: 0 24px 60px rgba(16,24,40,.26);
}

.auth-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2px;
}

.auth-title span {
  font-size: 20px;
  font-weight: 900;
}

.auth-title a,
.form-row button {
  border: 0;
  background: transparent;
  color: var(--mobile-brand);
  font-size: 13px;
  font-weight: 800;
  text-decoration: none;
}

.form-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--mobile-muted);
  font-size: 13px;
}

.form-error {
  border-radius: 12px;
  padding: 10px 12px;
  background: #fff1f0;
  color: var(--mobile-danger);
  font-size: 13px;
}
</style>
