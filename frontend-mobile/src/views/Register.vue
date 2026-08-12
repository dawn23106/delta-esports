<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { register as registerApi } from '../api/auth'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const auth = useAuthStore()
const phone = ref('')
const password = ref('')
const nickname = ref('')
const role = ref<'boss' | 'booster'>('boss')
const loading = ref(false)
const errorMsg = ref('')

const roleOptions = [
  { value: 'boss' as const, label: '我是老板', desc: '下单找陪玩', icon: 'manager-o' },
  { value: 'booster' as const, label: '我是陪玩', desc: '接单赚收益', icon: 'medal-o' },
]

async function handleRegister() {
  errorMsg.value = ''
  if (!/^1[3-9]\d{9}$/.test(phone.value)) {
    errorMsg.value = '请输入正确的手机号'
    return
  }
  if (!nickname.value.trim()) {
    errorMsg.value = '请输入昵称'
    return
  }
  if (!password.value || password.value.length < 6) {
    errorMsg.value = '密码至少 6 位'
    return
  }

  loading.value = true
  try {
    const result: any = await registerApi(phone.value, password.value, nickname.value.trim(), role.value)
    auth.setAuth(result, true)
    showToast({ message: '注册成功', icon: 'success' })
    router.push(role.value === 'booster' ? '/booster/pool' : '/boss/home')
  } catch (error: any) {
    errorMsg.value = error?.response?.data?.message || '注册失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <section class="brand-panel">
      <div class="brand-mark">
        <van-icon name="plus" />
      </div>
      <h1>创建账号</h1>
      <p>选择身份后进入对应移动端工作台。</p>
    </section>

    <section class="auth-card">
      <div class="auth-title">
        <span>加入沧月</span>
        <router-link to="/login">去登录</router-link>
      </div>

      <van-field v-model="phone" type="tel" maxlength="11" label="手机号" placeholder="请输入手机号" clearable />
      <van-field v-model="nickname" maxlength="20" label="昵称" placeholder="请输入昵称" clearable />
      <van-field v-model="password" type="password" label="密码" placeholder="至少 6 位" clearable @keyup.enter="handleRegister" />

      <div class="role-grid">
        <button
          v-for="option in roleOptions"
          :key="option.value"
          type="button"
          class="role-card"
          :class="{ active: role === option.value }"
          @click="role = option.value"
        >
          <van-icon :name="option.icon" />
          <span>{{ option.label }}</span>
          <small>{{ option.desc }}</small>
        </button>
      </div>

      <div v-if="errorMsg" class="form-error">{{ errorMsg }}</div>

      <van-button block round type="primary" size="large" :loading="loading" color="linear-gradient(135deg, #3157ff, #08b6d8)" @click="handleRegister">
        注册并进入
      </van-button>
    </section>
  </main>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  padding: 26px 18px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 18px;
  background:
    linear-gradient(135deg, rgba(16,19,35,.74), rgba(8,182,216,.42)),
    url('https://images.unsplash.com/photo-1493711662062-fa541adb3fc8?w=1000&h=1400&fit=crop');
  background-size: cover;
  background-position: center;
}

.brand-panel {
  color: #fff;
}

.brand-mark {
  width: 56px;
  height: 56px;
  border-radius: 19px;
  display: grid;
  place-items: center;
  background: rgba(255,255,255,.16);
  border: 1px solid rgba(255,255,255,.22);
  backdrop-filter: blur(18px);
  font-size: 28px;
}

.brand-panel h1 {
  margin: 16px 0 8px;
  font-size: 32px;
  font-weight: 950;
  letter-spacing: 0;
}

.brand-panel p {
  margin: 0;
  color: rgba(255,255,255,.76);
  font-size: 14px;
}

.auth-card {
  border-radius: 24px;
  background: rgba(255,255,255,.93);
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
}

.auth-title span {
  font-size: 20px;
  font-weight: 900;
}

.auth-title a {
  color: var(--mobile-brand);
  font-size: 13px;
  font-weight: 800;
  text-decoration: none;
}

.role-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.role-card {
  border: 1px solid var(--mobile-line);
  border-radius: 16px;
  background: #fff;
  padding: 14px 10px;
  display: grid;
  justify-items: center;
  gap: 4px;
  color: var(--mobile-muted);
}

.role-card .van-icon {
  font-size: 24px;
}

.role-card span {
  color: var(--mobile-ink);
  font-size: 14px;
  font-weight: 850;
}

.role-card small {
  font-size: 11px;
}

.role-card.active {
  border-color: rgba(49,87,255,.32);
  background: #eef4ff;
  color: var(--mobile-brand);
}

.form-error {
  border-radius: 12px;
  padding: 10px 12px;
  background: #fff1f0;
  color: var(--mobile-danger);
  font-size: 13px;
}
</style>
