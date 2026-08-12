<template>
  <view class="mobile-page profile-page">
    <view class="profile-hero">
      <view class="profile-row">
        <view class="avatar">{{ (user.nickname || auth.nickname || '沧').slice(0, 1) }}</view>
        <view class="profile-main">
          <text class="profile-name">{{ user.nickname || auth.nickname || (isBooster ? '打手' : '玩家') }}</text>
          <text class="profile-role">{{ isBooster ? '打手工作账号' : '玩家账号' }}</text>
        </view>
        <view v-if="isBooster" :class="['status-pill', { online: user.boosterStatus === 'idle' }]" @tap="toggleStatus">
          {{ user.boosterStatus === 'idle' ? '可接单' : '暂停接单' }}
        </view>
      </view>

      <view v-if="isBooster" class="money-grid">
        <view><text class="money-value">¥{{ settledIncome }}</text><text class="money-label">累计已结算</text></view>
        <view><text class="money-value">¥{{ pendingIncome }}</text><text class="money-label">待确认金额</text></view>
        <view><text class="money-value">{{ completedOrders }}</text><text class="money-label">完成订单</text></view>
      </view>
      <view v-else class="money-grid">
        <view><text class="money-value">¥{{ user.balance || 0 }}</text><text class="money-label">账户余额</text></view>
        <view><text class="money-value">{{ myOrders.length }}</text><text class="money-label">全部订单</text></view>
        <view><text class="money-value">{{ activeOrders.length }}</text><text class="money-label">进行中</text></view>
      </view>
    </view>

    <template v-if="isBooster">
      <view class="section-title"><text>工作台</text><text class="section-note">收入由后台统一结算</text></view>
      <view class="menu-card">
        <view class="menu-item" @tap="goWork"><view class="menu-icon green">单</view><view><text>订单池与我的订单</text><text>接单后自动进入我的订单</text></view><text class="arrow">→</text></view>
        <navigator url="/pages/messages/messages" class="menu-item"><view class="menu-icon gold">聊</view><view><text>订单聊天</text><text>传递房间号和对局信息</text></view><text class="arrow">→</text></navigator>
      </view>
      <view class="settlement-note">平台不提供打手自助提现入口。已完成订单由客服/管理员核验并统一结算，金额在这里展示。</view>
    </template>

    <template v-else>
      <view class="section-title"><text>我的订单</text><text class="section-note">{{ activeOrders.length ? '有订单正在处理' : '暂无进行中订单' }}</text></view>
      <navigator v-if="activeOrders[0]" :url="`/pages/boss/order-detail?id=${activeOrders[0].id}`" class="mobile-card active-order">
        <view><text class="active-name">{{ activeOrders[0].serviceName }}</text><text class="active-sub">订单 #{{ activeOrders[0].id }} · {{ statusText[activeOrders[0].status] }}</text></view><text class="active-price">¥{{ activeOrders[0].amount }}</text>
      </navigator>
      <view class="menu-card boss-menu">
        <navigator url="/pages/boss/order-history" class="menu-item"><view class="menu-icon green">单</view><view><text>全部订单</text><text>查看订单进度与历史记录</text></view><text class="arrow">→</text></navigator>
        <navigator url="/pages/messages/messages" class="menu-item"><view class="menu-icon gold">聊</view><view><text>订单聊天</text><text>打手接单后传递房间号</text></view><text class="arrow">→</text></navigator>
        <navigator url="/pages/boss/contact" class="menu-item"><view class="menu-icon cream">客</view><view><text>特殊需求客服</text><text>仅处理未上架服务与异常订单</text></view><text class="arrow">→</text></navigator>
        <navigator url="/pages/boss/settings" class="menu-item"><view class="menu-icon gray">设</view><view><text>账号设置</text><text>修改密码与基础信息</text></view><text class="arrow">→</text></navigator>
      </view>
    </template>

    <button class="logout-btn" @tap="handleLogout">退出登录</button>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from "vue"
import { onShow } from "@dcloudio/uni-app"
import { useAuthStore } from "@/store/auth"
import { getMyOrders } from "@/api/orders"
import { getUserProfile, toggleBoosterStatus } from "@/api/users"

const auth = useAuthStore()
const user = ref<any>({})
const myOrders = ref<any[]>([])
const isBooster = computed(() => auth.userRole === 'booster' && !auth.isGuest)
const activeOrders = computed(() => myOrders.value.filter((o) => ['pending_payment', 'pending', 'assigned', 'in_progress', 'submitted', 'refund_pending'].includes(o.status)))
const settledIncome = computed(() => sumOrders(['done', 'settled']))
const pendingIncome = computed(() => sumOrders(['submitted']))
const completedOrders = computed(() => myOrders.value.filter((o) => ['done', 'settled'].includes(o.status)).length)
const statusText: Record<string, string> = { pending_payment: '待支付', pending: '待接单', assigned: '待开始', in_progress: '服务中', submitted: '待确认', refund_pending: '退款中', done: '已完成', settled: '已结算' }

function sumOrders(statuses: string[]) { return myOrders.value.filter((o) => statuses.includes(o.status)).reduce((sum, o) => sum + Number(o.amount || 0), 0).toFixed(2) }
function goWork() { uni.switchTab({ url: '/pages/discover/discover' }) }

async function load() {
  try { user.value = await getUserProfile() || {} } catch { user.value = {} }
  try { const result: any = await getMyOrders(1, 100); myOrders.value = result.records || [] } catch { myOrders.value = [] }
}

async function toggleStatus() {
  const status = user.value.boosterStatus === 'idle' ? 'busy' : 'idle'
  try { await toggleBoosterStatus(status); user.value.boosterStatus = status; uni.showToast({ title: status === 'idle' ? '已开启接单' : '已暂停接单', icon: 'none' }) }
  catch (e: any) { uni.showToast({ title: e?.data?.message || '切换失败', icon: 'none' }) }
}

function handleLogout() {
  uni.showModal({ title: '退出登录', content: '确定退出当前账号吗？', confirmColor: '#c34c43', success: (res) => { if (res.confirm) { auth.logout(); uni.reLaunch({ url: '/pages/auth/login' }) } } })
}

onShow(load)
</script>

<style scoped>
.profile-page { padding-top: 16px; }
.profile-hero { padding: 20px 18px; border-radius: 24px; background: linear-gradient(145deg, #dfeee2, #fff2d7); box-shadow: 0 16px 38px rgba(44,112,77,.12); }
.profile-row { display: flex; align-items: center; gap: 12px; }
.avatar { width: 58px; height: 58px; flex-shrink: 0; border-radius: 19px; background: #2c704d; color: #fff; font-size: 22px; font-weight: 900; line-height: 58px; text-align: center; }
.profile-main { min-width: 0; flex: 1; }
.profile-name { color: #173e2a; font-size: 20px; font-weight: 950; }
.profile-role { display: block; margin-top: 4px; color: #64796a; font-size: 11px; }
.status-pill { padding: 7px 10px; border-radius: 999px; background: #e0e3df; color: #68726b; font-size: 10px; font-weight: 800; }
.status-pill.online { background: #d4ebd9; color: #286543; }
.money-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin-top: 20px; }
.money-grid view { padding: 10px 6px; border-radius: 14px; background: rgba(255,255,255,.65); text-align: center; }
.money-value { display: block; color: #235b3e; font-size: 17px; font-weight: 900; }
.money-label { display: block; margin-top: 3px; color: #718076; font-size: 9px; }
.menu-card { overflow: hidden; border: 1px solid #dfe8df; border-radius: 19px; background: rgba(255,255,255,.94); }
.menu-item { display: flex; align-items: center; gap: 12px; padding: 14px; border-bottom: 1px solid #edf1ed; }
.menu-item:last-child { border-bottom: 0; }
.menu-icon { width: 36px; height: 36px; flex-shrink: 0; border-radius: 12px; font-size: 12px; font-weight: 900; line-height: 36px; text-align: center; }
.menu-icon.green { background: #e0eee3; color: #2c704d; }.menu-icon.gold, .menu-icon.cream { background: #fff2d9; color: #9b6a2f; }.menu-icon.gray { background: #edf0ed; color: #647169; }
.menu-item view:nth-child(2) { min-width: 0; flex: 1; }
.menu-item view:nth-child(2) text:first-child { color: var(--mobile-ink); font-size: 14px; font-weight: 850; }
.menu-item view:nth-child(2) text:last-child { display: block; margin-top: 3px; color: var(--mobile-muted); font-size: 10px; }
.arrow { color: #95a299; }
.settlement-note { margin-top: 12px; padding: 13px; border-radius: 14px; background: #fff8e9; color: #806d55; font-size: 11px; line-height: 1.6; }
.active-order { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 12px; }
.active-name { color: var(--mobile-ink); font-size: 14px; font-weight: 900; }
.active-sub { display: block; margin-top: 4px; color: var(--mobile-muted); font-size: 10px; }
.active-price { color: var(--mobile-brand); font-size: 19px; font-weight: 900; }
.logout-btn { width: 100%; margin-top: 20px; border: 0; background: transparent; color: #b74d46; font-size: 13px; }
</style>
