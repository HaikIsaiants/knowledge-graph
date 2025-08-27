<template>
  <header class="bg-white shadow-sm border-b border-gray-200">
    <nav class="container mx-auto px-4 py-4">
      <div class="flex items-center justify-between">
        <div class="flex items-center space-x-8">
          <RouterLink to="/" class="flex items-center space-x-2">
            <div class="w-8 h-8 bg-primary-600 rounded-lg flex items-center justify-center">
              <svg class="w-5 h-5 text-white" fill="currentColor" viewBox="0 0 24 24">
                <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
              </svg>
            </div>
            <span class="text-xl font-semibold text-gray-900">Knowledge Graph</span>
          </RouterLink>
          
          <div class="hidden md:flex items-center space-x-6">
            <RouterLink 
              to="/" 
              class="text-gray-600 hover:text-gray-900 px-3 py-2 rounded-md text-sm font-medium transition-colors"
              active-class="text-primary-600 bg-primary-50"
            >
              Home
            </RouterLink>
            <RouterLink 
              to="/search" 
              class="text-gray-600 hover:text-gray-900 px-3 py-2 rounded-md text-sm font-medium transition-colors"
              active-class="text-primary-600 bg-primary-50"
            >
              Search
            </RouterLink>
            <RouterLink 
              to="/graph" 
              class="text-gray-600 hover:text-gray-900 px-3 py-2 rounded-md text-sm font-medium transition-colors"
              active-class="text-primary-600 bg-primary-50"
            >
              Graph
            </RouterLink>
            <RouterLink 
              to="/upload" 
              class="text-gray-600 hover:text-gray-900 px-3 py-2 rounded-md text-sm font-medium transition-colors"
              active-class="text-primary-600 bg-primary-50"
            >
              Upload
            </RouterLink>
          </div>
        </div>

        <div class="flex items-center space-x-4">
          <!-- Search input -->
          <div class="hidden sm:block">
            <div class="relative">
              <input
                v-model="searchQuery"
                type="text"
                placeholder="Quick search..."
                class="w-64 pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                @keyup.enter="handleQuickSearch"
              >
              <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                <svg class="h-5 w-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
              </div>
            </div>
          </div>

          <!-- Mobile menu button -->
          <button
            @click="mobileMenuOpen = !mobileMenuOpen"
            class="md:hidden p-2 rounded-md text-gray-600 hover:text-gray-900 hover:bg-gray-100"
          >
            <svg class="h-6 w-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          </button>
        </div>
      </div>

      <!-- Mobile menu -->
      <div v-if="mobileMenuOpen" class="md:hidden mt-4 pb-4 border-t border-gray-200 pt-4">
        <div class="flex flex-col space-y-2">
          <RouterLink 
            to="/" 
            class="text-gray-600 hover:text-gray-900 px-3 py-2 rounded-md text-base font-medium"
            active-class="text-primary-600 bg-primary-50"
            @click="mobileMenuOpen = false"
          >
            Home
          </RouterLink>
          <RouterLink 
            to="/search" 
            class="text-gray-600 hover:text-gray-900 px-3 py-2 rounded-md text-base font-medium"
            active-class="text-primary-600 bg-primary-50"
            @click="mobileMenuOpen = false"
          >
            Search
          </RouterLink>
          <RouterLink 
            to="/graph" 
            class="text-gray-600 hover:text-gray-900 px-3 py-2 rounded-md text-base font-medium"
            active-class="text-primary-600 bg-primary-50"
            @click="mobileMenuOpen = false"
          >
            Graph
          </RouterLink>
          <RouterLink 
            to="/upload" 
            class="text-gray-600 hover:text-gray-900 px-3 py-2 rounded-md text-base font-medium"
            active-class="text-primary-600 bg-primary-50"
            @click="mobileMenuOpen = false"
          >
            Upload
          </RouterLink>
        </div>
      </div>
    </nav>
  </header>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

const router = useRouter()
const searchQuery = ref('')
const mobileMenuOpen = ref(false)

const handleQuickSearch = () => {
  if (searchQuery.value.trim()) {
    router.push(`/search?q=${encodeURIComponent(searchQuery.value)}`)
    searchQuery.value = ''
  }
}
</script>