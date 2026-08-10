<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const orders = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const statusFilter = ref('')
const loading = ref(false)

const statusOptions = [
  { label: '全部', value: '' }, { label: '待支付', value: 'pending_payment' },
  { label: '待接单', value: 'pending' },
  { label: '进行中', value: 'in_progress' }, { label: '待审核', value: 'submitted' },
  { label: '已确认', value: 'done' }, { label: '已结算', value: 'settled' },
  { label: '退款中', value: 'refund_pending' },
  { label: '已取消', value: 'cancelled' }, { label: '争议中', value: 'disputed' }
]

function statusLabel(s: string) {
  const m: Record<string, string> = { pending_payment: '待支付', pending: '待接单', assigned: '已接单', in_progress: '进行中',
    submitted: '待审核', done: '已确认', settled: '已结算', refund_pending: '退款中', cancelled: '已取消', disputed: '争议中' }
  return m[s] || s
}

async function load() {
  loading.value = true
  try {
    const res: any = await request.get('/admin/orders', { params: { page: page.value, size: 10, status: statusFilter.value || undefined } })
    orders.value = res.records || []; total.value = res.total || 0
  } finally { loading.value = false }
}

async function assignOrder(orderId: number) {
  try {
    const { value } = await ElMessageBox.prompt('输入陪陪ID', '派单', { inputType: 'number' })
    await request.post(`/admin/orders/${orderId}/assign`, null, { params: { boosterId: Number(value) } })
    ElMessage.success('派单成功'); load()
  } catch { }
}

async function confirmOrder(orderId: number) {
  try {
    await ElMessageBox.confirm('确认该订单达标？', '确认', { type: 'warning' })
    await request.post(`/admin/orders/${orderId}/force-done`)
    ElMessage.success('已确认'); load()
  } catch { }
}

async function disputeOrder(orderId: number) {
  try {
    await ElMessageBox.confirm('将该订单标记为争议？', '确认', { type: 'warning' })
    await request.post(`/admin/orders/${orderId}/dispute`)
    ElMessage.success('已标记'); load()
  } catch { }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="mb-4 flex gap-2 flex-wrap">
      <el-tag v-for="s in statusOptions" :key="s.value"
        :type="statusFilter === s.value ? 'primary' : 'info'"
        class="cursor-pointer" @click="statusFilter = s.value; page = 1; load()">{{ s.label }}</el-tag>
    </div>
    <el-table :data="orders" v-loading="loading" stripe>
      <el-table-column prop="id" label="订单号" width="80" />
      <el-table-column prop="bossId" label="老板ID" width="80" />
      <el-table-column prop="boosterId" label="陪陪ID" width="80" />
      <el-table-column prop="amount" label="金额" width="90" />
      <el-table-column prop="isQualified" label="达标" width="70">
        <template #default="{ row }">{{ row.isQualified === null ? '-' : row.isQualified ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><el-tag size="small">{{ statusLabel(row.status) }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="130">
        <template #default="{ row }">{{ row.createdAt?.replace('T', ' ').substring(0, 16) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="260">
        <template #default="{ row }">
          <el-button v-if="row.status === 'pending'" size="small" type="primary" @click="assignOrder(row.id)">派单</el-button>
          <el-button v-if="row.status === 'submitted'" size="small" type="success" @click="confirmOrder(row.id)">确认达标</el-button>
          <el-button v-if="row.status === 'done'" size="small" type="warning" @click="disputeOrder(row.id)">标记争议</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="mt-4 text-right">
      <el-pagination v-model:current-page="page" :total="total" :page-size="10" layout="prev, pager, next" @current-change="load" />
    </div>
  </div>
</template>
