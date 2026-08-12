<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import request from '../../api/request'
import { useAuthStore } from '../../store/auth'
import { showToast } from 'vant'
import { useAuthGuard } from '../../composables/useAuthGuard'

const route = useRoute()
const auth = useAuthStore()
const orderId = Number(route.params.orderId)
const messages = ref<any[]>([])
const inputText = ref('')
const loading = ref(false)
const pageLoading = ref(true)
const orderInfo = ref<any>(null)
const { requireLogin } = useAuthGuard()

const statusLabels: Record<string, string> = {
  pending: '待接单', assigned: '已接单', in_progress: '进行中',
  completed: '待审核', done: '已确认', settled: '已结算',
  cancelled: '已取消', disputed: '争议中',
}

const canSendMessage = computed(() => {
  if (!orderInfo.value) return true
  const s = orderInfo.value.status
  return s !== 'cancelled' && s !== 'settled'
})

const inputHint = computed(() => {
  if (!orderInfo.value) return ''
  const s = orderInfo.value.status
  if (s === 'settled') return '📦 订单已结算，群聊已归档'
  if (s === 'cancelled') return '🚫 订单已取消，无法发送消息'
  return ''
})

async function loadOrderInfo() {
  try {
    const res: any = await request.get(`/orders/${orderId}`)
    orderInfo.value = res
  } catch {
    orderInfo.value = null
  }
}

let pollTimer: ReturnType<typeof setInterval> | null = null

async function loadMessages() {
  try {
    const res: any = await request.get(`/messages/${orderId}`)
    messages.value = res || []
    scrollBottom()
  } catch (e: any) {
    if (e?.response?.status === 403) {
      showToast('无权访问该订单群聊')
    }
    messages.value = []
  } finally {
    pageLoading.value = false
  }
}

async function pollMessages() {
  try {
    const res: any = await request.get(`/messages/${orderId}`)
    const latest: any[] = res || []
    if (latest.length !== messages.value.length || messages.value.some((m: any) => !m.id)) {
      messages.value = latest
      scrollBottom()
    }
  } catch { /* 静默跳过 */ }
}

function senderLabel(msg: any): string {
  if (msg.senderId === auth.userId) return ''
  if (orderInfo.value) {
    if (msg.senderId === orderInfo.value.bossId) return '老板'
    if (msg.senderId === orderInfo.value.boosterId) return '打手'
  }
  return ''
}

function startPolling() {
  stopPolling()
  pollTimer = setInterval(pollMessages, 3000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

async function sendMessage() {
  if (!inputText.value.trim()) return
  if (!await requireLogin('发送消息')) return
  loading.value = true
  try {
    await request.post('/messages', null, { params: { orderId, content: inputText.value, type: 'text' } })
    messages.value.push({
      senderId: auth.userId,
      content: inputText.value,
      type: 'text',
      createdAt: new Date().toISOString(),
    })
    inputText.value = ''
    scrollBottom()
  } catch (e: any) {
    showToast(e?.response?.data?.message || '发送失败')
  } finally {
    loading.value = false
  }
}

function scrollBottom() {
  nextTick(() => {
    const el = document.getElementById('chat-list')
    if (el) el.scrollTop = el.scrollHeight
  })
}

function formatTime(t: string) {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  const isToday = d.toDateString() === now.toDateString()
  if (isToday) {
    return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' }) + ' ' +
    d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const groupedMessages = ref<{ date: string; items: any[] }[]>([])

watch(() => messages.value.length, () => {
  const groups: { date: string; items: any[] }[] = []
  messages.value.forEach((msg: any) => {
    const date = new Date(msg.createdAt).toLocaleDateString('zh-CN', { month: 'long', day: 'numeric', weekday: 'short' })
    const last = groups[groups.length - 1]
    if (last && last.date === date) {
      last.items.push(msg)
    } else {
      groups.push({ date, items: [msg] })
    }
  })
  groupedMessages.value = groups
})

onMounted(async () => {
  await loadOrderInfo()
  await loadMessages()
  startPolling()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<template>
  <div class="chat-page">
    <van-nav-bar left-arrow @click-left="$router.back()" fixed placeholder>
      <template #title>
        <div class="nav-title-wrap">
          <span class="nav-title">订单群聊</span>
          <span v-if="orderInfo" class="nav-status" :class="orderInfo.status">
            {{ statusLabels[orderInfo.status] || orderInfo.status }}
          </span>
        </div>
      </template>
      <template #right>
        <span class="nav-oid">#{{ orderId }}</span>
      </template>
    </van-nav-bar>

    <div id="chat-list" class="chat-body">
      <div v-if="pageLoading" class="chat-loading">
        <van-loading color="#3157ff" />
      </div>

      <template v-else>
        <div v-if="orderInfo" class="order-info-bar">
          <div class="order-info-left">
            <strong class="order-info-amount">¥{{ orderInfo.amount }}</strong>
            <span v-if="orderInfo.gameMap" class="order-info-map">{{ orderInfo.gameMap }}</span>
          </div>
          <span class="order-info-status" :class="orderInfo.status">
            {{ statusLabels[orderInfo.status] || orderInfo.status }}
          </span>
        </div>

        <div v-for="group in groupedMessages" :key="group.date">
          <div class="date-divider">
            <span>{{ group.date }}</span>
          </div>

          <div
            v-for="(msg, i) in group.items"
            :key="i"
            :class="['msg-row', { mine: msg.senderId === auth.userId }]"
          >
            <template v-if="msg.senderId !== auth.userId">
              <div class="msg-avatar">
                <span class="avatar-emoji">{{ orderInfo && msg.senderId === orderInfo.boosterId ? '🎮' : '👤' }}</span>
              </div>
              <div class="msg-block">
                <div class="msg-sender">{{ senderLabel(msg) }}</div>
                <div class="msg-bubble">
                  <div class="msg-text">{{ msg.content }}</div>
                  <div class="msg-time">{{ formatTime(msg.createdAt) }}</div>
                </div>
              </div>
            </template>
            <template v-else>
              <div class="msg-bubble mine">
                <div class="msg-text">{{ msg.content }}</div>
                <div class="msg-time">{{ formatTime(msg.createdAt) }}</div>
              </div>
            </template>
          </div>
        </div>
      </template>

      <div v-if="!pageLoading && messages.length === 0" class="empty-state">
        <div>
          <h3>开始对话</h3>
          <p>{{ auth.userRole === 'booster' ? '发送第一条消息与老板沟通订单详情' : '发送第一条消息与陪陪沟通订单详情' }}</p>
        </div>
      </div>
    </div>

    <div v-if="canSendMessage" class="chat-input-bar">
      <div class="input-wrap">
        <input
          v-model="inputText"
          placeholder="输入消息..."
          maxlength="500"
          class="msg-input"
          @keyup.enter="sendMessage"
        />
        <button class="send-btn" :class="{ active: inputText.trim() }" :disabled="loading" @click="sendMessage">
          <span v-if="!loading">发送</span>
          <van-loading v-else size="16" color="#fff" />
        </button>
      </div>
    </div>

    <div v-else-if="inputHint" class="chat-input-bar archived-bar">
      <div class="archived-hint">{{ inputHint }}</div>
    </div>
  </div>
</template>

<style scoped>
.chat-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 18% -4%, rgba(49, 87, 255, .06), transparent 28%),
    linear-gradient(180deg, #f7faff 0%, #eef3f9 100%);
  display: flex;
  flex-direction: column;
}

/* ---- 导航栏 ---- */
.nav-title-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.nav-title {
  font-size: 16px;
  font-weight: 750;
  color: var(--mobile-ink);
}

.nav-oid {
  font-size: 12px;
  color: var(--mobile-faint);
}

.nav-status {
  font-size: 10px;
  padding: 1px 8px;
  border-radius: 8px;
  font-weight: 650;
}

.nav-status.in_progress,
.nav-status.assigned {
  background: #eef4ff;
  color: var(--mobile-brand);
}

.nav-status.completed,
.nav-status.done {
  background: #fffaeb;
  color: #b54708;
}

.nav-status.settled {
  background: #ecfdf5;
  color: var(--mobile-success);
}

.nav-status.cancelled,
.nav-status.disputed {
  background: #fef2f2;
  color: var(--mobile-danger);
}

/* ---- 聊天主体 ---- */
.chat-body {
  flex: 1;
  padding: 12px 14px;
  padding-bottom: 84px;
  overflow-y: auto;
}

.chat-loading {
  display: flex;
  justify-content: center;
  padding-top: 60px;
}

/* ---- 订单信息条 ---- */
.order-info-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 1px solid rgba(228, 231, 236, .9);
  border-radius: 16px;
  background: rgba(255, 255, 255, .9);
  padding: 10px 14px;
  margin-bottom: 14px;
  box-shadow: 0 6px 16px rgba(16, 24, 40, .04);
}

.order-info-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.order-info-amount {
  font-size: 16px;
  font-weight: 900;
  color: var(--mobile-brand);
}

.order-info-map {
  font-size: 12px;
  color: var(--mobile-muted);
  background: #f2f4f7;
  border-radius: 999px;
  padding: 3px 9px;
  font-weight: 650;
}

.order-info-status {
  font-size: 11px;
  padding: 2px 10px;
  border-radius: 999px;
  font-weight: 650;
}

.order-info-status.in_progress,
.order-info-status.assigned {
  background: #eef4ff;
  color: var(--mobile-brand);
}

.order-info-status.completed,
.order-info-status.done {
  background: #fffaeb;
  color: #b54708;
}

.order-info-status.settled {
  background: #ecfdf5;
  color: var(--mobile-success);
}

.order-info-status.cancelled,
.order-info-status.disputed {
  background: #fef2f2;
  color: var(--mobile-danger);
}

/* ---- 日期分隔 ---- */
.date-divider {
  display: flex;
  justify-content: center;
  margin: 16px 0;
}

.date-divider span {
  border-radius: 20px;
  background: rgba(255, 255, 255, .8);
  padding: 4px 16px;
  font-size: 12px;
  color: var(--mobile-faint);
  box-shadow: 0 1px 2px rgba(16, 24, 40, .03);
}

/* ---- 消息行 ---- */
.msg-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 14px;
}

.msg-row.mine {
  justify-content: flex-end;
}

.msg-avatar {
  flex-shrink: 0;
  margin-top: 2px;
}

.avatar-emoji {
  font-size: 28px;
  line-height: 36px;
  display: block;
  text-align: center;
}

.msg-block {
  display: flex;
  flex-direction: column;
  gap: 2px;
  max-width: 72%;
}

.msg-sender {
  font-size: 11px;
  color: var(--mobile-faint);
  padding: 0 4px;
}

/* ---- 气泡 ---- */
.msg-bubble {
  background: #fff;
  border-radius: 18px 18px 18px 6px;
  padding: 10px 14px;
  box-shadow: 0 2px 8px rgba(16, 24, 40, .04);
  border: 1px solid rgba(228, 231, 236, .5);
}

.msg-bubble.mine {
  background: linear-gradient(135deg, #3157ff, #08b6d8);
  border: 0;
  border-radius: 18px 18px 6px 18px;
  color: #fff;
  box-shadow: 0 6px 18px rgba(49, 87, 255, .22);
}

.msg-text {
  font-size: 15px;
  line-height: 1.5;
  word-break: break-word;
  color: var(--mobile-ink);
}

.msg-bubble.mine .msg-text {
  color: #fff;
}

.msg-time {
  margin-top: 4px;
  font-size: 10px;
  opacity: .45;
}

/* ---- 输入栏 ---- */
.chat-input-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 10px 12px;
  padding-bottom: calc(10px + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, .92);
  backdrop-filter: blur(20px);
  border-top: 1px solid rgba(228, 231, 236, .8);
}

.input-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #f2f4f7;
  border-radius: 24px;
  padding: 4px 4px 4px 16px;
}

.msg-input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 15px;
  outline: none;
  color: var(--mobile-ink);
}

.msg-input::placeholder {
  color: var(--mobile-faint);
}

.send-btn {
  padding: 8px 20px;
  border: none;
  border-radius: 20px;
  background: #cbd5e1;
  color: #fff;
  font-size: 14px;
  font-weight: 750;
  cursor: pointer;
  transition: all .2s;
  white-space: nowrap;
}

.send-btn.active {
  background: linear-gradient(135deg, #3157ff, #08b6d8);
  box-shadow: 0 4px 14px rgba(49, 87, 255, .3);
}

.send-btn:disabled {
  opacity: .6;
}

/* ---- 归档/取消提示 ---- */
.archived-bar {
  background: rgba(248, 250, 252, .95) !important;
  border-top: 1px solid #e2e8f0 !important;
}

.archived-hint {
  text-align: center;
  font-size: 13px;
  color: var(--mobile-muted);
  padding: 8px 0;
}

/* ---- 响应式居中 ---- */
@media (min-width: 560px) {
  .chat-input-bar {
    left: 50%;
    right: auto;
    width: 430px;
    transform: translateX(-50%);
  }
}
</style>
