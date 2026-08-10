<template>
  <view class="mobile-page">
    <view class="mobile-hero contact-hero">
      <text class="eyebrow">Support</text>
      <text class="page-title">客服中心</text>
      <text class="page-subtitle">遇到问题，随时联系我们。</text>
    </view>

    <!-- 联系信息 -->
    <view class="mobile-card contact-card">
      <view class="contact-row">
        <text>客服电话</text>
        <text class="contact-val">400-123-4567</text>
      </view>
      <view class="contact-row">
        <text>工作时间</text>
        <text class="contact-val">9:00 - 21:00</text>
      </view>
      <view class="contact-row">
        <text>客服微信</text>
        <text class="contact-val">DeltaEsportsCS</text>
      </view>
    </view>

    <!-- 公告列表 -->
    <text class="section-title">平台公告</text>
    <view v-if="announcements.length" class="anno-list">
      <view v-for="a in announcements" :key="a.id" class="mobile-card anno-card">
        <text class="anno-title">{{ a.title }}</text>
        <text class="anno-content">{{ a.content }}</text>
        <text class="anno-time">{{ a.createdAt?.replace('T', ' ').substring(0, 10) }}</text>
      </view>
    </view>
    <view v-else class="empty-state">
      <text class="empty-title">暂无公告</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue"
import { getAnnouncements } from "@/api/users"

const announcements = ref<any[]>([])

onMounted(async () => {
  try {
    announcements.value = (await getAnnouncements()) as any[] || []
  } catch { announcements.value = [] }
})
</script>

<style scoped>
.contact-hero {
  background-image: linear-gradient(135deg, #2c704d, #65a878);
  background-size: cover; background-position: center;
}

.contact-card { margin-top: 14px; display: flex; flex-direction: column; gap: 12px; }

.contact-row { display: flex; justify-content: space-between; font-size: 14px; color: var(--mobile-muted); }
.contact-val { font-weight: 750; color: var(--mobile-ink); }

.anno-list { display: flex; flex-direction: column; gap: 10px; }
.anno-card { display: flex; flex-direction: column; gap: 6px; }
.anno-title { font-size: 15px; font-weight: 850; color: var(--mobile-ink); }
.anno-content { font-size: 13px; color: var(--mobile-muted); line-height: 1.6; }
.anno-time { font-size: 11px; color: var(--mobile-faint); }

.empty-title { display: block; font-size: 17px; color: var(--mobile-ink); }
</style>
