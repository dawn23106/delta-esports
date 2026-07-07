import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Vant from 'vant'
import 'vant/lib/index.css'
import App from './App.vue'
import router from './router'
import './style.css'
import AppIcon from './components/AppIcon.vue'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(Vant)
app.component('AppIcon', AppIcon)
app.mount('#app')
