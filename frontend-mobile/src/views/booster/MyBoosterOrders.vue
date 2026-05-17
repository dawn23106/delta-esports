<template>
  <div class="page">
    <van-nav-bar title="我接的订单" />
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <van-list v-model:loading="loading" :finished="finished" @load="onLoad">
        <van-swipe-cell v-for="o in orders" :key="o.id">
          <van-cell :title="`${o.game} - ¥${o.price}`" :label="`${o.customerNickname} | ${statusText(o.status)}`" />
          <template #right v-if="o.status==='assigned'">
            <van-button square type="primary" text="开始" @click="doStart(o)" style="height:100%" />
          </template>
          <template #right v-if="o.status==='in_progress'">
            <van-button square type="success" text="完成" @click="doComplete(o)" style="height:100%" />
          </template>
        </van-swipe-cell>
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { showToast } from 'vant'
import { getBoosterOrders, startOrder, completeOrder } from '../../api/order'

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
    const res = await getBoosterOrders(page, 20)
    orders.value.push(...res.data.list)
    finished.value = orders.value.length >= res.data.total
    page++
  } finally { loading.value = false }
}

async function onRefresh() {
  page = 1
  const res = await getBoosterOrders(1, 20)
  orders.value = res.data.list
  finished.value = orders.value.length >= res.data.total
  loading.value = false
  refreshing.value = false
}

async function doStart(order: any) {
  await startOrder(order.id)
  showToast('已开始')
  onRefresh()
}

async function doComplete(order: any) {
  await completeOrder(order.id)
  showToast('已完成')
  onRefresh()
}
</script>
