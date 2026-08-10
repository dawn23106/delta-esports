/**
 * 导航 & 对话框工具函数 — 统一封装 UniApp API
 */
import type { ShowModalOptions, ShowModalRes, ShowToastOptions } from "./types"

/** Navigate to non-tab page */
export function navigateTo(url: string) {
  uni.navigateTo({ url })
}

/** Navigate to tab page */
export function switchTab(url: string) {
  uni.switchTab({ url })
}

/** Redirect to new page (replaces current) */
export function redirectTo(url: string) {
  uni.redirectTo({ url })
}

/** Go back */
export function navigateBack(delta = 1) {
  uni.navigateBack({ delta })
}

/** Show toast */
export function showToast(options: string | ShowToastOptions) {
  if (typeof options === "string") {
    uni.showToast({ title: options, icon: "none" })
  } else {
    uni.showToast({
      title: options.title,
      icon: options.icon || "none",
      duration: options.duration || 2000,
    })
  }
}

/** Show loading toast */
export function showLoading(title: string) {
  uni.showLoading({ title, mask: true })
}

/** Close loading */
export function hideLoading() {
  uni.hideLoading()
}

/** Show modal dialog */
export function showModal(options: ShowModalOptions): Promise<ShowModalRes> {
  return new Promise((resolve, reject) => {
    uni.showModal({
      title: options.title,
      content: options.content,
      confirmText: options.confirmText || "确定",
      cancelText: options.cancelText || "取消",
      confirmColor: options.confirmColor || "#3157ff",
      success: (res) => resolve({ confirm: res.confirm, cancel: res.cancel }),
      fail: reject,
    })
  })
}
