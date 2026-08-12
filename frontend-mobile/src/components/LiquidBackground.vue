<template>
  <div class="liquid-bg">
    <div class="blob blob-1" />
    <div class="blob blob-2" />
    <div class="blob blob-3" />
    <div class="blob blob-4" />
    <div class="overlay" />
    <div class="content"><slot /></div>
  </div>
</template>

<style scoped>
.liquid-bg {
  position: relative; width: 100%; min-height: 100vh; overflow: hidden;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 40%, #a78bfa 70%, #c084fc 100%);
}
.overlay { position: absolute; inset: 0; background: rgba(0,0,0,.15); z-index: 1; }
.content { position: relative; z-index: 2; }

.blob {
  position: absolute; border-radius: 50%; filter: blur(80px);
  animation: morph 8s ease-in-out infinite alternate;
}
.blob-1 {
  width: 60vw; height: 60vw; max-width: 500px; max-height: 500px;
  background: rgba(255,255,255,.15);
  top: -10%; left: -20%;
  animation-delay: 0s;
}
.blob-2 {
  width: 50vw; height: 50vw; max-width: 400px; max-height: 400px;
  background: rgba(255,255,255,.1);
  bottom: -15%; right: -15%;
  animation-delay: -2s; animation-duration: 10s;
}
.blob-3 {
  width: 40vw; height: 40vw; max-width: 350px; max-height: 350px;
  background: rgba(255,255,255,.08);
  top: 40%; left: 30%;
  animation-delay: -4s; animation-duration: 12s;
}
.blob-4 {
  width: 30vw; height: 30vw; max-width: 250px; max-height: 250px;
  background: rgba(255,255,255,.12);
  top: 20%; right: 10%;
  animation-delay: -6s; animation-duration: 9s;
}

@keyframes morph {
  0% { border-radius: 50% 50% 50% 50%; transform: translate(0, 0) scale(1); }
  25% { border-radius: 40% 60% 60% 40%; transform: translate(30px, -20px) scale(1.1); }
  50% { border-radius: 60% 40% 40% 60%; transform: translate(-20px, 30px) scale(.95); }
  75% { border-radius: 45% 55% 55% 45%; transform: translate(-40px, -10px) scale(1.05); }
  100% { border-radius: 55% 45% 45% 55%; transform: translate(20px, 20px) scale(.9); }
}
</style>
