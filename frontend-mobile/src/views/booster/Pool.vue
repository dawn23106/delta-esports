<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { closeToast, showLoadingToast, showToast } from 'vant'
import { claimOrder, getOrderPool } from '../../api/orders'
import { useAuthGuard } from '../../composables/useAuthGuard'
import MobileTabbar from '../../components/MobileTabbar.vue'

const orders = ref<any[]>([])
const refreshing = ref(false)
const { requireLogin } = useAuthGuard()

async function loadPool() {
  refreshing.value = true
  try {
    const result: any = await getOrderPool(1, 50)
    orders.value = result.records || []
  } finally {
    refreshing.value = false
  }
}

async function handleClaim(id: number) {
  if (!await requireLogin('抢单')) return
  showLoadingToast({ message: '正在抢单', duration: 0 })
  try {
    await claimOrder(id)
    closeToast()
    showToast({ message: '抢单成功', icon: 'success' })
    loadPool()
  } catch (error: any) {
    closeToast()
    showToast(error?.response?.data?.message || '抢单失败')
  }
}

function timeAgo(value: string) {
  if (!value) return ''
  const minutes = Math.max(0, Math.floor((Date.now() - new Date(value).getTime()) / 60000))
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes} 分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours} 小时前`
  return new Date(value).toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

onMounted(loadPool)
</script>

<template>
  <main class="mobile-page">
    <section class="mobile-hero pool-hero">
      <div class="eyebrow">Order Pool</div>
      <h1 class="page-title">订单池</h1>
      <p class="page-subtitle">空闲时快速接单，重点看地图、金额和备注。</p>
      <div class="metric-grid hero-metrics">
        <div class="metric"><strong>{{ orders.length }}</strong><span>可抢</span></div>
        <div class="metric"><strong>实时</strong><span>刷新</span></div>
        <div class="metric"><strong>安全</strong><span>结算</span></div>
      </div>
    </section>

    <van-pull-refresh v-model="refreshing" @refresh="loadPool">
      <section v-if="orders.length" class="order-list">
        <article v-for="order in orders" :key="order.id" class="pool-card">
          <div class="pool-head">
            <span>订单 #{{ order.id }}</span>
            <strong>￥{{ order.amount }}</strong>
          </div>
          <div class="pool-tags">
            <span>{{ order.gameMap || '未指定地图' }}</span>
            <span>{{ timeAgo(order.createdAt) }}</span>
          </div>
          <p v-if="order.bossNote">{{ order.bossNote }}</p>
          <van-button block round color="linear-gradient(135deg, #12b76a, #08b6d8)" @click="handleClaim(order.id)">
            立即抢单
          </van-button>
        </article>
      </section>

      <div v-else-if="!refreshing" class="empty-state">
        <div>
          <h3>暂无可抢订单</h3>
          <p>保持在线，新的老板订单会出现在这里。</p>
        </div>
      </div>
    </van-pull-refresh>

    <MobileTabbar role="booster" />
  </main>
</template>

<style scoped>
.pool-hero {
  background-image:
    linear-gradient(135deg, rgba(16,19,35,.76), rgba(18,183,106,.44)),
    url('https://images.unsplash.com/photo-1612287230202-1ff1d85d1bdf?w=1000&h=900&fit=crop');
}

.hero-metrics,
.order-list {
  margin-top: 18px;
}

.order-list {
  display: grid;
  gap: 12px;
}

.pool-card {
  border: 1px solid rgba(228,231,236,.95);
  border-radius: 20px;
  background: rgba(255,255,255,.92);
  padding: 16px;
  box-shadow: 0 10px 26px rgba(16,24,40,.06);
}

.pool-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.pool-head span {
  color: var(--mobile-muted);
  font-size: 13px;
  font-weight: 750;
}

.pool-head strong {
  color: var(--mobile-brand);
  font-size: 24px;
  font-weight: 950;
}

.pool-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 10px 0;
}

.pool-tags span {
  border-radius: 999px;
  background: #f2f4f7;
  color: var(--mobile-muted);
  padding: 5px 9px;
  font-size: 12px;
  font-weight: 750;
}

.pool-card p {
  margin: 0 0 13px;
  border-radius: 14px;
  background: #f8fafc;
  color: var(--mobile-muted);
  padding: 10px;
  font-size: 13px;
  line-height: 1.55;
}
</style>
