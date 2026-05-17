import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  { path: '/login', component: () => import('../views/Login.vue') },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    children: [
      { path: 'orders', component: () => import('../views/Orders.vue') },
      { path: 'users', component: () => import('../views/Users.vue') }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/orders' }
]

const router = createRouter({ history: createWebHashHistory(), routes })

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('accessToken')
  if (to.path !== '/login' && !token) return next('/login')
  next()
})

export default router
