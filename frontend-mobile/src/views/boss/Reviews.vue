<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../../api/request'

interface Review {
  id: number
  orderId: number
  reviewerId: number
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
    // 获取当前用户的评价
    const res: any = await request.get('/reviews/my')
    reviews.value = (res || []).filter((r: Review) =>
      ratingFilter.value === 0 || r.rating === ratingFilter.value
    )
  } catch {
    // 如果后端没有评价接口，显示示例数据
    reviews.value = []
  } finally {
    loading.value = false
  }
}

function starArray(rating: number): number[] {
  return Array.from({ length: 5 }, (_, i) => i + 1)
}

function formatTime(t: string) {
  return t?.replace('T', ' ').substring(0, 16) || ''
}

onMounted(loadReviews)
</script>

<template>
  <div class="page">
    <van-nav-bar title="我的评价" left-arrow @click-left="$router.back()" fixed placeholder />

    <!-- 筛选 -->
    <div class="filter-bar">
      <span
        v-for="n in [0, 5, 4, 3, 2, 1]"
        :key="n"
        :class="['filter-chip', { active: ratingFilter === n }]"
        @click="ratingFilter = n; loadReviews()"
      >
        {{ n === 0 ? '全部' : `${n}星` }}
      </span>
    </div>

    <!-- 评价列表 -->
    <div class="review-list" v-if="reviews.length > 0">
      <div v-for="r in reviews" :key="r.id" class="review-card">
        <div class="review-head">
          <van-image round width="40" height="40" :src="r.reviewerAvatar || 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'" />
          <div class="review-user">
            <div class="review-name">{{ r.reviewerName }}</div>
            <div class="review-stars">
              <span v-for="s in starArray(r.rating)" :key="s" class="star" :class="{ filled: s <= r.rating }">★</span>
            </div>
          </div>
          <span class="review-time">{{ formatTime(r.createdAt) }}</span>
        </div>
        <div class="review-content">{{ r.content }}</div>
        <div class="review-tags" v-if="r.tags?.length">
          <span v-for="t in r.tags" :key="t" class="tag">{{ t }}</span>
        </div>
        <div class="review-order">订单 #{{ r.orderId }}</div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!loading" class="empty">
      <div class="empty-icon">
        <span class="empty-emoji">⭐</span>
      </div>
      <div class="empty-title">暂无评价</div>
      <div class="empty-desc">完成订单后，你和陪陪可以互相评价</div>
    </div>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f8fafc;
}

.filter-bar {
  display: flex;
  gap: 8px;
  padding: 16px;
  overflow-x: auto;
}
.filter-chip {
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  cursor: pointer;
  background: #fff;
  color: #64748b;
  border: 1px solid #e2e8f0;
  transition: all 0.2s;
}
.filter-chip.active {
  background: #6366f1;
  color: #fff;
  border-color: #6366f1;
}

.review-list {
  padding: 0 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.review-card {
  background: #fff;
  border-radius: 18px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.03);
  border: 1px solid #f1f5f9;
}
.review-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.review-user {
  flex: 1;
}
.review-name {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}
.review-stars {
  display: flex;
  gap: 1px;
  margin-top: 2px;
}
.star {
  font-size: 14px;
  color: #e2e8f0;
}
.star.filled {
  color: #f59e0b;
}
.review-time {
  font-size: 12px;
  color: #94a3b8;
}
.review-content {
  font-size: 14px;
  color: #475569;
  line-height: 1.6;
  margin-bottom: 8px;
}
.review-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}
.tag {
  padding: 2px 10px;
  border-radius: 10px;
  background: #f1f5f9;
  color: #64748b;
  font-size: 12px;
}
.review-order {
  font-size: 11px;
  color: #cbd5e1;
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 0;
}
.empty-icon {
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
  margin-bottom: 6px;
}
.empty-desc {
  font-size: 13px;
  color: #94a3b8;
  text-align: center;
  max-width: 240px;
}
</style>
