<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { register as registerApi } from '../api/auth'
import { useAuthStore } from '../store/auth'
import { showToast } from 'vant'
import LiquidBackground from '../components/LiquidBackground.vue'

const router = useRouter()
const auth = useAuthStore()
const phone = ref('')
const password = ref('')
const nickname = ref('')
const role = ref<'boss' | 'booster'>('boss')
const loading = ref(false)
const errorMsg = ref('')

const roleOptions = [
  { value: 'boss' as const, label: '我是老板', desc: '找陪玩、下单', icon: '👑' },
  { value: 'booster' as const, label: '我是陪陪', desc: '接单赚钱', icon: '⚡' },
]

async function handleRegister() {
  errorMsg.value = ''
  if (!phone.value) { errorMsg.value = '请输入手机号'; return }
  if (!/^1[3-9]\d{9}$/.test(phone.value)) { errorMsg.value = '手机号格式不正确'; return }
  if (!nickname.value.trim()) { errorMsg.value = '请输入昵称'; return }
  if (!password.value || password.value.length < 6) { errorMsg.value = '密码至少6位'; return }
  loading.value = true
  try {
    const res: any = await registerApi(phone.value, password.value, nickname.value.trim(), role.value)
    auth.setAuth(res, true)
    showToast('注册成功！')
    router.push(role.value === 'booster' ? '/booster/pool' : '/boss/home')
  } catch (e: any) {
    errorMsg.value = e?.response?.data?.message || '注册失败，请重试'
  } finally { loading.value = false }
}
</script>

<template>
  <LiquidBackground>
    <div class="reg-page">
      <div class="logo-area">
        <div class="logo-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="1.5" class="logo-svg">
            <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/>
          </svg>
        </div>
        <h1>创建账号</h1>
        <p>加入沧月电竞</p>
      </div>

      <div class="card">
        <input v-model="phone" type="tel" maxlength="11" placeholder="手机号" class="input" />
        <input v-model="nickname" type="text" maxlength="20" placeholder="昵称" class="input" />
        <input v-model="password" type="password" placeholder="密码（至少6位）" class="input" @keyup.enter="handleRegister" />

        <div class="role-row">
          <div v-for="r in roleOptions" :key="r.value"
            :class="['role-card', { active: role === r.value }]" @click="role = r.value">
            <span class="role-emoji">{{ r.icon }}</span>
            <div>
              <div class="role-label">{{ r.label }}</div>
              <div class="role-desc">{{ r.desc }}</div>
            </div>
          </div>
        </div>

        <div v-if="errorMsg" class="error">{{ errorMsg }}</div>

        <button class="btn" :disabled="loading" @click="handleRegister">
          {{ loading ? '注册中...' : '注 册' }}
        </button>
      </div>

      <div class="links">
        <router-link to="/login" class="link">已有账号？去登录</router-link>
      </div>
    </div>
  </LiquidBackground>
</template>

<style scoped>
.reg-page { min-height: 100vh; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 0 28px; }

.logo-area { text-align: center; margin-bottom: 32px; }
.logo-icon { width: 64px; height: 64px; border-radius: 20px; background: rgba(255,255,255,.15); backdrop-filter: blur(12px); display: flex; align-items: center; justify-content: center; margin: 0 auto 12px; border: 2px solid rgba(255,255,255,.2); }
.logo-svg { width: 32px; height: 32px; }
.logo-area h1 { font-size: 24px; font-weight: 900; color: #fff; letter-spacing: 3px; margin: 0; }
.logo-area p { font-size: 12px; color: rgba(255,255,255,.5); letter-spacing: 4px; margin-top: 4px; }

.card { width: 100%; max-width: 360px; background: rgba(255,255,255,.1); backdrop-filter: blur(20px); border-radius: 20px; padding: 24px 20px; border: 1px solid rgba(255,255,255,.15); }
.input { width: 100%; padding: 14px 16px; border-radius: 12px; background: rgba(255,255,255,.12); border: 1px solid rgba(255,255,255,.1); color: #fff; font-size: 15px; outline: none; margin-bottom: 14px; box-sizing: border-box; }
.input::placeholder { color: rgba(255,255,255,.4); }
.input:focus { border-color: rgba(255,255,255,.3); background: rgba(255,255,255,.18); }

.role-row { display: flex; gap: 10px; margin-bottom: 14px; }
.role-card { flex: 1; padding: 12px; border-radius: 12px; background: rgba(255,255,255,.06); border: 2px solid rgba(255,255,255,.08); cursor: pointer; display: flex; align-items: center; gap: 10px; transition: all .2s; }
.role-card.active { background: rgba(255,255,255,.15); border-color: rgba(255,255,255,.3); }
.role-emoji { font-size: 24px; }
.role-label { font-size: 14px; font-weight: 600; color: #fff; }
.role-desc { font-size: 11px; color: rgba(255,255,255,.5); }

.error { background: rgba(239,68,68,.2); border: 1px solid rgba(239,68,68,.4); border-radius: 10px; padding: 10px 14px; color: #fecaca; font-size: 13px; margin-bottom: 14px; }

.btn { width: 100%; padding: 14px; border: none; border-radius: 14px; background: #fff; color: #6366f1; font-size: 16px; font-weight: 700; cursor: pointer; letter-spacing: 4px; transition: all .2s; }
.btn:disabled { opacity: .5; }

.links { margin-top: 24px; }
.link { color: rgba(255,255,255,.7); font-size: 13px; text-decoration: none; }
</style>
