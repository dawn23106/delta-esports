/** 通用类型定义 */

export interface ShowModalOptions {
  title: string
  content: string
  confirmText?: string
  cancelText?: string
  confirmColor?: string
}

export interface ShowModalRes {
  confirm: boolean
  cancel: boolean
}

export interface ShowToastOptions {
  title: string
  icon?: "success" | "error" | "loading" | "none"
  duration?: number
}
