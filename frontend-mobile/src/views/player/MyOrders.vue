<template>
  <div class="page">
    <van-nav-bar title="我的订单" />
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <van-list v-model:loading="loading" :finished="finished" @load="onLoad">
        <van-cell v-for="o in orders" :key="o.id" :title="o.game" :label="`${o.detail || ''} - ¥${o.price}`" :value="statusText(o.status)" />
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { getMyOrders, cancelOrder } from '../../api/order'
import { showToast } from 'vant'

const orders = ref<any[]>([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
let page = 1

function statusText(s: string) {
  const m: Record<string, string> = { pending:'待接单', assigned:'已接单', in_progress:'进行中', completed:'已完成', cancelled:'已取消' }
  return m[s] || s
}

async function onLoad() {
  loading.value = true
  try {
    const res = await getMyOrders(page, 20)
    orders.value.push(...res.data.list)
    finished.value = orders.value.length >= res.data.total
    page++
  } finally { loading.value = false }
}

async function onRefresh() {
  page = 1
  const res = await getMyOrders(1, 20)
  orders.value = res.data.list
  finished.value = orders.value.length >= res.data.total
  loading.value = false
  refreshing.value = false
}
</script>
