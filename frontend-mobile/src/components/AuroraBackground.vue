<template>
  <div class="aurora-wrap">
    <div class="aurora aurora-1" />
    <div class="aurora aurora-2" />
    <div class="aurora aurora-3" />
    <div class="aurora-content">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.aurora-wrap {
  position: relative; overflow: hidden;
  background: #eef2ff;
  width: 100%; min-height: 100vh;
}
.aurora {
  position: absolute; border-radius: 50%; filter: blur(100px); opacity: .55;
  animation: auroraFloat 8s ease-in-out infinite alternate;
}
.aurora-1 {
  width: 80vw; height: 80vw; max-width: 600px; max-height: 600px;
  background: radial-gradient(circle, rgba(129,140,248,.7) 0%, rgba(129,140,248,.2) 50%, transparent 70%);
  top: -25%; left: -15%;
  animation-delay: 0s;
}
.aurora-2 {
  width: 70vw; height: 70vw; max-width: 500px; max-height: 500px;
  background: radial-gradient(circle, rgba(192,132,252,.6) 0%, rgba(192,132,252,.15) 50%, transparent 70%);
  bottom: -20%; right: -10%;
  animation-delay: -3s; animation-duration: 10s;
}
.aurora-3 {
  width: 60vw; height: 60vw; max-width: 450px; max-height: 450px;
  background: radial-gradient(circle, rgba(96,165,250,.5) 0%, rgba(96,165,250,.12) 50%, transparent 70%);
  top: 35%; left: 35%;
  animation-delay: -6s; animation-duration: 12s;
}
.aurora-content { position: relative; z-index: 1; }

@keyframes auroraFloat {
  0% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(60px, -40px) scale(1.15); }
  50% { transform: translate(-30px, 50px) scale(.9); }
  75% { transform: translate(-50px, -30px) scale(1.1); }
  100% { transform: translate(40px, 30px) scale(1.05); }
}
</style>
