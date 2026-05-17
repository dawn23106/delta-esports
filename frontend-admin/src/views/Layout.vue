<template>
  <el-container>
    <el-header style="background:#1677ff;color:#fff;display:flex;align-items:center;justify-content:space-between">
      <span>三角洲陪玩 - 客服管理后台</span>
      <el-button text style="color:#fff" @click="logout">退出</el-button>
    </el-header>
    <el-container>
      <el-aside width="180px" style="background:#fff;min-height:calc(100vh - 60px);border-right:1px solid #ebeef5">
        <el-menu :default-active="route.path" router>
          <el-menu-item index="/orders">订单管理</el-menu-item>
          <el-menu-item index="/users">用户管理</el-menu-item>
        </el-menu>
      </el-aside>
      <el-main style="background:#f0f2f5">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
const router = useRouter()
const route = useRoute()
function logout() {
  localStorage.clear()
  router.push('/login')
}
</script>
