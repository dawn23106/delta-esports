<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getServices } from '../../api/services'

const services = ref<any[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const res: any = await getServices()
    services.value = res || []
  } catch { }
  loading.value = false
}

const categoryIcons: Record<string, string> = {
  '老板护航': '🛡️',
  '陪玩专区': '🎮',
  '监狱专区': '🔒',
  '趣味玩法': '🎯',
  '特殊玩法': '⚡',
}

onMounted(load)
</script>

<template>
  <main class="mobile-page no-tabbar">
    <van-nav-bar title="全部服务" left-arrow @click-left="$router.back()" />

    <div v-if="loading" class="loading-card mobile-card">
      <van-loading color="#3157ff" />
      <span>正在加载服务</span>
    </div>

    <template v-else-if="services.length">
      <div class="service-list">
        <article v-for="s in services" :key="s.id" class="service-card">
          <div class="service-head">
            <span class="service-emoji">{{ categoryIcons[s.category] || '🎮' }}</span>
            <div class="service-info">
              <strong class="service-name">{{ s.name }}</strong>
              <span class="service-category">{{ s.category }}</span>
            </div>
            <div class="service-price">
              <span class="price-num">¥{{ s.basePrice }}</span>
              <span class="price-unit">/{{ s.priceUnit === 'hour' ? '小时' : '局' }}</span>
            </div>
          </div>
          <div v-if="s.guaranteeDesc || s.refundPolicy" class="service-body">
            <p v-if="s.guaranteeDesc" class="service-guarantee">
              <span class="guarantee-dot" />
              {{ s.guaranteeDesc }}
            </p>
            <p v-if="s.refundPolicy" class="service-refund">
              <span class="refund-label">退款规则</span> {{ s.refundPolicy }}
            </p>
          </div>
        </article>
      </div>
    </template>

    <div v-else class="empty-state">
      <div>
        <h3>暂无服务</h3>
        <p>服务列表会自动同步后台。</p>
      </div>
    </div>
  </main>
</template>

<style scoped>
.loading-card {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: var(--mobile-muted);
}

.service-list {
  display: grid;
  gap: 12px;
}

.service-card {
  border: 1px solid rgba(228, 231, 236, .95);
  border-radius: 18px;
  background: rgba(255, 255, 255, .92);
  padding: 16px;
  box-shadow: 0 8px 20px rgba(16, 24, 40, .05);
}

.service-head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.service-emoji {
  font-size: 32px;
  flex: 0 0 auto;
}

.service-info {
  flex: 1;
  min-width: 0;
}

.service-name {
  display: block;
  color: var(--mobile-ink);
  font-size: 15px;
  font-weight: 850;
}

.service-category {
  display: block;
  margin-top: 2px;
  color: var(--mobile-faint);
  font-size: 12px;
}

.service-price {
  flex: 0 0 auto;
  text-align: right;
}

.price-num {
  display: block;
  color: var(--mobile-brand);
  font-size: 20px;
  font-weight: 950;
}

.price-unit {
  display: block;
  margin-top: 1px;
  color: var(--mobile-faint);
  font-size: 11px;
}

.service-body {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--mobile-line);
}

.service-guarantee {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 6px;
  color: var(--mobile-muted);
  font-size: 13px;
  line-height: 1.5;
}

.guarantee-dot {
  width: 6px;
  height: 6px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: var(--mobile-success);
}

.service-refund {
  margin: 0;
  color: var(--mobile-faint);
  font-size: 12px;
}

.refund-label {
  color: var(--mobile-muted);
  font-weight: 650;
}
</style>
