<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const boosters = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)

const levelLabel: Record<string, string> = { entertainment: '娱乐', tech: '技术', top: '顶尖' }

async function load() {
  loading.value = true
  try {
    const res: any = await request.get('/admin/boosters', { params: { page: page.value, size: 10 } })
    boosters.value = res.records || []; total.value = res.total || 0
  } finally { loading.value = false }
}

async function setLevel(id: number) {
  try {
    const { value } = await ElMessageBox.prompt('等级: entertainment / tech / top', '设置陪陪等级', { inputValue: 'tech' })
    if (value) { await request.put(`/admin/users/${id}/booster-level`, null, { params: { level: value } })
    ElMessage.success('已更新'); load() }
  } catch { }
}

onMounted(load)
</script>

<template>
  <div>
    <el-table :data="boosters" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="nickname" label="昵称" width="120" />
      <el-table-column label="等级" width="100">
        <template #default="{ row }">{{ levelLabel[row.boosterLevel] || row.boosterLevel }}</template>
      </el-table-column>
      <el-table-column prop="boosterStatus" label="状态" width="80">
        <template #default="{ row }"><el-tag :type="row.boosterStatus === 'idle' ? 'success' : 'warning'" size="small">{{ row.boosterStatus === 'idle' ? '空闲' : '忙碌' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="rating" label="评分" width="70" />
      <el-table-column prop="orderCount" label="接单数" width="80" />
      <el-table-column prop="totalEarned" label="收入" width="100" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }"><el-button size="small" @click="setLevel(row.id)">设等级</el-button></template>
      </el-table-column>
    </el-table>
    <div class="mt-4 text-right">
      <el-pagination v-model:current-page="page" :total="total" :page-size="10" layout="prev, pager, next" @current-change="load" />
    </div>
  </div>
</template>
