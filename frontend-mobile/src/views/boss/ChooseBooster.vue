<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../../api/request'
import { createOrder } from '../../api/orders'
import { showToast, showLoadingToast, closeToast } from 'vant'
import { useAuthGuard } from '../../composables/useAuthGuard'

const active = ref(1)
const filter = ref('')
const boosters = ref<any[]>([])
const showOrder = ref(false)
const showGift = ref(false)
const selectedBooster = ref<any>(null)
const services = ref<any[]>([])
const selectedServiceId = ref<number>(0)
const giftAmount = ref('')
const giftName = ref('')
const giftMessage = ref('')

const filters = [
  { label: '全部', value: '' },
  { label: '娱乐陪', value: 'entertainment' },
  { label: '技术陪', value: 'tech' },
  { label: '顶尖陪', value: 'top' },
]
const levelLabel: Record<string, string> = { entertainment: '娱乐陪', tech: '技术陪', top: '顶尖陪' }
const filtered = ref<any[]>([])

function doFilter() {
  filtered.value = filter.value
    ? boosters.value.filter((b: any) => b.boosterLevel === filter.value)
    : boosters.value
}
async function load() {
  try {
    const r: any = await request.get('/users/boosters', { params: { page: 1, size: 50 } })
    boosters.value = (r.records || []).filter((b: any) => b.boosterStatus !== 'offline')
    doFilter()
  } catch { }
  try { const r: any = await request.get('/services'); services.value = r || [] } catch { }
}

const { requireLogin } = useAuthGuard()
async function pick(b: any) { selectedBooster.value = b; showOrder.value = true }
async function gift(b: any) { selectedBooster.value = b; showGift.value = true }
async function submitOrder() {
  if (!await requireLogin('下单')) return
  showLoadingToast('下单中...')
  try {
    await createOrder({ serviceId: selectedServiceId.value, boosterId: selectedBooster.value.id })
    closeToast(); showToast('下单成功！'); showOrder.value = false
  } catch (e: any) { closeToast(); showToast(e?.response?.data?.message || '失败') }
}
async function sendGift() {
  if (!await requireLogin('送礼物')) return
  if (!giftAmount.value || !giftName.value) { showToast('请填写礼物信息'); return }
  try {
    await request.post('/gifts', null, {
      params: { boosterId: selectedBooster.value.id, giftName: giftName.value, amount: Number(giftAmount.value), message: giftMessage.value }
    })
    showToast('已送出！🎁'); showGift.value = false
  } catch (e: any) { showToast(e?.response?.data?.message || '失败') }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="filter-bar">
      <span
        v-for="f in filters" :key="f.value"
        :class="['filter-chip', { active: filter === f.value }]"
        @click="filter = f.value; doFilter()"
      >{{ f.label }}</span>
    </div>

    <div class="list" v-if="filtered.length">
      <div v-for="b in filtered" :key="b.id" class="booster-card">
        <div class="b-left">
          <div class="avatar-wrap">
            <van-image round width="48" height="48" :src="b.avatar || 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'" />
            <span class="dot" :class="{ on: b.boosterStatus === 'idle' }"></span>
          </div>
          <div>
            <div class="b-name">
              {{ b.nickname }}
              <van-tag :type="b.boosterLevel === 'top' ? 'danger' : b.boosterLevel === 'tech' ? 'primary' : 'default'" round size="medium">
                {{ levelLabel[b.boosterLevel] || b.boosterLevel }}
              </van-tag>
            </div>
            <div class="b-meta">
              <span>⭐ {{ b.rating || '5.0' }}</span>
              <span>{{ b.orderCount || 0 }}单</span>
              <span :class="b.boosterStatus === 'idle' ? 'text-green' : 'text-orange'">● {{ b.boosterStatus === 'idle' ? '空闲' : '忙碌' }}</span>
            </div>
          </div>
        </div>
        <div class="b-actions">
          <van-button size="small" round type="primary" class="btn" :disabled="b.boosterStatus !== 'idle'" @click="pick(b)">下单</van-button>
          <van-button size="small" round plain hairline class="btn-gift" @click="gift(b)">送礼物</van-button>
        </div>
      </div>
    </div>

    <div v-else class="empty">
      <span class="text-3xl mb-2">🧑‍🤝‍🧑</span>
      <div class="text-gray-400 text-sm">暂无陪陪在线</div>
    </div>

    <!-- 下单弹窗 -->
    <van-action-sheet v-model:show="showOrder" title="选择服务" :close-on-click-overlay="true" :style="{ borderRadius:'20px 20px 0 0' }">
      <div class="sheet" v-if="selectedBooster">
        <div class="sheet-hero">
          <van-image round width="36" height="36" src="https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg" />
          <span class="sheet-hero-text">{{ selectedBooster.nickname }} · 指定陪陪</span>
        </div>
        <van-radio-group v-model="selectedServiceId">
          <div v-for="s in services" :key="s.id" class="svc-row" @click="selectedServiceId = s.id">
            <div>
              <div class="svc-name">{{ s.name }}</div>
              <div class="svc-price">¥{{ s.basePrice }}</div>
            </div>
            <van-radio :name="s.id" checked-color="#6366f1" />
          </div>
        </van-radio-group>
        <van-button round block type="primary" class="sheet-btn" :disabled="!selectedServiceId" @click="submitOrder" color="linear-gradient(135deg, #6366f1, #8b5cf6)">确认下单</van-button>
      </div>
    </van-action-sheet>

    <!-- 送礼弹窗 -->
    <van-action-sheet v-model:show="showGift" title="送礼物" :close-on-click-overlay="true" :style="{ borderRadius:'20px 20px 0 0' }">
      <div class="sheet" v-if="selectedBooster">
        <div class="sheet-hero"><span>送给 <b>{{ selectedBooster.nickname }}</b></span></div>
        <van-field v-model="giftName" label="礼物" placeholder="鲜花/跑车/火箭..." :border="true" />
        <van-field v-model="giftAmount" label="金额" type="number" placeholder="¥" :border="true" />
        <van-field v-model="giftMessage" label="留言" placeholder="说点什么..." :border="true" />
        <van-button round block type="warning" class="sheet-btn" @click="sendGift" color="linear-gradient(135deg, #f59e0b, #f97316)">确认送出</van-button>
      </div>
    </van-action-sheet>

    <van-tabbar v-model="active" route :border="false" active-color="#6366f1" inactive-color="#94a3b8" safe-area-inset-bottom class="tabbar">
      <van-tabbar-item icon="home-o" to="/boss/home">首页</van-tabbar-item>
      <van-tabbar-item icon="friends-o" to="/boss/choose">选陪陪</van-tabbar-item>
      <van-tabbar-item icon="chat-o" to="/boss/messages">消息</van-tabbar-item>
      <van-tabbar-item icon="user-o" to="/boss/profile">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f8fafc 0%, #fff 50%, #f8fafc 100%);
  padding-bottom: 60px;
}
.filter-bar {
  display: flex;
  gap: 8px;
  padding: 16px;
  overflow-x: auto;
}
.filter-bar::-webkit-scrollbar { display: none; }
.filter-chip {
  padding: 8px 18px;
  border-radius: 14px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  background: #fff;
  color: #64748b;
  border: 1px solid #e2e8f0;
  transition: all 0.2s;
  white-space: nowrap;
}
.filter-chip.active {
  background: #6366f1;
  color: #fff;
  border-color: #6366f1;
  box-shadow: 0 4px 12px rgba(99,102,241,0.25);
}
.list {
  padding: 0 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.booster-card {
  background: #fff;
  border-radius: 18px;
  padding: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 3px rgba(0,0,0,0.03);
  border: 1px solid #f1f5f9;
}
.b-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}
.avatar-wrap {
  position: relative;
  flex-shrink: 0;
}
.dot {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid #fff;
}
.dot.on { background: #10b981; }
.dot:not(.on) { background: #f59e0b; }
.b-name {
  font-weight: 600;
  color: #1e293b;
  font-size: 15px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.b-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #94a3b8;
  margin-top: 4px;
}
.text-green { color: #10b981 !important; }
.text-orange { color: #f59e0b !important; }
.b-actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex-shrink: 0;
}
.btn {
  height: 30px !important;
  font-size: 12px !important;
  padding: 0 14px !important;
}
.btn-gift {
  height: 30px !important;
  font-size: 12px !important;
  padding: 0 14px !important;
  border-color: #fde68a !important;
  color: #f59e0b !important;
}
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 0;
}
.sheet {
  padding: 0 20px 30px;
}
.sheet-hero {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  margin-bottom: 12px;
  background: linear-gradient(135deg, #eef2ff, #faf5ff);
  border-radius: 14px;
  border: 1px solid #e0e7ff;
}
.sheet-hero-text {
  font-size: 14px;
  color: #1e293b;
}
.svc-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-radius: 12px;
  margin-bottom: 6px;
  cursor: pointer;
  background: #f8fafc;
}
.svc-name {
  font-size: 14px;
  font-weight: 500;
  color: #334155;
}
.svc-price {
  font-size: 12px;
  color: #94a3b8;
}
.sheet-btn {
  height: 48px !important;
  font-size: 15px !important;
  font-weight: 600 !important;
  border-radius: 16px !important;
  margin-top: 16px !important;
}
.tabbar {
  background: rgba(255,255,255,0.9) !important;
  backdrop-filter: blur(20px) !important;
  border-top: 1px solid #f1f5f9 !important;
}
</style>
