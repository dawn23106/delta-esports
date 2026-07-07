<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const orders = ref<any[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const res: any = await request.get('/admin/orders', { params: { page: 1, size: 50, status: 'done' } })
    orders.value = res.records || []
  } finally { loading.value = false }
}

function timeLeft(doneAt: string) {
  if (!doneAt) return '-'
  const done = new Date(doneAt)
  const settle = new Date(done.getTime() + 3 * 24 * 60 * 60 * 1000)
  const now = new Date()
  const diff = settle.getTime() - now.getTime()
  if (diff <= 0) return '可结算'
  const hours = Math.floor(diff / (1000 * 60 * 60))
  return `${Math.floor(hours / 24)}天${hours % 24}小时`
}

async function settleOrder(id: number) {
  try {
    await ElMessageBox.confirm('提前结算该订单？', '确认', { type: 'warning' })
    await request.put(`/admin/orders/${id}/settle`) // 后端需要添加此接口
    ElMessage.success('已结算'); load()
  } catch { }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="mb-2 text-sm text-gray-500">待结算订单（确认达标后等待3天）</div>
    <el-table :data="orders" v-loading="loading" stripe>
      <el-table-column prop="id" label="订单号" width="80" />
      <el-table-column prop="bossId" label="老板ID" width="80" />
      <el-table-column prop="boosterId" label="陪陪ID" width="80" />
      <el-table-column prop="amount" label="金额" width="90" />
      <el-table-column prop="isQualified" label="达标" width="70">
        <template #default="{ row }">{{ row.isQualified ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="等待时间" width="130">
        <template #default="{ row }"><el-tag :type="timeLeft(row.doneAt) === '可结算' ? 'success' : 'warning'">{{ timeLeft(row.doneAt) }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="doneAt" label="确认时间" width="140">
        <template #default="{ row }">{{ row.doneAt?.replace('T', ' ').substring(0, 16) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="settleOrder(row.id)">提前结算</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>
