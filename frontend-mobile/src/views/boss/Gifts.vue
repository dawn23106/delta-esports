<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../../api/request'

const gifts = ref<any[]>([])

async function load() {
  try { const res: any = await request.get('/gifts/sent'); gifts.value = res || [] } catch { }
}

onMounted(load)
</script>

<template>
  <main class="mobile-page no-tabbar">
    <van-nav-bar title="我的礼物" left-arrow @click-left="$router.back()" />

    <template v-if="gifts.length">
      <div class="gift-list">
        <article v-for="g in gifts" :key="g.id" class="gift-card">
          <div class="gift-head">
            <span class="gift-name">{{ g.giftName }}</span>
            <strong class="gift-amount">¥{{ g.amount }}</strong>
          </div>
          <p v-if="g.message" class="gift-msg">{{ g.message }}</p>
          <p v-else class="gift-msg muted">无留言</p>
          <time class="gift-time">{{ g.createdAt?.replace('T', ' ').substring(0, 16) }}</time>
        </article>
      </div>
    </template>

    <div v-else class="empty-state">
      <div>
        <h3>暂无礼物</h3>
        <p>收到礼物后会在这里显示。</p>
      </div>
    </div>
  </main>
</template>

<style scoped>
.gift-list {
  display: grid;
  gap: 10px;
}

.gift-card {
  border: 1px solid rgba(228, 231, 236, .95);
  border-radius: 18px;
  background: rgba(255, 255, 255, .92);
  padding: 16px;
  box-shadow: 0 8px 20px rgba(16, 24, 40, .05);
}

.gift-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.gift-name {
  color: var(--mobile-ink);
  font-size: 15px;
  font-weight: 850;
}

.gift-amount {
  color: var(--mobile-warning);
  font-size: 20px;
  font-weight: 950;
}

.gift-msg {
  margin: 8px 0 6px;
  color: var(--mobile-muted);
  font-size: 13px;
  line-height: 1.5;
}

.gift-msg.muted {
  opacity: .55;
}

.gift-time {
  color: var(--mobile-faint);
  font-size: 11px;
  font-weight: 500;
}
</style>
