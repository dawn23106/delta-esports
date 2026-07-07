import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../store/auth'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue'), meta: { guest: true } },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue'), meta: { guest: true } },
  // 老板端
  { path: '/boss/home', name: 'BossHome', component: () => import('../views/boss/Home.vue') },
  { path: '/boss/choose', name: 'ChooseBooster', component: () => import('../views/boss/ChooseBooster.vue') },
  { path: '/boss/messages', name: 'Messages', component: () => import('../views/boss/Messages.vue') },
  { path: '/boss/messages/:orderId', name: 'Chat', component: () => import('../views/boss/Chat.vue') },
  { path: '/boss/profile', name: 'BossProfile', component: () => import('../views/boss/Profile.vue') },
  { path: '/boss/orders', name: 'OrderHistory', component: () => import('../views/boss/OrderHistory.vue') },
  { path: '/boss/order/:id', name: 'OrderDetail', component: () => import('../views/boss/OrderDetail.vue') },
  { path: '/boss/gifts', name: 'Gifts', component: () => import('../views/boss/Gifts.vue') },
  { path: '/boss/reviews', name: 'Reviews', component: () => import('../views/boss/Reviews.vue') },
  { path: '/boss/service', name: 'ServicePage', component: () => import('../views/boss/ServicePage.vue') },
  { path: '/boss/contact', name: 'Contact', component: () => import('../views/boss/Contact.vue') },
  { path: '/boss/settings', name: 'Settings', component: () => import('../views/boss/Settings.vue') },
  // 陪陪端
  { path: '/booster/pool', name: 'Pool', component: () => import('../views/booster/Pool.vue') },
  { path: '/booster/orders', name: 'OrderProcess', component: () => import('../views/booster/OrderProcess.vue') },
  { path: '/booster/profile', name: 'BoosterProfile', component: () => import('../views/booster/Profile.vue') },
  { path: '/booster/messages', name: 'BoosterMessages', component: () => import('../views/booster/Messages.vue') },
  { path: '/booster/messages/:orderId', name: 'BoosterChat', component: () => import('../views/boss/Chat.vue') },
  { path: '/', redirect: '/boss/home' },
]

const router = createRouter({ history: createWebHistory(), routes })

// 路由守卫
router.beforeEach((to, _from, next) => {
  const auth = useAuthStore()
  // 游客允许访问登录/注册页
  if (auth.isGuest && to.meta.guest) {
    next()
    return
  }
  if (!auth.isLoggedIn && !to.meta.guest) {
    next('/login')
  } else if (auth.isLoggedIn && to.meta.guest) {
    next(auth.userRole === 'booster' ? '/booster/pool' : '/boss/home')
  } else {
    next()
  }
})

export default router
