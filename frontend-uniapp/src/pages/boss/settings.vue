<template>
  <view class="mobile-page">
    <!-- 修改密码 -->
    <view class="mobile-card setting-card" @tap="showPassword = true">
      <view>
        <text class="setting-label">修改密码</text>
        <text class="setting-hint">建议使用字母、数字组合</text>
      </view>
      <text class="setting-arrow">→</text>
    </view>

    <!-- 通知设置 -->
    <view class="mobile-card setting-card">
      <view>
        <text class="setting-label">订单通知</text>
        <text class="setting-hint">接单、完成等关键节点提醒</text>
      </view>
      <switch :checked="notifyOrder" @change="notifyOrder = !notifyOrder" color="#3157ff" />
    </view>

    <view class="mobile-card setting-card">
      <view>
        <text class="setting-label">消息通知</text>
        <text class="setting-hint">聊天消息推送</text>
      </view>
      <switch :checked="notifyMsg" @change="notifyMsg = !notifyMsg" color="#3157ff" />
    </view>

    <!-- 关于 -->
    <view class="mobile-card setting-card">
      <text class="setting-label">关于沧月电竞</text>
      <text class="setting-hint" style="margin-top: 4px;">版本 1.0.0</text>
    </view>

    <!-- 缓存 -->
    <view class="mobile-card setting-card" @tap="clearCache">
      <text class="setting-label">清除缓存</text>
      <text class="setting-arrow">→</text>
    </view>

    <!-- 修改密码弹层 -->
    <view v-if="showPassword" class="action-sheet-overlay" @tap="showPassword = false">
      <view class="action-sheet-panel" @tap.stop>
        <view class="action-sheet-header">修改密码</view>
        <view class="sheet">
          <view class="field">
            <text class="field-label">旧密码</text>
            <input v-model="oldPassword" type="password" placeholder="请输入旧密码" class="field-input" />
          </view>
          <view class="field">
            <text class="field-label">新密码</text>
            <input v-model="newPassword" type="password" placeholder="至少 6 位" class="field-input" />
          </view>
          <button class="btn-primary" @tap="changePassword">确认修改</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue"
import { changePassword as changePwd } from "@/api/users"
import { showModal } from "@/utils/nav"

const showPassword = ref(false)
const oldPassword = ref("")
const newPassword = ref("")
const notifyOrder = ref(true)
const notifyMsg = ref(true)

async function changePassword() {
  if (!oldPassword.value || !newPassword.value) {
    uni.showToast({ title: '请填写完整', icon: 'none' })
    return
  }
  if (newPassword.value.length < 6) {
    uni.showToast({ title: '新密码至少 6 位', icon: 'none' })
    return
  }
  try {
    await changePwd(oldPassword.value, newPassword.value)
    uni.showToast({ title: '密码修改成功', icon: 'success' })
    showPassword.value = false
  } catch (e: any) {
    uni.showToast({ title: e?.data?.message || '修改失败', icon: 'error' })
  }
}

function clearCache() {
  uni.clearStorageSync()
  uni.showToast({ title: '缓存已清除', icon: 'success' })
}
</script>

<style scoped>
.setting-card {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 10px;
}

.setting-label { font-size: 15px; font-weight: 750; color: var(--mobile-ink); display: block; }
.setting-hint { font-size: 12px; color: var(--mobile-muted); display: block; margin-top: 2px; }
.setting-arrow { color: var(--mobile-faint); font-size: 16px; }

.sheet { padding: 0 16px 24px; display: flex; flex-direction: column; gap: 12px; }

.field {
  border-radius: 14px; background: #f8fafc; padding: 12px 14px;
  border: 1px solid var(--mobile-line);
}

.field-label { display: block; font-size: 12px; color: var(--mobile-muted); font-weight: 650; margin-bottom: 4px; }
.field-input { font-size: 15px; color: var(--mobile-ink); background: transparent; height: 28px; }
</style>
