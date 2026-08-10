<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { showConfirmDialog, showToast } from 'vant'
import request from '../../api/request'
import { getMyOrders } from '../../api/orders'
import { useAuthStore } from '../../store/auth'
import MobileTabbar from '../../components/MobileTabbar.vue'

const auth = useAuthStore()
const user = ref<any>({ nickname: '', boosterLevel: '', boosterStatus: 'offline', rating: 5, orderCount: 0 })
const totalEarned = ref(0)
const pendingSettlement = ref(0)
const completedCount = ref(0)
const completionRate = ref(0)

const levelLabel: Record<string, string> = {
  entertainment: '娱乐陪',
  tech: '技术陪',
  top: '顶尖陪',
}

async function loadData() {
  try {
    const profile: any = await request.get('/users/me')
    user.value = profile || user.value
    totalEarned.value = user.value.totalEarned || 0
  } catch {
    user.value.nickname = auth.nickname
  }

  try {
    const result: any = await getMyOrders(1, 100)
    const orders = result.records || []
    completedCount.value = orders.filter((order: any) => order.status === 'settled').length
    const validOrders = orders.filter((order: any) => order.status !== 'cancelled').length
    completionRate.value = validOrders > 0 ? Math.round(completedCount.value / validOrders * 100) : 0
    pendingSettlement.value = orders
      .filter((order: any) => ['done', 'completed'].includes(order.status))
      .reduce((sum: number, order: any) => sum + (order.amount || 0), 0)
  } catch {
    completedCount.value = 0
  }
}

async function toggleStatus() {
  const newStatus = user.value.boosterStatus === 'idle' ? 'busy' : 'idle'
  try {
    await request.put('/users/booster/status', null, { params: { status: newStatus } })
    user.value.boosterStatus = newStatus
    showToast(newStatus === 'idle' ? '已切换为空闲，可接单' : '已切换为忙碌')
  } catch (error: any) {
    showToast(error?.response?.data?.message || '切换失败')
  }
}

async function handleLogout() {
  try {
    await showConfirmDialog({ title: '退出登录', message: '确定要退出当前账号吗？', confirmButtonText: '退出', confirmButtonColor: '#f04438' })
    auth.logout()
    window.location.href = '/login'
  } catch {
    // user cancelled
  }
}

onMounted(loadData)
</script>

<template>
  <main class="mobile-page">
    <section class="mobile-hero booster-hero">
      <div class="profile-row">
        <div class="avatar-wrap">
          <van-image round width="66" height="66" :src="user.avatar || 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'" />
          <span :class="['online-dot', { on: user.boosterStatus === 'idle' }]" />
        </div>
        <div class="profile-main">
          <h1>{{ user.nickname || auth.nickname || '陪玩' }}</h1>
          <p>{{ levelLabel[user.boosterLevel] || '陪玩' }} · {{ user.boosterStatus === 'idle' ? '空闲中' : '忙碌中' }}</p>
        </div>
        <button type="button" class="status-toggle" :class="{ on: user.boosterStatus === 'idle' }" @click="toggleStatus">
          <span />
        </button>
      </div>
      <div class="metric-grid hero-metrics">
        <div class="metric"><strong>￥{{ totalEarned }}</strong><span>累计收入</span></div>
        <div class="metric"><strong>￥{{ pendingSettlement }}</strong><span>待结算</span></div>
        <div class="metric"><strong>￥{{ totalEarned - pendingSettlement }}</strong><span>可提现</span></div>
      </div>
    </section>

    <section class="stats-grid">
      <div class="mobile-card stat-card">
        <van-icon name="orders-o" />
        <strong>{{ user.orderCount || 0 }}</strong>
        <span>总接单</span>
      </div>
      <div class="mobile-card stat-card">
        <van-icon name="passed" />
        <strong>{{ completionRate }}%</strong>
        <span>完成率</span>
      </div>
      <div class="mobile-card stat-card">
        <van-icon name="star-o" />
        <strong>{{ user.rating || '5.0' }}</strong>
        <span>评分</span>
      </div>
    </section>

    <section class="menu">
      <button type="button" @click="$router.push('/booster/messages')">
        <van-icon name="chat-o" />
        <span>我的消息</span>
        <van-icon name="arrow" />
      </button>
      <button type="button">
        <van-icon name="setting-o" />
        <span>设置</span>
        <van-icon name="arrow" />
      </button>
      <button type="button" class="danger" @click="handleLogout">
        <van-icon name="revoke" />
        <span>退出登录</span>
        <van-icon name="arrow" />
      </button>
    </section>

    <MobileTabbar role="booster" />
  </main>
</template>

<style scoped>
.booster-hero {
  background-image:
    linear-gradient(135deg, rgba(16,19,35,.76), rgba(18,183,106,.42)),
    url('https://images.unsplash.com/photo-1511882150382-421056c89033?w=1000&h=900&fit=crop');
}

.profile-row {
  display: flex;
  align-items: center;
  gap: 13px;
}

.avatar-wrap {
  position: relative;
  flex: 0 0 auto;
}

.online-dot {
  position: absolute;
  right: 2px;
  bottom: 2px;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2px solid #fff;
  background: var(--mobile-warning);
}

.online-dot.on {
  background: var(--mobile-success);
}

.profile-main {
  flex: 1;
  min-width: 0;
}

.profile-main h1 {
  margin: 0;
  overflow: hidden;
  font-size: 21px;
  font-weight: 950;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-main p {
  margin: 5px 0 0;
  color: rgba(255,255,255,.72);
  font-size: 13px;
}

.status-toggle {
  width: 48px;
  height: 28px;
  flex: 0 0 auto;
  border: 1px solid rgba(255,255,255,.24);
  border-radius: 999px;
  background: rgba(255,255,255,.2);
  padding: 3px;
}

.status-toggle span {
  display: block;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #fff;
  transition: transform .2s;
}

.status-toggle.on {
  background: var(--mobile-success);
}

.status-toggle.on span {
  transform: translateX(19px);
}

.hero-metrics {
  margin-top: 20px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.stat-card {
  display: grid;
  justify-items: center;
  gap: 5px;
  padding: 15px 8px;
}

.stat-card .van-icon {
  color: var(--mobile-brand);
  font-size: 23px;
}

.stat-card strong {
  color: var(--mobile-ink);
  font-size: 18px;
  font-weight: 950;
}

.stat-card span {
  color: var(--mobile-muted);
  font-size: 11px;
}

.menu {
  display: grid;
  gap: 8px;
  margin-top: 16px;
}

.menu button {
  border: 1px solid var(--mobile-line);
  border-radius: 18px;
  background: rgba(255,255,255,.92);
  padding: 15px;
  display: flex;
  align-items: center;
  gap: 11px;
  color: var(--mobile-ink);
  font-size: 14px;
  font-weight: 850;
}

.menu button span {
  flex: 1;
  text-align: left;
}

.menu button > .van-icon:first-child {
  color: var(--mobile-brand);
  font-size: 22px;
}

.menu button.danger,
.menu button.danger > .van-icon:first-child {
  color: var(--mobile-danger);
}
</style>
