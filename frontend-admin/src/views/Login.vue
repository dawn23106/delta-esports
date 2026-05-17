<template>
  <div class="login">
    <el-card style="width:360px">
      <h2 style="text-align:center">客服管理后台</h2>
      <el-form @submit.prevent="doLogin">
        <el-form-item><el-input v-model="phone" placeholder="手机号" /></el-form-item>
        <el-form-item><el-input v-model="password" type="password" placeholder="密码" /></el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading" style="width:100%">登录</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { login } from '../api/admin'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const phone = ref('')
const password = ref('')
const loading = ref(false)
const router = useRouter()

async function doLogin() {
  loading.value = true
  try {
    const res = await login(phone.value, password.value)
    localStorage.setItem('accessToken', res.data.accessToken)
    localStorage.setItem('refreshToken', res.data.refreshToken)
    ElMessage.success('登录成功')
    router.push('/orders')
  } finally { loading.value = false }
}
</script>

<style scoped>
.login { display:flex; align-items:center; justify-content:center; min-height:100vh; background:#f0f2f5; }
</style>
