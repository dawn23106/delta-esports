import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getMe } from '../api/auth'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<any>(null)
  const role = ref('')

  function setToken(access: string, refresh: string) {
    localStorage.setItem('accessToken', access)
    localStorage.setItem('refreshToken', refresh)
  }

  function logout() {
    localStorage.clear()
    user.value = null
    role.value = ''
  }

  async function fetchMe() {
    try {
      const res = await getMe()
      user.value = res.data
      role.value = res.data.role
    } catch {
      logout()
    }
  }

  return { user, role, setToken, logout, fetchMe }
})
