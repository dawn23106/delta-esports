<template>
  <div class="bento-grid">
    <slot />
  </div>
</template>

<style scoped>
.bento-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}
@media (min-width: 768px) {
  .bento-grid { grid-template-columns: repeat(4, 1fr); }
}
</style>
