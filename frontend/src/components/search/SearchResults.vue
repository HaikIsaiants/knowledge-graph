<template>
  <div class="space-y-4">
    <!-- Results Header -->
    <div v-if="results.length > 0" class="flex items-center justify-between">
      <h2 class="text-xl font-semibold text-gray-900">
        {{ totalElements }} Results
        <span v-if="searchTime" class="text-sm text-gray-500 ml-2">
          ({{ searchTime }}ms)
        </span>
      </h2>
      
      <!-- Sort Options -->
      <select
        v-model="sortBy"
        @change="$emit('sort', sortBy)"
        class="text-sm border border-gray-300 rounded-md px-3 py-1"
      >
        <option value="relevance">Relevance</option>
        <option value="date">Date</option>
        <option value="type">Type</option>
      </select>
    </div>

    <!-- Results List -->
    <div class="space-y-3">
      <div
        v-for="result in results"
        :key="result.id"
        class="bg-white rounded-lg shadow-sm border border-gray-200 p-4 hover:shadow-md transition-shadow cursor-pointer"
        @click="$emit('select', result)"
      >
        <div class="flex items-start justify-between">
          <div class="flex-1">
            <!-- Type Badge and Score -->
            <div class="flex items-center space-x-2 mb-2">
              <span
                :class="[
                  'px-2 py-1 text-xs font-medium rounded-full',
                  getTypeColor(result.type || 'UNKNOWN')
                ]"
              >
                {{ result.type || 'UNKNOWN' }}
              </span>
              <span class="text-xs text-gray-500">
                Score: {{ result.score.toFixed(3) }}
              </span>
              <span v-if="result.connectionCount" class="text-xs text-gray-500">
                • {{ result.connectionCount }} connections
              </span>
            </div>

            <!-- Title -->
            <h3 class="text-lg font-semibold text-gray-900 mb-1">
              {{ result.title }}
            </h3>

            <!-- Highlighted Snippet or Regular Snippet -->
            <p 
              v-if="result.highlightedSnippet" 
              class="text-gray-600 text-sm mb-2"
              v-html="result.highlightedSnippet"
            ></p>
            <p v-else-if="result.snippet" class="text-gray-600 text-sm mb-2">
              {{ result.snippet }}
            </p>

            <!-- Metadata -->
            <div class="flex items-center space-x-4 text-xs text-gray-500">
              <span v-if="result.sourceUri" class="flex items-center space-x-1">
                <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
                </svg>
                <span>{{ truncateUri(result.sourceUri) }}</span>
              </span>
              <span v-if="result.createdAt">
                {{ formatDate(result.createdAt) }}
              </span>
            </div>
          </div>

          <!-- Action Arrow -->
          <div class="ml-4">
            <svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
            </svg>
          </div>
        </div>
      </div>
    </div>

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="flex justify-center items-center space-x-2 mt-6">
      <button
        @click="$emit('page', currentPage - 1)"
        :disabled="currentPage === 0"
        class="px-3 py-1 rounded-md border border-gray-300 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
      >
        Previous
      </button>
      
      <div class="flex space-x-1">
        <button
          v-for="page in visiblePages"
          :key="page"
          @click="$emit('page', page - 1)"
          :class="[
            'px-3 py-1 rounded-md',
            page - 1 === currentPage
              ? 'bg-primary-600 text-white'
              : 'border border-gray-300 hover:bg-gray-50'
          ]"
        >
          {{ page }}
        </button>
      </div>
      
      <button
        @click="$emit('page', currentPage + 1)"
        :disabled="currentPage === totalPages - 1"
        class="px-3 py-1 rounded-md border border-gray-300 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
      >
        Next
      </button>
    </div>

    <!-- Empty State -->
    <div v-if="results.length === 0 && !isLoading" class="text-center py-12">
      <svg class="w-16 h-16 text-gray-400 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
      </svg>
      <h3 class="text-lg font-semibold text-gray-900 mb-2">No results found</h3>
      <p class="text-gray-600">Try adjusting your search terms or filters</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { SearchResultDTO } from '@/api/searchApi'

const props = defineProps<{
  results: SearchResultDTO[]
  totalElements: number
  totalPages: number
  currentPage: number
  searchTime?: number
  isLoading?: boolean
  sortBy?: string
}>()

const emit = defineEmits<{
  select: [result: SearchResultDTO]
  page: [page: number]
  sort: [sortBy: string]
}>()

const sortBy = ref(props.sortBy || 'relevance')

// Simplified: Cleaner pagination calculation
const visiblePages = computed(() => {
  const maxVisible = 7
  const current = props.currentPage + 1
  const total = props.totalPages
  
  // Calculate centered range
  const halfVisible = Math.floor(maxVisible / 2)
  const start = Math.max(1, Math.min(current - halfVisible, total - maxVisible + 1))
  const end = Math.min(total, start + maxVisible - 1)
  
  // Generate page array using Array.from
  return Array.from({ length: end - start + 1 }, (_, i) => start + i)
})

// Simplified: Use Map for better performance and cleaner fallback
const typeColorMap = new Map([
  ['PERSON', 'bg-blue-100 text-blue-800'],
  ['ORGANIZATION', 'bg-green-100 text-green-800'],
  ['EVENT', 'bg-purple-100 text-purple-800'],
  ['LOCATION', 'bg-orange-100 text-orange-800'],
  ['PLACE', 'bg-orange-100 text-orange-800'],
  ['CONCEPT', 'bg-indigo-100 text-indigo-800'],
  ['DOCUMENT', 'bg-gray-100 text-gray-800'],
  ['PROJECT', 'bg-pink-100 text-pink-800'],
  ['SYSTEM', 'bg-yellow-100 text-yellow-800']
])

const getTypeColor = (type: string) => 
  typeColorMap.get(type) || 'bg-gray-100 text-gray-800'

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))
  
  if (diffDays === 0) return 'Today'
  if (diffDays === 1) return 'Yesterday'
  if (diffDays < 7) return `${diffDays} days ago`
  if (diffDays < 30) return `${Math.floor(diffDays / 7)} weeks ago`
  if (diffDays < 365) return `${Math.floor(diffDays / 30)} months ago`
  return `${Math.floor(diffDays / 365)} years ago`
}

// Simplified: Use ternary for cleaner one-liner
const truncateUri = (uri: string, maxLength = 50) => 
  uri.length <= maxLength ? uri : `${uri.substring(0, maxLength)}...`
</script>

<style scoped>
/* Highlight search terms */
:deep(mark) {
  background-color: #fef3c7;
  color: #92400e;
  padding: 0.125rem 0.25rem;
  border-radius: 0.125rem;
}
</style>