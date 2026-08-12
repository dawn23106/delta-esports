<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import { cancelOrder, getOrderDetail } from '../../api/orders'
import { useAuthGuard } from '../../composables/useAuthGuard'

const route = useRoute()
const order = ref<any>(null)
const loading = ref(false)
const loadFailed = ref(false)
const id = Number(route.params.id)
const { requireLogin } = useAuthGuard()

const statusText: Record<string, string> = {
  pending: '待接单',
  assigned: '已接单',
  in_progress: '进行中',
  completed: '待验收',
  done: '已确认',
  settled: '已结算',
  cancelled: '已取消',
  disputed: '争议中',
}

const statusHint: Record<string, string> = {
  pending: '订单已创建，正在等待陪玩接单。',
  assigned: '陪玩已接单，即将开始服务。',
  in_progress: '服务正在进行，请保持沟通。',
  completed: '陪玩已提交结单，请确认结果。',
  done: '你已确认达标，等待平台结算。',
  settled: '订单已完成并结算。',
  cancelled: '订单已取消。',
  disputed: '订单正在争议处理中。',
}

const steps = ref<{ label: string; time: string; desc?: string }[]>([])

function buildSteps(source: any) {
  const list = []
  if (source.createdAt) list.push({ label: '下单成功', time: source.createdAt, desc: source.bossNote })
  if (source.assignedAt) list.push({ label: '陪玩接单', time: source.assignedAt })
  if (source.startedAt) list.push({ label: '开始服务', time: source.startedAt })
  if (source.completedAt) list.push({ label: '申请结单', time: source.completedAt, desc: source.isQualified ? '陪玩自评：达标' : '陪玩自评：未达标' })
  if (source.doneAt) list.push({ label: '确认达标', time: source.doneAt })
  if (source.settledAt) list.push({ label: '完成结算', time: source.settledAt })
  if (source.status === 'cancelled') list.push({ label: '订单取消', time: source.updatedAt || source.createdAt })
  return list
}

function formatTime(value: string) {
  return value?.replace('T', ' ').substring(0, 16) || ''
}

function resultImages() {
  if (!order.value?.resultImages) return []
  try {
    return JSON.parse(order.value.resultImages)
  } catch {
    return []
  }
}

async function load() {
  loading.value = true
  loadFailed.value = false
  try {
    const result: any = await getOrderDetail(id)
    order.value = result
    steps.value = buildSteps(result)
  } catch {
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

async function handleCancel() {
  if (!await requireLogin('取消订单')) return
  try {
    await showConfirmDialog({ title: '取消订单', message: '确定要取消这个订单吗？', confirmButtonText: '取消订单', confirmButtonColor: '#f04438' })
    await cancelOrder(id)
    showToast('订单已取消')
    load()
  } catch {
    // user cancelled
  }
}

onMounted(load)
</script>

<template>
  <main class="mobile-page no-tabbar">
    <van-nav-bar title="订单详情" left-arrow @click-left="$router.back()" />

    <div v-if="loading" class="mobile-card loading-card">
      <van-loading color="#3157ff" />
      <span>正在加载订单</span>
    </div>

    <template v-else-if="order">
      <section class="mobile-hero detail-hero">
        <div class="eyebrow">Order #{{ order.id }}</div>
        <h1 class="page-title">{{ statusText[order.status] || order.status }}</h1>
        <p class="page-subtitle">{{ statusHint[order.status] || '订单状态已更新。' }}</p>
        <div class="metric-grid hero-metrics">
          <div class="metric"><strong>￥{{ order.amount }}</strong><span>金额</span></div>
          <div class="metric"><strong>{{ order.gameMap ? '已选' : '默认' }}</strong><span>地图</span></div>
          <div class="metric"><strong>{{ steps.length }}</strong><span>节点</span></div>
        </div>
      </section>

      <section class="mobile-card info-card">
        <div v-if="order.serviceId" class="info-row">
          <span>服务编号</span>
          <strong>#{{ order.serviceId }}</strong>
        </div>
        <div class="info-row">
          <span>地图模式</span>
          <strong>{{ order.gameMap || '未指定地图' }}</strong>
        </div>
        <div v-if="order.bossNote" class="info-row note">
          <span>我的备注</span>
          <strong>{{ order.bossNote }}</strong>
        </div>
      </section>

      <section class="mobile-card timeline-card">
        <h2>订单进度</h2>
        <div class="timeline">
          <div v-for="(step, index) in steps" :key="`${step.label}-${index}`" class="step">
            <span class="dot" />
            <div>
              <strong>{{ step.label }}</strong>
              <small>{{ formatTime(step.time) }}</small>
              <p v-if="step.desc">{{ step.desc }}</p>
            </div>
          </div>
        </div>
      </section>

      <section v-if="resultImages().length" class="mobile-card result-card">
        <h2>结果截图</h2>
        <div class="image-grid">
          <img v-for="(image, index) in resultImages()" :key="index" :src="image" alt="订单结果截图" />
        </div>
      </section>

      <section v-if="order.resultNote" class="mobile-card result-card">
        <h2>结单备注</h2>
        <p>{{ order.resultNote }}</p>
      </section>

      <van-button v-if="!['settled', 'cancelled'].includes(order.status)" block round plain type="danger" class="cancel-btn" @click="handleCancel">
        取消订单
      </van-button>
    </template>

    <div v-else class="empty-state">
      <div>
        <h3>{{ loadFailed ? '无法查看订单' : '订单不存在' }}</h3>
        <p>请返回订单记录重新进入。</p>
      </div>
    </div>
  </main>
</template>

<style scoped>
.loading-card {
  margin-top: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.detail-hero {
  margin-top: 14px;
  background-image:
    linear-gradient(135deg, rgba(16,19,35,.76), rgba(49,87,255,.52)),
    url('https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1000&h=900&fit=crop');
}

.hero-metrics {
  margin-top: 18px;
}

.info-card,
.timeline-card,
.result-card {
  margin-top: 12px;
}

.info-card {
  display: grid;
  gap: 12px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  color: var(--mobile-muted);
  font-size: 13px;
}

.info-row strong {
  max-width: 64%;
  color: var(--mobile-ink);
  text-align: right;
  font-size: 14px;
}

.info-row.note {
  align-items: flex-start;
}

.timeline-card h2,
.result-card h2 {
  margin: 0 0 14px;
  color: var(--mobile-ink);
  font-size: 16px;
  font-weight: 900;
}

.timeline {
  display: grid;
  gap: 0;
}

.step {
  display: flex;
  gap: 12px;
  position: relative;
  padding-bottom: 18px;
}

.step:last-child {
  padding-bottom: 0;
}

.step:not(:last-child)::before {
  content: "";
  position: absolute;
  top: 14px;
  left: 5px;
  bottom: 0;
  width: 2px;
  background: #dbe7ff;
}

.dot {
  width: 12px;
  height: 12px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: var(--mobile-brand);
  margin-top: 3px;
  box-shadow: 0 0 0 4px #eef4ff;
}

.step strong,
.step small {
  display: block;
}

.step strong {
  color: var(--mobile-ink);
  font-size: 14px;
}

.step small {
  margin-top: 3px;
  color: var(--mobile-faint);
  font-size: 11px;
}

.step p,
.result-card p {
  margin: 8px 0 0;
  border-radius: 12px;
  background: #f8fafc;
  color: var(--mobile-muted);
  padding: 9px 10px;
  font-size: 13px;
  line-height: 1.55;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.image-grid img {
  width: 100%;
  aspect-ratio: 1;
  border-radius: 12px;
  object-fit: cover;
}

.cancel-btn {
  margin-top: 14px;
}
</style>
