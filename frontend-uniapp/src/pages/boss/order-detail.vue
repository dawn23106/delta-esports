<template>
  <view class="mobile-page">
    <view v-if="loading" class="mobile-card loading-card">
      <view class="spinner" />
      <text>正在加载订单</text>
    </view>

    <template v-else-if="order">
      <!-- 状态 Hero -->
      <view class="mobile-hero detail-hero">
        <text class="eyebrow">Order #{{ order.id }}</text>
        <text class="page-title">{{ statusText[order.status] || order.status }}</text>
        <text class="page-subtitle">{{ statusHint[order.status] || '订单状态已更新。' }}</text>
        <view class="metric-grid hero-metrics">
          <view class="metric"><text class="metric-val">￥{{ order.amount }}</text><text class="metric-label">金额</text></view>
          <view class="metric"><text class="metric-val">{{ order.gameMap ? '已选' : '默认' }}</text><text class="metric-label">地图</text></view>
          <view class="metric"><text class="metric-val">{{ steps.length }}</text><text class="metric-label">节点</text></view>
        </view>
      </view>

      <!-- 订单信息 -->
      <view class="mobile-card info-card">
        <view v-if="order.serviceId" class="info-row">
          <text>服务编号</text>
          <text>#{{ order.serviceId }}</text>
        </view>
        <view class="info-row">
          <text>地图模式</text>
          <text>{{ order.gameMap || '未指定地图' }}</text>
        </view>
        <view v-if="order.bossNote" class="info-row note">
          <text>我的备注</text>
          <text>{{ order.bossNote }}</text>
        </view>
      </view>

      <!-- 进度时间线 -->
      <view class="mobile-card timeline-card">
        <text class="timeline-title">订单进度</text>
        <view class="timeline">
          <view v-for="(step, i) in steps" :key="i" class="step">
            <view class="dot" />
            <view>
              <text class="step-label">{{ step.label }}</text>
              <text class="step-time">{{ formatTime(step.time) }}</text>
              <text v-if="step.desc" class="step-desc">{{ step.desc }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 结果截图 -->
      <view v-if="resultImages().length" class="mobile-card result-card">
        <text class="timeline-title">结果截图</text>
        <view class="image-grid">
          <image v-for="(img, i) in resultImages()" :key="i" :src="img" class="result-img" mode="aspectFill" />
        </view>
      </view>

      <!-- 结单备注 -->
      <view v-if="order.resultNote" class="mobile-card result-card">
        <text class="timeline-title">结单备注</text>
        <text class="result-note">{{ order.resultNote }}</text>
      </view>

      <button v-if="order.status === 'submitted'" class="btn-primary confirm-btn" @tap="handleConfirm">
        确认服务完成
      </button>

      <button v-if="order.status === 'pending_payment'" class="btn-primary confirm-btn" :disabled="paying" @tap="handlePay">
        {{ paying ? '正在拉起微信支付…' : `立即支付 ¥${order.amount}` }}
      </button>

      <!-- 取消按钮 -->
      <button
        v-if="['pending_payment', 'pending', 'assigned'].includes(order.status)"
        class="btn-cancel"
        @tap="handleCancel"
      >
        取消订单
      </button>
    </template>

    <view v-else class="empty-state">
      <text class="empty-title">{{ loadFailed ? '无法查看订单' : '订单不存在' }}</text>
      <text class="empty-desc">请返回订单记录重新进入。</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue"
import { onLoad } from "@dcloudio/uni-app"
import { cancelOrder, confirmOrder, getOrderDetail } from "@/api/orders"
import { payOrder } from "@/api/payments"
import { showModal } from "@/utils/nav"

const order = ref<any>(null)
const loading = ref(false)
const loadFailed = ref(false)
const id = ref(0)
const paying = ref(false)

onLoad((options: any) => {
  id.value = Number(options?.id) || 0
})

const statusText: Record<string, string> = {
  pending_payment: '待支付', pending: '待接单', assigned: '已接单', in_progress: '进行中',
  submitted: '待确认', done: '已完成', settled: '已结算',
  refund_pending: '退款中', cancelled: '已取消', disputed: '争议中',
}

const statusHint: Record<string, string> = {
  pending_payment: '完成微信支付后，订单才会进入打手接单池。',
  pending: '订单已创建，正在等待陪玩接单。',
  assigned: '陪玩已接单，即将开始服务。',
  in_progress: '服务正在进行，请保持沟通。',
  submitted: '打手已提交结单，请确认服务结果。',
  done: '你已确认达标，等待平台结算。',
  settled: '订单已完成并结算。',
  cancelled: '订单已取消。',
  refund_pending: '退款申请已提交，到账时间以微信支付为准。',
  disputed: '订单正在争议处理中。',
}

const steps = ref<{ label: string; time: string; desc?: string }[]>([])

function buildSteps(source: any) {
  const list = []
  if (source.createdAt) list.push({ label: '下单成功', time: source.createdAt, desc: source.bossNote })
  if (source.assignedAt) list.push({ label: '陪玩接单', time: source.assignedAt })
  if (source.startedAt) list.push({ label: '开始服务', time: source.startedAt })
  if (source.completedAt) list.push({ label: '申请结单', time: source.completedAt, desc: source.isQualified ? '陪玩自评：达标' : '陪玩自评：未达标' })
  if (source.doneAt) list.push({ label: '确认达标', time: source.doneAt })
  if (source.settledAt) list.push({ label: '完成结算', time: source.settledAt })
  if (source.status === 'cancelled') list.push({ label: '订单取消', time: source.updatedAt || source.createdAt })
  return list
}

function formatTime(value: string) {
  return value?.replace('T', ' ').substring(0, 16) || ''
}

function resultImages() {
  if (!order.value?.resultImages) return []
  try { return JSON.parse(order.value.resultImages) } catch { return [] }
}

async function load() {
  loading.value = true; loadFailed.value = false
  try {
    const result: any = await getOrderDetail(id.value)
    order.value = result
    steps.value = buildSteps(result)
  } catch { loadFailed.value = true }
  finally { loading.value = false }
}

async function handleCancel() {
  try {
    const res = await showModal({
      title: '取消订单', content: '确定要取消这个订单吗？',
      confirmText: '取消订单', confirmColor: '#f04438',
    })
    if (res.confirm) {
      await cancelOrder(id.value)
      uni.showToast({ title: '订单已取消', icon: 'success' })
      load()
    }
  } catch { /* user cancelled */ }
}

async function handleConfirm() {
  try {
    const res = await showModal({
      title: '确认服务完成',
      content: '确认后订单金额将结算给打手，请核对结单说明。',
      confirmText: '确认完成',
      confirmColor: '#2c704d',
    })
    if (res.confirm) {
      await confirmOrder(id.value)
      uni.showToast({ title: '已确认完成', icon: 'success' })
      await load()
    }
  } catch (e: any) {
    if (e?.data?.message) uni.showToast({ title: e.data.message, icon: 'none' })
  }
}

async function handlePay() {
  if (paying.value) return
  paying.value = true
  try {
    const paymentResult: any = await payOrder(id.value)
    const confirmed = paymentResult?.orderStatus !== 'pending_payment'
    uni.showToast({ title: confirmed ? '支付成功' : '支付结果确认中', icon: confirmed ? 'success' : 'none' })
    await load()
  } catch (e: any) {
    if (!String(e?.errMsg || '').includes('cancel')) {
      uni.showToast({ title: e?.data?.message || e?.message || '支付未完成', icon: 'none' })
    }
  } finally {
    paying.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.loading-card { display: flex; align-items: center; justify-content: center; gap: 10px; color: var(--mobile-muted); }

.detail-hero {
  margin-top: 14px;
  background-image: linear-gradient(135deg, #2c704d, #65a878);
  background-size: cover; background-position: center;
}

.confirm-btn { margin-top: 14px; }

.hero-metrics { margin-top: 18px; }

.metric-val { display: block; font-size: 18px; line-height: 1.1; font-weight: bold; }
.metric-label { display: block; margin-top: 4px; color: rgba(255,255,255,.68); font-size: 11px; }

.info-card, .timeline-card, .result-card { margin-top: 12px; }

.info-card { display: flex; flex-direction: column; gap: 12px; }

.info-row { display: flex; justify-content: space-between; font-size: 13px; color: var(--mobile-muted); }
.info-row text:last-child { max-width: 64%; color: var(--mobile-ink); text-align: right; font-size: 14px; }

.timeline-title { display: block; margin-bottom: 14px; font-size: 16px; font-weight: 900; color: var(--mobile-ink); }

.timeline { display: flex; flex-direction: column; }

.step { display: flex; gap: 12px; position: relative; padding-bottom: 18px; }

.step:last-child { padding-bottom: 0; }

.step:not(:last-child)::before {
  content: ""; position: absolute; top: 14px; left: 5px; bottom: 0;
  width: 2px; background: #dbe7ff;
}

.dot {
  width: 12px; height: 12px; flex-shrink: 0; border-radius: 50%;
  background: var(--mobile-brand); margin-top: 3px;
  box-shadow: 0 0 0 4px #eef4ff;
}

.step-label { display: block; font-size: 14px; font-weight: 900; color: var(--mobile-ink); }
.step-time { display: block; margin-top: 3px; font-size: 11px; color: var(--mobile-faint); }
.step-desc {
  display: block; margin-top: 8px; border-radius: 12px; background: #f8fafc;
  color: var(--mobile-muted); padding: 9px 10px; font-size: 13px; line-height: 1.55;
}

.image-grid { display: flex; gap: 8px; flex-wrap: wrap; }
.result-img { width: calc(33.33% - 6px); aspect-ratio: 1; border-radius: 12px; }
.result-note { display: block; margin-top: 8px; border-radius: 12px; background: #f8fafc; color: var(--mobile-muted); padding: 9px 10px; font-size: 13px; line-height: 1.55; }

.btn-cancel {
  width: 100%; margin-top: 14px; padding: 14px;
  border: 1px solid var(--mobile-danger); border-radius: 999px;
  background: transparent; color: var(--mobile-danger); font-size: 15px; font-weight: 750;
}

.empty-title { display: block; font-size: 17px; color: var(--mobile-ink); }
.empty-desc { display: block; font-size: 13px; color: var(--mobile-muted); margin-top: 6px; }
</style>
