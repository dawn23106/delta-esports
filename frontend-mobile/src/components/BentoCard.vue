<template>
  <div :class="['card-wrap', props.span === 2 ? 'span-2' : '']" @click="$emit('click')">
    <div class="card-inner">
      <div class="card-media placeholder">
        <span class="placeholder-icon">{{ icon }}</span>
        <div class="card-tag-overlay" v-if="tag">{{ tag }}</div>
      </div>
      <div class="card-body">
        <h4 class="card-title">{{ title }}</h4>
        <p class="card-desc" v-if="desc">{{ desc }}</p>
        <div class="card-footer" v-if="price">
          <span class="card-price">{{ price }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{
  span?: number; icon?: string; tag?: string
  title?: string; desc?: string; price?: string
}>()
defineEmits(['click'])
</script>

<style scoped>
.card-wrap {
  border-radius: 12px; overflow: hidden; background: #1a1a1a;
  cursor: pointer; user-select: none; transition: transform .15s;
}
.card-wrap:active { transform: scale(.97); }
.card-wrap.span-2 { grid-column: span 2; }
.card-inner { display: flex; flex-direction: column; height: 100%; }
.card-media {
  aspect-ratio: 16/9; display: flex; align-items: center; justify-content: center;
  position: relative;
}
.card-media.placeholder {
  background: linear-gradient(135deg, #242424 0%, #1a1a1a 100%);
}
.placeholder-icon { font-size: 40px; opacity: .4; }
.card-tag-overlay {
  position: absolute; top: 8px; right: 8px;
  background: rgba(0,0,0,.7); color: #f6d146; font-size: 11px;
  font-weight: 700; padding: 3px 8px; border-radius: 6px;
}
.card-body { padding: 14px 16px; flex: 1; display: flex; flex-direction: column; }
.card-title { font-size: 15px; font-weight: 700; color: #fafafa; margin: 0 0 6px; }
.card-desc { font-size: 12px; color: #cbcacc; margin: 0 0 10px; line-height: 1.5; }
.card-footer { margin-top: auto; padding-top: 4px; }
.card-price { font-size: 18px; font-weight: 700; color: #87fe0d; }
</style>
