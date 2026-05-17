<template>
  <div class="page">
    <h3>订单管理</h3>
    <el-row :gutter="10" style="margin-bottom:16px">
      <el-col :span="6">
        <el-select v-model="filterStatus" placeholder="状态筛选" clearable @change="load">
          <el-option v-for="(v,k) in statusMap" :key="k" :label="v" :value="k" />
        </el-select>
      </el-col>
      <el-col :span="4">
        <el-button type="primary" @click="showCreate = true">创建订单</el-button>
      </el-col>
    </el-row>
    <el-table :data="orders" border stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="game" label="游戏" width="120" />
      <el-table-column prop="detail" label="要求" />
      <el-table-column prop="price" label="价格" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="{row}"><el-tag :type="tagType(row.status)">{{ statusMap[row.status] }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="customerNickname" label="客户" width="100" />
      <el-table-column prop="boosterNickname" label="打手" width="100" />
      <el-table-column label="操作" width="220">
        <template #default="{row}">
          <el-button size="small" v-if="row.status==='pending'" type="primary" @click="openAssign(row)">派单</el-button>
          <el-button size="small" v-if="row.status!=='completed'&&row.status!=='cancelled'" type="danger" @click="doCancel(row.id)">取消</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :total="total" :page-size="20" layout="prev,pager,next" @current-change="load" style="margin-top:16px;justify-content:center" />

    <!-- 创建订单对话框 -->
    <el-dialog v-model="showCreate" title="手动创建订单" width="400px">
      <el-form @submit.prevent="doCreate">
        <el-form-item label="客户ID"><el-input v-model="create.customerId" type="number" /></el-form-item>
        <el-form-item label="打手ID"><el-input v-model="create.boosterId" type="number" /></el-form-item>
        <el-form-item label="游戏"><el-input v-model="create.game" /></el-form-item>
        <el-form-item label="要求"><el-input v-model="create.detail" /></el-form-item>
        <el-form-item label="价格"><el-input v-model="create.price" type="number" /></el-form-item>
        <el-button type="primary" native-type="submit">创建</el-button>
      </el-form>
    </el-dialog>

    <!-- 派单对话框 -->
    <el-dialog v-model="showAssign" title="派单" width="350px">
      <el-select v-model="assignBoosterId" placeholder="选择打手" style="width:100%">
        <el-option v-for="b in boosters" :key="b.id" :label="`${b.nickname} (${b.phone})`" :value="b.id" />
      </el-select>
      <el-button type="primary" style="margin-top:10px;width:100%" @click="doAssign">确认派单</el-button>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getOrders, createOrder, assignOrder, cancelOrder, getBoosters } from '../api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

const orders = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)
const filterStatus = ref('')

const statusMap: Record<string, string> = { pending:'待接单', assigned:'已接单', in_progress:'进行中', completed:'已完成', cancelled:'已取消' }

function tagType(s: string) {
  const t: Record<string, string> = { pending:'info', assigned:'warning', in_progress:'', completed:'success', cancelled:'danger' }
  return t[s] || 'info'
}

async function load() {
  loading.value = true
  try {
    const res = await getOrders({ status: filterStatus.value || undefined, page: page.value, pageSize: 20 })
    orders.value = res.data.list
    total.value = res.data.total
  } finally { loading.value = false }
}

onMounted(load)

// 创建
const showCreate = ref(false)
const create = ref({ customerId: '', boosterId: '', game: '', detail: '', price: '' })
async function doCreate() {
  await createOrder({
    customerId: Number(create.value.customerId),
    boosterId: Number(create.value.boosterId),
    game: create.value.game,
    detail: create.value.detail,
    price: Number(create.value.price)
  })
  ElMessage.success('创建成功')
  showCreate.value = false
  load()
}

// 派单
const showAssign = ref(false)
const assignBoosterId = ref<number | null>(null)
const assignOrderId = ref(0)
const boosters = ref<any[]>([])

async function openAssign(order: any) {
  assignOrderId.value = order.id
  const res = await getBoosters()
  boosters.value = res.data
  showAssign.value = true
}

async function doAssign() {
  if (!assignBoosterId.value) return ElMessage.warning('请选择打手')
  await assignOrder(assignOrderId.value, assignBoosterId.value)
  ElMessage.success('派单成功')
  showAssign.value = false
  load()
}

// 取消
async function doCancel(id: number) {
  try {
    await ElMessageBox.confirm('确认取消该订单？', '提示', { type: 'warning' })
    await cancelOrder(id)
    ElMessage.success('已取消')
    load()
  } catch {}
}
</script>
