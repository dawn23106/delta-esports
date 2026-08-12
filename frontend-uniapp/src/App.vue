<script setup lang="ts">
import { onLaunch, onShow } from "@dcloudio/uni-app"
import { useAuthStore } from "./store/auth"

onLaunch(() => {
  console.log("Delta Esports App Launch")
})

onShow(() => {
  // Token 过期检查等
  console.log("App Show")
})
</script>

<style>
@import "./global.css";
</style>
