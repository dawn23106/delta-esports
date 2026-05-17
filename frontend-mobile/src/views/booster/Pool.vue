<template>
  <div class="page">
    <van-nav-bar title="订单池" />
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <van-list v-model:loading="loading" :finished="finished" @load="onLoad">
        <van-cell v-for="o in orders" :key="o.id"
          :title="`${o.game} - ¥${o.price}`"
          :label="`${o.detail || ''} | ${o.customerNickname}`"
          is-link @click="doClaim(o)" />
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { showDialog, showToast } from 'vant'
import { getPool, claimOrder } from '../../api/order'

const orders = ref<any[]>([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
let page = 1

async function onLoad() {
  loading.value = true
  try {
    const res = await getPool(undefined, page, 20)
    orders.value.push(...res.data.list)
    finished.value = orders.value.length >= res.data.total
    page++
  } finally { loading.value = false }
}

async function onRefresh() {
  page = 1
  const res = await getPool(undefined, 1, 20)
  orders.value = res.data.list
  finished.value = orders.value.length >= res.data.total
  loading.value = false
  refreshing.value = false
}

async function doClaim(order: any) {
  try {
    await showDialog({ title: '确认接单', message: `${order.game} - ¥${order.price}` })
    await claimOrder(order.id)
    showToast('接单成功')
    onRefresh()
  } catch { /* 用户取消 */ }
}
</script>
