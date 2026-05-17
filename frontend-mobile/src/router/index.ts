import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/player',
    component: () => import('../views/player/PlayerLayout.vue'),
    children: [
      { path: '', redirect: '/player/home' },
      { path: 'home', component: () => import('../views/player/Home.vue') },
      { path: 'orders', component: () => import('../views/player/MyOrders.vue') }
    ]
  },
  {
    path: '/booster',
    component: () => import('../views/booster/BoosterLayout.vue'),
    children: [
      { path: '', redirect: '/booster/pool' },
      { path: 'pool', component: () => import('../views/booster/Pool.vue') },
      { path: 'orders', component: () => import('../views/booster/MyBoosterOrders.vue') }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/login' }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('accessToken')
  if (to.path !== '/login' && !token) return next('/login')
  next()
})

export default router
