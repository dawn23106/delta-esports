<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../api/request'

const boosters = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const res: any = await request.get('/admin/boosters', { params: { page: page.value, size: 10 } })
    boosters.value = res.records || []; total.value = res.total || 0
  } finally { loading.value = false }
}

onMounted(load)
</script>

<template>
  <div>
    <el-table :data="boosters" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="nickname" label="昵称" width="120" />
      <el-table-column prop="boosterStatus" label="状态" width="80">
        <template #default="{ row }"><el-tag :type="row.boosterStatus === 'idle' ? 'success' : 'warning'" size="small">{{ row.boosterStatus === 'idle' ? '空闲' : '忙碌' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="rating" label="评分" width="70" />
      <el-table-column prop="totalOrders" label="接单数" width="80" />
    </el-table>
    <div class="mt-4 text-right">
      <el-pagination v-model:current-page="page" :total="total" :page-size="10" layout="prev, pager, next" @current-change="load" />
    </div>
  </div>
</template>
