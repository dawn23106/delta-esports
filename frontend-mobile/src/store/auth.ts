import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(localStorage.getItem('accessToken') || '')
  const refreshToken = ref(localStorage.getItem('refreshToken') || '')
  const userId = ref(Number(localStorage.getItem('userId')) || 0)
  const nickname = ref(localStorage.getItem('nickname') || '')
  const userRole = ref(localStorage.getItem('userRole') || '')
  const rememberMe = ref(localStorage.getItem('rememberMe') === 'true')
  const isGuest = ref(localStorage.getItem('isGuest') === 'true')

  const isLoggedIn = computed(() => !!accessToken.value || isGuest.value)

  function setAuth(data: { accessToken: string; refreshToken: string; userId: number; nickname: string; role: string }, remember: boolean) {
    accessToken.value = data.accessToken
    refreshToken.value = data.refreshToken
    userId.value = data.userId
    nickname.value = data.nickname
    userRole.value = data.role
    rememberMe.value = remember
    isGuest.value = false

    localStorage.setItem('accessToken', data.accessToken)
    localStorage.setItem('userId', String(data.userId))
    localStorage.setItem('nickname', data.nickname)
    localStorage.setItem('userRole', data.role)
    localStorage.removeItem('isGuest')

    if (remember) {
      localStorage.setItem('refreshToken', data.refreshToken)
      localStorage.setItem('rememberMe', 'true')
    } else {
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('rememberMe')
    }
  }

  function setGuest() {
    isGuest.value = true
    accessToken.value = ''
    refreshToken.value = ''
    userId.value = 0
    nickname.value = '游客'
    userRole.value = 'boss'
    rememberMe.value = false
    localStorage.setItem('isGuest', 'true')
    localStorage.setItem('nickname', '游客')
    localStorage.setItem('userRole', 'boss')
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('userId')
  }

  function logout() {
    accessToken.value = ''
    refreshToken.value = ''
    userId.value = 0
    nickname.value = ''
    userRole.value = ''
    rememberMe.value = false
    isGuest.value = false
    localStorage.clear()
  }

  return { accessToken, refreshToken, userId, nickname, userRole, rememberMe, isGuest, isLoggedIn, setAuth, setGuest, logout }
})
