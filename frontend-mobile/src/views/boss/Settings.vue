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
      newPassword: newPassword.value
    })
    showToast('密码修改成功')
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
      confirmButtonColor: '#ef4444',
    })
    auth.logout()
    router.push('/login')
  } catch { }
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="设置" left-arrow @click-left="$router.back()" fixed placeholder />

    <div class="content">
      <!-- 账号安全 -->
      <div class="section">
        <div class="section-title">账号安全</div>
        <van-cell-group inset>
          <van-cell title="修改密码" is-link @click="showPwdDialog = true">
            <template #icon><span class="cell-icon">🔒</span></template>
          </van-cell>
          <van-cell title="绑定手机" :value="auth.userId ? '已绑定' : '未绑定'" />
        </van-cell-group>
      </div>

      <!-- 通知设置 -->
      <div class="section">
        <div class="section-title">通知设置</div>
        <van-cell-group inset>
          <van-cell title="订单消息通知" center>
            <template #icon><span class="cell-icon">🔔</span></template>
            <template #right-icon><van-switch :model-value="true" active-color="#6366f1" size="22px" /></template>
          </van-cell>
          <van-cell title="系统公告通知" center>
            <template #icon><span class="cell-icon">📢</span></template>
            <template #right-icon><van-switch :model-value="true" active-color="#6366f1" size="22px" /></template>
          </van-cell>
        </van-cell-group>
      </div>

      <!-- 其他 -->
      <div class="section">
        <div class="section-title">其他</div>
        <van-cell-group inset>
          <van-cell title="关于沧月电竞" is-link value="v1.0.0">
            <template #icon><span class="cell-icon">📱</span></template>
          </van-cell>
          <van-cell title="用户协议" is-link>
            <template #icon><span class="cell-icon">📄</span></template>
          </van-cell>
          <van-cell title="隐私政策" is-link>
            <template #icon><span class="cell-icon">🛡️</span></template>
          </van-cell>
          <van-cell title="清理缓存" is-link @click="showToast('缓存已清理')">
            <template #icon><span class="cell-icon">🗑️</span></template>
          </van-cell>
        </van-cell-group>
      </div>

      <!-- 退出 -->
      <div class="logout-section">
        <van-button round block type="danger" plain @click="handleLogout">退出登录</van-button>
      </div>
    </div>

    <!-- 修改密码弹窗 -->
    <van-dialog
      v-model:show="showPwdDialog"
      title="修改密码"
      show-cancel-button
      :confirm-button-text="changingPwd ? '修改中...' : '确认修改'"
      :confirm-button-color="'#6366f1'"
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
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f8fafc;
}

.content {
  padding-bottom: 40px;
}
.section {
  margin-top: 16px;
}
.section-title {
  font-size: 13px;
  color: #94a3b8;
  font-weight: 500;
  padding: 0 20px 8px;
}
.cell-icon {
  font-size: 18px;
  margin-right: 4px;
}

.logout-section {
  padding: 32px 20px;
}

.pwd-form {
  padding: 8px 0;
}
</style>
