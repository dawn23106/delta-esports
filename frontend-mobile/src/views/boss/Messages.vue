<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getMyOrders } from '../../api/orders'
import MobileTabbar from '../../components/MobileTabbar.vue'

const orders = ref<any[]>([])
const refreshing = ref(false)

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

async function loadOrders() {
  refreshing.value = true
  try {
    const result: any = await getMyOrders(1, 50)
    orders.value = result.records || []
  } finally {
    refreshing.value = false
  }
}

function timeAgo(value: string) {
  if (!value) return ''
  const diff = Date.now() - new Date(value).getTime()
  const minutes = Math.max(0, Math.floor(diff / 60000))
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes} 分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours} 小时前`
  return new Date(value).toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

onMounted(loadOrders)
</script>

<template>
  <main class="mobile-page">
    <section class="mobile-hero messages-hero">
      <div class="eyebrow">Messages</div>
      <h1 class="page-title">订单消息</h1>
      <p class="page-subtitle">每个订单都会生成一个沟通入口，方便跟进进度。</p>
    </section>

    <van-pull-refresh v-model="refreshing" @refresh="loadOrders">
      <section v-if="orders.length" class="message-list">
        <article v-for="order in orders" :key="order.id" class="message-card" @click="$router.push(`/boss/messages/${order.id}`)">
          <div class="message-avatar">
            <van-icon name="chat-o" />
          </div>
          <div class="message-main">
            <div class="message-top">
              <strong>订单 #{{ order.id }}</strong>
              <span>{{ timeAgo(order.updatedAt || order.createdAt) }}</span>
            </div>
            <p>￥{{ order.amount }} · {{ order.gameMap || '未指定地图' }}</p>
          </div>
          <span class="message-status">{{ statusText[order.status] || order.status }}</span>
        </article>
      </section>

      <div v-else-if="!refreshing" class="empty-state">
        <div>
          <h3>暂无消息</h3>
          <p>下单后这里会出现订单沟通入口。</p>
        </div>
      </div>
    </van-pull-refresh>

    <MobileTabbar role="boss" />
  </main>
</template>

<style scoped>
.messages-hero {
  background-image:
    linear-gradient(135deg, rgba(16,19,35,.74), rgba(49,87,255,.54)),
    url('https://images.unsplash.com/photo-1552820728-8b83bb6b2cf3?w=1000&h=900&fit=crop');
}

.message-list {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.message-card {
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px solid rgba(228,231,236,.95);
  border-radius: 18px;
  background: rgba(255,255,255,.9);
  padding: 13px;
  box-shadow: 0 8px 22px rgba(16,24,40,.05);
}

.message-avatar {
  width: 44px;
  height: 44px;
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  border-radius: 15px;
  color: var(--mobile-brand);
  background: #eef4ff;
  font-size: 23px;
}

.message-main {
  min-width: 0;
  flex: 1;
}

.message-top {
  display: flex;
  gap: 10px;
  justify-content: space-between;
}

.message-top strong {
  color: var(--mobile-ink);
  font-size: 15px;
}

.message-top span {
  color: var(--mobile-faint);
  font-size: 11px;
}

.message-main p {
  margin: 5px 0 0;
  overflow: hidden;
  color: var(--mobile-muted);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-status {
  flex: 0 0 auto;
  color: var(--mobile-brand);
  font-size: 12px;
  font-weight: 850;
}
</style>
