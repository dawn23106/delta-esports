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
        <text class="eyebrow">ONLINE SQUAD</text>
        <text class="page-title">选择心仪陪陪</text>
        <text class="page-subtitle">看评分、接单量和在线状态，也可以指定陪陪下单。</text>
        <view class="work-metrics">
          <view><text>{{ onlineBoosters.length }}</text><text>当前在线</text></view>
          <view><text>{{ boosters.length }}</text><text>全部陪陪</text></view>
          <view><text>{{ averageRating }}</text><text>平均评分</text></view>
        </view>
      </view>

      <view v-if="loading" class="mobile-card loading">正在寻找在线陪陪…</view>
      <view v-else-if="onlineBoosters.length" class="booster-list">
        <view v-for="booster in onlineBoosters" :key="booster.id" class="mobile-card booster-card">
          <view class="booster-avatar">
            <image v-if="booster.avatar" :src="booster.avatar" mode="aspectFill" />
            <text v-else>{{ (booster.nickname || '陪').slice(0, 1) }}</text>
            <view :class="['status-dot', booster.boosterStatus]" />
          </view>
          <view class="booster-info">
            <view class="booster-title"><text>{{ booster.nickname || '未命名陪陪' }}</text><text>{{ booster.boosterStatus === 'idle' ? '空闲' : '忙碌' }}</text></view>
            <view class="booster-meta"><text>★ {{ booster.rating || '5.0' }}</text><text>{{ booster.totalOrders || 0 }} 单</text></view>
            <text class="booster-intro">{{ booster.introduction || '认真陪玩，接单后可在聊天中沟通房间号。' }}</text>
            <button class="choose-btn" :disabled="booster.boosterStatus !== 'idle'" @tap="openBoosterOrder(booster)">{{ booster.boosterStatus === 'idle' ? '指定 TA 下单' : '服务中' }}</button>
          </view>
        </view>
      </view>
      <view v-else class="empty-state"><view><text class="empty-title">暂无在线陪陪</text><text class="empty-desc">陪陪注册并开启接单后会显示在这里。</text></view></view>
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

    <view v-if="showBoosterOrder" class="action-sheet-overlay" @tap="showBoosterOrder = false">
      <view class="action-sheet-panel" @tap.stop>
        <view class="complete-sheet">
          <text class="sheet-title">指定 {{ selectedBooster?.nickname }} 下单</text>
          <scroll-view scroll-y class="service-picker">
            <view v-for="service in services" :key="service.id" :class="['service-option', { active: selectedServiceId === service.id }]" @tap="selectedServiceId = service.id">
              <view><text>{{ service.name }}</text><text>{{ service.guaranteeDesc || service.description }}</text></view>
              <text>¥{{ service.basePrice }}</text>
            </view>
          </scroll-view>
          <button class="btn-primary" :disabled="!selectedServiceId || creatingOrder" @tap="submitBoosterOrder">{{ creatingOrder ? '正在创建订单…' : '确认并查看订单' }}</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from "vue"
import { onShow } from "@dcloudio/uni-app"
import { useAuthStore } from "@/store/auth"
import { claimOrder, completeOrder, createOrder, getMyOrders, getOrderPool, startOrder } from "@/api/orders"
import { getBoosters } from "@/api/users"
import { getServices } from "@/api/services"

const auth = useAuthStore()
const tab = ref<'pool' | 'mine'>('pool')
const poolOrders = ref<any[]>([])
const myOrders = ref<any[]>([])
const loading = ref(false)
const claimingId = ref(0)
const showComplete = ref(false)
const completingOrder = ref<any>(null)
const resultNote = ref('')
const boosters = ref<any[]>([])
const services = ref<any[]>([])
const selectedBooster = ref<any>(null)
const selectedServiceId = ref(0)
const showBoosterOrder = ref(false)
const creatingOrder = ref(false)

const statusText: Record<string, string> = {
  pending: '待接单', assigned: '待开始', in_progress: '服务中', submitted: '待老板确认',
  done: '已完成', settled: '已结算', cancelled: '已取消', disputed: '争议中',
}
const activeOrders = computed(() => myOrders.value.filter((o) => ['assigned', 'in_progress', 'submitted'].includes(o.status)))
const finishedCount = computed(() => myOrders.value.filter((o) => ['done', 'settled'].includes(o.status)).length)
const shownOrders = computed(() => tab.value === 'pool' ? poolOrders.value : myOrders.value)
const onlineBoosters = computed(() => boosters.value.filter((item) => item.boosterStatus !== 'offline'))
const averageRating = computed(() => onlineBoosters.value.length ? (onlineBoosters.value.reduce((sum, item) => sum + Number(item.rating || 5), 0) / onlineBoosters.value.length).toFixed(1) : '—')

function canChat(order: any) { return ['assigned', 'in_progress', 'submitted', 'done', 'settled'].includes(order.status) }
function openBoosterOrder(booster: any) {
  selectedBooster.value = booster
  selectedServiceId.value = services.value[0]?.id || 0
  showBoosterOrder.value = true
}

async function submitBoosterOrder() {
  if (!selectedBooster.value || !selectedServiceId.value) return
  if (auth.isGuest) return void uni.navigateTo({ url: '/pages/auth/login' })
  creatingOrder.value = true
  try {
    const order: any = await createOrder({ serviceId: selectedServiceId.value, boosterId: selectedBooster.value.id, gameRegion: '微信区', gameRank: '不限', gameMap: '指定陪陪', bossNote: `指定陪陪：${selectedBooster.value.nickname}` })
    showBoosterOrder.value = false
    uni.navigateTo({ url: `/pages/boss/order-detail?id=${order.id}` })
  } catch (e: any) { uni.showToast({ title: e?.data?.message || '下单失败', icon: 'none' }) }
  finally { creatingOrder.value = false }
}

async function load() {
  const isBooster = auth.userRole === 'booster' && !auth.isGuest
  uni.setTabBarItem({ index: 1, text: isBooster ? '接单' : '陪陪' })
  loading.value = true
  try {
    if (isBooster) {
      const [poolResult, myResult]: any[] = await Promise.all([getOrderPool(1, 50), getMyOrders(1, 50)])
      poolOrders.value = poolResult.records || []
      myOrders.value = myResult.records || []
    } else {
      const [boosterResult, serviceResult]: any[] = await Promise.all([getBoosters(1, 50), getServices()])
      boosters.value = boosterResult || []
      services.value = Array.isArray(serviceResult) ? serviceResult : []
    }
  } catch {
    poolOrders.value = []
    myOrders.value = []
    boosters.value = []
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
.booster-list { display: flex; flex-direction: column; gap: 11px; margin-top: 16px; }
.booster-card { display: flex; gap: 13px; }
.booster-avatar { position: relative; display: flex; width: 58px; height: 58px; flex: 0 0 58px; align-items: center; justify-content: center; overflow: visible; border-radius: 19px; background: linear-gradient(145deg, #dbeadd, #f7e6c9); color: #285f42; font-size: 22px; font-weight: 900; }
.booster-avatar image { width: 100%; height: 100%; border-radius: 19px; }
.status-dot { position: absolute; right: -2px; bottom: -2px; width: 12px; height: 12px; border: 2px solid #fff; border-radius: 50%; background: #d39a4c; }
.status-dot.idle { background: #45a667; }
.booster-info { min-width: 0; flex: 1; }
.booster-title { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.booster-title text:first-child { overflow: hidden; color: var(--mobile-ink); font-size: 16px; font-weight: 900; text-overflow: ellipsis; white-space: nowrap; }
.booster-title text:last-child { flex: 0 0 auto; padding: 3px 8px; border-radius: 999px; background: #edf7ef; color: #397a52; font-size: 10px; font-weight: 800; }
.booster-meta { display: flex; gap: 12px; margin-top: 5px; color: #95703f; font-size: 11px; font-weight: 750; }
.booster-intro { display: -webkit-box; overflow: hidden; margin-top: 7px; color: var(--mobile-muted); font-size: 11px; line-height: 1.45; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.choose-btn { margin: 10px 0 0; padding: 8px 13px; border: 0; border-radius: 999px; background: linear-gradient(135deg, #285f42, #65a878); color: #fff; font-size: 11px; font-weight: 850; line-height: 1.4; }
.choose-btn[disabled] { opacity: .45; }
.service-picker { max-height: 45vh; }
.service-option { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 8px; padding: 12px; border: 1px solid var(--mobile-line); border-radius: 14px; background: #fff; }
.service-option.active { border-color: #65a878; background: #f0f8f1; }
.service-option view { min-width: 0; }
.service-option view text:first-child { display: block; color: var(--mobile-ink); font-size: 13px; font-weight: 850; }
.service-option view text:last-child { display: block; overflow: hidden; margin-top: 3px; color: var(--mobile-muted); font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }
.service-option > text { flex: 0 0 auto; color: #2c704d; font-size: 15px; font-weight: 900; }
</style>
