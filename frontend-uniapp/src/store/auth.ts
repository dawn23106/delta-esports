import { defineStore } from "pinia"
import { computed, ref } from "vue"

const storage = {
  get(key: string): string | null { try { return uni.getStorageSync(key) || null } catch { return null } },
  set(key: string, value: string) { try { uni.setStorageSync(key, value) } catch { /* ignore */ } },
  remove(key: string) { try { uni.removeStorageSync(key) } catch { /* ignore */ } },
  clear() { try { uni.clearStorageSync() } catch { /* ignore */ } },
}

export const useAuthStore = defineStore("auth", () => {
  const accessToken = ref(storage.get("accessToken") || "")
  const refreshToken = ref(storage.get("refreshToken") || "")
  const userId = ref(Number(storage.get("userId")) || 0)
  const nickname = ref(storage.get("nickname") || "")
  const userRole = ref(storage.get("userRole") || "")
  const rememberMe = ref(storage.get("rememberMe") === "true")
  const isGuest = ref(storage.get("isGuest") === "true")
  const isLoggedIn = computed(() => Boolean(accessToken.value) || isGuest.value)

  function setAuth(data: { accessToken: string; refreshToken: string; userId: number; nickname: string; role: string }, remember: boolean) {
    accessToken.value = data.accessToken
    refreshToken.value = data.refreshToken
    userId.value = data.userId
    nickname.value = data.nickname
    userRole.value = data.role
    rememberMe.value = remember
    isGuest.value = false
    storage.set("accessToken", data.accessToken)
    storage.set("userId", String(data.userId))
    storage.set("nickname", data.nickname)
    storage.set("userRole", data.role)
    storage.remove("isGuest")
    if (remember) {
      storage.set("refreshToken", data.refreshToken)
      storage.set("rememberMe", "true")
    } else {
      storage.remove("refreshToken")
      storage.remove("rememberMe")
    }
  }

  function setGuest() {
    accessToken.value = ""
    refreshToken.value = ""
    userId.value = 0
    nickname.value = "游客"
    userRole.value = "boss"
    rememberMe.value = false
    isGuest.value = true
    storage.clear()
    storage.set("isGuest", "true")
    storage.set("nickname", "游客")
    storage.set("userRole", "boss")
  }

  function logout() {
    accessToken.value = ""
    refreshToken.value = ""
    userId.value = 0
    nickname.value = ""
    userRole.value = ""
    rememberMe.value = false
    isGuest.value = false
    storage.clear()
  }

  return { accessToken, refreshToken, userId, nickname, userRole, rememberMe, isGuest, isLoggedIn, setAuth, setGuest, logout }
})
