<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../api/request'

const stats = ref({ orders: 0, bosses: 0, boosters: 0, revenue: 0 })
const recentOrders = ref<any[]>([])
const loading = ref(false)

const statCards = [
  { key: 'orders', label: '总订单', icon: 'Document', color: '#6366f1', bg: '#F8F5FF' },
  { key: 'bosses', label: '老板数', icon: 'User', color: '#10b981', bg: '#ECFDF5' },
  { key: 'boosters', label: '陪陪数', icon: 'Avatar', color: '#f59e0b', bg: '#FFF9F2' },
  { key: 'revenue', label: '今日流水', icon: 'Money', color: '#ef4444', bg: '#FEF2F2', prefix: '¥' },
]

async function loadData() {
  loading.value = true
  try { const r: any = await request.get('/admin/orders', { params: { page: 1, size: 5 } }); recentOrders.value = r.records || []; stats.value.orders = r.total || 0 } catch { }
  try { const r: any = await request.get('/admin/bosses', { params: { page: 1, size: 1 } }); stats.value.bosses = r.total || 0 } catch { }
  try { const r: any = await request.get('/admin/boosters', { params: { page: 1, size: 1 } }); stats.value.boosters = r.total || 0 } catch { }
  loading.value = false
}

function statusLabel(s: string) {
  const m: Record<string, string> = { pending:'待接单', assigned:'已接单', in_progress:'进行中', completed:'待审核', done:'已确认', settled:'已结算', cancelled:'已取消', disputed:'争议中' }
  return m[s] || s
}

onMounted(loadData)
</script>

<template>
  <div class="dashboard">
    <div class="welcome">
      <h2 class="welcome-title">数据看板</h2>
      <p class="welcome-sub">实时监控平台运营数据</p>
    </div>

    <div class="stats-grid">
      <div v-for="card in statCards" :key="card.key" class="stat-card" :style="{ background: card.bg }">
        <div class="stat-icon" :style="{ background: card.color }">
          <el-icon size="20" color="#fff"><component :is="card.icon" /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-label">{{ card.label }}</div>
          <div class="stat-val">{{ card.prefix || '' }}{{ stats[card.key as keyof typeof stats] }}</div>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-head"><h3 class="card-title">最近订单</h3></div>
      <div class="table-wrap" v-if="recentOrders.length">
        <table>
          <thead><tr><th>订单号</th><th>老板ID</th><th>陪陪ID</th><th>金额</th><th>状态</th><th>时间</th></tr></thead>
          <tbody>
            <tr v-for="o in recentOrders" :key="o.id">
              <td class="mono">#{{ o.id }}</td>
              <td>{{ o.bossId }}</td>
              <td>{{ o.boosterId || '-' }}</td>
              <td class="price">¥{{ o.amount }}</td>
              <td><span :class="['badge', o.status]">{{ statusLabel(o.status) }}</span></td>
              <td class="date">{{ o.createdAt?.replace('T',' ').substring(0,16) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-else class="empty">暂无订单数据</div>
    </div>
  </div>
</template>

<style scoped>
.dashboard { max-width: 960px; }
.welcome { margin-bottom: 20px; }
.welcome-title { font-size: 20px; font-weight: 700; color: #1a1a1a; margin: 0 0 4px; }
.welcome-sub { font-size: 13px; color: #999; margin: 0; }

.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 20px; }
.stat-card { border-radius: 14px; padding: 18px; display: flex; align-items: center; gap: 14px; }
.stat-icon { width: 44px; height: 44px; border-radius: 12px; display: flex; align-items: center; justify-content: center; }
.stat-label { font-size: 12px; color: #666; margin-bottom: 2px; }
.stat-val { font-size: 22px; font-weight: 800; color: #1a1a1a; }

.card { background: #fff; border-radius: 16px; box-shadow: 0 1px 3px rgba(0,0,0,.04); }
.card-head { padding: 18px 20px 0; }
.card-title { font-size: 15px; font-weight: 600; color: #1a1a1a; margin: 0; }

.table-wrap { padding: 12px 20px 20px; }
table { width: 100%; border-collapse: collapse; }
th { text-align: left; font-size: 11px; font-weight: 600; color: #999; text-transform: uppercase; padding: 0 0 10px; border-bottom: 1px solid #F0F0F0; }
td { font-size: 13px; color: #333; padding: 10px 0; border-bottom: 1px solid #F8F8F8; }
.mono { font-family: monospace; font-weight: 600; color: #1a1a1a; }
.price { font-weight: 700; color: #6366f1; }
.date { font-size: 12px; color: #999; }

.badge { display: inline-block; padding: 2px 10px; border-radius: 20px; font-size: 11px; font-weight: 500; background: #FFF9F2; color: #92400e; border: 1px solid #FDE68A; }
.badge.settled { background: #ECFDF5; color: #065f46; border-color: #A7F3D0; }
.badge.in_progress { background: #EFF6FF; color: #1e40af; border-color: #BFDBFE; }
.badge.completed { background: #F8F5FF; color: #5b21b6; border-color: #DDD6FE; }
.badge.pending { background: #F9FAFB; color: #374151; border-color: #E5E7EB; }
.badge.cancelled { background: #FEF2F2; color: #991b1b; border-color: #FECACA; }
.badge.disputed { background: #FFF7ED; color: #9a3412; border-color: #FED7AA; }

.empty { padding: 40px; text-align: center; color: #999; font-size: 13px; }
</style>
