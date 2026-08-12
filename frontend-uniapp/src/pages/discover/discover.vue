<template>
  <view class="mobile-page">
    <template v-if="auth.userRole === 'booster' && !auth.isGuest">
      <view class="mobile-hero work-hero">
        <text class="eyebrow">打手工作台</text>
        <text class="page-title">接单与履约</text>
        <text class="page-subtitle">接单后会自动进入“我的订单”，并开放与老板的订单聊天。</text>
        <view class="work-metrics">
          <view><text>{{ poolOrders.length }}</text><text>可接订单</text></view>
          <view><text>{{ activeOrders.length }}</text><text>进行中</text></view>
          <view><text>{{ finishedCount }}</text><text>已完成</text></view>
        </view>
      </view>

      <view class="tabs">
        <view :class="['tab', { active: tab === 'pool' }]" @tap="tab = 'pool'">订单池</view>
        <view :class="['tab', { active: tab === 'mine' }]" @tap="tab = 'mine'">我的订单</view>
      </view>

      <view v-if="loading" class="mobile-card loading">正在加载订单…</view>

      <template v-else-if="shownOrders.length">
        <view v-for="order in shownOrders" :key="order.id" class="mobile-card order-card">
          <view class="order-head">
            <view><text class="order-name">{{ order.serviceName || `订单 #${order.id}` }}</text><text class="order-no">#{{ order.id }}</text></view>
            <text class="order-amount">¥{{ order.amount }}</text>
          </view>
          <view class="order-meta">
            <text>{{ order.gameMap || '标准服务' }}</text>
            <text>{{ statusText[order.status] || order.status }}</text>
          </view>
          <text v-if="order.bossNote" class="order-note">{{ order.bossNote }}</text>

          <button v-if="tab === 'pool'" class="btn-primary" :disabled="claimingId === order.id" @tap="handleClaim(order)">
            {{ claimingId === order.id ? '正在接单…' : '接下此单' }}
          </button>
          <view v-else class="order-actions">
            <navigator v-if="canChat(order)" :url="`/pages/boss/chat?orderId=${order.id}`" class="outline-btn">订单聊天</navigator>
            <button v-if="order.status === 'assigned'" class="solid-btn" @tap="handleStart(order)">开始服务</button>
            <button v-if="order.status === 'in_progress'" class="solid-btn" @tap="openComplete(order)">提交结单</button>
            <text v-if="['submitted', 'done', 'settled'].includes(order.status)" class="finished-hint">{{ statusText[order.status] }}</text>
          </view>
        </view>
      </template>

      <view v-else class="empty-state">
        <view><text class="empty-title">{{ tab === 'pool' ? '暂时没有新订单' : '还没有我的订单' }}</text><text class="empty-desc">{{ tab === 'pool' ? '稍后刷新再看看。' : '从订单池接单后会自动出现在这里。' }}</text></view>
      </view>
    </template>

    <template v-else>
      <view class="mobile-hero customer-hero">
        <text class="eyebrow">服务说明</text>
        <text class="page-title">标准服务，不用先问客服</text>
        <text class="page-subtitle">首页选择已上架服务直接下单；只有列表里没有的特殊需求，才由客服沟通创建订单。</text>
      </view>
      <view class="steps">
        <view v-for="(step, index) in steps" :key="step.title" class="mobile-card step-card">
          <text class="step-index">0{{ index + 1 }}</text>
          <view><text class="step-title">{{ step.title }}</text><text class="step-desc">{{ step.desc }}</text></view>
        </view>
      </view>
      <button class="btn-primary" @tap="goHome">去选择服务</button>
      <navigator url="/pages/boss/contact" class="custom-link">没有合适服务？联系人工客服 →</navigator>
    </template>

    <view v-if="showComplete" class="action-sheet-overlay" @tap="showComplete = false">
      <view class="action-sheet-panel" @tap.stop>
        <view class="complete-sheet">
          <text class="sheet-title">提交结单</text>
          <textarea v-model="resultNote" class="result-input" maxlength="500" placeholder="填写完成情况或截图说明" />
          <button class="btn-primary" @tap="handleComplete">确认提交</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from "vue"
import { onShow } from "@dcloudio/uni-app"
import { useAuthStore } from "@/store/auth"
import { claimOrder, completeOrder, getMyOrders, getOrderPool, startOrder } from "@/api/orders"

const auth = useAuthStore()
const tab = ref<'pool' | 'mine'>('pool')
const poolOrders = ref<any[]>([])
const myOrders = ref<any[]>([])
const loading = ref(false)
const claimingId = ref(0)
const showComplete = ref(false)
const completingOrder = ref<any>(null)
const resultNote = ref('')

const statusText: Record<string, string> = {
  pending: '待接单', assigned: '待开始', in_progress: '服务中', submitted: '待老板确认',
  done: '已完成', settled: '已结算', cancelled: '已取消', disputed: '争议中',
}
const steps = [
  { title: '首页选服务', desc: '小时陪玩、撤离护航等已上架服务都可以直接查看价格。' },
  { title: '确认并下单', desc: '无需填写复杂申请，也不用先与客服咨询。' },
  { title: '接单后聊天', desc: '打手接单后开放聊天，用来传房间号和对局信息。' },
]

const activeOrders = computed(() => myOrders.value.filter((o) => ['assigned', 'in_progress', 'submitted'].includes(o.status)))
const finishedCount = computed(() => myOrders.value.filter((o) => ['done', 'settled'].includes(o.status)).length)
const shownOrders = computed(() => tab.value === 'pool' ? poolOrders.value : myOrders.value)

function canChat(order: any) { return ['assigned', 'in_progress', 'submitted', 'done', 'settled'].includes(order.status) }
function goHome() { uni.switchTab({ url: '/pages/home/home' }) }

async function load() {
  if (auth.userRole !== 'booster' || auth.isGuest) return
  loading.value = true
  try {
    const [poolResult, myResult]: any[] = await Promise.all([getOrderPool(1, 50), getMyOrders(1, 50)])
    poolOrders.value = poolResult.records || []
    myOrders.value = myResult.records || []
  } catch {
    poolOrders.value = []
    myOrders.value = []
  } finally { loading.value = false }
}

async function handleClaim(order: any) {
  claimingId.value = order.id
  try {
    await claimOrder(order.id)
    tab.value = 'mine'
    await load()
    uni.showToast({ title: '接单成功，已进入我的订单', icon: 'none' })
  } catch (e: any) {
    uni.showToast({ title: e?.data?.message || '接单失败', icon: 'none' })
  } finally { claimingId.value = 0 }
}

async function handleStart(order: any) {
  try { await startOrder(order.id); await load(); uni.showToast({ title: '服务已开始', icon: 'success' }) }
  catch (e: any) { uni.showToast({ title: e?.data?.message || '操作失败', icon: 'none' }) }
}

function openComplete(order: any) { completingOrder.value = order; resultNote.value = ''; showComplete.value = true }
async function handleComplete() {
  if (!completingOrder.value) return
  try {
    await completeOrder({ orderId: completingOrder.value.id, isQualified: true, resultNote: resultNote.value || undefined })
    showComplete.value = false
    await load()
    uni.showToast({ title: '已提交，等待老板确认', icon: 'none' })
  } catch (e: any) { uni.showToast({ title: e?.data?.message || '提交失败', icon: 'none' }) }
}

onShow(load)
</script>

<style scoped>
.work-hero, .customer-hero { background: linear-gradient(145deg, #dfeee2, #fff4dd); color: #173e2a; box-shadow: 0 16px 38px rgba(44,112,77,.12); }
.work-hero .eyebrow, .customer-hero .eyebrow { color: #4e765d; }
.work-hero .page-title, .customer-hero .page-title { color: #173e2a; }
.work-hero .page-subtitle, .customer-hero .page-subtitle { color: #587060; }
.work-metrics { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin-top: 18px; }
.work-metrics view { padding: 10px; border-radius: 14px; background: rgba(255,255,255,.68); text-align: center; }
.work-metrics text:first-child { display: block; color: #245d40; font-size: 19px; font-weight: 900; }
.work-metrics text:last-child { color: #6f8175; font-size: 10px; }
.tabs { display: grid; grid-template-columns: 1fr 1fr; margin: 18px 0 12px; padding: 4px; border-radius: 999px; background: #e5ebe5; }
.tab { padding: 10px; border-radius: 999px; text-align: center; color: #718076; font-size: 13px; font-weight: 800; }
.tab.active { background: #fff; color: var(--mobile-brand); box-shadow: 0 5px 15px rgba(43,77,55,.08); }
.loading { text-align: center; color: var(--mobile-muted); }
.order-card { margin-bottom: 10px; }
.order-head { display: flex; justify-content: space-between; gap: 12px; }
.order-name { color: var(--mobile-ink); font-size: 15px; font-weight: 900; }
.order-no { margin-left: 7px; color: var(--mobile-faint); font-size: 11px; }
.order-amount { color: var(--mobile-brand); font-size: 20px; font-weight: 900; }
.order-meta { display: flex; justify-content: space-between; margin-top: 8px; color: var(--mobile-muted); font-size: 12px; }
.order-note { display: block; margin: 10px 0; padding: 9px 11px; border-radius: 10px; background: #f5f7f4; color: #647169; font-size: 11px; }
.order-card .btn-primary { margin-top: 13px; padding: 11px; font-size: 14px; }
.order-actions { display: flex; align-items: center; justify-content: flex-end; gap: 9px; margin-top: 13px; }
.outline-btn, .solid-btn { min-width: 88px; margin: 0; padding: 9px 14px; border-radius: 999px; font-size: 12px; text-align: center; }
.outline-btn { border: 1px solid #7eaa8b; color: var(--mobile-brand); }
.solid-btn { border: 0; background: var(--mobile-brand); color: #fff; }
.finished-hint { color: var(--mobile-muted); font-size: 12px; }
.steps { display: flex; flex-direction: column; gap: 10px; margin: 18px 0; }
.step-card { display: flex; align-items: flex-start; gap: 14px; }
.step-index { color: #d4964e; font-size: 20px; font-weight: 900; }
.step-title { color: var(--mobile-ink); font-size: 15px; font-weight: 850; }
.step-desc { display: block; margin-top: 4px; color: var(--mobile-muted); font-size: 12px; line-height: 1.5; }
.custom-link { display: block; padding: 16px; text-align: center; color: #9b6a2f; font-size: 12px; }
.complete-sheet { display: flex; flex-direction: column; gap: 14px; padding: 20px 18px 28px; }
.sheet-title { color: var(--mobile-ink); font-size: 20px; font-weight: 900; }
.result-input { width: 100%; min-height: 100px; box-sizing: border-box; padding: 13px; border: 1px solid var(--mobile-line); border-radius: 14px; background: #fff; }
.empty-title { display: block; color: var(--mobile-ink); font-size: 16px; font-weight: 850; }
.empty-desc { display: block; margin-top: 5px; color: var(--mobile-muted); font-size: 12px; }
</style>
