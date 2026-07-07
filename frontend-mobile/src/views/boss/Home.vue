<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getServices } from '../../api/services'
import { createOrder } from '../../api/orders'
import { showToast, showLoadingToast, closeToast } from 'vant'
import { useAuthGuard } from '../../composables/useAuthGuard'
import Confetti from '../../components/Confetti.vue'
import AuroraBackground from '../../components/AuroraBackground.vue'
import ImagesSlider from '../../components/ImagesSlider.vue'
import GradientButton from '../../components/ui/GradientButton.vue'

const services = ref<any[]>([])
const active = ref(0)
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
const confettiRef = ref<InstanceType<typeof Confetti> | null>(null)

const sliderImages = [
  'https://images.unsplash.com/photo-1542751371-adc38448a05e?w=1200&h=600&fit=crop',
  'https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200&h=600&fit=crop',
  'https://images.unsplash.com/photo-1493711662062-fa541adb3fc8?w=1200&h=600&fit=crop',
  'https://images.unsplash.com/photo-1538481199705-c710c4e965fc?w=1200&h=600&fit=crop',
]

const mapList = ['零号大坝', '巴克什', '航天基地', '潮汐监狱', '长弓溪谷']

const mapModes: Record<string, Record<string, string[]>> = {
  '危险行动': { '零号大坝': ['机密','绝密'], '长弓溪谷': ['机密','绝密'], '巴克什': ['机密','绝密'], '航天基地': ['机密','绝密'], '潮汐监狱': ['机密','绝密'] },
  '黑夜行动': { '零号大坝': ['永夜'] },
  '赤枭寻猎': { '零号大坝': ['机密'], '巴克什': ['机密','绝密'], '航天基地': ['机密','绝密'] },
}

const availableCategories = computed(() => {
  if (!gameMap.value) return [] as string[]
  return Object.keys(mapModes).filter(c => mapModes[c][gameMap.value])
})

const availableModes = computed(() => {
  if (!gameMap.value || !modeCategory.value) return [] as string[]
  return mapModes[modeCategory.value]?.[gameMap.value] || []
})

const catIcons: Record<string, string> = {
  '老板护航': '🛡️', '陪玩专区': '🎯', '监狱专区': '🔥', '趣味玩法': '🎪', '特殊玩法': '⚡',
}

const subtitles: Record<string, string> = {
  '老板护航': '带老板撤离 · 保底收益',
  '陪玩专区': '按小时计费',
  '监狱专区': '炸单加保底',
  '趣味玩法': '指定掉落 · 不出退全款',
  '特殊玩法': '特色玩法 · 规则见详情',
}

// 分组：category → subcategory → items
const grouped = computed(() => {
  const cats: Record<string, { icon: string; subcats: Record<string, any[]> }> = {}
  for (const s of services.value) {
    const cat = s.category
    if (!cats[cat]) cats[cat] = { icon: catIcons[cat] || '📋', subcats: {} }
    const parts = s.name.split('·')
    const sub = parts.length > 1 ? parts[0].trim() : '其他'
    if (!cats[cat].subcats[sub]) cats[cat].subcats[sub] = []
    cats[cat].subcats[sub].push(s)
  }
  return Object.entries(cats).map(([name, data]) => ({
    name, icon: data.icon,
    groups: Object.entries(data.subcats).map(([sub, items]) => ({ sub, items })),
  }))
})

async function load() {
  loading.value = true
  try { const r: any = await getServices(); services.value = Array.isArray(r) ? r : [] } catch { }
  loading.value = false
}

function pick(s: any) {
  selectedService.value = s
  showOrder.value = true
  gameMap.value = ''
  modeCategory.value = ''
  gameMode.value = ''
  bossNote.value = ''
}

function onMapChange(m: string) {
  gameMap.value = m
  modeCategory.value = ''
  gameMode.value = ''
  showMapPicker.value = false
}

async function submit() {
  if (!await requireLogin('下单')) return
  const mapWithMode = [gameMap.value, modeCategory.value, gameMode.value].filter(Boolean).join(' · ')
  showLoadingToast({ message: '下单中...', duration: 0 })
  try {
    await createOrder({ serviceId: selectedService.value.id, gameMap: mapWithMode || undefined, bossNote: bossNote.value || undefined })
    closeToast(); showToast({ message: '下单成功！', icon: 'success' }); showOrder.value = false
    confettiRef.value?.fire()
  } catch (e: any) { closeToast(); showToast({ message: e?.response?.data?.message || '失败', icon: 'fail' }) }
}

// 统一大卡宽度
function cardW(_i: number) { return '210px' }

function placeholderImg(id: number) {
  const imgs = [
    'https://images.unsplash.com/photo-1612287230202-1ff1d85d1bdf?w=400&h=400&fit=crop',
    'https://images.unsplash.com/photo-1612404730960-5c71577fca11?w=400&h=400&fit=crop',
    'https://images.unsplash.com/photo-1592155931584-901ac15763e3?w=400&h=400&fit=crop',
    'https://images.unsplash.com/photo-1552820728-8b83bb6b2cf3?w=400&h=400&fit=crop',
  ]
  return imgs[id % imgs.length]
}

// 横向滚动时阻止纵向
function onWheel(e: WheelEvent) {
  if (Math.abs(e.deltaY) > Math.abs(e.deltaX)) { e.preventDefault(); window.scrollBy({ top: e.deltaY }) }
}

onMounted(load)
</script>

<template>
  <AuroraBackground>
    <Confetti ref="confettiRef" />

    <!-- 品牌区 -->
    <section class="hero">
      <ImagesSlider :images="sliderImages" autoplay :interval="4000">
        <div class="hero-overlay">
          <h1 class="hero-title">沧月电竞</h1>
          <div class="hero-stats">
            <span>🎯 {{ services.length }}个服务</span>
            <span>⚡ 3位陪陪在线</span>
            <span>⭐ 98.6%好评</span>
          </div>
        </div>
      </ImagesSlider>
    </section>

    <div v-if="loading" class="loading-wrap"><van-loading size="28" color="#6366f1" /></div>

    <!-- 服务列表 -->
    <template v-else-if="services.length">
      <section v-for="cat in grouped" :key="cat.name" class="cat-section">
        <!-- 分类头 -->
        <div class="cat-header">
          <span class="cat-icon">{{ cat.icon }}</span>
          <div>
            <h2 class="cat-title">{{ cat.name }}</h2>
            <p class="cat-subtitle">{{ subtitles[cat.name] || '' }}</p>
          </div>
        </div>

        <!-- 子分类组 -->
        <div v-for="grp in cat.groups" :key="grp.sub" class="sub-group">
          <div class="sub-label">{{ grp.sub }}</div>
          <div class="h-scroll" @wheel="onWheel">
            <div class="h-scroll-row">
              <div
                v-for="(s, j) in grp.items" :key="s.id"
                class="svc-card"
                :style="{ width: cardW(j) }"
                @click="pick(s)"
              >
                <div class="svc-img-wrap">
                  <img :src="placeholderImg(s.id)" class="svc-img" />
                  <span class="svc-tag">{{ s.priceUnit === 'hour' ? '时' : '局' }}</span>
                </div>
                <div class="svc-body">
                  <h3 class="svc-name">{{ s.name.split('·').slice(1).join('·').trim() || s.name }}</h3>
                  <div class="svc-footer">¥{{ s.basePrice }}</div>
                  <div class="svc-desc" v-if="s.guaranteeDesc">{{ s.guaranteeDesc }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 重要规则 -->
      <section class="rules-section">
        <div class="rules-header">
          <span>📋</span>
          <h3>重要规则</h3>
        </div>
        <div class="rules-list">
          <div class="rule-item">护航期间听打手指挥，否则炸单不补偿</div>
          <div class="rule-item">老板撤离失败即炸单，机密加保底20万，绝密加50万</div>
          <div class="rule-item">连续失败3把可免费换陪陪</div>
          <div class="rule-item">打手辱骂或私藏物品，减20%费用并换人</div>
          <div class="rule-item">老板故意挂机或乱丢物品可直接结单</div>
          <div class="rule-item">所有护航默认双护</div>
          <div class="rule-item rule-muted">最终解释权归沧月电竞</div>
        </div>
      </section>
    </template>

    <div v-else class="empty">暂无服务</div>
    <div style="height:80px" />

    <!-- ==================== 下单弹窗 ==================== -->
    <van-action-sheet v-model:show="showOrder" title="确认下单" :close-on-click-overlay="true" :style="{ borderRadius:'20px 20px 0 0' }">
      <div class="sheet" v-if="selectedService">
        <div class="sheet-card">
          <div class="sheet-name">{{ selectedService.name }}</div>
          <div class="sheet-detail">
            <span class="sheet-price">¥{{ selectedService.basePrice }}</span>
            <span>/{{ selectedService.priceUnit === 'hour' ? '小时' : '局' }}</span>
          </div>
          <div class="sheet-guarantee" v-if="selectedService.guaranteeDesc">{{ selectedService.guaranteeDesc }}</div>
        </div>

        <!-- 地图 -->
        <van-field v-model="gameMap" label="📍 地图" placeholder="请选择地图" :border="true" input-align="right" readonly is-link @click="showMapPicker = true" />
        <!-- 模式分类（选了地图后出现） -->
        <van-field v-if="gameMap" v-model="modeCategory" label="🏷 分类" placeholder="请选择行动类型" :border="true" input-align="right" readonly is-link @click="showCategoryPicker = true" />
        <!-- 模式（选了分类后出现） -->
        <van-field v-if="modeCategory && availableModes.length" v-model="gameMode" label="🎯 模式" placeholder="请选择模式" :border="true" input-align="right" readonly is-link @click="showModePicker = true" />
        <!-- 备注 -->
        <van-field v-model="bossNote" label="📝 备注" placeholder="有什么要求（选填）" type="textarea" rows="2" maxlength="200" show-word-limit :border="true" />

        <div class="sheet-tip">⏱️ 预计 5 分钟内匹配空闲陪陪</div>
        <GradientButton @click="submit" color-from="#6366f1" color-to="#8b5cf6">确认下单</GradientButton>
      </div>
    </van-action-sheet>

    <!-- 地图选择器 -->
    <van-action-sheet v-model:show="showMapPicker" title="选择地图" :close-on-click-overlay="true" :style="{ borderRadius:'20px 20px 0 0' }">
      <div class="picker-list">
        <div v-for="m in mapList" :key="m" class="picker-item" :class="{ selected: gameMap === m }" @click="onMapChange(m)">
          <span>{{ m }}</span>
          <span v-if="gameMap === m" class="picker-check">✓</span>
        </div>
      </div>
    </van-action-sheet>

    <!-- 分类选择器 -->
    <van-action-sheet v-model:show="showCategoryPicker" title="选择行动类型" :close-on-click-overlay="true" :style="{ borderRadius:'20px 20px 0 0' }">
      <div class="picker-list">
        <div v-for="c in availableCategories" :key="c" class="picker-item" :class="{ selected: modeCategory === c }" @click="modeCategory = c; gameMode = ''; showCategoryPicker = false">
          <span>{{ c }}</span>
          <span v-if="modeCategory === c" class="picker-check">✓</span>
        </div>
      </div>
    </van-action-sheet>

    <!-- 模式选择器 -->
    <van-action-sheet v-model:show="showModePicker" title="选择模式" :close-on-click-overlay="true" :style="{ borderRadius:'20px 20px 0 0' }">
      <div class="picker-list">
        <div v-for="m in availableModes" :key="m" class="picker-item" :class="{ selected: gameMode === m }" @click="gameMode = m; showModePicker = false">
          <span>{{ m }}</span>
          <span v-if="gameMode === m" class="picker-check">✓</span>
        </div>
      </div>
    </van-action-sheet>

    <van-tabbar v-model="active" route :border="false" active-color="#6366f1" inactive-color="#94a3b8" safe-area-inset-bottom class="tabbar">
      <van-tabbar-item icon="home-o" to="/boss/home">首页</van-tabbar-item>
      <van-tabbar-item icon="friends-o" to="/boss/choose">选陪陪</van-tabbar-item>
      <van-tabbar-item icon="chat-o" to="/boss/messages">消息</van-tabbar-item>
      <van-tabbar-item icon="user-o" to="/boss/profile">我的</van-tabbar-item>
    </van-tabbar>
  </AuroraBackground>
</template>

<style scoped>
/* ========== 品牌区 ========== */
.hero { margin: 12px 0 8px; padding: 0 12px; }
.hero :deep(.slider-wrap) { border-radius: 12px; max-height: 180px; }
.hero-overlay { padding: 16px 20px; color: #fff; }
.hero-title { font-size: 22px; font-weight: 900; margin: 0 0 6px; letter-spacing: 2px; text-shadow: 0 2px 8px rgba(0,0,0,.5); }
.hero-stats { display: flex; gap: 14px; font-size: 11px; opacity: .8; }

/* ========== 分类区块 ========== */
.cat-section { padding: 8px 0 4px; }
.cat-header { display: flex; align-items: center; gap: 10px; padding: 12px 16px 4px; }
.cat-icon { font-size: 26px; }
.cat-title { font-size: 17px; font-weight: 700; color: #1e293b; margin: 0; }
.cat-subtitle { font-size: 12px; color: #94a3b8; margin: 2px 0 0; }

/* ========== 子分类 ========== */
.sub-group { padding: 2px 0; }
.sub-label {
  font-size: 12px; font-weight: 600; color: #64748b;
  padding: 8px 16px 4px;
}

/* ========== 横向滚动 ========== */
.h-scroll { overflow-x: auto; padding: 8px 12px 0; }
.h-scroll::-webkit-scrollbar { display: none; }
.h-scroll-row { display: flex; flex-wrap: wrap; gap: 6px; width: max-content; max-height: 440px; }

/* ========== 服务卡片 ========== */
.svc-card {
  background: rgba(255,255,255,.85); backdrop-filter: blur(8px);
  border-radius: 18px; overflow: hidden; flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(0,0,0,.08); cursor: pointer;
  display: flex; flex-direction: column; height: 200px;
  transition: box-shadow .25s, transform .25s;
  border: none; outline: none;
}
.svc-card:hover { box-shadow: 0 6px 20px rgba(0,0,0,.12); transform: translateY(-2px); }
.svc-card:hover .svc-img { transform: scale(1.05); }
.svc-card > * { min-height: 0; flex-shrink: 1; }

.svc-img-wrap { flex: 1; overflow: hidden; position: relative; min-height: 0; }
.svc-img { width: 100%; height: 100%; object-fit: cover; display: block; transition: transform .3s ease; }
.svc-tag { position: absolute; bottom: 6px; right: 6px; padding: 2px 8px; border-radius: 6px; background: rgba(0,0,0,.45); color: #fff; font-size: 10px; }

.svc-body { padding: 6px 8px; flex-shrink: 0; text-align: center; }
.svc-name { font-size: 12px; font-weight: 600; color: #222; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin: 0; }
.svc-footer { font-size: 13px; color: #6366f1; font-weight: 700; margin-top: 2px; }
.svc-desc { font-size: 10px; color: #94a3b8; margin-top: 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* ========== 重要规则 ========== */
.rules-section { margin: 16px 14px; background: rgba(255,255,255,.7); border-radius: 14px; padding: 14px 16px; border: 1px solid #f1f5f9; }
.rules-header { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.rules-header h3 { font-size: 14px; font-weight: 700; color: #475569; margin: 0; }
.rules-list { display: flex; flex-direction: column; gap: 6px; }
.rule-item { font-size: 12px; color: #64748b; padding-left: 12px; position: relative; line-height: 1.5; }
.rule-item::before { content: '•'; position: absolute; left: 0; color: #cbd5e1; }
.rule-muted { color: #94a3b8; font-style: italic; }

/* ========== 下单弹窗 ========== */
.sheet { padding: 0 16px 28px; }
.sheet-card { background: linear-gradient(135deg, #eef2ff, #faf5ff); border-radius: 14px; padding: 16px; margin-bottom: 14px; border: 1px solid #e0e7ff; }
.sheet-name { font-size: 16px; font-weight: 700; color: #1e293b; }
.sheet-detail { margin-top: 6px; display: flex; align-items: baseline; gap: 4px; }
.sheet-price { font-size: 28px; font-weight: 800; color: #6366f1; }
.sheet-detail > span:last-child { font-size: 13px; color: #64748b; }
.sheet-guarantee { margin-top: 8px; font-size: 12px; color: #92400e; background: #fffbeb; padding: 6px 10px; border-radius: 8px; line-height: 1.5; }
.sheet-tip { margin: 12px 0 16px; padding: 10px; background: #fffbeb; border-radius: 10px; font-size: 12px; color: #92400e; }

/* ========== 选择器 ========== */
.picker-list { padding: 8px 16px 28px; }
.picker-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 0; font-size: 15px; color: #334155;
  border-bottom: 1px solid #f1f5f9; cursor: pointer;
}
.picker-item:last-child { border-bottom: none; }
.picker-item:active { background: #f8fafc; margin: 0 -16px; padding-left: 16px; padding-right: 16px; }
.picker-item.selected { color: #6366f1; font-weight: 600; }
.picker-check { color: #6366f1; font-weight: 700; }

/* ========== 通用 ========== */
.loading-wrap { display: flex; justify-content: center; padding: 80px 0; }
.empty { text-align: center; padding: 60px 0; color: #94a3b8; font-size: 13px; }
.tabbar { background: rgba(255,255,255,.9) !important; backdrop-filter: blur(20px) !important; border-top: 1px solid #f1f5f9 !important; }
</style>
