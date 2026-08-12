<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const settlements = ref<any[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const res: any = await request.get('/admin/settlements', { params: { page: 1, size: 50 } })
    settlements.value = res.records || []
  } finally { loading.value = false }
}

async function settleOrder(id: number) {
  try {
    await ElMessageBox.confirm('提前结算该订单？', '确认', { type: 'warning' })
    await request.put(`/admin/settlements/${id}`, null, { params: { status: 'settled', remark: '管理员确认结算' } })
    ElMessage.success('已结算'); load()
  } catch { }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="mb-2 text-sm text-gray-500">订单结算记录</div>
    <el-table :data="settlements" v-loading="loading" stripe>
      <el-table-column prop="orderId" label="订单号" width="100" />
      <el-table-column prop="boosterId" label="陪陪ID" width="80" />
      <el-table-column prop="amount" label="金额" width="90" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }"><el-tag :type="row.status === 'settled' ? 'success' : 'warning'">{{ row.status === 'settled' ? '已结算' : '待结算' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="160">
        <template #default="{ row }">{{ row.createdAt?.replace('T', ' ').substring(0, 16) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button v-if="row.status !== 'settled'" size="small" type="primary" @click="settleOrder(row.id)">确认结算</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>
