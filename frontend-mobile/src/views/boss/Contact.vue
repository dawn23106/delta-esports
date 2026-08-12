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
  <main class="mobile-page no-tabbar">
    <van-nav-bar title="联系客服" left-arrow @click-left="$router.back()" />

    <section class="contact-card">
      <div class="contact-icon">
        <span>🎧</span>
      </div>
      <strong class="contact-phone">400-123-4567</strong>
      <p class="contact-hours">客服在线时间：每天 9:00 - 24:00</p>
      <van-button round block type="primary" class="call-btn" color="linear-gradient(135deg, #3157ff, #08b6d8)">
        <a href="tel:400-123-4567" class="call-link">拨打电话</a>
      </van-button>
    </section>

    <div class="section-title">常见问题</div>
    <section class="mobile-card faq-card">
      <div class="faq-item">
        <strong>如何下单？</strong>
        <p>选择服务 → 填写信息 → 等待匹配</p>
      </div>
      <div class="faq-item">
        <strong>订单如何进行？</strong>
        <p>匹配陪陪后开始代练，完成后确认</p>
      </div>
      <div class="faq-item">
        <strong>退款规则是什么？</strong>
        <p>不达标退50%，超时退全额</p>
      </div>
      <div class="faq-item">
        <strong>如何评价陪陪？</strong>
        <p>订单确认后即可评价</p>
      </div>
    </section>

    <template v-if="announcements.length">
      <div class="section-title">最新公告</div>
      <section class="mobile-card ann-card">
        <div v-for="(a, i) in announcements.filter((a: any) => a.isActive).slice(0, 3)" :key="a.id" class="ann-item" :class="{ last: i === Math.min(announcements.filter((a: any) => a.isActive).length, 3) - 1 }">
          <strong>{{ a.title }}</strong>
          <p>{{ a.content }}</p>
        </div>
      </section>
    </template>
  </main>
</template>

<style scoped>
.contact-card {
  border-radius: 22px;
  padding: 32px 24px;
  text-align: center;
  color: #fff;
  margin-bottom: 22px;
  background: linear-gradient(135deg, rgba(16, 19, 35, .78), rgba(49, 87, 255, .64)),
    url('https://images.unsplash.com/photo-1612287230202-1ff1d85d1bdf?w=1000&h=900&fit=crop');
  background-size: cover;
  background-position: center;
  box-shadow: 0 18px 42px rgba(16, 24, 40, .18);
}

.contact-icon {
  margin-bottom: 12px;
  font-size: 42px;
}

.contact-phone {
  display: block;
  font-size: 26px;
  font-weight: 950;
  letter-spacing: 2px;
  margin-bottom: 6px;
}

.contact-hours {
  margin: 0 0 20px;
  font-size: 13px;
  opacity: .78;
}

.call-btn {
  max-width: 210px;
  margin: 0 auto;
}

.call-link {
  color: #fff;
  text-decoration: none;
}

.faq-card {
  margin-bottom: 22px;
}

.faq-item {
  padding: 12px 0;
  border-bottom: 1px solid var(--mobile-line);
}

.faq-item:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.faq-item:first-child {
  padding-top: 0;
}

.faq-item strong {
  display: block;
  color: var(--mobile-ink);
  font-size: 14px;
  font-weight: 800;
}

.faq-item p {
  margin: 4px 0 0;
  color: var(--mobile-muted);
  font-size: 12px;
  line-height: 1.5;
}

.ann-card {
  display: grid;
  gap: 0;
}

.ann-item {
  padding: 13px 0;
  border-bottom: 1px solid var(--mobile-line);
}

.ann-item.last {
  border-bottom: 0;
  padding-bottom: 0;
}

.ann-item:first-child {
  padding-top: 0;
}

.ann-item strong {
  display: block;
  color: var(--mobile-ink);
  font-size: 14px;
  font-weight: 800;
}

.ann-item p {
  margin: 5px 0 0;
  color: var(--mobile-muted);
  font-size: 12px;
  line-height: 1.55;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
</style>
