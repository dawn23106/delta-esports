<template>
  <div class="page">
    <van-nav-bar title="下单" />
    <van-form @submit="onSubmit">
      <van-cell-group inset>
        <van-field v-model="game" name="game" label="游戏" placeholder="如：三角洲行动" :rules="[{ required: true, message: '请输入游戏名' }]" />
        <van-field v-model="serviceType" name="serviceType" label="服务类型" is-link readonly @click="showTypePicker = true" placeholder="选择服务类型" />
        <van-field v-model="detail" name="detail" label="要求" placeholder="段位/任务说明" />
        <van-field v-model="price" name="price" label="价格" type="number" placeholder="输入价格" :rules="[{ required: true, message: '请输入价格' }]" />
      </van-cell-group>
      <van-popup v-model:show="showTypePicker" position="bottom">
        <van-picker :columns="typeOptions" @confirm="onConfirmType" @cancel="showTypePicker = false" />
      </van-popup>
      <div style="margin:16px">
        <van-button round block type="primary" native-type="submit" :loading="loading">立即下单</van-button>
      </div>
    </van-form>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { showToast } from 'vant'
import { createOrder } from '../../api/order'

const game = ref('')
const serviceType = ref('技术支持')
const detail = ref('')
const price = ref('')
const loading = ref(false)
const showTypePicker = ref(false)
const typeOptions = [
  { text: '技术支持（上分/排位）', value: 'tech' },
  { text: '娱乐陪玩（聊天/开黑）', value: 'entertain' },
  { text: '任务陪玩（通行证/周常）', value: 'quest' }
]

function onConfirmType({ selectedOptions }: any) {
  serviceType.value = selectedOptions[0].text
  showTypePicker.value = false
}

async function onSubmit() {
  loading.value = true
  try {
    await createOrder(game.value, serviceType.value, detail.value, Number(price.value), 'web_h5')
    showToast('下单成功')
    game.value = ''; serviceType.value = '技术支持'; detail.value = ''; price.value = ''
  } finally { loading.value = false }
}
</script>

<style scoped>
.page { min-height:100vh; background:#f7f8fa; }
</style>
