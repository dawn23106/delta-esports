<template>
  <view class="mobile-page">
    <!-- 状态筛选 -->
    <scroll-view scroll-x class="pill-row">
      <view
        v-for="tab in tabs"
        :key="tab.value"
        class="pill"
        :class="{ active: statusFilter === tab.value }"
        @tap="statusFilter = tab.value"
      >
        {{ tab.label }}
      </view>
    </scroll-view>

    <view v-if="loading" class="mobile-card loading-card">
      <view class="spinner" />
      <text>加载中</text>
    </view>

    <view v-else-if="filtered.length" class="order-list">
      <navigator
        v-for="order in filtered"
        :key="order.id"
        :url="'/pages/boss/order-detail?id=' + order.id"
        class="order-card"
      >
        <view class="order-head">
          <text>订单 #{{ order.id }}</text>
          <text class="order-status-badge" :class="order.status">
            {{ statusText[order.status] || order.status }}
          </text>
        </view>
        <view class="order-body">
          <text>{{ order.gameMap || '未指定地图' }}</text>
          <text class="order-amount">￥{{ order.amount }}</text>
        </view>
        <text class="order-time">{{ formatTime(order.createdAt) }}</text>
      </navigator>
    </view>

    <view v-else class="empty-state">
      <text class="empty-title">暂无订单</text>
      <text class="empty-desc">{{ statusFilter ? '该状态下没有订单' : '你还没有下过单，去首页逛逛吧' }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue"
import { getMyOrders } from "@/api/orders"

const orders = ref<any[]>([])
const statusFilter = ref("")
const loading = ref(false)

const tabs = [
  { label: '全部', value: '' },
  { label: '待支付', value: 'pending_payment' },
  { label: '待接单', value: 'pending' },
  { label: '进行中', value: 'in_progress' },
  { label: '已完成', value: 'settled' },
  { label: '已取消', value: 'cancelled' },
]

const statusText: Record<string, string> = {
  pending_payment: '待支付', pending: '待接单', assigned: '已接单', in_progress: '进行中',
  submitted: '待确认', done: '已完成', settled: '已结算',
  refund_pending: '退款中', cancelled: '已取消', disputed: '争议中',
}

const filtered = computed(() =>
  statusFilter.value
    ? orders.value.filter((o) => o.status === statusFilter.value)
    : orders.value
)

function formatTime(value: string) {
  return value?.replace('T', ' ').substring(0, 10) || ''
}

async function load() {
  loading.value = true
  try {
    const result: any = await getMyOrders(1, 100)
    orders.value = result.records || []
  } catch { orders.value = [] }
  finally { loading.value = false }
}

onMounted(load)
</script>

<style scoped>
.loading-card { display: flex; align-items: center; justify-content: center; gap: 10px; color: var(--mobile-muted); }

.order-list { display: flex; flex-direction: column; gap: 10px; margin-top: 12px; }

.order-card {
  display: flex; flex-direction: column; gap: 6px;
  border: 1px solid rgba(228,231,236,.95); border-radius: 18px;
  background: rgba(255,255,255,.9); padding: 14px;
  box-shadow: 0 8px 22px rgba(16,24,40,.05);
}

.order-head { display: flex; justify-content: space-between; font-size: 14px; }
.order-body { display: flex; justify-content: space-between; font-size: 13px; color: var(--mobile-muted); }
.order-amount { font-size: 17px; font-weight: 900; color: var(--mobile-brand); }

.order-status-badge {
  font-size: 11px; padding: 2px 8px; border-radius: 999px; font-weight: 650;
}
.order-status-badge.pending { background: #fff7ed; color: #c2410c; }
.order-status-badge.pending_payment { background: #fff8e9; color: #9b6a2f; }
.order-status-badge.refund_pending { background: #eef4ff; color: #355f9c; }
.order-status-badge.in_progress, .order-status-badge.assigned { background: #eef4ff; color: var(--mobile-brand); }
.order-status-badge.settled, .order-status-badge.done { background: #ecfdf5; color: var(--mobile-success); }
.order-status-badge.cancelled { background: #fef2f2; color: var(--mobile-danger); }

.order-time { font-size: 11px; color: var(--mobile-faint); }

.empty-title { display: block; font-size: 17px; color: var(--mobile-ink); }
.empty-desc { display: block; font-size: 13px; color: var(--mobile-muted); margin-top: 6px; }
</style>
