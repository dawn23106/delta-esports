<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getMyOrders, startOrder, completeOrder } from '../../api/orders'
import { showToast, showLoadingToast, closeToast } from 'vant'
import { useAuthGuard } from '../../composables/useAuthGuard'

const active = ref(1)
const myOrder = ref<any>(null)
const showComplete = ref(false)
const isQualified = ref(true)
const resultNote = ref('')
const resultImages = ref('[]')
const { requireLogin } = useAuthGuard()

const statusSteps: Record<string, { label: string; icon: string }> = {
  assigned: { label: '待开始', icon: '⏳' },
  in_progress: { label: '进行中', icon: '🎮' },
  completed: { label: '待审核', icon: '⏰' },
  done: { label: '等待结算', icon: '💰' },
  settled: { label: '已结算', icon: '✅' },
}

async function loadOrder() {
  try {
    const res: any = await getMyOrders(1, 10)
    const orders = res.records || []
    myOrder.value = orders.find((o: any) => !['settled', 'cancelled', 'pending'].includes(o.status)) || null
  } catch { }
}

async function handleStart() {
  if (!myOrder.value) return
  if (!await requireLogin('开始代练')) return
  showLoadingToast('开始中...')
  try { await startOrder(myOrder.value.id); closeToast(); showToast('已开始！💪'); loadOrder() }
  catch (e: any) { closeToast(); showToast(e?.response?.data?.message || '操作失败') }
}

async function handleComplete() {
  if (!await requireLogin('申请结单')) return
  showLoadingToast('提交中...')
  try {
    await completeOrder({
      orderId: myOrder.value.id,
      isQualified: isQualified.value,
      resultNote: resultNote.value || undefined,
      resultImages: resultImages.value !== '[]' ? resultImages.value : undefined
    })
    closeToast(); showToast('结单申请已提交！🎉'); showComplete.value = false; loadOrder()
  } catch (e: any) { closeToast(); showToast(e?.response?.data?.message || '提交失败') }
}

onMounted(loadOrder)
</script>

<template>
  <div class="page">
    <!-- 当前订单 -->
    <div class="section" v-if="myOrder">
      <!-- 状态卡片 -->
      <div class="status-card">
        <div class="status-icon">{{ statusSteps[myOrder.status]?.icon || '📋' }}</div>
        <div class="status-label">{{ statusSteps[myOrder.status]?.label || myOrder.status }}</div>
        <div class="status-id">订单 #{{ myOrder.id }}</div>
      </div>

      <!-- 订单信息 -->
      <div class="info-card">
        <div class="info-row">
          <span class="info-key">金额</span>
          <span class="info-val price">¥{{ myOrder.amount }}</span>
        </div>
        <div class="info-row" v-if="myOrder.gameMap">
          <span class="info-key">地图</span>
          <span class="info-val">{{ myOrder.gameMap }}</span>
        </div>
        <div class="info-row" v-if="myOrder.bossNote">
          <span class="info-key">老板备注</span>
          <span class="info-val note">{{ myOrder.bossNote }}</span>
        </div>
        <div class="info-row">
          <span class="info-key">创建时间</span>
          <span class="info-val">{{ myOrder.createdAt?.replace('T', ' ').substring(0, 16) }}</span>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="actions">
        <van-button
          v-if="myOrder.status === 'assigned'"
          block round type="primary" size="large"
          class="action-btn"
          color="linear-gradient(135deg, #10b981, #06b6d4)"
          @click="handleStart"
        >
          🎮 开始代练
        </van-button>
        <van-button
          v-if="myOrder.status === 'in_progress'"
          block round type="primary" size="large"
          class="action-btn"
          color="linear-gradient(135deg, #6366f1, #8b5cf6)"
          @click="showComplete = true"
        >
          ✅ 申请结单
        </van-button>
        <div v-if="myOrder.status === 'completed' || myOrder.status === 'done'" class="waiting-hint">
          <span>⏰</span>
          <span>{{ myOrder.status === 'done' ? '等待系统结算中...' : '等待老板确认中...' }}</span>
        </div>
      </div>

      <!-- 快捷入口 -->
      <div class="quick-links">
        <div class="quick-item" @click="$router.push(`/booster/messages/${myOrder.id}`)">
          <span class="quick-icon">💬</span>
          <span class="quick-label">订单群聊</span>
          <span class="quick-arrow">›</span>
        </div>
      </div>
    </div>

    <!-- 无订单 -->
    <div v-else class="empty">
      <div class="empty-art">
        <span class="empty-emoji">📭</span>
      </div>
      <div class="empty-title">暂无进行中的订单</div>
      <div class="empty-desc">去订单池抢单，开启赚钱之旅</div>
      <van-button round type="primary" class="empty-btn" @click="$router.push('/booster/pool')"
        color="linear-gradient(135deg, #6366f1, #8b5cf6)">去订单池</van-button>
    </div>

    <!-- 结单弹窗 -->
    <van-action-sheet v-model:show="showComplete" title="申请结单" :close-on-click-overlay="false" :style="{ borderRadius: '20px 20px 0 0' }">
      <div class="sheet">
        <div class="sheet-order">
          <div class="sheet-row">
            <span>订单 #{{ myOrder?.id }}</span>
            <span class="sheet-price">¥{{ myOrder?.amount }}</span>
          </div>
        </div>
        <div class="sheet-section">
          <div class="sheet-label">代练结果</div>
          <van-radio-group v-model="isQualified" direction="horizontal" class="sheet-radio-group">
            <van-radio :name="true" checked-color="#10b981" icon-size="18px">
              <span class="radio-text green">✅ 达标</span>
            </van-radio>
            <van-radio :name="false" checked-color="#ef4444" icon-size="18px">
              <span class="radio-text red">❌ 未达标</span>
            </van-radio>
          </van-radio-group>
        </div>
        <van-field v-model="resultNote" label="备注说明" placeholder="简单描述代练结果..." type="textarea" rows="2" maxlength="200" show-word-limit />
        <van-button round block type="primary" class="sheet-btn" @click="handleComplete"
          color="linear-gradient(135deg, #6366f1, #8b5cf6)">确认提交</van-button>
      </div>
    </van-action-sheet>

    <van-tabbar v-model="active" route :border="false" active-color="#6366f1" inactive-color="#94a3b8" safe-area-inset-bottom class="tabbar">
      <van-tabbar-item icon="orders-o" to="/booster/pool">订单池</van-tabbar-item>
      <van-tabbar-item icon="logistics" to="/booster/orders">进行中</van-tabbar-item>
      <van-tabbar-item icon="user-o" to="/booster/profile">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f8fafc;
  padding-bottom: 60px;
}

/* 状态卡片 */
.status-card {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  margin: 16px;
  border-radius: 20px;
  padding: 28px 24px;
  text-align: center;
  color: #fff;
  box-shadow: 0 8px 32px rgba(99,102,241,0.2);
}
.status-icon {
  font-size: 40px;
  margin-bottom: 8px;
}
.status-label {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 4px;
}
.status-id {
  font-size: 13px;
  opacity: 0.7;
}

/* 信息卡片 */
.info-card {
  background: #fff;
  margin: 0 16px 16px;
  border-radius: 18px;
  padding: 4px 0;
  box-shadow: 0 1px 3px rgba(0,0,0,0.03);
  border: 1px solid #f1f5f9;
}
.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;
  border-bottom: 1px solid #f8fafc;
}
.info-row:last-child {
  border-bottom: none;
}
.info-key {
  font-size: 13px;
  color: #94a3b8;
}
.info-val {
  font-size: 14px;
  color: #475569;
  font-weight: 500;
}
.info-val.price {
  color: #6366f1;
  font-weight: 800;
  font-size: 18px;
}
.info-val.note {
  color: #64748b;
  max-width: 200px;
  text-align: right;
  line-height: 1.4;
}

/* 操作 */
.actions {
  padding: 0 16px;
  margin-bottom: 16px;
}
.action-btn {
  height: 50px !important;
  font-size: 16px !important;
  font-weight: 600 !important;
  border-radius: 16px !important;
  box-shadow: 0 8px 24px rgba(0,0,0,0.15) !important;
}
.waiting-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px;
  background: #fffbeb;
  border-radius: 14px;
  border: 1px solid #fde68a;
  font-size: 13px;
  color: #92400e;
}

/* 快捷入口 */
.quick-links {
  padding: 0 16px;
}
.quick-item {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #fff;
  border-radius: 14px;
  padding: 14px 16px;
  cursor: pointer;
  box-shadow: 0 1px 3px rgba(0,0,0,0.03);
  border: 1px solid #f1f5f9;
}
.quick-icon {
  font-size: 20px;
}
.quick-label {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
  color: #475569;
}
.quick-arrow {
  font-size: 20px;
  color: #cbd5e1;
}

/* 空状态 */
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 100px;
}
.empty-art {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}
.empty-emoji {
  font-size: 40px;
}
.empty-title {
  font-size: 16px;
  font-weight: 600;
  color: #64748b;
}
.empty-desc {
  font-size: 13px;
  color: #94a3b8;
  margin-top: 4px;
  margin-bottom: 20px;
}
.empty-btn {
  height: 44px !important;
  font-size: 15px !important;
  font-weight: 600 !important;
  border-radius: 14px !important;
  box-shadow: 0 6px 20px rgba(99,102,241,0.25) !important;
}

/* 结单弹窗 */
.sheet {
  padding: 0 20px 30px;
}
.sheet-order {
  background: linear-gradient(135deg, #eef2ff, #faf5ff);
  border-radius: 14px;
  padding: 14px 16px;
  margin-bottom: 16px;
  border: 1px solid #e0e7ff;
}
.sheet-row {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #1e293b;
  font-weight: 500;
}
.sheet-price {
  color: #6366f1;
  font-weight: 700;
}
.sheet-section {
  margin-bottom: 12px;
}
.sheet-label {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
  margin-bottom: 10px;
}
.sheet-radio-group {
  display: flex;
  gap: 24px;
}
.radio-text {
  font-size: 15px;
  font-weight: 600;
}
.radio-text.green { color: #10b981; }
.radio-text.red { color: #ef4444; }
.sheet-btn {
  height: 50px !important;
  font-size: 16px !important;
  font-weight: 600 !important;
  border-radius: 16px !important;
  margin-top: 16px !important;
  box-shadow: 0 8px 24px rgba(99,102,241,0.25) !important;
}

.tabbar {
  background: rgba(255,255,255,0.9) !important;
  backdrop-filter: blur(20px) !important;
  border-top: 1px solid #f1f5f9 !important;
}
</style>
