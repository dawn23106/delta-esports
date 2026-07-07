<script setup lang="ts">
import { ref, onMounted } from 'vue'

import { getMyOrders } from '../../api/orders'


const activeTab = ref('')
const orders = ref<any[]>([])
const tabs = [
  { label: '全部', value: '' }, { label: '待接单', value: 'pending' }, { label: '进行中', value: 'in_progress' },
  { label: '待审核', value: 'completed' }, { label: '已结算', value: 'settled' }, { label: '已取消', value: 'cancelled' }
]

function statusLabel(s: string) {
  return { pending: '待接单', assigned: '已接单', in_progress: '进行中', completed: '待审核',
    done: '已确认', settled: '已结算', cancelled: '已取消', disputed: '争议中' }[s] || s
}

async function loadOrders() {
  try {
    const res: any = await getMyOrders(1, 50, activeTab.value || undefined)
    orders.value = res.records || []
  } catch { }
}

onMounted(loadOrders)
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <van-nav-bar title="订单记录" left-arrow @click-left="$router.back()" fixed placeholder />
    <van-tabs v-model:active="activeTab" @change="loadOrders" sticky>
      <van-tab v-for="t in tabs" :key="t.value" :title="t.label" :name="t.value" />
    </van-tabs>
    <div class="px-4 mt-3 space-y-3 pb-10">
      <div v-for="o in orders" :key="o.id" class="bg-white rounded-xl shadow-sm p-4 cursor-pointer active:bg-gray-50"
        @click="$router.push(`/boss/order/${o.id}`)">
        <div class="flex justify-between items-center">
          <span class="font-medium text-sm">订单 #{{ o.id }}</span>
          <van-tag :type="o.status === 'in_progress' ? 'primary' : o.status === 'settled' ? 'success' : 'default'">
            {{ statusLabel(o.status) }}
          </van-tag>
        </div>
        <div class="text-sm text-gray-400 mt-2">¥{{ o.amount }} · {{ o.gameMap || '未指定地图' }}</div>
        <div class="text-xs text-gray-300 mt-1">{{ o.createdAt?.replace('T', ' ').substring(0, 16) }}</div>
        <app-icon name="arrow" color="#ccc" class="absolute right-4 top-1/2" />
      </div>
      <div v-if="orders.length === 0" class="text-center py-20 text-gray-400">暂无订单</div>
    </div>
  </div>
</template>
