<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getOrderDetail, cancelOrder } from '../../api/orders'
import { showToast, showConfirmDialog } from 'vant'
import { useAuthGuard } from '../../composables/useAuthGuard'
import { Motion } from 'motion-v'
import Confetti from '../../components/Confetti.vue'

const route = useRoute()
const order = ref<any>(null)
const id = Number(route.params.id)
const loadFailed = ref(false)
const { requireLogin } = useAuthGuard()
const confettiRef = ref<InstanceType<typeof Confetti> | null>(null)

function statusLabel(s: string) {
  return { pending:'待接单',assigned:'已接单',in_progress:'进行中',completed:'待审核',done:'已确认',settled:'已结算',cancelled:'已取消',disputed:'争议中' }[s] || s
}
function statusColor(s: string) {
  return { pending:'#f59e0b',assigned:'#3b82f6',in_progress:'#6366f1',completed:'#8b5cf6',done:'#10b981',settled:'#6b7280',cancelled:'#ef4444',disputed:'#f97316' }[s] || '#94a3b8'
}

const steps = ref<{ label:string; time:string; desc?:string; done:boolean }[]>([])

function buildSteps(o: any) {
  const s: any[] = []
  if (o.createdAt) s.push({ label:'下单成功', time: o.createdAt, desc: o.bossNote || '', done: true })
  if (o.assignedAt) s.push({ label:'陪陪接单', time: o.assignedAt, done: true })
  if (o.startedAt) s.push({ label:'开始代练', time: o.startedAt, done: true })
  if (o.completedAt) s.push({ label:'申请结单', time: o.completedAt, desc: o.isQualified ? '自评：达标' : '自评：未达标', done: true })
  if (o.doneAt) s.push({ label:'确认达标', time: o.doneAt, done: true })
  if (o.settledAt) s.push({ label:'已结算', time: o.settledAt, done: true })
  if (o.status === 'cancelled') s.push({ label:'已取消', time: new Date().toISOString(), done: true })
  return s
}

async function load() {
  loadFailed.value = false
  try {
    const res: any = await getOrderDetail(id)
    order.value = res
    steps.value = buildSteps(res)
    if (res.status === 'settled' || res.status === 'done') {
      setTimeout(() => confettiRef.value?.fire(), 500)
    }
  } catch { loadFailed.value = true }
}

async function handleCancel() {
  if (!await requireLogin('取消订单')) return
  try {
    await showConfirmDialog({ title:'确认取消', message:'确定要取消这个订单吗？' })
    await cancelOrder(id)
    showToast('已取消')
    load()
  } catch { }
}

onMounted(load)
</script>

<template>
  <!-- 订单详情 -->
  <div class="page" v-if="order">
    <Confetti ref="confettiRef" />
    <van-nav-bar title="订单详情" left-arrow @click-left="$router.back()" fixed placeholder />

    <!-- 状态头部 -->
    <Motion :initial="{ opacity: 0, y: -20 }" :animate="{ opacity: 1, y: 0 }" :transition="{ duration: .5 }">
    <div class="status-head" :style="{ background: statusColor(order.status) }">
      <div class="status-blob" />
      <div class="status-label">{{ statusLabel(order.status) }}</div>
      <div class="status-order">订单 #{{ order.id }}</div>
    </div>
    </Motion>

    <!-- 金额卡片 -->
    <div class="info-card">
      <div class="info-row">
        <span>订单金额</span>
        <span class="info-price">¥{{ order.amount }}</span>
      </div>
      <div class="info-row" v-if="order.serviceId">
        <span>服务编号</span>
        <span>#{{ order.serviceId }}</span>
      </div>
      <div class="info-row" v-if="order.gameMap">
        <span>游戏地图</span>
        <span>{{ order.gameMap }}</span>
      </div>
      <div class="info-row" v-if="order.bossNote">
        <span>老板备注</span>
        <span>{{ order.bossNote }}</span>
      </div>
    </div>

    <!-- 时间线 -->
    <div class="timeline-card">
      <div class="tl-title">订单进度</div>
      <div class="tl-list">
        <Motion
          v-for="(s, i) in steps" :key="i"
          :initial="{ opacity: 0, x: -12 }" :animate="{ opacity: 1, x: 0 }"
          :transition="{ duration: .35, delay: .1 * i }"
          class="tl-item" :class="{ done: s.done }"
        >
          <div class="tl-dot" :class="{ active: i === steps.length - 1 && s.done }" />
          <div class="tl-line" v-if="i < steps.length - 1" />
          <div class="tl-content">
            <div class="tl-label">{{ s.label }}</div>
            <div class="tl-time">{{ s.time?.replace('T',' ').substring(5,16) }}</div>
            <div class="tl-desc" v-if="s.desc">{{ s.desc }}</div>
          </div>
        </Motion>
      </div>
    </div>

    <!-- 结果截图 -->
    <div class="info-card" v-if="order.resultImages">
      <div class="card-subtitle">结果截图</div>
      <div class="img-row">
        <img v-for="(img, i) in JSON.parse(order.resultImages || '[]')" :key="i" :src="img" class="result-img" />
      </div>
    </div>

    <!-- 结单备注 -->
    <div class="info-card" v-if="order.resultNote">
      <div class="card-subtitle">结单备注</div>
      <p class="card-text">{{ order.resultNote }}</p>
    </div>

    <!-- 取消 -->
    <div class="action-bar" v-if="!['settled','cancelled'].includes(order.status)">
      <van-button round block type="danger" plain @click="handleCancel">取消订单</van-button>
    </div>
  </div>

  <!-- 空状态 -->
  <div v-else class="page empty-page">
    <van-nav-bar title="订单详情" left-arrow @click-left="$router.back()" fixed placeholder />
    <div class="empty-content">
      <span class="empty-icon">🔒</span>
      <p>无法查看该订单</p>
      <van-button round type="primary" @click="$router.push('/login')" color="linear-gradient(135deg, #6366f1, #8b5cf6)">去登录</van-button>
    </div>
  </div>
</template>

<style scoped>
.page { min-height: 100vh; background: #f5f6fa; padding-bottom: 40px; }

/* Status Head */
.status-head { padding: 28px 20px; position: relative; overflow: hidden; color: #fff; }
.status-blob { position: absolute; top: -30px; right: -20px; width: 120px; height: 120px; border-radius: 50%; background: rgba(255,255,255,.08); }
.status-label { font-size: 20px; font-weight: 800; position: relative; z-index: 1; }
.status-order { font-size: 13px; opacity: .7; margin-top: 4px; position: relative; z-index: 1; }

/* Info Card */
.info-card { background: #fff; border-radius: 14px; padding: 16px; margin: 10px 12px; box-shadow: 0 1px 3px rgba(0,0,0,.03); }
.info-row { display: flex; justify-content: space-between; align-items: center; padding: 8px 0; font-size: 14px; color: #64748b; }
.info-row + .info-row { border-top: 1px solid #f5f5f5; }
.info-row span:last-child { color: #1e293b; font-weight: 500; }
.info-price { font-size: 22px !important; font-weight: 800 !important; color: #6366f1 !important; }

/* Timeline */
.timeline-card { background: #fff; border-radius: 14px; padding: 16px; margin: 10px 12px; box-shadow: 0 1px 3px rgba(0,0,0,.03); }
.tl-title { font-size: 15px; font-weight: 700; color: #1e293b; margin-bottom: 16px; }
.tl-list { position: relative; padding-left: 20px; }
.tl-item { position: relative; padding-bottom: 18px; }
.tl-item:last-child { padding-bottom: 0; }
.tl-dot { position: absolute; left: -20px; top: 4px; width: 10px; height: 10px; border-radius: 50%; background: #e2e8f0; z-index: 1; }
.tl-dot.active { background: #6366f1; box-shadow: 0 0 0 4px rgba(99,102,241,.2); }
.tl-line { position: absolute; left: -16px; top: 14px; bottom: 0; width: 2px; background: #e2e8f0; }
.tl-item.done .tl-line { background: #6366f1; }
.tl-item.done .tl-dot { background: #6366f1; }
.tl-label { font-size: 14px; font-weight: 600; color: #1e293b; }
.tl-time { font-size: 11px; color: #94a3b8; margin-top: 2px; }
.tl-desc { font-size: 12px; color: #64748b; margin-top: 4px; background: #f8fafc; padding: 6px 10px; border-radius: 8px; }

/* Images */
.card-subtitle { font-size: 14px; font-weight: 600; color: #1e293b; margin-bottom: 10px; }
.card-text { font-size: 13px; color: #64748b; margin: 0; line-height: 1.6; }
.img-row { display: flex; gap: 8px; flex-wrap: wrap; }
.result-img { width: 80px; height: 80px; object-fit: cover; border-radius: 10px; }

/* Action */
.action-bar { padding: 16px 12px; }

/* Empty */
.empty-page { display: flex; flex-direction: column; }
.empty-content { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; }
.empty-icon { font-size: 48px; }
.empty-content p { color: #94a3b8; font-size: 14px; }
</style>
