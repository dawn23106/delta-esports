<template>
  <view class="mobile-page">
    <view class="message-head">
      <text class="page-heading">订单聊天</text>
      <text class="page-hint">仅用于老板与打手传递房间号和对局信息</text>
    </view>

    <view v-if="loading" class="mobile-card loading-card">正在加载聊天…</view>

    <view v-else-if="orders.length" class="msg-list">
      <navigator v-for="order in orders" :key="order.id" :url="`/pages/boss/chat?orderId=${order.id}`" class="msg-card">
        <view class="msg-avatar"><text>聊</text></view>
        <view class="msg-main">
          <view class="msg-head"><text class="msg-title">{{ order.serviceName || `订单 #${order.id}` }}</text><text class="msg-time">{{ timeAgo(order.updatedAt || order.createdAt) }}</text></view>
          <view class="msg-sub"><text class="msg-preview">{{ order.gameMap || '点击进入订单聊天' }}</text><text :class="['msg-status', order.status]">{{ statusText[order.status] || order.status }}</text></view>
        </view>
      </navigator>
    </view>

    <view v-else class="empty-state">
      <view>
        <text class="empty-icon">•••</text>
        <text class="empty-title">还没有可用的订单聊天</text>
        <text class="empty-desc">创建订单后先等待打手接单；接单成功，聊天才会自动出现在这里。</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue"
import { onShow } from "@dcloudio/uni-app"
import { getMyOrders } from "@/api/orders"

const orders = ref<any[]>([])
const loading = ref(false)
const statusText: Record<string, string> = {
  assigned: '待开始', in_progress: '服务中', submitted: '待确认', done: '已完成', settled: '已归档',
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

async function load() {
  loading.value = true
  try {
    const result: any = await getMyOrders(1, 50)
    orders.value = (result.records || []).filter((o: any) =>
      o.boosterId && ['assigned', 'in_progress', 'submitted', 'done', 'settled'].includes(o.status)
    )
  } catch { orders.value = [] }
  finally { loading.value = false }
}

onShow(load)
</script>

<style scoped>
.message-head { margin: 8px 2px 18px; }
.page-heading { display: block; color: var(--mobile-ink); font-size: 25px; font-weight: 950; }
.page-hint { display: block; margin-top: 5px; color: var(--mobile-muted); font-size: 12px; }
.loading-card { text-align: center; color: var(--mobile-muted); }
.msg-list { display: flex; flex-direction: column; gap: 10px; }
.msg-card { display: flex; align-items: center; gap: 12px; padding: 14px; border: 1px solid #dfe8df; border-radius: 18px; background: rgba(255,255,255,.94); box-shadow: 0 8px 22px rgba(43,77,55,.05); }
.msg-avatar { width: 48px; height: 48px; flex-shrink: 0; border-radius: 15px; background: #e1efe4; color: #2c704d; font-size: 15px; font-weight: 900; line-height: 48px; text-align: center; }
.msg-main { min-width: 0; flex: 1; }
.msg-head, .msg-sub { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.msg-title { overflow: hidden; color: var(--mobile-ink); font-size: 15px; font-weight: 850; text-overflow: ellipsis; white-space: nowrap; }
.msg-time { flex-shrink: 0; color: var(--mobile-faint); font-size: 10px; }
.msg-sub { margin-top: 5px; }
.msg-preview { overflow: hidden; color: var(--mobile-muted); font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.msg-status { flex-shrink: 0; padding: 3px 8px; border-radius: 999px; background: #e5f1e8; color: #2c704d; font-size: 10px; font-weight: 750; }
.msg-status.submitted { background: #fff4dd; color: #9b6a2f; }
.msg-status.done, .msg-status.settled { background: #f0f2f0; color: #718076; }
.empty-icon { display: block; color: #7eaa8b; font-size: 28px; letter-spacing: 4px; }
.empty-title { display: block; margin-top: 10px; color: var(--mobile-ink); font-size: 16px; font-weight: 850; }
.empty-desc { display: block; max-width: 280px; margin-top: 6px; color: var(--mobile-muted); font-size: 12px; line-height: 1.6; }
</style>
