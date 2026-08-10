<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { closeToast, showLoadingToast, showToast } from 'vant'
import { completeOrder, getMyOrders, startOrder } from '../../api/orders'
import { useAuthGuard } from '../../composables/useAuthGuard'
import MobileTabbar from '../../components/MobileTabbar.vue'

const myOrder = ref<any>(null)
const showComplete = ref(false)
const isQualified = ref(true)
const resultNote = ref('')
const { requireLogin } = useAuthGuard()

const statusMeta: Record<string, { label: string; hint: string }> = {
  assigned: { label: '待开始', hint: '确认信息后即可开始代练。' },
  in_progress: { label: '进行中', hint: '完成目标后提交结单申请。' },
  completed: { label: '待老板验收', hint: '等待老板确认结果。' },
  done: { label: '等待结算', hint: '老板已确认，等待平台结算。' },
  settled: { label: '已结算', hint: '该订单已经完成。' },
}

async function loadOrder() {
  try {
    const result: any = await getMyOrders(1, 10)
    const orders = result.records || []
    myOrder.value = orders.find((order: any) => !['settled', 'cancelled', 'pending'].includes(order.status)) || null
  } catch {
    myOrder.value = null
  }
}

async function handleStart() {
  if (!myOrder.value) return
  if (!await requireLogin('开始代练')) return
  showLoadingToast({ message: '正在开始', duration: 0 })
  try {
    await startOrder(myOrder.value.id)
    closeToast()
    showToast({ message: '已开始', icon: 'success' })
    loadOrder()
  } catch (error: any) {
    closeToast()
    showToast(error?.response?.data?.message || '操作失败')
  }
}

async function handleComplete() {
  if (!myOrder.value) return
  if (!await requireLogin('申请结单')) return
  showLoadingToast({ message: '正在提交', duration: 0 })
  try {
    await completeOrder({
      orderId: myOrder.value.id,
      isQualified: isQualified.value,
      resultNote: resultNote.value || undefined,
    })
    closeToast()
    showToast({ message: '结单申请已提交', icon: 'success' })
    showComplete.value = false
    loadOrder()
  } catch (error: any) {
    closeToast()
    showToast(error?.response?.data?.message || '提交失败')
  }
}

onMounted(loadOrder)
</script>

<template>
  <main class="mobile-page">
    <template v-if="myOrder">
      <section class="mobile-hero process-hero">
        <div class="eyebrow">Current Task</div>
        <h1 class="page-title">{{ statusMeta[myOrder.status]?.label || myOrder.status }}</h1>
        <p class="page-subtitle">{{ statusMeta[myOrder.status]?.hint || '请按订单要求推进。' }}</p>
        <div class="metric-grid hero-metrics">
          <div class="metric"><strong>#{{ myOrder.id }}</strong><span>订单</span></div>
          <div class="metric"><strong>￥{{ myOrder.amount }}</strong><span>金额</span></div>
          <div class="metric"><strong>{{ myOrder.gameMap ? '已选' : '默认' }}</strong><span>地图</span></div>
        </div>
      </section>

      <section class="mobile-card info-card">
        <div class="info-row">
          <span>地图模式</span>
          <strong>{{ myOrder.gameMap || '未指定地图' }}</strong>
        </div>
        <div v-if="myOrder.bossNote" class="info-row note">
          <span>老板备注</span>
          <strong>{{ myOrder.bossNote }}</strong>
        </div>
        <div class="info-row">
          <span>创建时间</span>
          <strong>{{ myOrder.createdAt?.replace('T', ' ').substring(0, 16) }}</strong>
        </div>
      </section>

      <section class="action-stack">
        <van-button v-if="myOrder.status === 'assigned'" block round size="large" color="linear-gradient(135deg, #12b76a, #08b6d8)" @click="handleStart">
          开始代练
        </van-button>
        <van-button v-if="myOrder.status === 'in_progress'" block round size="large" color="linear-gradient(135deg, #3157ff, #08b6d8)" @click="showComplete = true">
          申请结单
        </van-button>
        <div v-if="myOrder.status === 'completed' || myOrder.status === 'done'" class="waiting-card">
          {{ statusMeta[myOrder.status]?.hint }}
        </div>
        <button type="button" class="chat-link" @click="$router.push(`/booster/messages/${myOrder.id}`)">
          <van-icon name="chat-o" />
          <span>进入订单沟通</span>
          <van-icon name="arrow" />
        </button>
      </section>
    </template>

    <div v-else class="empty-state">
      <div>
        <h3>暂无进行中的订单</h3>
        <p>去订单池抢单后，这里会显示当前任务。</p>
        <van-button round type="primary" color="linear-gradient(135deg, #3157ff, #08b6d8)" @click="$router.push('/booster/pool')">
          去订单池
        </van-button>
      </div>
    </div>

    <van-action-sheet v-model:show="showComplete" title="申请结单" round>
      <div class="sheet">
        <div class="sheet-summary">
          <span>订单 #{{ myOrder?.id }}</span>
          <strong>￥{{ myOrder?.amount }}</strong>
        </div>
        <van-radio-group v-model="isQualified" direction="horizontal" class="radio-row">
          <van-radio :name="true" checked-color="#12b76a">已达标</van-radio>
          <van-radio :name="false" checked-color="#f04438">未达标</van-radio>
        </van-radio-group>
        <van-field v-model="resultNote" label="备注" placeholder="简单描述结单结果" type="textarea" rows="3" maxlength="200" show-word-limit />
        <van-button block round type="primary" size="large" color="linear-gradient(135deg, #3157ff, #08b6d8)" @click="handleComplete">
          确认提交
        </van-button>
      </div>
    </van-action-sheet>

    <MobileTabbar role="booster" />
  </main>
</template>

<style scoped>
.process-hero {
  background-image:
    linear-gradient(135deg, rgba(16,19,35,.76), rgba(49,87,255,.52)),
    url('https://images.unsplash.com/photo-1592155931584-901ac15763e3?w=1000&h=900&fit=crop');
}

.hero-metrics {
  margin-top: 18px;
}

.info-card {
  margin-top: 14px;
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

.action-stack {
  margin-top: 14px;
  display: grid;
  gap: 10px;
}

.waiting-card {
  border-radius: 16px;
  background: #fffaeb;
  color: #b54708;
  padding: 14px;
  text-align: center;
  font-size: 13px;
  font-weight: 750;
}

.chat-link {
  border: 1px solid var(--mobile-line);
  border-radius: 18px;
  background: rgba(255,255,255,.9);
  padding: 15px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--mobile-ink);
  font-size: 14px;
  font-weight: 850;
}

.chat-link span {
  flex: 1;
  text-align: left;
}

.sheet {
  padding: 0 16px 24px;
  display: grid;
  gap: 14px;
}

.sheet-summary {
  display: flex;
  justify-content: space-between;
  border-radius: 18px;
  background: #eef4ff;
  padding: 14px;
}

.sheet-summary strong {
  color: var(--mobile-brand);
  font-size: 20px;
}

.radio-row {
  display: flex;
  gap: 24px;
}
</style>
