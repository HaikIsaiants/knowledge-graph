<template>
  <div class="relative w-full">
    <div class="relative">
      <!-- Search Type Selector -->
      <div class="absolute inset-y-0 left-0 flex items-center pl-3">
        <select
          v-model="searchType"
          class="text-sm bg-transparent border-none focus:ring-0 pr-8 text-gray-600"
          @change="$emit('typeChange', searchType)"
        >
          <option value="hybrid">Hybrid</option>
          <option value="fulltext">Full Text</option>
          <option value="vector">Semantic</option>
          <option value="adaptive">Adaptive</option>
        </select>
      </div>

      <!-- Search Input -->
      <input
        v-model="searchQuery"
        type="text"
        :placeholder="placeholder"
        class="w-full pl-28 pr-24 py-3 text-lg border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
        @input="handleInput"
        @keyup.enter="handleSearch"
        @focus="showSuggestions = true"
      />

      <!-- Right Side Actions -->
      <div class="absolute inset-y-0 right-0 flex items-center pr-3 space-x-2">
        <!-- Clear Button -->
        <button
          v-if="searchQuery"
          @click="clearSearch"
          class="p-1 text-gray-400 hover:text-gray-600"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>

        <!-- Loading Spinner -->
        <div v-if="isLoading" class="animate-spin">
          <svg class="w-5 h-5 text-primary-500" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
        </div>

        <!-- Search Button -->
        <button
          @click="handleSearch"
          class="px-4 py-1.5 bg-primary-600 text-white rounded-md hover:bg-primary-700 transition-colors"
        >
          Search
        </button>
      </div>
    </div>

    <!-- Search Suggestions -->
    <div
      v-if="showSuggestions && (suggestions.length > 0 || searchHistory.length > 0)"
      class="absolute z-10 w-full mt-2 bg-white rounded-lg shadow-lg border border-gray-200 max-h-96 overflow-auto"
    >
      <!-- Recent Searches -->
      <div v-if="!searchQuery && searchHistory.length > 0" class="p-3">
        <h3 class="text-sm font-semibold text-gray-500 mb-2">Recent Searches</h3>
        <div class="space-y-1">
          <button
            v-for="item in searchHistory"
            :key="item"
            @click="selectSuggestion(item)"
            class="w-full text-left px-3 py-2 hover:bg-gray-50 rounded-md flex items-center space-x-2"
          >
            <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span class="text-sm">{{ item }}</span>
          </button>
        </div>
      </div>

      <!-- Query Suggestions -->
      <div v-if="searchQuery && suggestions.length > 0" class="p-3">
        <h3 class="text-sm font-semibold text-gray-500 mb-2">Suggestions</h3>
        <div class="space-y-1">
          <button
            v-for="suggestion in suggestions"
            :key="suggestion"
            @click="selectSuggestion(suggestion)"
            class="w-full text-left px-3 py-2 hover:bg-gray-50 rounded-md flex items-center space-x-2"
          >
            <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <span class="text-sm">{{ suggestion }}</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useDebouncedFn } from '@/composables/useDebounce'
import { searchApi } from '@/api/searchApi'

const props = defineProps<{
  modelValue: string
  placeholder?: string
  isLoading?: boolean
  searchHistory?: string[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'search': [query: string, type: string]
  'typeChange': [type: string]
}>()

// Simplified: Use computed property for v-model instead of manual syncing
const searchQuery = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const searchType = ref<'hybrid' | 'fulltext' | 'vector' | 'adaptive'>('hybrid')
const showSuggestions = ref(false)
const suggestions = ref<string[]>([])

// Simplified: Consolidated empty query check
const fetchSuggestions = useDebouncedFn(async (query: string) => {
  // Clear suggestions if query is too short
  if (!query?.trim() || query.length < 2) {
    suggestions.value = []
    return
  }

  try {
    suggestions.value = await searchApi.getSuggestions(query)
  } catch (error) {
    console.error('Failed to fetch suggestions:', error)
    suggestions.value = []
  }
}, 300)

// Simplified: Direct call to fetchSuggestions handles empty query
const handleInput = () => {
  fetchSuggestions(searchQuery.value)
}

const handleSearch = () => {
  if (searchQuery.value.trim()) {
    emit('search', searchQuery.value, searchType.value)
    showSuggestions.value = false
  }
}

const clearSearch = () => {
  searchQuery.value = ''
  suggestions.value = []
  emit('update:modelValue', '')
}

const selectSuggestion = (suggestion: string) => {
  searchQuery.value = suggestion
  handleSearch()
  showSuggestions.value = false
}

// Close suggestions on click outside
const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  if (!target.closest('.relative')) {
    showSuggestions.value = false
  }
}

// Add click outside listener
document.addEventListener('click', handleClickOutside)
</script>