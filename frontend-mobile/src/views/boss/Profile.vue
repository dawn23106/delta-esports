<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog } from 'vant'
import request from '../../api/request'
import { getMyOrders } from '../../api/orders'
import { useAuthStore } from '../../store/auth'
import MobileTabbar from '../../components/MobileTabbar.vue'

const router = useRouter()
const auth = useAuthStore()
const user = ref<any>({ nickname: '', starLevel: 0, totalSpent: 0, avatar: '' })
const activeOrder = ref<any>(null)
const totalOrders = ref(0)
const levelProgress = ref(0)
const nextLevelAmount = ref(0)
const starLevels = [0, 100, 500, 2000, 5000, 10000]
const starNames = ['Lv.0', 'Lv.1', 'Lv.2', 'Lv.3', 'Lv.4', 'Lv.5']

const entries = [
  { label: '订单记录', icon: 'orders-o', path: '/boss/orders' },
  { label: '我的礼物', icon: 'gift-o', path: '/boss/gifts' },
  { label: '我的评价', icon: 'star-o', path: '/boss/reviews' },
  { label: '客服中心', icon: 'service-o', path: '/boss/service' },
  { label: '联系客服', icon: 'phone-o', path: '/boss/contact' },
  { label: '设置', icon: 'setting-o', path: '/boss/settings' },
]

const statusText: Record<string, string> = {
  pending: '待接单',
  assigned: '已接单',
  in_progress: '进行中',
  completed: '待验收',
  done: '已确认',
  settled: '已结算',
  cancelled: '已取消',
  disputed: '争议中',
}

async function loadData() {
  try {
    const result: any = await request.get('/users/me')
    user.value = result || user.value
    const level = user.value.starLevel || 0
    if (level < 5) {
      nextLevelAmount.value = Math.max(0, starLevels[level + 1] - (user.value.totalSpent || 0))
      levelProgress.value = Math.round(Math.min(100, Math.max(0, ((user.value.totalSpent || 0) - starLevels[level]) / (starLevels[level + 1] - starLevels[level]) * 100)))
    } else {
      levelProgress.value = 100
    }
  } catch {
    user.value.nickname = auth.nickname || '游客'
  }

  try {
    const result: any = await getMyOrders(1, 100)
    const orders = result.records || []
    totalOrders.value = orders.length
    activeOrder.value = orders.find((order: any) => !['settled', 'cancelled'].includes(order.status)) || null
  } catch {
    totalOrders.value = 0
  }
}

async function handleLogout() {
  try {
    await showConfirmDialog({ title: '退出登录', message: '确定要退出当前账号吗？', confirmButtonText: '退出', confirmButtonColor: '#f04438' })
    auth.logout()
    router.push('/login')
  } catch {
    // user cancelled
  }
}

onMounted(loadData)
</script>

<template>
  <main class="mobile-page">
    <section class="profile-hero mobile-hero">
      <div class="profile-row">
        <van-image round width="70" height="70" :src="user.avatar || 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'" />
        <div>
          <h1>{{ user.nickname || auth.nickname || '沧月玩家' }}</h1>
          <p>{{ starNames[user.starLevel || 0] }} · 累计消费 ￥{{ user.totalSpent || 0 }}</p>
        </div>
      </div>
      <div class="metric-grid hero-metrics">
        <div class="metric"><strong>{{ totalOrders }}</strong><span>订单</span></div>
        <div class="metric"><strong>{{ levelProgress }}%</strong><span>升级</span></div>
        <div class="metric"><strong>￥{{ nextLevelAmount }}</strong><span>差额</span></div>
      </div>
    </section>

    <section class="mobile-card level-card">
      <div class="level-head">
        <strong>等级进度</strong>
        <span>{{ (user.starLevel || 0) >= 5 ? '已满级' : `距离 ${starNames[(user.starLevel || 0) + 1]} 还需 ￥${nextLevelAmount}` }}</span>
      </div>
      <div class="progress">
        <span :style="{ width: levelProgress + '%' }" />
      </div>
    </section>

    <div class="section-title">当前订单</div>
    <section v-if="activeOrder" class="mobile-card active-order" @click="router.push(`/boss/order/${activeOrder.id}`)">
      <div>
        <strong>订单 #{{ activeOrder.id }}</strong>
        <p>{{ activeOrder.gameMap || '未指定地图' }}</p>
      </div>
      <div>
        <span>￥{{ activeOrder.amount }}</span>
        <small>{{ statusText[activeOrder.status] || activeOrder.status }}</small>
      </div>
    </section>
    <section v-else class="mobile-card empty-order" @click="router.push('/boss/home')">
      <span>暂无进行中的订单</span>
      <van-icon name="arrow" />
    </section>

    <div class="section-title">服务入口</div>
    <section class="entry-grid">
      <button v-for="entry in entries" :key="entry.path" type="button" class="entry" @click="router.push(entry.path)">
        <van-icon :name="entry.icon" />
        <span>{{ entry.label }}</span>
      </button>
    </section>

    <button type="button" class="logout" @click="handleLogout">退出登录</button>

    <MobileTabbar role="boss" />
  </main>
</template>

<style scoped>
.profile-hero {
  background-image:
    linear-gradient(135deg, rgba(16,19,35,.76), rgba(49,87,255,.56)),
    url('https://images.unsplash.com/photo-1612404730960-5c71577fca11?w=1000&h=900&fit=crop');
}

.profile-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.profile-row h1 {
  margin: 0;
  font-size: 22px;
  font-weight: 950;
}

.profile-row p {
  margin: 5px 0 0;
  color: rgba(255,255,255,.72);
  font-size: 13px;
}

.hero-metrics {
  margin-top: 18px;
}

.level-card {
  margin-top: 14px;
}

.level-head {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  color: var(--mobile-muted);
  font-size: 12px;
}

.level-head strong {
  color: var(--mobile-ink);
  font-size: 15px;
}

.progress {
  margin-top: 12px;
  height: 8px;
  overflow: hidden;
  border-radius: 99px;
  background: #eef2f6;
}

.progress span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #3157ff, #08b6d8);
}

.active-order {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
}

.active-order strong {
  color: var(--mobile-ink);
  font-size: 15px;
}

.active-order p {
  margin: 5px 0 0;
  color: var(--mobile-muted);
  font-size: 13px;
}

.active-order div:last-child {
  display: grid;
  justify-items: end;
  gap: 4px;
}

.active-order span {
  color: var(--mobile-brand);
  font-size: 20px;
  font-weight: 950;
}

.active-order small {
  color: var(--mobile-muted);
  font-size: 12px;
}

.empty-order {
  display: flex;
  justify-content: space-between;
  color: var(--mobile-muted);
  font-size: 14px;
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.entry {
  min-height: 84px;
  border: 1px solid rgba(228,231,236,.95);
  border-radius: 18px;
  background: rgba(255,255,255,.9);
  display: grid;
  place-items: center;
  align-content: center;
  gap: 8px;
  color: var(--mobile-muted);
  box-shadow: 0 8px 22px rgba(16,24,40,.05);
}

.entry .van-icon {
  color: var(--mobile-brand);
  font-size: 24px;
}

.entry span {
  color: var(--mobile-ink);
  font-size: 12px;
  font-weight: 800;
}

.logout {
  width: 100%;
  margin-top: 18px;
  border: 0;
  background: transparent;
  color: var(--mobile-danger);
  font-size: 14px;
  font-weight: 800;
  padding: 12px;
}
</style>
