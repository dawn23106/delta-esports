<template>
  <view class="mobile-page">
    <!-- 评分筛选 -->
    <scroll-view scroll-x class="pill-row">
      <view
        v-for="tab in tabs"
        :key="tab.value"
        class="pill"
        :class="{ active: starFilter === tab.value }"
        @tap="starFilter = tab.value"
      >
        {{ tab.label }}
      </view>
    </scroll-view>

    <view v-if="loading" class="mobile-card loading-card">
      <view class="spinner" />
      <text>加载中</text>
    </view>

    <view v-else-if="filtered.length" class="review-list">
      <view v-for="review in filtered" :key="review.id" class="review-card">
        <view class="review-head">
          <text class="review-user">{{ review.bossNickname || '匿名用户' }}</text>
          <view class="star-row">
            <text v-for="s in 5" :key="s" class="star" :class="{ active: s <= review.rating }">★</text>
          </view>
        </view>
        <text v-if="review.content" class="review-content">{{ review.content }}</text>
        <view v-if="review.tags" class="tag-row">
          <text v-for="tag in review.tags?.split(',')" :key="tag" class="tag">{{ tag }}</text>
        </view>
      </view>
    </view>

    <view v-else class="empty-state">
      <text class="empty-title">暂无评价</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue"
import { getMyReviews } from "@/api/users"

const reviews = ref<any[]>([])
const starFilter = ref(0)
const loading = ref(false)

const tabs = [
  { label: '全部', value: 0 },
  { label: '5星', value: 5 },
  { label: '4星', value: 4 },
  { label: '3星', value: 3 },
  { label: '低分', value: -1 },
]

const filtered = computed(() =>
  starFilter.value === 0
    ? reviews.value
    : starFilter.value === -1
      ? reviews.value.filter((r) => r.rating <= 2)
      : reviews.value.filter((r) => r.rating === starFilter.value)
)

onMounted(async () => {
  loading.value = true
  try {
    reviews.value = (await getMyReviews()) as any[] || []
  } catch { reviews.value = [] }
  finally { loading.value = false }
})
</script>

<style scoped>
.loading-card { display: flex; align-items: center; justify-content: center; gap: 10px; color: var(--mobile-muted); }

.review-list { display: flex; flex-direction: column; gap: 10px; margin-top: 12px; }

.review-card {
  border: 1px solid rgba(228,231,236,.95); border-radius: 18px;
  background: rgba(255,255,255,.9); padding: 14px;
  box-shadow: 0 8px 22px rgba(16,24,40,.05);
  display: flex; flex-direction: column; gap: 8px;
}

.review-head { display: flex; justify-content: space-between; align-items: center; }
.review-user { font-size: 14px; font-weight: 850; color: var(--mobile-ink); }

.review-content { font-size: 13px; color: var(--mobile-muted); line-height: 1.6; }

.tag-row { display: flex; flex-wrap: wrap; gap: 6px; }
.tag {
  font-size: 11px; padding: 3px 8px; border-radius: 999px;
  background: #f2f4f7; color: var(--mobile-muted);
}

.empty-title { display: block; font-size: 17px; color: var(--mobile-ink); }
</style>
