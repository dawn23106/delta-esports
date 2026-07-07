<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getOrderPool, claimOrder } from '../../api/orders'
import { showToast, showLoadingToast, closeToast } from 'vant'
import { useAuthGuard } from '../../composables/useAuthGuard'

const active = ref(0)
const orders = ref<any[]>([])
const refreshing = ref(false)
const { requireLogin } = useAuthGuard()

async function loadPool() {
  refreshing.value = true
  try {
    const res: any = await getOrderPool(1, 50)
    orders.value = res.records || []
  } catch { } finally { refreshing.value = false }
}

async function handleClaim(id: number) {
  if (!await requireLogin('抢单')) return
  showLoadingToast('抢单中...')
  try {
    await claimOrder(id)
    closeToast()
    showToast('抢单成功！')
    loadPool()
  } catch (e: any) {
    closeToast()
    showToast(e?.response?.data?.message || '抢单失败')
  }
}

function timeAgo(t: string) {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return '刚刚'
  if (mins < 60) return `${mins}分钟前`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours}小时前`
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

onMounted(loadPool)
</script>

<template>
  <div class="page">
    <!-- 顶部横幅 -->
    <div class="hero">
      <div class="hero-bg"></div>
      <div class="hero-content">
        <div class="hero-title">订单池</div>
        <div class="hero-sub">抢单接单，随时开打</div>
        <div class="hero-stat">
          <span class="hero-num">{{ orders.length }}</span>
          <span class="hero-label">个可抢订单</span>
        </div>
      </div>
    </div>

    <!-- 订单列表 -->
    <van-pull-refresh v-model="refreshing" @refresh="loadPool" class="pull-wrap">
      <div class="list" v-if="orders.length">
        <div v-for="o in orders" :key="o.id" class="order-card">
          <div class="card-head">
            <div class="card-id">#{{ o.id }}</div>
            <div class="card-price">¥<span class="card-price-num">{{ o.amount }}</span></div>
          </div>
          <div class="card-tags">
            <span class="tag" v-if="o.gameMap">🗺️ {{ o.gameMap }}</span>
            <span class="tag time">🕐 {{ timeAgo(o.createdAt) }}</span>
          </div>
          <div class="card-note" v-if="o.bossNote">
            <span class="note-label">备注</span>
            {{ o.bossNote }}
          </div>
          <van-button round block type="primary" class="claim-btn" @click="handleClaim(o.id)"
            color="linear-gradient(135deg, #6366f1, #8b5cf6)">
            立即抢单
          </van-button>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else-if="!refreshing" class="empty">
        <div class="empty-art">
          <span class="empty-emoji">🍵</span>
        </div>
        <div class="empty-title">暂无可抢订单</div>
        <div class="empty-desc">喝杯茶等等，老板们正在赶来</div>
      </div>
    </van-pull-refresh>

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

/* 顶部横幅 */
.hero {
  position: relative;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  padding: 32px 20px 40px;
  overflow: hidden;
}
.hero-bg {
  position: absolute;
  top: -30px;
  right: -30px;
  width: 150px;
  height: 150px;
  border-radius: 50%;
  background: rgba(255,255,255,0.08);
}
.hero-content {
  position: relative;
  z-index: 1;
}
.hero-title {
  font-size: 22px;
  font-weight: 800;
  color: #fff;
  letter-spacing: 1px;
}
.hero-sub {
  font-size: 13px;
  color: rgba(255,255,255,0.7);
  margin-top: 4px;
}
.hero-stat {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-top: 16px;
}
.hero-num {
  font-size: 36px;
  font-weight: 800;
  color: #fff;
}
.hero-label {
  font-size: 14px;
  color: rgba(255,255,255,0.6);
}

.pull-wrap {
  margin-top: -16px;
  border-radius: 20px 20px 0 0;
  background: #f8fafc;
  position: relative;
  z-index: 2;
  min-height: 60vh;
}

/* 订单卡片 */
.list {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.order-card {
  background: #fff;
  border-radius: 18px;
  padding: 18px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  border: 1px solid #f1f5f9;
}
.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.card-id {
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
}
.card-price {
  font-size: 13px;
  color: #6366f1;
  font-weight: 500;
}
.card-price-num {
  font-size: 22px;
  font-weight: 800;
}
.card-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}
.tag {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 8px;
  background: #f1f5f9;
  color: #64748b;
}
.tag.time {
  background: #fff7ed;
  color: #ea580c;
}
.card-note {
  font-size: 13px;
  color: #94a3b8;
  background: #f8fafc;
  padding: 10px 12px;
  border-radius: 10px;
  margin-bottom: 14px;
  line-height: 1.5;
}
.note-label {
  font-size: 11px;
  font-weight: 600;
  color: #94a3b8;
  margin-right: 6px;
}
.claim-btn {
  height: 44px !important;
  font-size: 15px !important;
  font-weight: 600 !important;
  border-radius: 14px !important;
  box-shadow: 0 6px 20px rgba(99,102,241,0.25) !important;
}

/* 空状态 */
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 80px;
}
.empty-art {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}
.empty-emoji {
  font-size: 40px;
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
