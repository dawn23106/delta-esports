<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getMyOrders } from '../../api/orders'

const orders = ref<any[]>([])
const loading = ref(true)
const active = ref(1)

async function load() {
  loading.value = true
  try {
    const res: any = await getMyOrders(1, 50)
    orders.value = (res.records || []).filter((o: any) => !['cancelled'].includes(o.status))
  } catch { }
  loading.value = false
}

function timeAgo(t: string) {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 60) return `${mins}分钟前`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours}小时前`
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

onMounted(load)
</script>

<template>
  <div class="page">
    <van-nav-bar title="我的消息" fixed placeholder />

    <van-pull-refresh v-model="loading" @refresh="load">
      <div class="list" v-if="orders.length">
        <div v-for="o in orders" :key="o.id" class="msg-item" @click="$router.push(`/booster/messages/${o.id}`)">
          <van-image round width="48" height="48" src="https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg" class="avatar" />
          <div class="msg-info">
            <div class="msg-top">
              <span class="msg-title">订单 #{{ o.id }}</span>
              <span class="msg-time">{{ timeAgo(o.updatedAt || o.createdAt) }}</span>
            </div>
            <div class="msg-bottom">
              <span class="msg-preview">¥{{ o.amount }} · {{ o.gameMap || '未指定' }}</span>
              <span class="msg-badge" v-if="o.status === 'in_progress'">进行中</span>
              <span class="msg-badge done" v-else-if="o.status === 'settled'">已结算</span>
            </div>
          </div>
        </div>
      </div>

      <div v-else-if="!loading" class="empty">
        <div class="empty-icon-wrap">
          <span class="empty-emoji">💬</span>
        </div>
        <div class="empty-title">暂无消息</div>
        <div class="empty-desc">接单后这里会出现订单群聊</div>
      </div>
    </van-pull-refresh>

    <van-tabbar v-model="active" route :border="false" active-color="#6366f1" inactive-color="#94a3b8" safe-area-inset-bottom class="tabbar">
      <van-tabbar-item icon="orders-o" to="/booster/pool">订单池</van-tabbar-item>
      <van-tabbar-item icon="logistics" to="/booster/orders">订单处理</van-tabbar-item>
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
.list {
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.msg-item {
  background: #fff;
  border-radius: 16px;
  padding: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  box-shadow: 0 1px 3px rgba(0,0,0,0.03);
  border: 1px solid #f1f5f9;
  transition: background 0.15s;
}
.msg-item:active {
  background: #f8fafc;
}
.avatar {
  flex-shrink: 0;
}
.msg-info {
  flex: 1;
  min-width: 0;
}
.msg-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}
.msg-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}
.msg-time {
  font-size: 11px;
  color: #94a3b8;
}
.msg-bottom {
  display: flex;
  align-items: center;
  gap: 8px;
}
.msg-preview {
  font-size: 13px;
  color: #94a3b8;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.msg-badge {
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 10px;
  background: #eef2ff;
  color: #6366f1;
  font-weight: 500;
}
.msg-badge.done {
  background: #ecfdf5;
  color: #10b981;
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 100px;
}
.empty-icon-wrap {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}
.empty-emoji {
  font-size: 36px;
}
.empty-title {
  font-size: 16px;
  font-weight: 600;
  color: #64748b;
}
.empty-desc {
  font-size: 13px;
  color: #94a3b8;
  margin-top: 4px;
}

.tabbar {
  background: rgba(255,255,255,0.9) !important;
  backdrop-filter: blur(20px) !important;
  border-top: 1px solid #f1f5f9 !important;
}
</style>
