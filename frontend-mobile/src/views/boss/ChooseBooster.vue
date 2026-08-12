<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { closeToast, showLoadingToast, showToast } from 'vant'
import request from '../../api/request'
import { createOrder } from '../../api/orders'
import { useAuthGuard } from '../../composables/useAuthGuard'
import MobileTabbar from '../../components/MobileTabbar.vue'

const filter = ref('')
const boosters = ref<any[]>([])
const services = ref<any[]>([])
const showOrder = ref(false)
const showGift = ref(false)
const selectedBooster = ref<any>(null)
const selectedServiceId = ref<number>(0)
const giftAmount = ref('')
const giftName = ref('')
const giftMessage = ref('')
const loading = ref(false)
const { requireLogin } = useAuthGuard()

const filters = [
  { label: '全部', value: '' },
  { label: '娱乐陪', value: 'entertainment' },
  { label: '技术陪', value: 'tech' },
  { label: '顶尖陪', value: 'top' },
]

const levelLabel: Record<string, string> = {
  entertainment: '娱乐陪',
  tech: '技术陪',
  top: '顶尖陪',
}

const filtered = computed(() => {
  const online = boosters.value.filter((booster) => booster.boosterStatus !== 'offline')
  return filter.value ? online.filter((booster) => booster.boosterLevel === filter.value) : online
})

async function load() {
  loading.value = true
  try {
    const result: any = await request.get('/users/boosters', { params: { page: 1, size: 50 } })
    boosters.value = result.records || []
  } finally {
    loading.value = false
  }

  try {
    const result: any = await request.get('/services')
    services.value = Array.isArray(result) ? result : []
  } catch {
    services.value = []
  }
}

function pick(booster: any) {
  selectedBooster.value = booster
  selectedServiceId.value = services.value[0]?.id || 0
  showOrder.value = true
}

function gift(booster: any) {
  selectedBooster.value = booster
  giftName.value = ''
  giftAmount.value = ''
  giftMessage.value = ''
  showGift.value = true
}

async function submitOrder() {
  if (!selectedBooster.value || !selectedServiceId.value) return
  if (!await requireLogin('下单')) return
  showLoadingToast({ message: '正在下单', duration: 0 })
  try {
    await createOrder({ serviceId: selectedServiceId.value, boosterId: selectedBooster.value.id })
    closeToast()
    showToast({ message: '下单成功', icon: 'success' })
    showOrder.value = false
  } catch (error: any) {
    closeToast()
    showToast(error?.response?.data?.message || '下单失败')
  }
}

async function sendGift() {
  if (!selectedBooster.value) return
  if (!await requireLogin('送礼物')) return
  if (!giftName.value || !giftAmount.value) {
    showToast('请填写礼物名称和金额')
    return
  }

  try {
    await request.post('/gifts', null, {
      params: {
        boosterId: selectedBooster.value.id,
        giftName: giftName.value,
        amount: Number(giftAmount.value),
        message: giftMessage.value,
      },
    })
    showToast({ message: '礼物已送出', icon: 'success' })
    showGift.value = false
  } catch (error: any) {
    showToast(error?.response?.data?.message || '送礼失败')
  }
}

function avatarOf(booster: any) {
  return booster.avatar || 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'
}

onMounted(load)
</script>

<template>
  <main class="mobile-page">
    <section class="mobile-hero choose-hero">
      <div class="eyebrow">Online Squad</div>
      <h1 class="page-title">选择陪玩</h1>
      <p class="page-subtitle">筛选在线陪玩，指定下单或先送个礼物打招呼。</p>
      <div class="metric-grid hero-metrics">
        <div class="metric"><strong>{{ filtered.length }}</strong><span>在线</span></div>
        <div class="metric"><strong>{{ boosters.length }}</strong><span>总人数</span></div>
        <div class="metric"><strong>4.9</strong><span>均分</span></div>
      </div>
    </section>

    <div class="pill-row filter-row">
      <button v-for="item in filters" :key="item.value" type="button" class="pill" :class="{ active: filter === item.value }" @click="filter = item.value">
        {{ item.label }}
      </button>
    </div>

    <div v-if="loading" class="mobile-card loading-card">
      <van-loading color="#3157ff" />
      <span>正在匹配在线陪玩</span>
    </div>

    <section v-else-if="filtered.length" class="booster-list">
      <article v-for="booster in filtered" :key="booster.id" class="booster-card">
        <div class="avatar">
          <van-image round width="56" height="56" :src="avatarOf(booster)" />
          <span :class="['status-dot', { idle: booster.boosterStatus === 'idle' }]" />
        </div>
        <div class="booster-main">
          <div class="booster-top">
            <h2>{{ booster.nickname || '未命名陪玩' }}</h2>
            <span>{{ levelLabel[booster.boosterLevel] || '陪玩' }}</span>
          </div>
          <div class="booster-meta">
            <span>评分 {{ booster.rating || '5.0' }}</span>
            <span>{{ booster.orderCount || 0 }} 单</span>
            <span :class="{ idle: booster.boosterStatus === 'idle' }">{{ booster.boosterStatus === 'idle' ? '空闲' : '忙碌' }}</span>
          </div>
          <div class="booster-actions">
            <van-button round size="small" color="linear-gradient(135deg, #3157ff, #08b6d8)" :disabled="booster.boosterStatus !== 'idle'" @click="pick(booster)">
              指定下单
            </van-button>
            <van-button round size="small" plain color="#f79009" @click="gift(booster)">送礼物</van-button>
          </div>
        </div>
      </article>
    </section>

    <div v-else class="empty-state">
      <div>
        <h3>暂无在线陪玩</h3>
        <p>换个筛选条件，或稍后再试。</p>
      </div>
    </div>

    <van-action-sheet v-model:show="showOrder" title="指定陪玩下单" round>
      <div v-if="selectedBooster" class="sheet">
        <div class="sheet-profile">
          <van-image round width="42" height="42" :src="avatarOf(selectedBooster)" />
          <div>
            <strong>{{ selectedBooster.nickname }}</strong>
            <span>{{ levelLabel[selectedBooster.boosterLevel] || '陪玩' }}</span>
          </div>
        </div>

        <van-radio-group v-model="selectedServiceId">
          <button v-for="service in services" :key="service.id" type="button" class="service-row" @click="selectedServiceId = service.id">
            <span>
              <strong>{{ service.name }}</strong>
              <small>￥{{ service.basePrice }}</small>
            </span>
            <van-radio :name="service.id" checked-color="#3157ff" />
          </button>
        </van-radio-group>

        <van-button block round type="primary" size="large" :disabled="!selectedServiceId" color="linear-gradient(135deg, #3157ff, #08b6d8)" @click="submitOrder">
          确认下单
        </van-button>
      </div>
    </van-action-sheet>

    <van-action-sheet v-model:show="showGift" title="送礼物" round>
      <div v-if="selectedBooster" class="sheet">
        <div class="sheet-profile">
          <van-image round width="42" height="42" :src="avatarOf(selectedBooster)" />
          <div>
            <strong>送给 {{ selectedBooster.nickname }}</strong>
            <span>礼物会记录在你的账户中</span>
          </div>
        </div>
        <van-field v-model="giftName" label="礼物" placeholder="鲜花、跑车、火箭..." />
        <van-field v-model="giftAmount" label="金额" type="number" placeholder="请输入金额" />
        <van-field v-model="giftMessage" label="留言" placeholder="写一句鼓励的话" />
        <van-button block round type="primary" size="large" color="linear-gradient(135deg, #f79009, #f04438)" @click="sendGift">
          确认送出
        </van-button>
      </div>
    </van-action-sheet>

    <MobileTabbar role="boss" />
  </main>
</template>

<style scoped>
.choose-hero {
  background-image:
    linear-gradient(135deg, rgba(16,19,35,.74), rgba(8,182,216,.54)),
    url('https://images.unsplash.com/photo-1538481199705-c710c4e965fc?w=1000&h=900&fit=crop');
}

.hero-metrics,
.filter-row {
  margin-top: 18px;
}

.loading-card {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: var(--mobile-muted);
}

.booster-list {
  display: grid;
  gap: 12px;
}

.booster-card {
  display: flex;
  gap: 13px;
  border-radius: 20px;
  border: 1px solid rgba(228,231,236,.95);
  background: rgba(255,255,255,.9);
  padding: 14px;
  box-shadow: 0 10px 26px rgba(16,24,40,.06);
}

.avatar {
  position: relative;
  flex: 0 0 auto;
}

.status-dot {
  position: absolute;
  right: 2px;
  bottom: 2px;
  width: 13px;
  height: 13px;
  border-radius: 50%;
  border: 2px solid #fff;
  background: var(--mobile-warning);
}

.status-dot.idle {
  background: var(--mobile-success);
}

.booster-main {
  min-width: 0;
  flex: 1;
}

.booster-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.booster-top h2 {
  margin: 0;
  overflow: hidden;
  color: var(--mobile-ink);
  font-size: 16px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.booster-top span {
  flex: 0 0 auto;
  border-radius: 999px;
  padding: 4px 8px;
  background: #eef4ff;
  color: var(--mobile-brand);
  font-size: 11px;
  font-weight: 800;
}

.booster-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 6px;
  color: var(--mobile-muted);
  font-size: 12px;
}

.booster-meta .idle {
  color: var(--mobile-success);
  font-weight: 800;
}

.booster-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.sheet {
  padding: 0 16px 24px;
  display: grid;
  gap: 12px;
}

.sheet-profile {
  display: flex;
  align-items: center;
  gap: 12px;
  border-radius: 18px;
  background: linear-gradient(135deg, #eef4ff, #edfdfa);
  padding: 14px;
  border: 1px solid #dbe7ff;
}

.sheet-profile strong,
.sheet-profile span {
  display: block;
}

.sheet-profile strong {
  color: var(--mobile-ink);
  font-size: 15px;
}

.sheet-profile span {
  margin-top: 2px;
  color: var(--mobile-muted);
  font-size: 12px;
}

.service-row {
  width: 100%;
  border: 1px solid var(--mobile-line);
  border-radius: 15px;
  background: #fff;
  margin-bottom: 8px;
  padding: 13px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  text-align: left;
}

.service-row strong,
.service-row small {
  display: block;
}

.service-row strong {
  color: var(--mobile-ink);
  font-size: 14px;
}

.service-row small {
  margin-top: 3px;
  color: var(--mobile-brand);
  font-size: 13px;
  font-weight: 850;
}
</style>
