<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../api/request'

const users = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const res: any = await request.get('/admin/bosses', { params: { page: page.value, size: 10 } })
    users.value = res.records || []; total.value = res.total || 0
  } finally { loading.value = false }
}

function starLabel(level: number) { return `Lv.${level}` }

onMounted(load)
</script>

<template>
  <div>
    <el-table :data="users" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="nickname" label="昵称" width="130" />
      <el-table-column label="星享等级" width="100">
        <template #default="{ row }"><el-tag type="warning">{{ starLabel(row.starLevel) }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="totalSpent" label="累计消费" width="100" />
      <el-table-column prop="createdAt" label="注册时间" width="140">
        <template #default="{ row }">{{ row.createdAt?.replace('T', ' ').substring(0, 16) }}</template>
      </el-table-column>
    </el-table>
    <div class="mt-4 text-right">
      <el-pagination v-model:current-page="page" :total="total" :page-size="10" layout="prev, pager, next" @current-change="load" />
    </div>
  </div>
</template>
