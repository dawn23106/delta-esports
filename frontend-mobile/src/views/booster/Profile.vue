<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '../../store/auth'
import request from '../../api/request'
import { getMyOrders } from '../../api/orders'
import { showToast, showDialog } from 'vant'

const auth = useAuthStore()
const active = ref(2)

const user = ref<any>({ nickname: '', boosterLevel: '', boosterStatus: 'offline', rating: 5, orderCount: 0 })
const totalEarned = ref(0)
const pendingSettlement = ref(0)
const completedCount = ref(0)
const completionRate = ref(0)
const levelLabel: Record<string, string> = { entertainment: '娱乐陪', tech: '技术陪', top: '顶尖陪' }

async function loadData() {
  try {
    const r1: any = await request.get('/users/me')
    user.value = r1 || user.value
    totalEarned.value = user.value.totalEarned || 0
  } catch { }
  try {
    const r2: any = await getMyOrders(1, 100)
    const orders = r2.records || []
    completedCount.value = orders.filter((o: any) => o.status === 'settled').length
    const total = orders.filter((o: any) => o.status !== 'cancelled').length
    completionRate.value = total > 0 ? Math.round(completedCount.value / total * 100) : 0
    pendingSettlement.value = orders
      .filter((o: any) => o.status === 'done' || o.status === 'completed')
      .reduce((sum: number, o: any) => sum + (o.amount || 0), 0)
  } catch { }
}

async function toggleStatus() {
  const newStatus = user.value.boosterStatus === 'idle' ? 'busy' : 'idle'
  try {
    await request.put('/users/booster/status', null, { params: { status: newStatus } })
    user.value.boosterStatus = newStatus
    showToast(newStatus === 'idle' ? '已切换为空闲，可以接单啦！' : '已切换为忙碌')
  } catch (e: any) { showToast(e?.response?.data?.message || '切换失败') }
}

async function handleLogout() {
  try {
    await showDialog({
      title: '退出登录',
      message: '确定要退出当前账号吗？',
      confirmButtonColor: '#ef4444',
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      showCancelButton: true,
    })
    auth.logout()
    // router will redirect via guard
    window.location.href = '/login'
  } catch { }
}

onMounted(loadData)
</script>

<template>
  <div class="page">
    <!-- 头部 -->
    <div class="header">
      <div class="header-deco"></div>
      <div class="header-content">
        <div class="user-row">
          <div class="avatar-wrap">
            <van-image round width="64" height="64" src="https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg" class="avatar" />
            <div class="online-dot" :class="{ on: user.boosterStatus === 'idle' }"></div>
          </div>
          <div class="user-info">
            <div class="nickname">{{ user.nickname || auth.nickname }}</div>
            <div class="level-badge">{{ levelLabel[user.boosterLevel] || user.boosterLevel }}</div>
          </div>
          <div class="toggle-area" @click="toggleStatus">
            <div class="toggle-track" :class="{ active: user.boosterStatus === 'idle' }">
              <div class="toggle-thumb"></div>
            </div>
            <div class="toggle-label">{{ user.boosterStatus === 'idle' ? '接单中' : '休息中' }}</div>
          </div>
        </div>

        <!-- 收入三栏 -->
        <div class="income-row">
          <div class="income-item">
            <div class="income-val">¥{{ totalEarned }}</div>
            <div class="income-label">累计收入</div>
          </div>
          <div class="income-divider"></div>
          <div class="income-item">
            <div class="income-val pending">¥{{ pendingSettlement }}</div>
            <div class="income-label">待结算</div>
          </div>
          <div class="income-divider"></div>
          <div class="income-item">
            <div class="income-val avail">¥{{ totalEarned - pendingSettlement }}</div>
            <div class="income-label">可提现</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 数据统计 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon" style="background:#eef2ff">📋</div>
        <div class="stat-num">{{ user.orderCount || 0 }}</div>
        <div class="stat-label">总接单</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:#ecfdf5">✅</div>
        <div class="stat-num">{{ completionRate }}%</div>
        <div class="stat-label">完成率</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:#fffbeb">⭐</div>
        <div class="stat-num">{{ user.rating || '5.0' }}</div>
        <div class="stat-label">评分</div>
      </div>
    </div>

    <!-- 菜单 -->
    <div class="menu-section">
      <div class="menu-item" @click="$router.push('/booster/messages')">
        <span class="menu-icon">💬</span>
        <span class="menu-label">我的消息</span>
        <span class="menu-arrow">›</span>
      </div>
      <div class="menu-item">
        <span class="menu-icon">⚙️</span>
        <span class="menu-label">设置</span>
        <span class="menu-arrow">›</span>
      </div>
      <div class="menu-item logout" @click="handleLogout">
        <span class="menu-icon">🚪</span>
        <span class="menu-label">退出登录</span>
        <span class="menu-arrow">›</span>
      </div>
    </div>

    <van-tabbar v-model="active" route :border="false" active-color="#6366f1" inactive-color="#94a3b8" safe-area-inset-bottom class="tabbar">
      <van-tabbar-item icon="orders-o" to="/booster/pool">订单池</van-tabbar-item>
      <van-tabbar-item icon="logistics" to="/booster/orders">进行中</van-tabbar-item>
      <van-tabbar-item icon="user-o" to="/booster/profile">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f8fafc;
  padding-bottom: 60px;
}

/* 头部 */
.header {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 40%, #a78bfa 100%);
  padding: 32px 20px 28px;
  position: relative;
  overflow: hidden;
}
.header-deco {
  position: absolute;
  top: -40px;
  right: -20px;
  width: 160px;
  height: 160px;
  border-radius: 50%;
  background: rgba(255,255,255,0.06);
}
.header-content {
  position: relative;
  z-index: 1;
}

/* 用户行 */
.user-row {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 24px;
}
.avatar-wrap {
  position: relative;
  flex-shrink: 0;
}
.avatar {
  border: 3px solid rgba(255,255,255,0.3);
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}
.online-dot {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #ef4444;
  border: 2px solid #fff;
}
.online-dot.on {
  background: #10b981;
}
.user-info {
  flex: 1;
}
.nickname {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
}
.level-badge {
  display: inline-block;
  margin-top: 4px;
  padding: 2px 12px;
  border-radius: 20px;
  background: rgba(255,255,255,0.2);
  backdrop-filter: blur(8px);
  font-size: 12px;
  color: #fff;
  font-weight: 500;
}
.toggle-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}
.toggle-track {
  width: 48px;
  height: 28px;
  border-radius: 14px;
  background: rgba(255,255,255,0.25);
  position: relative;
  transition: background 0.3s;
}
.toggle-track.active {
  background: #10b981;
}
.toggle-thumb {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0,0,0,0.15);
  transition: transform 0.3s;
}
.toggle-track.active .toggle-thumb {
  transform: translateX(20px);
}
.toggle-label {
  font-size: 11px;
  color: rgba(255,255,255,0.7);
}

/* 收入 */
.income-row {
  display: flex;
  background: rgba(255,255,255,0.12);
  backdrop-filter: blur(12px);
  border-radius: 16px;
  padding: 16px 8px;
}
.income-item {
  flex: 1;
  text-align: center;
}
.income-val {
  font-size: 18px;
  font-weight: 800;
  color: #fff;
}
.income-val.pending {
  color: #fde68a;
}
.income-val.avail {
  color: #a7f3d0;
}
.income-label {
  font-size: 11px;
  color: rgba(255,255,255,0.6);
  margin-top: 2px;
}
.income-divider {
  width: 1px;
  background: rgba(255,255,255,0.15);
}

/* 统计 */
.stats-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 10px;
  padding: 16px;
  margin-top: -12px;
  position: relative;
  z-index: 2;
}
.stat-card {
  background: #fff;
  border-radius: 16px;
  padding: 16px 8px;
  text-align: center;
  box-shadow: 0 4px 16px rgba(0,0,0,0.04);
  border: 1px solid #f1f5f9;
}
.stat-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 8px;
  font-size: 16px;
}
.stat-num {
  font-size: 18px;
  font-weight: 800;
  color: #1e293b;
}
.stat-label {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 2px;
}

/* 菜单 */
.menu-section {
  padding: 0 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #fff;
  border-radius: 14px;
  cursor: pointer;
  box-shadow: 0 1px 3px rgba(0,0,0,0.02);
  border: 1px solid #f8fafc;
  transition: background 0.15s;
}
.menu-item:active {
  background: #f8fafc;
}
.menu-item.logout {
  margin-top: 8px;
}
.menu-icon {
  font-size: 20px;
}
.menu-label {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
  color: #475569;
}
.menu-item.logout .menu-label {
  color: #ef4444;
}
.menu-arrow {
  font-size: 20px;
  color: #cbd5e1;
}

.tabbar {
  background: rgba(255,255,255,0.9) !important;
  backdrop-filter: blur(20px) !important;
  border-top: 1px solid #f1f5f9 !important;
}
</style>
