<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../store/auth'
import { getMyOrders } from '../../api/orders'
import request from '../../api/request'
import { showToast, showDialog } from 'vant'

const router = useRouter()
const auth = useAuthStore()
const active = ref(3)
const user = ref<any>({ nickname: '', starLevel: 0, totalSpent: 0, avatar: '' })
const activeOrder = ref<any>(null)
const nextLevelAmount = ref(0)
const levelProgress = ref(0)
const totalOrders = ref(0)
const starLevels = [0, 100, 500, 2000, 5000, 10000]
const starNames = ['', 'Lv.1', 'Lv.2', 'Lv.3', 'Lv.4', 'Lv.5']

const entries = [
  { label: '订单记录', icon: '📋', path: '/boss/orders' },
  { label: '我的礼物', icon: '🎁', path: '/boss/gifts' },
  { label: '我的评价', icon: '⭐', path: '/boss/reviews' },
  { label: '客服中心', icon: '🎧', path: '/boss/service' },
  { label: '联系客服', icon: '📞', path: '/boss/contact' },
  { label: '设置', icon: '⚙️', path: '/boss/settings' },
]

function statusLabel(s: string) {
  const m: Record<string, string> = { pending:'待接单',assigned:'已接单',in_progress:'进行中',completed:'待审核',done:'已确认',settled:'已结算',cancelled:'已取消',disputed:'争议中' }
  return m[s] || s
}

function statusColor(s: string) {
  const m: Record<string, string> = { pending:'#f59e0b',assigned:'#3b82f6',in_progress:'#6366f1',completed:'#8b5cf6',done:'#10b981',settled:'#6b7280',cancelled:'#ef4444' }
  return m[s] || '#94a3b8'
}

async function loadData() {
  try {
    const r: any = await request.get('/users/me'); user.value = r || user.value
    const lv = user.value.starLevel || 0
    if (lv < 5) {
      nextLevelAmount.value = starLevels[lv + 1] - (user.value.totalSpent || 0)
      levelProgress.value = Math.min(100, Math.max(0, ((user.value.totalSpent || 0) - starLevels[lv]) / (starLevels[lv + 1] - starLevels[lv]) * 100))
    } else { levelProgress.value = 100 }
  } catch { }
  try {
    const r: any = await getMyOrders(1, 100)
    const orders = r.records || []
    totalOrders.value = orders.length
    activeOrder.value = orders.find((o: any) => !['settled', 'cancelled'].includes(o.status)) || null
  } catch { }
}

async function handleLogout() {
  try {
    await showDialog({ title: '退出登录', message: '确定要退出吗？', confirmButtonColor: '#ef4444', showCancelButton: true })
    auth.logout(); router.push('/login')
  } catch { }
}

onMounted(loadData)
</script>

<template>
  <div class="page">
    <!-- 头部 -->
    <div class="header">
      <div class="header-blob" />
      <div class="header-content">
        <div class="avatar-wrap">
          <van-image round width="72" height="72" :src="user.avatar || 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'" class="avatar" />
          <div class="star-badge">{{ user.starLevel || 0 }}</div>
        </div>
        <div class="user-info">
          <div class="nickname">{{ user.nickname || auth.nickname }}</div>
          <div class="star-tag">{{ starNames[user.starLevel || 0] }}</div>
        </div>
      </div>
      <div class="header-stats">
        <div class="h-stat">
          <span class="h-stat-val">{{ totalOrders }}</span>
          <span class="h-stat-lbl">总订单</span>
        </div>
        <div class="h-stat-div" />
        <div class="h-stat">
          <span class="h-stat-val">¥{{ user.totalSpent || 0 }}</span>
          <span class="h-stat-lbl">累计消费</span>
        </div>
        <div class="h-stat-div" />
        <div class="h-stat">
          <span class="h-stat-val">{{ levelProgress }}%</span>
          <span class="h-stat-lbl">升级进度</span>
        </div>
      </div>
    </div>

    <!-- 进度条 -->
    <div class="progress-wrap">
      <div class="progress-bar">
        <div class="progress-fill" :style="{ width: levelProgress + '%' }" />
      </div>
      <div class="progress-text">
        <span v-if="(user.starLevel || 0) < 5">距 {{ starNames[(user.starLevel || 0) + 1] }} 还需 ¥{{ nextLevelAmount }}</span>
        <span v-else>已满级 🎉</span>
      </div>
    </div>

    <!-- 当前订单 -->
    <div class="section">
      <div class="section-title">当前订单</div>
      <div v-if="activeOrder" class="order-card" @click="router.push(`/boss/order/${activeOrder.id}`)">
        <div class="order-left">
          <div class="order-badge" :style="{ background: statusColor(activeOrder.status) }">{{ statusLabel(activeOrder.status) }}</div>
          <div>
            <div class="order-id">#{{ activeOrder.id }}</div>
            <div class="order-meta">{{ activeOrder.gameMap || '未指定地图' }} · {{ activeOrder.createdAt?.replace('T',' ').substring(5,16) }}</div>
          </div>
        </div>
        <div class="order-right">
          <span class="order-price">¥{{ activeOrder.amount }}</span>
          <span class="order-arrow">›</span>
        </div>
      </div>
      <div v-else class="order-empty" @click="router.push('/boss/home')">
        <span>📭 暂无进行中的订单，去首页逛逛</span>
        <span class="order-empty-arrow">›</span>
      </div>
    </div>

    <!-- 快捷入口 -->
    <div class="section">
      <div class="section-title">服务</div>
      <div class="entries">
        <div v-for="e in entries" :key="e.label" class="entry" @click="router.push(e.path)">
          <span class="entry-icon">{{ e.icon }}</span>
          <span class="entry-label">{{ e.label }}</span>
        </div>
      </div>
    </div>

    <!-- 退出 -->
    <div class="logout-wrap">
      <span class="logout-btn" @click="handleLogout">退出登录</span>
    </div>

    <div style="height:60px" />

    <van-tabbar v-model="active" route :border="false" active-color="#6366f1" inactive-color="#94a3b8" safe-area-inset-bottom class="tabbar">
      <van-tabbar-item icon="home-o" to="/boss/home">首页</van-tabbar-item>
      <van-tabbar-item icon="friends-o" to="/boss/choose">选陪陪</van-tabbar-item>
      <van-tabbar-item icon="chat-o" to="/boss/messages">消息</van-tabbar-item>
      <van-tabbar-item icon="user-o" to="/boss/profile">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<style scoped>
.page { min-height: 100vh; background: #f5f6fa; padding-bottom: 60px; }

/* Header */
.header { background: linear-gradient(135deg, #6366f1, #8b5cf6); padding: 28px 20px 20px; position: relative; overflow: hidden; }
.header-blob { position: absolute; top: -50px; right: -30px; width: 160px; height: 160px; border-radius: 50%; background: rgba(255,255,255,.06); }
.header-content { display: flex; align-items: center; gap: 16px; position: relative; z-index: 1; margin-bottom: 20px; }
.avatar-wrap { position: relative; flex-shrink: 0; }
.avatar { border: 3px solid rgba(255,255,255,.25); box-shadow: 0 4px 16px rgba(0,0,0,.15); border-radius: 50%; overflow: hidden; }
.star-badge { position: absolute; bottom: -2px; right: -2px; width: 26px; height: 26px; border-radius: 50%; background: #fbbf24; color: #78350f; font-size: 12px; font-weight: 800; display: flex; align-items: center; justify-content: center; box-shadow: 0 2px 6px rgba(0,0,0,.15); border: 2px solid #fff; }
.nickname { font-size: 22px; font-weight: 700; color: #fff; }
.star-tag { background: rgba(255,255,255,.2); padding: 2px 12px; border-radius: 20px; font-size: 12px; color: #fff; display: inline-block; margin-top: 4px; }

.header-stats { display: flex; background: rgba(255,255,255,.12); border-radius: 14px; padding: 14px 8px; position: relative; z-index: 1; backdrop-filter: blur(8px); }
.h-stat { flex: 1; text-align: center; }
.h-stat-val { font-size: 18px; font-weight: 800; color: #fff; display: block; }
.h-stat-lbl { font-size: 11px; color: rgba(255,255,255,.6); margin-top: 2px; display: block; }
.h-stat-div { width: 1px; background: rgba(255,255,255,.12); }

/* Progress */
.progress-wrap { padding: 12px 20px; background: #fff; margin: 0 0 4px; }
.progress-bar { height: 6px; border-radius: 3px; background: #f0f0f0; overflow: hidden; }
.progress-fill { height: 100%; border-radius: 3px; background: linear-gradient(90deg, #6366f1, #8b5cf6); transition: width .8s ease-out; }
.progress-text { font-size: 12px; color: #888; margin-top: 6px; }

/* Section */
.section { padding: 12px 16px 0; }
.section-title { font-size: 14px; font-weight: 700; color: #1e293b; margin-bottom: 10px; }

/* Order Card */
.order-card { background: #fff; border-radius: 14px; padding: 16px; display: flex; justify-content: space-between; align-items: center; box-shadow: 0 1px 3px rgba(0,0,0,.04); cursor: pointer; }
.order-left { display: flex; align-items: center; gap: 12px; }
.order-badge { padding: 4px 12px; border-radius: 8px; color: #fff; font-size: 12px; font-weight: 600; }
.order-id { font-size: 15px; font-weight: 600; color: #1e293b; }
.order-meta { font-size: 12px; color: #94a3b8; margin-top: 2px; }
.order-right { display: flex; align-items: center; gap: 6px; }
.order-price { font-size: 18px; font-weight: 800; color: #6366f1; }
.order-arrow { font-size: 20px; color: #cbd5e1; }
.order-empty { background: #fff; border-radius: 14px; padding: 16px; display: flex; justify-content: space-between; align-items: center; font-size: 13px; color: #94a3b8; cursor: pointer; box-shadow: 0 1px 3px rgba(0,0,0,.04); }
.order-empty-arrow { font-size: 18px; }

/* Entries */
.entries { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 10px; }
.entry { background: #fff; border-radius: 14px; padding: 18px 10px; text-align: center; cursor: pointer; box-shadow: 0 1px 3px rgba(0,0,0,.03); transition: all .15s; }
.entry:active { background: #f8fafc; transform: scale(.96); }
.entry-icon { font-size: 28px; display: block; margin-bottom: 6px; }
.entry-label { font-size: 12px; color: #475569; font-weight: 500; }

/* Logout */
.logout-wrap { padding: 24px 16px 0; text-align: center; }
.logout-btn { color: #94a3b8; font-size: 13px; cursor: pointer; }

.tabbar { background: rgba(255,255,255,.9) !important; backdrop-filter: blur(20px) !important; border-top: 1px solid #f1f5f9 !important; }
</style>
