import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { showDialog } from 'vant'

/**
 * 需要登录才能执行的操作守卫
 * 游客点击下单/送礼等操作时弹出登录引导
 */
export function useAuthGuard() {
  const router = useRouter()
  const auth = useAuthStore()

  /**
   * 检查是否需要登录，如果是游客则弹窗引导
   * @returns true = 可以继续，false = 已拦截（游客）
   */
  async function requireLogin(action: string = '此操作'): Promise<boolean> {
    if (!auth.isGuest) return true

    try {
      await showDialog({
        title: '需要登录',
        message: `${action}需要登录账号哦～`,
        confirmButtonText: '去登录',
        cancelButtonText: '再逛逛',
        confirmButtonColor: '#6366f1',
        showCancelButton: true,
      })
      router.push('/login')
    } catch {
      // 用户点了「再逛逛」，留在当前页继续逛
    }
    return false
  }

  return { requireLogin, isGuest: auth.isGuest }
}
