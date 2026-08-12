<template>
  <view class="mobile-page">
    <view v-if="loading" class="mobile-card loading-card">
      <view class="spinner" />
      <text>加载中</text>
    </view>

    <view v-else-if="gifts.length" class="gift-list">
      <view v-for="gift in gifts" :key="gift.id" class="gift-card">
        <view class="gift-head">
          <text class="gift-name">{{ gift.giftName }}</text>
          <text class="gift-price">￥{{ gift.price }}</text>
        </view>
        <text v-if="gift.message" class="gift-msg">{{ gift.message }}</text>
        <text class="gift-time">{{ gift.createdAt?.replace('T', ' ').substring(0, 16) }}</text>
      </view>
    </view>

    <view v-else class="empty-state">
      <text class="empty-title">暂无礼物</text>
      <text class="empty-desc">你还没有送出过礼物。</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue"
import { getSentGifts } from "@/api/users"

const gifts = ref<any[]>([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    gifts.value = (await getSentGifts()) as any[] || []
  } catch { gifts.value = [] }
  finally { loading.value = false }
})
</script>

<style scoped>
.loading-card { display: flex; align-items: center; justify-content: center; gap: 10px; color: var(--mobile-muted); }

.gift-list { display: flex; flex-direction: column; gap: 10px; }

.gift-card {
  border: 1px solid rgba(228,231,236,.95); border-radius: 18px;
  background: rgba(255,255,255,.9); padding: 14px;
  box-shadow: 0 8px 22px rgba(16,24,40,.05);
  display: flex; flex-direction: column; gap: 6px;
}

.gift-head { display: flex; justify-content: space-between; }
.gift-name { font-size: 15px; font-weight: 850; color: var(--mobile-ink); }
.gift-price { font-size: 17px; font-weight: 900; color: var(--mobile-warning); }
.gift-msg { font-size: 13px; color: var(--mobile-muted); }
.gift-time { font-size: 11px; color: var(--mobile-faint); }

.empty-title { display: block; font-size: 17px; color: var(--mobile-ink); }
.empty-desc { display: block; font-size: 13px; color: var(--mobile-muted); margin-top: 6px; }
</style>
