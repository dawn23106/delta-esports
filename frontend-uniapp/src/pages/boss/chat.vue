<template>
  <view class="chat-page">
    <view v-if="orderInfo" class="order-card">
      <view><text class="order-title">{{ orderInfo.serviceName || `订单 #${orderInfo.id}` }}</text><text class="order-meta">¥{{ orderInfo.amount }} · {{ orderInfo.gameMap || '标准服务' }}</text></view>
      <text :class="['order-status', orderInfo.status]">{{ statusLabels[orderInfo.status] || orderInfo.status }}</text>
    </view>

    <view v-if="roomCode" class="room-card" @tap="copyRoomCode">
      <view><text class="room-kicker">已识别房间号</text><text class="room-code">{{ roomCode }}</text></view>
      <text class="copy-btn">复制</text>
    </view>

    <scroll-view class="chat-body" scroll-y :scroll-into-view="scrollToId" :scroll-with-animation="true">
      <view class="security-tip">请勿发送账号密码；这里只沟通房间号、开局时间和对局信息。</view>
      <view v-if="pageLoading" class="chat-loading">加载中…</view>
      <template v-else>
        <view v-for="(group, gi) in groupedMessages" :key="group.date">
          <view class="date-divider"><text>{{ group.date }}</text></view>
          <view v-for="(msg, mi) in group.items" :id="`msg-${gi}-${mi}`" :key="msg.id || `${gi}-${mi}`" :class="['msg-row', { mine: msg.senderId === auth.userId }]">
            <view v-if="msg.senderId !== auth.userId" class="avatar">{{ senderLabel(msg).slice(0, 1) }}</view>
            <view :class="['msg-bubble', { mine: msg.senderId === auth.userId }]">
              <text v-if="msg.senderId !== auth.userId" class="sender">{{ senderLabel(msg) }}</text>
              <text class="msg-text">{{ msg.content }}</text>
              <text class="msg-time">{{ formatTime(msg.createdAt) }}</text>
            </view>
          </view>
        </view>
        <view v-if="messages.length === 0" class="empty-chat"><text>还没有消息</text><text>{{ auth.userRole === 'booster' ? '可先发送房间号和开局时间。' : '等待打手发送房间号，或先确认开局时间。' }}</text></view>
      </template>
      <view id="msg-bottom" class="bottom-anchor" />
    </scroll-view>

    <view v-if="canSendMessage" class="quick-row">
      <text @tap="useQuick('房间号：')">房间号</text><text @tap="useQuick('我已准备好，可以开局。')">已准备</text><text @tap="useQuick('预计几分钟后开始？')">确认时间</text>
    </view>
    <view v-if="canSendMessage" class="chat-input-bar">
      <view class="input-wrap">
        <input v-model="inputText" placeholder="输入房间号或对局消息" maxlength="500" class="msg-input" @confirm="sendMessage" />
        <button :class="['send-btn', { active: inputText.trim() }]" :disabled="loading || !inputText.trim()" @tap="sendMessage">发送</button>
      </view>
    </view>
    <view v-else class="archived-bar">{{ inputHint }}</view>
  </view>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from "vue"
import { onLoad } from "@dcloudio/uni-app"
import { useAuthStore } from "@/store/auth"
import { getMessages, sendMessage as sendMsgApi } from "@/api/users"
import { getOrderDetail } from "@/api/orders"

const auth = useAuthStore()
const orderId = ref(0)
const messages = ref<any[]>([])
const inputText = ref('')
const loading = ref(false)
const pageLoading = ref(true)
const orderInfo = ref<any>(null)
const scrollToId = ref('')
let pollTimer: ReturnType<typeof setInterval> | null = null

const statusLabels: Record<string, string> = { assigned: '待开始', in_progress: '服务中', submitted: '待确认', done: '已完成', settled: '已归档', cancelled: '已取消' }
const canSendMessage = computed(() => ['assigned', 'in_progress'].includes(orderInfo.value?.status))
const inputHint = computed(() => orderInfo.value?.status === 'submitted' ? '订单已提交，聊天暂时只读' : '订单已结束，聊天记录已归档')
const roomCode = computed(() => {
  for (let i = messages.value.length - 1; i >= 0; i--) {
    const match = String(messages.value[i].content || '').match(/(?:房间号[：:\s]*)?([A-Z0-9]{6,10})/i)
    if (match && /\d/.test(match[1])) return match[1]
  }
  return ''
})
const groupedMessages = computed(() => {
  const groups: { date: string; items: any[] }[] = []
  messages.value.forEach((msg) => {
    const date = new Date(msg.createdAt).toLocaleDateString('zh-CN', { month: 'long', day: 'numeric' })
    const last = groups[groups.length - 1]
    if (last?.date === date) last.items.push(msg)
    else groups.push({ date, items: [msg] })
  })
  return groups
})

onLoad((options: any) => { orderId.value = Number(options?.orderId) || 0 })
function senderLabel(msg: any) { return msg.senderId === orderInfo.value?.boosterId ? '打手' : '老板' }
function formatTime(value: string) { return value ? new Date(value).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }) : '' }
function scrollToBottom() { nextTick(() => { scrollToId.value = ''; nextTick(() => { scrollToId.value = 'msg-bottom' }) }) }
function useQuick(text: string) { inputText.value = text }
function copyRoomCode() { uni.setClipboardData({ data: roomCode.value, success: () => uni.showToast({ title: '房间号已复制', icon: 'none' }) }) }

async function loadOrder() { try { orderInfo.value = await getOrderDetail(orderId.value) } catch { orderInfo.value = null } }
async function loadMessages(showLoading = false) {
  if (showLoading) pageLoading.value = true
  try { const result: any = await getMessages(orderId.value); messages.value = result || []; scrollToBottom() }
  catch { if (showLoading) messages.value = [] }
  finally { pageLoading.value = false }
}
async function sendMessage() {
  const content = inputText.value.trim()
  if (!content || loading.value || !canSendMessage.value) return
  loading.value = true
  try { const saved: any = await sendMsgApi(orderId.value, content); messages.value.push(saved); inputText.value = ''; scrollToBottom() }
  catch (e: any) { uni.showToast({ title: e?.data?.message || '发送失败', icon: 'none' }) }
  finally { loading.value = false }
}

watch(() => orderInfo.value?.status, (status) => { if (!['assigned', 'in_progress'].includes(status) && pollTimer) { clearInterval(pollTimer); pollTimer = null } })
onMounted(async () => { await loadOrder(); await loadMessages(true); if (canSendMessage.value) pollTimer = setInterval(() => loadMessages(false), 3000) })
onUnmounted(() => { if (pollTimer) clearInterval(pollTimer) })
</script>

<style scoped>
.chat-page { min-height: 100vh; box-sizing: border-box; background: linear-gradient(180deg, #f3f7f2, #eef3ec); padding-top: 1px; }
.order-card { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin: 12px 14px 0; padding: 13px 14px; border: 1px solid #dce7dc; border-radius: 16px; background: rgba(255,255,255,.94); }
.order-title { color: var(--mobile-ink); font-size: 14px; font-weight: 900; }
.order-meta { display: block; margin-top: 4px; color: var(--mobile-muted); font-size: 11px; }
.order-status { flex-shrink: 0; padding: 4px 9px; border-radius: 999px; background: #e5f1e8; color: #2c704d; font-size: 10px; }
.order-status.submitted { background: #fff4dd; color: #9b6a2f; }
.room-card { display: flex; align-items: center; justify-content: space-between; margin: 10px 14px 0; padding: 12px 14px; border-radius: 15px; background: #234f38; color: #fff; }
.room-kicker { display: block; color: rgba(255,255,255,.65); font-size: 10px; }
.room-code { display: block; margin-top: 2px; font-size: 22px; font-weight: 900; letter-spacing: 2px; }
.copy-btn { padding: 6px 12px; border-radius: 999px; background: rgba(255,255,255,.15); font-size: 11px; }
.chat-body { height: calc(100vh - 175px); box-sizing: border-box; padding: 12px 14px 150px; }
.security-tip { padding: 8px 10px; border-radius: 10px; background: #fff8e9; color: #806d55; font-size: 10px; text-align: center; }
.chat-loading, .empty-chat { padding: 55px 20px; color: var(--mobile-muted); text-align: center; }
.empty-chat text { display: block; margin-top: 5px; font-size: 12px; }
.date-divider { margin: 14px 0; color: var(--mobile-faint); font-size: 10px; text-align: center; }
.msg-row { display: flex; align-items: flex-start; gap: 8px; margin-bottom: 13px; }
.msg-row.mine { justify-content: flex-end; }
.avatar { width: 32px; height: 32px; flex-shrink: 0; border-radius: 11px; background: #fff; color: #557361; font-size: 12px; font-weight: 900; line-height: 32px; text-align: center; }
.msg-bubble { max-width: 72%; padding: 9px 12px; border: 1px solid #e0e8e0; border-radius: 16px 16px 16px 5px; background: #fff; }
.msg-bubble.mine { border: 0; border-radius: 16px 16px 5px 16px; background: #3d7b57; color: #fff; }
.sender { display: block; margin-bottom: 3px; color: #7a8a7f; font-size: 9px; }
.msg-text { font-size: 14px; line-height: 1.5; }
.msg-time { display: block; margin-top: 4px; font-size: 9px; opacity: .48; text-align: right; }
.quick-row { position: fixed; right: 0; bottom: 66px; left: 0; z-index: 5; display: flex; gap: 8px; padding: 8px 12px; background: rgba(247,250,246,.94); }
.quick-row text { padding: 6px 10px; border: 1px solid #d7e3d7; border-radius: 999px; background: #fff; color: #4d6d58; font-size: 10px; }
.chat-input-bar { position: fixed; right: 0; bottom: 0; left: 0; z-index: 5; padding: 9px 12px calc(9px + env(safe-area-inset-bottom)); border-top: 1px solid #dfe7df; background: rgba(255,255,255,.96); }
.input-wrap { display: flex; align-items: center; gap: 8px; padding: 4px 4px 4px 14px; border-radius: 999px; background: #edf2ec; }
.msg-input { min-width: 0; flex: 1; height: 36px; font-size: 13px; }
.send-btn { margin: 0; padding: 8px 17px; border: 0; border-radius: 999px; background: #bdc9c0; color: #fff; font-size: 12px; }
.send-btn.active { background: #3d7b57; }
.archived-bar { position: fixed; right: 0; bottom: 0; left: 0; padding: 15px; border-top: 1px solid #dfe7df; background: #f8faf7; color: var(--mobile-muted); font-size: 12px; text-align: center; }
.bottom-anchor { height: 2px; }
</style>
