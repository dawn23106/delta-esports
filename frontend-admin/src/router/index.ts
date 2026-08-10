import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue'), meta: { guest: true } },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '数据看板' } },
      { path: 'orders', name: 'Orders', component: () => import('../views/Orders.vue'), meta: { title: '订单管理' } },
      { path: 'bosses', name: 'Bosses', component: () => import('../views/Bosses.vue'), meta: { title: '老板管理' } },
      { path: 'boosters', name: 'Boosters', component: () => import('../views/Boosters.vue'), meta: { title: '陪陪管理' } },
      { path: 'services', name: 'Services', component: () => import('../views/Services.vue'), meta: { title: '服务管理' } },
      { path: 'settlements', name: 'Settlements', component: () => import('../views/Settlements.vue'), meta: { title: '结算审核' } },
      { path: 'announcements', name: 'Announcements', component: () => import('../views/Announcements.vue'), meta: { title: '公告管理' } },
      { path: 'gifts', name: 'Gifts', component: () => import('../views/Gifts.vue'), meta: { title: '礼物记录' } },
    ]
  },
]

const router = createRouter({ history: createWebHistory(import.meta.env.BASE_URL), routes })

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('adminToken')
  if (!token && !to.meta.guest) {
    next('/login')
  } else if (token && to.meta.guest) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
