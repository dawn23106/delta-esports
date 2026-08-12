<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../api/request'

const gifts = ref<any[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    // 后端没有全量礼物查询接口，暂且空着
    const res: any = await request.get('/admin/gifts', { params: { page: 1, size: 50 } })
    gifts.value = res || []
  } catch { gifts.value = [] } finally { loading.value = false }
}

onMounted(load)
</script>

<template>
  <div>
    <el-table :data="gifts" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="bossId" label="送礼人ID" width="90" />
      <el-table-column prop="boosterId" label="收礼人ID" width="90" />
      <el-table-column prop="giftName" label="礼物" width="120" />
      <el-table-column prop="amount" label="金额" width="90" />
      <el-table-column prop="message" label="留言" min-width="200" />
      <el-table-column prop="createdAt" label="时间" width="140">
        <template #default="{ row }">{{ row.createdAt?.replace('T', ' ').substring(0, 16) }}</template>
      </el-table-column>
    </el-table>
    <div v-if="gifts.length === 0" class="text-center py-10 text-gray-400">暂无礼物记录</div>
  </div>
</template>
