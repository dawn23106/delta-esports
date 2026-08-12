<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
const collapsed = ref(false)
const user = JSON.parse(localStorage.getItem('adminUser') || '{}')

const menuItems = [
  { path: '/dashboard', title: '数据看板', icon: 'DataAnalysis' },
  { path: '/orders', title: '订单管理', icon: 'Document' },
  { path: '/bosses', title: '老板管理', icon: 'User' },
  { path: '/boosters', title: '陪陪管理', icon: 'Avatar' },
  { path: '/services', title: '服务管理', icon: 'Menu' },
  { path: '/settlements', title: '结算审核', icon: 'Money' },
  { path: '/announcements', title: '公告管理', icon: 'Notification' },
  { path: '/gifts', title: '礼物记录', icon: 'Present' },
]

const activeMenu = computed(() => route.path)

function logout() {
  localStorage.clear()
  router.push('/login')
}
</script>

<template>
  <div class="layout">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed }">
      <div class="sidebar-logo" @click="router.push('/dashboard')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="logo-svg">
          <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/>
        </svg>
        <span v-show="!collapsed" class="logo-text">沧月</span>
      </div>
      <nav class="sidebar-nav">
        <div v-for="item in menuItems" :key="item.path"
          :class="['nav-item', { active: activeMenu === item.path }]"
          @click="router.push(item.path)"
          :title="collapsed ? item.title : ''"
        >
          <el-icon size="20"><component :is="item.icon" /></el-icon>
          <span v-show="!collapsed" class="nav-label">{{ item.title }}</span>
        </div>
      </nav>
      <div class="sidebar-foot">
        <div class="nav-item" @click="collapsed = !collapsed" :title="collapsed ? '展开' : '收起'">
          <el-icon size="18"><component :is="collapsed ? 'DArrowRight' : 'DArrowLeft'" /></el-icon>
          <span v-show="!collapsed" class="nav-label">收起</span>
        </div>
        <div class="nav-item" @click="logout" title="退出">
          <el-icon size="18"><component is="SwitchButton" /></el-icon>
          <span v-show="!collapsed" class="nav-label">退出</span>
        </div>
      </div>
    </aside>

    <!-- 主区域 -->
    <main class="main" :class="{ expanded: collapsed }">
      <header class="topbar">
        <div class="topbar-left">
          <span class="topbar-title">{{ route.meta.title || '管理后台' }}</span>
        </div>
        <div class="topbar-right">
          <div class="user-avatar">
            <el-icon size="18"><component is="User" /></el-icon>
          </div>
          <span class="user-name">{{ user.nickname || 'Admin' }}</span>
        </div>
      </header>
      <div class="main-content">
        <router-view />
      </div>
    </main>
  </div>
</template>

<style scoped>
.layout { display: flex; min-height: 100vh; background: #F5F5F5; }

/* 侧边栏 */
.sidebar {
  width: 200px; background: #000; display: flex; flex-direction: column;
  position: fixed; top: 0; left: 0; bottom: 0; z-index: 100;
  transition: width .2s ease;
}
.sidebar.collapsed { width: 60px; }

.sidebar-logo {
  padding: 18px 16px; display: flex; align-items: center; gap: 10px;
  cursor: pointer; border-bottom: 1px solid rgba(255,255,255,.08);
}
.logo-svg { width: 22px; height: 22px; color: #fff; flex-shrink: 0; }
.logo-text { font-size: 16px; font-weight: 700; color: #fff; white-space: nowrap; }

.sidebar-nav { flex: 1; padding: 12px 8px; display: flex; flex-direction: column; gap: 2px; overflow-y: auto; }
.sidebar-foot { padding: 8px; border-top: 1px solid rgba(255,255,255,.08); display: flex; flex-direction: column; gap: 2px; }

.nav-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px; border-radius: 8px; cursor: pointer;
  color: rgba(255,255,255,.45); transition: all .15s; white-space: nowrap;
}
.nav-item:hover { background: rgba(255,255,255,.06); color: rgba(255,255,255,.75); }
.nav-item.active { background: rgba(99,102,241,.2); color: #A5B4FC; }
.nav-label { font-size: 13px; font-weight: 500; }

/* 主区域 */
.main { flex: 1; margin-left: 200px; display: flex; flex-direction: column; transition: margin-left .2s ease; }
.main.expanded { margin-left: 60px; }

.topbar {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 24px; background: #fff; border-bottom: 1px solid #F0F0F0;
  position: sticky; top: 0; z-index: 50;
}
.topbar-title { font-size: 15px; font-weight: 600; color: #333; }
.topbar-right { display: flex; align-items: center; gap: 10px; }
.user-avatar { width: 34px; height: 34px; border-radius: 50%; background: #F0F0F0; display: flex; align-items: center; justify-content: center; }
.user-name { font-size: 13px; font-weight: 500; color: #333; }

.main-content { flex: 1; padding: 20px 24px; overflow-y: auto; }
</style>
