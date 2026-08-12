import { useAuthStore } from "../store/auth"
import { navigateTo, showModal } from "../utils/nav"

/**
 * 需要登录才能执行的操作守卫
 * 游客点击下单/送礼等操作时弹出登录引导
 */
export function useAuthGuard() {
  const auth = useAuthStore()

  async function requireLogin(action: string = "此操作"): Promise<boolean> {
    if (!auth.isGuest) return true

    try {
      const res = await showModal({
        title: "需要登录",
        content: `${action}需要登录账号哦～`,
        confirmText: "去登录",
        cancelText: "再逛逛",
        confirmColor: "#3157ff",
      })
      if (res.confirm) {
        uni.navigateTo({ url: "/pages/auth/login" })
      }
    } catch {
      // 弹窗出错
    }
    return false
  }

  return { requireLogin, isGuest: auth.isGuest }
}
