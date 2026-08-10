<script setup lang="ts">
import { onMounted, ref } from 'vue'
import request from '../../api/request'

interface Review {
  id: number
  orderId: number
  reviewerName: string
  reviewerAvatar: string
  rating: number
  content: string
  tags: string[]
  createdAt: string
}

const reviews = ref<Review[]>([])
const loading = ref(false)
const ratingFilter = ref(0)

async function loadReviews() {
  loading.value = true
  try {
    const result: any = await request.get('/reviews/my')
    reviews.value = (result || []).filter((review: Review) => ratingFilter.value === 0 || review.rating === ratingFilter.value)
  } catch {
    reviews.value = []
  } finally {
    loading.value = false
  }
}

function formatTime(value: string) {
  return value?.replace('T', ' ').substring(0, 16) || ''
}

onMounted(loadReviews)
</script>

<template>
  <main class="mobile-page no-tabbar">
    <van-nav-bar title="我的评价" left-arrow @click-left="$router.back()" />

    <div class="pill-row">
      <button
        v-for="rating in [0, 5, 4, 3, 2, 1]"
        :key="rating"
        type="button"
        class="pill"
        :class="{ active: ratingFilter === rating }"
        @click="ratingFilter = rating; loadReviews()"
      >
        {{ rating === 0 ? '全部' : `${rating} 星` }}
      </button>
    </div>

    <div v-if="loading" class="mobile-card loading-card">
      <van-loading color="#3157ff" />
      <span>正在加载评价</span>
    </div>

    <section v-else-if="reviews.length" class="review-list">
      <article v-for="review in reviews" :key="review.id" class="review-card">
        <div class="review-head">
          <van-image round width="42" height="42" :src="review.reviewerAvatar || 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'" />
          <div>
            <strong>{{ review.reviewerName || '匿名用户' }}</strong>
            <span>{{ formatTime(review.createdAt) }}</span>
          </div>
          <van-rate :model-value="review.rating" readonly size="14" color="#f79009" void-color="#d0d5dd" />
        </div>
        <p>{{ review.content || '用户暂未填写文字评价。' }}</p>
        <div v-if="review.tags?.length" class="tag-row">
          <span v-for="tag in review.tags" :key="tag">{{ tag }}</span>
        </div>
        <small>订单 #{{ review.orderId }}</small>
      </article>
    </section>

    <div v-else class="empty-state">
      <div>
        <h3>暂无评价</h3>
        <p>完成订单后，你和陪玩可以互相评价。</p>
      </div>
    </div>
  </main>
</template>

<style scoped>
.pill-row {
  margin-top: 14px;
}

.loading-card {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.review-list {
  display: grid;
  gap: 10px;
  margin-top: 10px;
}

.review-card {
  border: 1px solid rgba(228,231,236,.95);
  border-radius: 18px;
  background: rgba(255,255,255,.92);
  padding: 15px;
  box-shadow: 0 8px 22px rgba(16,24,40,.05);
}

.review-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.review-head div {
  flex: 1;
  min-width: 0;
}

.review-head strong,
.review-head span {
  display: block;
}

.review-head strong {
  color: var(--mobile-ink);
  font-size: 14px;
}

.review-head span {
  margin-top: 2px;
  color: var(--mobile-faint);
  font-size: 11px;
}

.review-card p {
  margin: 12px 0 8px;
  color: var(--mobile-muted);
  font-size: 14px;
  line-height: 1.65;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}

.tag-row span {
  border-radius: 999px;
  background: #f2f4f7;
  color: var(--mobile-muted);
  padding: 4px 8px;
  font-size: 11px;
  font-weight: 750;
}

.review-card small {
  color: var(--mobile-faint);
  font-size: 11px;
}
</style>
