<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getMyOrders } from '../../api/orders'

const activeTab = ref('')
const orders = ref<any[]>([])
const loading = ref(false)
const tabs = [
  { label: '全部', value: '' },
  { label: '待接单', value: 'pending' },
  { label: '进行中', value: 'in_progress' },
  { label: '待验收', value: 'completed' },
  { label: '已结算', value: 'settled' },
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

async function loadOrders() {
  loading.value = true
  try {
    const result: any = await getMyOrders(1, 50, activeTab.value || undefined)
    orders.value = result.records || []
  } finally {
    loading.value = false
  }
}

onMounted(loadOrders)
</script>

<template>
  <main class="mobile-page no-tabbar">
    <van-nav-bar title="订单记录" left-arrow @click-left="$router.back()" />

    <div class="pill-row">
      <button v-for="tab in tabs" :key="tab.value" type="button" class="pill" :class="{ active: activeTab === tab.value }" @click="activeTab = tab.value; loadOrders()">
        {{ tab.label }}
      </button>
    </div>

    <div v-if="loading" class="mobile-card loading-card">
      <van-loading color="#3157ff" />
      <span>正在加载订单</span>
    </div>

    <section v-else-if="orders.length" class="order-list">
      <article v-for="order in orders" :key="order.id" class="order-card" @click="$router.push(`/boss/order/${order.id}`)">
        <div>
          <strong>订单 #{{ order.id }}</strong>
          <p>{{ order.gameMap || '未指定地图' }}</p>
          <small>{{ order.createdAt?.replace('T', ' ').substring(0, 16) }}</small>
        </div>
        <div class="order-side">
          <span>￥{{ order.amount }}</span>
          <van-tag round color="#eef4ff" text-color="#3157ff">{{ statusText[order.status] || order.status }}</van-tag>
        </div>
      </article>
    </section>

    <div v-else class="empty-state">
      <div>
        <h3>暂无订单</h3>
        <p>下单后会在这里沉淀完整记录。</p>
      </div>
    </div>
  </main>
</template>

<style scoped>
.pill-row {
  margin-top: 14px;
}

.loading-card {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.order-list {
  display: grid;
  gap: 10px;
  margin-top: 10px;
}

.order-card {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid rgba(228,231,236,.95);
  border-radius: 18px;
  background: rgba(255,255,255,.92);
  padding: 15px;
  box-shadow: 0 8px 22px rgba(16,24,40,.05);
}

.order-card strong {
  color: var(--mobile-ink);
  font-size: 15px;
}

.order-card p {
  margin: 6px 0 4px;
  color: var(--mobile-muted);
  font-size: 13px;
}

.order-card small {
  color: var(--mobile-faint);
  font-size: 11px;
}

.order-side {
  flex: 0 0 auto;
  display: grid;
  justify-items: end;
  gap: 8px;
}

.order-side span {
  color: var(--mobile-brand);
  font-size: 19px;
  font-weight: 950;
}
</style>
