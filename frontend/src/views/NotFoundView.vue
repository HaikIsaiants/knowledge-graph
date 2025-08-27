<template>
  <div class="text-center py-12">
    <div class="text-gray-400 mb-6">
      <svg class="w-24 h-24 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.732-.833-2.502 0L4.732 15.5c-.77.833.192 2.5 1.732 2.5z" />
      </svg>
    </div>
    <h1 class="text-4xl font-bold text-gray-900 mb-4">404 - Page Not Found</h1>
    <p class="text-xl text-gray-600 mb-8">The page you're looking for doesn't exist.</p>
    
    <div class="flex flex-col sm:flex-row gap-4 justify-center">
      <RouterLink to="/" class="btn btn-primary">
        Go Home
      </RouterLink>
      <RouterLink to="/search" class="btn btn-secondary">
        Search Knowledge Graph
      </RouterLink>
    </div>
  </div>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>