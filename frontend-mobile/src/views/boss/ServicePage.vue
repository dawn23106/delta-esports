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
  '陪玩专区': '🎮',
  '监狱专区': '🔒',
  '趣味玩法': '🎯',
}

onMounted(load)
</script>

<template>
  <div class="page">
    <van-nav-bar title="全部服务" left-arrow @click-left="$router.back()" fixed placeholder />

    <!-- 加载中 -->
    <div v-if="loading" class="flex justify-center py-20">
      <van-loading size="24" color="#6366f1" />
    </div>

    <!-- 服务列表 -->
    <div v-else class="content">
      <div v-for="s in services" :key="s.id" class="service-card">
        <div class="service-header">
          <span class="service-emoji">{{ categoryIcons[s.category] || '🎮' }}</span>
          <div>
            <div class="service-name">{{ s.name }}</div>
            <div class="service-category">{{ s.category }}</div>
          </div>
          <div class="service-price">
            <span class="price-num">¥{{ s.basePrice }}</span>
            <span class="price-unit">/{{ s.priceUnit === 'hour' ? '小时' : '局' }}</span>
          </div>
        </div>
        <div class="service-body">
          <div class="service-guarantee" v-if="s.guaranteeDesc">
            <span class="guarantee-dot"></span>
            {{ s.guaranteeDesc }}
          </div>
          <div class="service-refund" v-if="s.refundPolicy">
            <span class="refund-label">退款规则：</span>{{ s.refundPolicy }}
          </div>
        </div>
      </div>

      <div v-if="services.length === 0 && !loading" class="empty">
        <span class="text-4xl mb-3">📋</span>
        <div class="text-gray-400">暂无服务项目</div>
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
  padding: 12px 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.service-card {
  background: #fff;
  border-radius: 18px;
  padding: 18px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.03);
  border: 1px solid #f1f5f9;
}
.service-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.service-emoji {
  font-size: 32px;
}
.service-name {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
}
.service-category {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 2px;
}
.service-price {
  margin-left: auto;
  text-align: right;
}
.price-num {
  font-size: 20px;
  font-weight: 800;
  color: #6366f1;
}
.price-unit {
  font-size: 12px;
  color: #94a3b8;
}
.service-body {
  border-top: 1px solid #f1f5f9;
  padding-top: 12px;
}
.service-guarantee {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #64748b;
  margin-bottom: 6px;
}
.guarantee-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #10b981;
  flex-shrink: 0;
}
.service-refund {
  font-size: 12px;
  color: #94a3b8;
}
.refund-label {
  color: #64748b;
  font-weight: 500;
}
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 0;
}
</style>
