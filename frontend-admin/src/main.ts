import { createApp } from 'vue'
import { createPinia } from 'pinia'
import {
  ElButton, ElDialog, ElForm, ElFormItem, ElIcon, ElInput, ElInputNumber,
  ElOption, ElPagination, ElSelect, ElSwitch, ElTable, ElTableColumn, ElTag
} from 'element-plus'
import 'element-plus/dist/index.css'
import {
  Avatar, DArrowLeft, DArrowRight, DataAnalysis, Document, Menu, Money,
  Notification, Plus, Present, SwitchButton, Upload, User
} from '@element-plus/icons-vue'
import './style.css'
import App from './App.vue'
import router from './router'

const app = createApp(App)
app.use(createPinia())
app.use(router)

// 只注册实际使用的组件和图标，避免把整个 Element Plus 打进首屏包。
const elementComponents = {
  ElButton, ElDialog, ElForm, ElFormItem, ElIcon, ElInput, ElInputNumber,
  ElOption, ElPagination, ElSelect, ElSwitch, ElTable, ElTableColumn, ElTag
}
const elementIcons = {
  Avatar, DArrowLeft, DArrowRight, DataAnalysis, Document, Menu, Money,
  Notification, Plus, Present, SwitchButton, Upload, User
}
for (const [name, component] of Object.entries({ ...elementComponents, ...elementIcons })) {
  app.component(name, component)
}
app.mount('#app')
