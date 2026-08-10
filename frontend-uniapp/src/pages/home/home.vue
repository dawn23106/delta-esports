<template>
  <view class="mobile-page home-page">
    <view class="mobile-hero hero">
      <view class="brand-line"><text class="brand-dot" /><text>沧月电竞</text></view>
      <text class="page-title">找个靠谱队友，马上开玩</text>
      <text class="page-subtitle">小时陪玩为主，撤离护航为辅。服务内容和价格清楚展示，选好即可支付下单。</text>
      <view class="hero-actions">
        <text class="hero-chip">真人陪玩</text>
        <text class="hero-chip">平台售后</text>
        <text class="hero-chip">接单后聊天</text>
      </view>
    </view>

    <view class="section-title">
      <text>常用服务</text>
      <text class="section-note">点击直接下单</text>
    </view>

    <view v-if="loading" class="mobile-card loading-card">正在同步服务…</view>

    <template v-else-if="grouped.length">
      <view v-for="group in grouped" :key="group.name" class="service-block">
        <view class="service-head">
          <view>
            <text class="service-group-name">{{ group.name }}</text>
            <text class="service-group-desc">{{ categoryDescription(group.name) }}</text>
          </view>
          <text class="service-count">{{ group.items.length }} 项</text>
        </view>

        <view class="service-list">
          <view v-for="service in group.items" :key="service.id" class="service-card" @tap="pick(service)">
            <view class="service-main">
              <text class="service-name">{{ cleanName(service.name) }}</text>
              <text class="service-desc">{{ service.guaranteeDesc || service.description || '按订单约定提供服务，接单后沟通房间号。' }}</text>
            </view>
            <view class="service-side">
              <text class="service-price">¥{{ service.basePrice }}</text>
              <text class="price-unit">/{{ priceUnit(service.priceUnit) }}</text>
              <text class="order-link">立即下单</text>
            </view>
          </view>
        </view>
      </view>
    </template>

    <view v-else class="mobile-card empty-card">
      <text class="empty-title">暂时没有可售服务</text>
      <text class="empty-desc">服务将从后台自动同步，请稍后再试。</text>
    </view>

    <navigator url="/pages/boss/contact" class="custom-card">
      <view>
        <text class="custom-kicker">列表里没有？</text>
        <text class="custom-title">特殊玩法与定制护航</text>
        <text class="custom-desc">只有未上架的服务才需要联系客服，由客服沟通后创建订单。</text>
      </view>
      <text class="custom-arrow">→</text>
    </navigator>

    <view v-if="showOrder" class="action-sheet-overlay" @tap="closeOrder">
      <view v-if="selectedService" class="action-sheet-panel" @tap.stop>
        <view class="sheet-handle" />
        <view class="sheet">
          <text class="sheet-title">确认服务</text>
          <view class="sheet-summary">
            <view>
              <text class="sheet-name">{{ selectedService.name }}</text>
              <text class="sheet-desc">{{ selectedService.guaranteeDesc || '按订单约定提供服务，接单后在聊天中传递房间号。' }}</text>
            </view>
            <text class="sheet-price">¥{{ selectedService.basePrice }}</text>
          </view>
          <view class="flow-row">
            <text>1 微信支付</text><text>2 打手接单</text><text>3 聊天进房</text>
          </view>
          <view class="notice">
            <text class="notice-title">下单说明</text>
            <text>标准服务无需先咨询客服；微信支付成功后进入待接单状态，打手接单后聊天自动开放。</text>
          </view>
          <button class="btn-primary" :disabled="submitting" @tap="submit">
            {{ submitting ? '正在拉起支付…' : `微信支付 ¥${selectedService.basePrice}` }}
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue"
import { onShareAppMessage } from "@dcloudio/uni-app"
import { createOrder } from "@/api/orders"
import { payOrder } from "@/api/payments"
import { getServices } from "@/api/services"
import { useAuthGuard } from "@/composables/useAuthGuard"

onShareAppMessage(() => ({ title: '沧月电竞｜小时陪玩与撤离护航', path: '/pages/home/home' }))

const services = ref<any[]>([])
const loading = ref(false)
const submitting = ref(false)
const showOrder = ref(false)
const selectedService = ref<any>(null)
const { requireLogin } = useAuthGuard()

const grouped = computed(() => {
  const buckets = new Map<string, any[]>()
  services.value.forEach((service) => {
    const category = service.category || '推荐服务'
    buckets.set(category, [...(buckets.get(category) || []), service])
  })
  return [...buckets.entries()].map(([name, items]) => ({ name, items }))
})

function categoryDescription(name: string) {
  if (name.includes('陪玩')) return '按小时计费，娱乐、技术和顶尖陪玩按需选择。'
  if (name.includes('护航')) return '按局计费，提供组队、路线和战术协作服务。'
  return '价格透明，确认后直接创建订单。'
}

function cleanName(name = '') { return name.split('·').pop()?.trim() || name }
function priceUnit(unit: string) { return unit === 'hour' ? '小时' : '局' }

async function load() {
  loading.value = true
  try {
    const result: any = await getServices()
    services.value = Array.isArray(result) ? result : []
  } catch {
    services.value = []
  } finally {
    loading.value = false
  }
}

function pick(service: any) {
  selectedService.value = service
  showOrder.value = true
}

function closeOrder() {
  if (!submitting.value) showOrder.value = false
}

async function submit() {
  if (!selectedService.value || submitting.value) return
  if (!await requireLogin('下单')) return
  submitting.value = true
  try {
    const order: any = await createOrder({
      serviceId: selectedService.value.id,
      gameRegion: '微信区',
      gameRank: '不限',
      gameMap: selectedService.value.category || '标准服务',
      bossNote: '标准服务直接下单',
    })
    try {
      const paymentResult: any = await payOrder(order.id)
      showOrder.value = false
      const confirmed = paymentResult?.orderStatus !== 'pending_payment'
      uni.showToast({ title: confirmed ? '支付成功' : '支付结果确认中', icon: confirmed ? 'success' : 'none' })
      setTimeout(() => uni.navigateTo({ url: `/pages/boss/order-detail?id=${order.id}` }), 500)
    } catch (payError: any) {
      showOrder.value = false
      const cancelled = String(payError?.errMsg || '').includes('cancel')
      uni.showModal({
        title: cancelled ? '已取消支付' : '暂未完成支付',
        content: '订单已经保留，可在订单详情中重新支付。未支付订单不会进入接单池。',
        showCancel: false,
        confirmText: '查看订单',
        success: () => uni.navigateTo({ url: `/pages/boss/order-detail?id=${order.id}` }),
      })
    }
  } catch (error: any) {
    uni.showToast({ title: error?.data?.message || error?.message || '下单失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.home-page { padding-top: 16px; }
.hero { min-height: 210px; display: flex; flex-direction: column; justify-content: center; background: linear-gradient(145deg, #e6f2e8, #f8f1df); color: #173e2a; box-shadow: 0 16px 38px rgba(44,112,77,.13); }
.brand-line { display: flex; align-items: center; gap: 8px; color: #2c704d; font-size: 13px; font-weight: 800; }
.brand-dot { width: 9px; height: 9px; border-radius: 50%; background: #df9b49; }
.page-title { margin-top: 18px; max-width: 280px; font-size: 27px; color: #173e2a; }
.page-subtitle { max-width: 310px; color: #587060; }
.hero-actions { display: flex; gap: 8px; margin-top: 20px; }
.hero-chip { padding: 6px 10px; border-radius: 999px; background: rgba(255,255,255,.72); color: #2c704d; font-size: 11px; font-weight: 750; }
.loading-card, .empty-card { text-align: center; color: var(--mobile-muted); }
.service-block { margin-top: 14px; }
.service-head { display: flex; justify-content: space-between; gap: 16px; margin: 0 2px 10px; }
.service-group-name { color: var(--mobile-ink); font-size: 17px; font-weight: 900; }
.service-group-desc { display: block; margin-top: 3px; color: var(--mobile-muted); font-size: 12px; }
.service-count { flex-shrink: 0; color: var(--mobile-brand); font-size: 12px; }
.service-list { display: flex; flex-direction: column; gap: 10px; }
.service-card { display: flex; align-items: center; gap: 14px; padding: 15px; border: 1px solid #dfe8df; border-radius: 18px; background: rgba(255,255,255,.94); box-shadow: 0 8px 22px rgba(43,77,55,.05); }
.service-main { min-width: 0; flex: 1; }
.service-name { color: var(--mobile-ink); font-size: 15px; font-weight: 850; }
.service-desc { display: -webkit-box; margin-top: 5px; overflow: hidden; color: var(--mobile-muted); font-size: 11px; line-height: 1.45; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.service-side { flex-shrink: 0; text-align: right; }
.service-price { color: var(--mobile-brand); font-size: 19px; font-weight: 900; }
.price-unit { color: var(--mobile-faint); font-size: 10px; }
.order-link { display: block; margin-top: 7px; color: #b56f24; font-size: 11px; font-weight: 750; }
.custom-card { display: flex; align-items: center; justify-content: space-between; margin-top: 18px; padding: 18px; border: 1px dashed #caa568; border-radius: 20px; background: #fff8e9; }
.custom-kicker { color: #9b6a2f; font-size: 11px; font-weight: 750; }
.custom-title { display: block; margin-top: 4px; color: #46331d; font-size: 16px; font-weight: 900; }
.custom-desc { display: block; max-width: 270px; margin-top: 5px; color: #806d55; font-size: 12px; line-height: 1.5; }
.custom-arrow { color: #9b6a2f; font-size: 25px; }
.sheet-handle { width: 42px; height: 4px; margin: 10px auto 2px; border-radius: 99px; background: #c9d4c9; }
.sheet { display: flex; flex-direction: column; gap: 14px; padding: 12px 18px 28px; }
.sheet-title { color: var(--mobile-ink); font-size: 20px; font-weight: 900; }
.sheet-summary { display: flex; align-items: center; justify-content: space-between; gap: 14px; padding: 15px; border: 1px solid #dbe8dc; border-radius: 18px; background: #f4faf4; }
.sheet-name { color: var(--mobile-ink); font-size: 15px; font-weight: 850; }
.sheet-desc { display: block; max-width: 235px; margin-top: 5px; color: var(--mobile-muted); font-size: 11px; line-height: 1.5; }
.sheet-price { flex-shrink: 0; color: var(--mobile-brand); font-size: 24px; font-weight: 900; }
.flow-row { display: flex; justify-content: space-between; color: #3f6d50; font-size: 11px; }
.notice { padding: 13px; border-radius: 14px; background: #fff8e9; color: #806d55; font-size: 12px; line-height: 1.6; }
.notice-title { display: block; color: #69491f; font-weight: 850; }
.empty-title { display: block; color: var(--mobile-ink); font-weight: 850; }
.empty-desc { display: block; margin-top: 4px; font-size: 12px; }
</style>
