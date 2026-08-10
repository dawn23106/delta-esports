<template>
  <view class="mobile-page">
    <view v-if="loading" class="mobile-card loading-card">
      <view class="spinner" />
      <text>加载中</text>
    </view>

    <view v-else class="service-list">
      <view v-for="service in services" :key="service.id" class="service-card">
        <view class="sc-left">
          <text class="sc-icon">{{ categoryIcon(service.category) }}</text>
        </view>
        <view class="sc-main">
          <text class="sc-name">{{ service.name }}</text>
          <text v-if="service.guaranteeDesc" class="sc-desc">{{ service.guaranteeDesc }}</text>
          <text class="sc-refund" v-if="service.refundPolicy">{{ service.refundPolicy }}</text>
        </view>
        <view class="sc-right">
          <text class="sc-price">￥{{ service.basePrice }}</text>
          <text class="sc-unit">/ {{ service.priceUnit === 'hour' ? '小时' : '局' }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue"
import { getServices } from "@/api/services"

const services = ref<any[]>([])
const loading = ref(false)

const catIcons: Record<string, string> = {
  陪玩专区: '🎧', 老板护航: '🛡️', 监狱专区: '🔥', 趣味玩法: '🎯', 特殊玩法: '⚡',
}

function categoryIcon(cat: string) { return catIcons[cat] || '🎮' }

onMounted(async () => {
  loading.value = true
  try {
    const result: any = await getServices()
    services.value = Array.isArray(result) ? result : []
  } finally { loading.value = false }
})
</script>

<style scoped>
.loading-card { display: flex; align-items: center; justify-content: center; gap: 10px; color: var(--mobile-muted); }

.service-list { display: flex; flex-direction: column; gap: 10px; }

.service-card {
  display: flex; gap: 12px; align-items: center;
  border: 1px solid rgba(228,231,236,.95); border-radius: 18px;
  background: rgba(255,255,255,.9); padding: 14px;
  box-shadow: 0 8px 22px rgba(16,24,40,.05);
}

.sc-icon { font-size: 32px; }

.sc-main { flex: 1; min-width: 0; }
.sc-name { font-size: 15px; font-weight: 850; color: var(--mobile-ink); display: block; }
.sc-desc { font-size: 12px; color: var(--mobile-muted); display: block; margin-top: 4px; }
.sc-refund { font-size: 11px; color: var(--mobile-success); display: block; margin-top: 4px; }

.sc-right { text-align: right; flex-shrink: 0; }
.sc-price { font-size: 17px; font-weight: 900; color: var(--mobile-brand); display: block; }
.sc-unit { font-size: 11px; color: var(--mobile-faint); display: block; margin-top: 2px; }
</style>
