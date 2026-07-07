<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../../api/request'

const announcements = ref<any[]>([])

async function load() {
  try {
    const res: any = await request.get('/announcements')
    announcements.value = res || []
  } catch { }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <van-nav-bar title="联系客服" left-arrow @click-left="$router.back()" fixed placeholder />

    <div class="content">
      <!-- 客服信息卡片 -->
      <div class="contact-card">
        <div class="contact-icon-wrap">
          <span class="text-4xl">🎧</span>
        </div>
        <div class="contact-phone">400-123-4567</div>
        <div class="contact-hours">客服在线时间：每天 9:00 - 24:00</div>
        <van-button round block type="primary" class="call-btn" color="linear-gradient(135deg, #6366f1, #8b5cf6)">
          <a href="tel:400-123-4567" class="call-link">拨打电话</a>
        </van-button>
      </div>

      <!-- 快捷入口 -->
      <div class="quick-links">
        <div class="section-title">常见问题</div>
        <van-cell-group inset>
          <van-cell title="如何下单？" is-link label="选择服务 → 填写信息 → 等待匹配" />
          <van-cell title="订单如何进行？" is-link label="匹配陪陪后开始代练，完成后确认" />
          <van-cell title="退款规则是什么？" is-link label="不达标退50%，超时退全额" />
          <van-cell title="如何评价陪陪？" is-link label="订单确认后即可评价" />
        </van-cell-group>
      </div>

      <!-- 公告 -->
      <div class="announcements" v-if="announcements.length">
        <div class="section-title">最新公告</div>
        <div class="ann-list">
          <div v-for="a in announcements.filter((a: any) => a.isActive).slice(0, 3)" :key="a.id" class="ann-item">
            <div class="ann-title">{{ a.title }}</div>
            <div class="ann-content">{{ a.content }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f8fafc;
}
.content {
  padding: 16px;
}
.contact-card {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border-radius: 20px;
  padding: 32px 24px;
  text-align: center;
  color: #fff;
  margin-bottom: 24px;
  box-shadow: 0 8px 32px rgba(99,102,241,0.25);
}
.contact-icon-wrap {
  margin-bottom: 12px;
}
.contact-phone {
  font-size: 26px;
  font-weight: 800;
  letter-spacing: 2px;
  margin-bottom: 6px;
}
.contact-hours {
  font-size: 13px;
  opacity: 0.8;
  margin-bottom: 20px;
}
.call-btn {
  max-width: 200px;
  margin: 0 auto;
  height: 44px !important;
  border-radius: 14px !important;
}
.call-link {
  color: #fff;
  text-decoration: none;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: #94a3b8;
  padding: 0 8px 8px;
  margin-top: 8px;
}

.quick-links {
  margin-bottom: 24px;
}

.announcements {
  margin-top: 8px;
}
.ann-list {
  background: #fff;
  border-radius: 16px;
  padding: 8px 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.03);
  border: 1px solid #f1f5f9;
}
.ann-item {
  padding: 12px 0;
  border-bottom: 1px solid #f1f5f9;
}
.ann-item:last-child {
  border-bottom: none;
}
.ann-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}
.ann-content {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 4px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
