<template>
  <div class="login-page">
    <h2>三角洲陪玩接单</h2>
    <van-form @submit="onSubmit">
      <van-cell-group inset>
        <van-field v-model="phone" name="phone" label="手机号" placeholder="输入手机号"
          :rules="[{ required: true, pattern: /^1[3-9]\d{9}$/, message: '请输入正确手机号' }]" />
        <van-field v-model="password" name="password" label="密码" placeholder="输入密码" type="password"
          :rules="[{ required: true, message: '请输入密码' }]" />
      </van-cell-group>
      <div style="margin: 16px 16px">
        <van-button round block type="primary" native-type="submit" :loading="loading">登录</van-button>
        <van-button round block plain style="margin-top:10px" @click="doRegister" :loading="loading">注册</van-button>
      </div>
    </van-form>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { showToast } from 'vant'
import { login, register } from '../api/auth'
import { useAuthStore } from '../store/auth'
import { useRouter } from 'vue-router'

const phone = ref('')
const password = ref('')
const loading = ref(false)
const auth = useAuthStore()
const router = useRouter()

async function doLogin() {
  loading.value = true
  try {
    const res = await login(phone.value, password.value)
    auth.setToken(res.data.accessToken, res.data.refreshToken)
    await auth.fetchMe()
    showToast('登录成功')
    router.push(auth.role === 'booster' ? '/booster/pool' : '/player/home')
  } finally { loading.value = false }
}

async function doRegister() {
  loading.value = true
  try {
    const res = await register(phone.value, password.value)
    auth.setToken(res.data.accessToken, res.data.refreshToken)
    await auth.fetchMe()
    showToast('注册成功')
    router.push('/player/home')
  } finally { loading.value = false }
}

function onSubmit() { doLogin() }
</script>

<style scoped>
.login-page { display:flex; flex-direction:column; align-items:center; justify-content:center; min-height:100vh; background:#f7f8fa; }
h2 { margin-bottom: 24px; color: #1677ff; }
</style>
