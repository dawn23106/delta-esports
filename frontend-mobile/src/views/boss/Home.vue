<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { closeToast, showLoadingToast, showToast } from 'vant'
import { createOrder } from '../../api/orders'
import { getServices } from '../../api/services'
import { useAuthGuard } from '../../composables/useAuthGuard'
import MobileTabbar from '../../components/MobileTabbar.vue'

const services = ref<any[]>([])
const loading = ref(false)
const showOrder = ref(false)
const selectedService = ref<any>(null)
const gameMap = ref('')
const modeCategory = ref('')
const gameMode = ref('')
const bossNote = ref('')
const showMapPicker = ref(false)
const showCategoryPicker = ref(false)
const showModePicker = ref(false)
const { requireLogin } = useAuthGuard()

const mapList = ['零号大坝', '巴克什', '航天基地', '潮汐监狱', '长弓溪谷']
const mapModes: Record<string, Record<string, string[]>> = {
  危险行动: {
    零号大坝: ['机密', '绝密'],
    长弓溪谷: ['机密', '绝密'],
    巴克什: ['机密', '绝密'],
    航天基地: ['机密', '绝密'],
    潮汐监狱: ['机密', '绝密'],
  },
  黑夜行动: { 零号大坝: ['永夜'] },

}

const categoryMeta: Record<string, { tone: string; desc: string }> = {
  老板护航: { tone: '护航撤离', desc: '适合稳妥下单，打手全程带节奏。' },
  陪玩专区: { tone: '轻松开黑', desc: '按小时计费，适合娱乐陪玩。' },
  监狱专区: { tone: '高强度局', desc: '热门地图与高收益玩法。' },
  趣味玩法: { tone: '指定玩法', desc: '指定掉落、体验局和特殊目标。' },
  特殊玩法: { tone: '定制规则', desc: '下单前可在备注说明细节。' },
}

const grouped = computed(() => {
  const buckets = new Map<string, any[]>()
  for (const service of services.value) {
    const category = service.category || '推荐服务'
    buckets.set(category, [...(buckets.get(category) || []), service])
  }
  return [...buckets.entries()].map(([name, items]) => ({ name, items }))
})

const availableCategories = computed(() => {
  if (!gameMap.value) return []
  return Object.keys(mapModes).filter((category) => mapModes[category][gameMap.value])
})

const availableModes = computed(() => {
  if (!gameMap.value || !modeCategory.value) return []
  return mapModes[modeCategory.value]?.[gameMap.value] || []
})

async function load() {
  loading.value = true
  try {
    const result: any = await getServices()
    services.value = Array.isArray(result) ? result : []
  } finally {
    loading.value = false
  }
}

function cleanName(name: string) {
  return name?.split('·').pop()?.trim() || name
}

function priceUnit(unit: string) {
  return unit === 'hour' ? '小时' : '局'
}

function pick(service: any) {
  selectedService.value = service
  showOrder.value = true
  gameMap.value = ''
  modeCategory.value = ''
  gameMode.value = ''
  bossNote.value = ''
}

function onMapChange(map: string) {
  gameMap.value = map
  modeCategory.value = ''
  gameMode.value = ''
  showMapPicker.value = false
}

async function submit() {
  if (!selectedService.value) return
  if (!await requireLogin('下单')) return
  const mapWithMode = [gameMap.value, modeCategory.value, gameMode.value].filter(Boolean).join(' · ')
  showLoadingToast({ message: '正在提交订单', duration: 0 })
  try {
    await createOrder({
      serviceId: selectedService.value.id,
      gameMap: mapWithMode || undefined,
      bossNote: bossNote.value || undefined,
    })
    closeToast()
    showToast({ message: '下单成功', icon: 'success' })
    showOrder.value = false
  } catch (error: any) {
    closeToast()
    showToast({ message: error?.response?.data?.message || '下单失败', icon: 'fail' })
  }
}

onMounted(load)
</script>

<template>
  <main class="mobile-page">
    <section
      class="mobile-hero hero"
      style="--hero-image: url('https://images.unsplash.com/photo-1542751371-adc38448a05e?w=1000&h=900&fit=crop')"
    >
      <div class="eyebrow">Delta Esports</div>
      <h1 class="page-title">沧月电竞</h1>
      <p class="page-subtitle">快速匹配靠谱陪玩，地图、模式、备注一次说清。</p>
      <div class="metric-grid hero-metrics">
        <div class="metric"><strong>{{ services.length }}</strong><span>服务</span></div>
        <div class="metric"><strong>5 min</strong><span>平均响应</span></div>
        <div class="metric"><strong>98%</strong><span>好评率</span></div>
      </div>
    </section>

    <div class="section-title">
      <span>热门服务</span>
      <span class="section-note">点击卡片下单</span>
    </div>

    <div v-if="loading" class="loading-card mobile-card">
      <van-loading color="#3157ff" />
      <span>正在加载服务</span>
    </div>

    <template v-else-if="grouped.length">
      <section v-for="group in grouped" :key="group.name" class="service-block">
        <div class="service-head">
          <div>
            <h2>{{ group.name }}</h2>
            <p>{{ categoryMeta[group.name]?.desc || '精选高频服务，按需选择即可。' }}</p>
          </div>
          <span>{{ categoryMeta[group.name]?.tone || '推荐' }}</span>
        </div>

        <div class="service-grid">
          <button v-for="service in group.items" :key="service.id" class="service-card" type="button" @click="pick(service)">
            <span class="service-name">{{ cleanName(service.name) }}</span>
            <span v-if="service.guaranteeDesc" class="service-desc">{{ service.guaranteeDesc }}</span>
            <span class="service-price">￥{{ service.basePrice }} <small>/ {{ priceUnit(service.priceUnit) }}</small></span>
          </button>
        </div>
      </section>

      <section class="rules mobile-card">
        <div class="rules-title">下单须知</div>
        <p>护航期间请听从陪玩指挥；连续失败可沟通更换陪玩；特殊掉落、打法和时间要求请写在备注里。</p>
      </section>
    </template>

    <div v-else class="empty-state">
      <div>
        <h3>暂无服务</h3>
        <p>稍后再来看看，服务列表会自动同步后台。</p>
      </div>
    </div>

    <van-action-sheet v-model:show="showOrder" title="确认下单" round>
      <div v-if="selectedService" class="sheet">
        <div class="sheet-summary">
          <div>
            <span class="sheet-kicker">已选服务</span>
            <h3>{{ selectedService.name }}</h3>
          </div>
          <strong>￥{{ selectedService.basePrice }}</strong>
        </div>

        <van-field v-model="gameMap" label="地图" placeholder="选择地图" readonly is-link @click="showMapPicker = true" />
        <van-field v-if="gameMap" v-model="modeCategory" label="玩法" placeholder="选择行动类型" readonly is-link @click="showCategoryPicker = true" />
        <van-field v-if="modeCategory && availableModes.length" v-model="gameMode" label="模式" placeholder="选择模式" readonly is-link @click="showModePicker = true" />
        <van-field v-model="bossNote" label="备注" placeholder="补充时间、打法或特殊要求" type="textarea" rows="3" maxlength="200" show-word-limit />

        <van-button block round type="primary" size="large" color="linear-gradient(135deg, #3157ff, #08b6d8)" @click="submit">
          确认下单
        </van-button>
      </div>
    </van-action-sheet>

    <van-action-sheet v-model:show="showMapPicker" title="选择地图" round>
      <div class="picker-list">
        <button v-for="map in mapList" :key="map" type="button" class="picker-item" @click="onMapChange(map)">
          <span>{{ map }}</span>
          <van-icon v-if="gameMap === map" name="success" />
        </button>
      </div>
    </van-action-sheet>

    <van-action-sheet v-model:show="showCategoryPicker" title="选择行动类型" round>
      <div class="picker-list">
        <button
          v-for="category in availableCategories"
          :key="category"
          type="button"
          class="picker-item"
          @click="modeCategory = category; gameMode = ''; showCategoryPicker = false"
        >
          <span>{{ category }}</span>
          <van-icon v-if="modeCategory === category" name="success" />
        </button>
      </div>
    </van-action-sheet>

    <van-action-sheet v-model:show="showModePicker" title="选择模式" round>
      <div class="picker-list">
        <button v-for="mode in availableModes" :key="mode" type="button" class="picker-item" @click="gameMode = mode; showModePicker = false">
          <span>{{ mode }}</span>
          <van-icon v-if="gameMode === mode" name="success" />
        </button>
      </div>
    </van-action-sheet>

    <MobileTabbar role="boss" />
  </main>
</template>

<style scoped>
.hero {
  min-height: 226px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}

.hero-metrics {
  margin-top: 18px;
}

.loading-card {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: var(--mobile-muted);
}

.service-block {
  margin-top: 12px;
}

.service-head {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
  margin: 0 2px 10px;
}

.service-head h2 {
  margin: 0;
  color: var(--mobile-ink);
  font-size: 16px;
}

.service-head p {
  margin: 3px 0 0;
  color: var(--mobile-muted);
  font-size: 12px;
  line-height: 1.4;
}

.service-head span {
  flex: 0 0 auto;
  border-radius: 999px;
  padding: 5px 9px;
  background: #eaf0ff;
  color: var(--mobile-brand);
  font-size: 11px;
  font-weight: 800;
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.service-card {
  min-height: 128px;
  border: 1px solid rgba(228,231,236,.95);
  border-radius: 18px;
  padding: 14px;
  text-align: left;
  background: #fff;
  box-shadow: 0 8px 20px rgba(16,24,40,.05);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.service-card:active {
  transform: scale(.98);
}

.service-name {
  color: var(--mobile-ink);
  font-size: 15px;
  font-weight: 850;
  line-height: 1.35;
}

.service-desc {
  margin-top: 8px;
  color: var(--mobile-muted);
  font-size: 11px;
  line-height: 1.45;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.service-price {
  margin-top: 12px;
  color: var(--mobile-brand);
  font-size: 18px;
  font-weight: 900;
}

.service-price small {
  color: var(--mobile-faint);
  font-size: 11px;
  font-weight: 650;
}

.rules {
  margin-top: 18px;
}

.rules-title {
  margin-bottom: 6px;
  color: var(--mobile-ink);
  font-size: 15px;
  font-weight: 850;
}

.rules p {
  margin: 0;
  color: var(--mobile-muted);
  font-size: 13px;
  line-height: 1.7;
}

.sheet {
  padding: 0 16px 24px;
  display: grid;
  gap: 12px;
}

.sheet-summary {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 15px;
  border-radius: 18px;
  background: linear-gradient(135deg, #eef4ff, #edfdfa);
  border: 1px solid #dbe7ff;
}

.sheet-kicker {
  color: var(--mobile-muted);
  font-size: 11px;
  font-weight: 800;
}

.sheet-summary h3 {
  margin: 4px 0 0;
  font-size: 15px;
  line-height: 1.4;
}

.sheet-summary strong {
  color: var(--mobile-brand);
  font-size: 24px;
}

.picker-list {
  padding: 0 16px 24px;
}

.picker-item {
  width: 100%;
  border: 0;
  border-bottom: 1px solid var(--mobile-line);
  background: transparent;
  color: var(--mobile-ink);
  min-height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 15px;
  font-weight: 750;
}

.picker-item .van-icon {
  color: var(--mobile-brand);
}
</style>
