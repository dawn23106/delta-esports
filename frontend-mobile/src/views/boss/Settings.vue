<script setup lang="ts">
import { ref } from 'vue'
import { useAuthStore } from '../../store/auth'
import { useRouter } from 'vue-router'
import request from '../../api/request'
import { showToast, showDialog } from 'vant'

const auth = useAuthStore()
const router = useRouter()

const showPwdDialog = ref(false)
const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const changingPwd = ref(false)
const orderNotify = ref(true)
const systemNotify = ref(true)

async function changePassword() {
  if (!oldPassword.value || !newPassword.value || !confirmPassword.value) {
    showToast('请填写完整信息')
    return
  }
  if (newPassword.value.length < 6) {
    showToast('新密码至少6位')
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    showToast('两次密码不一致')
    return
  }
  changingPwd.value = true
  try {
    await request.put('/users/me/password', {
      oldPassword: oldPassword.value,
      newPassword: newPassword.value,
    })
    showToast({ message: '密码修改成功', icon: 'success' })
    showPwdDialog.value = false
    oldPassword.value = ''
    newPassword.value = ''
    confirmPassword.value = ''
  } catch (e: any) {
    showToast(e?.response?.data?.message || '修改失败')
  } finally {
    changingPwd.value = false
  }
}

async function handleLogout() {
  try {
    await showDialog({
      title: '退出登录',
      message: '确定要退出当前账号吗？',
      confirmButtonColor: '#f04438',
    })
    auth.logout()
    router.push('/login')
  } catch { }
}
</script>

<template>
  <main class="mobile-page no-tabbar">
    <van-nav-bar title="设置" left-arrow @click-left="$router.back()" />

    <div class="section-title">账号安全</div>
    <section class="mobile-card setting-group">
      <button type="button" class="setting-row" @click="showPwdDialog = true">
        <van-icon name="lock" class="setting-icon" />
        <span class="setting-label">修改密码</span>
        <van-icon name="arrow" class="setting-arrow" />
      </button>
      <div class="setting-row">
        <van-icon name="phone" class="setting-icon" />
        <span class="setting-label">绑定手机</span>
        <span class="setting-value">{{ auth.userId ? '已绑定' : '未绑定' }}</span>
      </div>
    </section>

    <div class="section-title">通知设置</div>
    <section class="mobile-card setting-group">
      <div class="setting-row">
        <van-icon name="bell" class="setting-icon" />
        <span class="setting-label">订单消息通知</span>
        <van-switch v-model="orderNotify" active-color="#3157ff" size="22px" />
      </div>
      <div class="setting-row last">
        <van-icon name="volume" class="setting-icon" />
        <span class="setting-label">系统公告通知</span>
        <van-switch v-model="systemNotify" active-color="#3157ff" size="22px" />
      </div>
    </section>

    <div class="section-title">其他</div>
    <section class="mobile-card setting-group">
      <button type="button" class="setting-row">
        <van-icon name="info-o" class="setting-icon" />
        <span class="setting-label">关于沧月电竞</span>
        <span class="setting-value">v1.0.0</span>
        <van-icon name="arrow" class="setting-arrow" />
      </button>
      <button type="button" class="setting-row">
        <van-icon name="description" class="setting-icon" />
        <span class="setting-label">用户协议</span>
        <van-icon name="arrow" class="setting-arrow" />
      </button>
      <button type="button" class="setting-row">
        <van-icon name="shield-o" class="setting-icon" />
        <span class="setting-label">隐私政策</span>
        <van-icon name="arrow" class="setting-arrow" />
      </button>
      <button type="button" class="setting-row last" @click="showToast({ message: '缓存已清理', icon: 'success' })">
        <van-icon name="delete-o" class="setting-icon" />
        <span class="setting-label">清理缓存</span>
        <van-icon name="arrow" class="setting-arrow" />
      </button>
    </section>

    <button type="button" class="logout-btn" @click="handleLogout">退出登录</button>

    <van-dialog
      v-model:show="showPwdDialog"
      title="修改密码"
      show-cancel-button
      :confirm-button-text="changingPwd ? '修改中...' : '确认修改'"
      confirm-button-color="#3157ff"
      :before-close="(action: string) => {
        if (action === 'confirm') { changePassword(); return false }
        return true
      }"
    >
      <div class="pwd-form">
        <van-field v-model="oldPassword" type="password" label="原密码" placeholder="输入原密码" />
        <van-field v-model="newPassword" type="password" label="新密码" placeholder="至少6位新密码" />
        <van-field v-model="confirmPassword" type="password" label="确认密码" placeholder="再次输入新密码" />
      </div>
    </van-dialog>
  </main>
</template>

<style scoped>
.setting-group {
  padding: 0;
  overflow: hidden;
  margin-bottom: 6px;
}

.setting-row {
  width: 100%;
  border: 0;
  border-bottom: 1px solid var(--mobile-line);
  background: transparent;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 15px 16px;
  color: var(--mobile-ink);
  font-size: 14px;
  font-weight: 650;
}

.setting-row:last-child,
.setting-row.last {
  border-bottom: 0;
}

.setting-icon {
  color: var(--mobile-brand);
  font-size: 20px;
  flex: 0 0 auto;
}

.setting-label {
  flex: 1;
  text-align: left;
  color: var(--mobile-ink);
  font-size: 14px;
  font-weight: 650;
}

.setting-value {
  color: var(--mobile-faint);
  font-size: 13px;
  font-weight: 500;
}

.setting-arrow {
  color: var(--mobile-faint);
  font-size: 14px;
  flex: 0 0 auto;
}

.logout-btn {
  width: 100%;
  margin-top: 22px;
  border: 1px solid rgba(240, 68, 56, .24);
  border-radius: 18px;
  background: rgba(255, 255, 255, .9);
  color: var(--mobile-danger);
  font-size: 15px;
  font-weight: 800;
  padding: 14px;
}

.pwd-form {
  padding: 8px 0;
}
</style>
