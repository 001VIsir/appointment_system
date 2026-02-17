<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import MerchantLayout from '@/layouts/MerchantLayout.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'

const route = useRoute()

const layoutComponent = computed(() => {
  const layout = route.meta?.layout as string | undefined
  switch (layout) {
    case 'merchant':
      return MerchantLayout
    case 'admin':
      return AdminLayout
    case 'none':
      return null
    default:
      return DefaultLayout
  }
})
</script>

<template>
  <component :is="layoutComponent" v-if="layoutComponent">
    <router-view />
  </component>
  <router-view v-else />
</template>

<style>
#app {
  width: 100%;
  min-height: 100vh;
}
</style>
