<template>
  <div class="min-h-screen bg-gray-50 py-8">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <!-- Header -->
      <div class="mb-8">
        <h1 class="text-3xl font-bold text-gray-900 mb-4">Knowledge Graph Search</h1>
        <p class="text-gray-600">Search through nodes, relationships, and documents in your knowledge graph</p>
      </div>

      <!-- Search Bar -->
      <div class="mb-6">
        <SearchBar
          v-model="searchQuery"
          :placeholder="'Search nodes, documents, or relationships...'"
          :is-loading="isLoading"
          :search-history="searchHistory"
          @search="handleSearch"
          @typeChange="handleTypeChange"
        />
      </div>

      <!-- Main Content Area -->
      <div class="flex gap-6">
        <!-- Filters Sidebar -->
        <aside class="w-64 flex-shrink-0">
          <SearchFilters
            v-model="filters"
            :node-type-counts="nodeTypeCounts"
            @apply="handleFiltersApply"
          />
        </aside>

        <!-- Results Area -->
        <main class="flex-1">
          <!-- Loading State -->
          <div v-if="isLoading" class="flex justify-center py-12">
            <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
          </div>

          <!-- Search Results -->
          <SearchResults
            v-else-if="hasSearched"
            :results="searchResults"
            :total-elements="totalElements"
            :total-pages="totalPages"
            :current-page="currentPage"
            :search-time="searchTime"
            :is-loading="isLoading"
            :sort-by="sortBy"
            @select="handleResultSelect"
            @page="handlePageChange"
            @sort="handleSortChange"
          />

          <!-- Initial State -->
          <div v-else class="bg-white rounded-lg shadow-sm border border-gray-200 p-12 text-center">
            <svg class="w-20 h-20 text-gray-400 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <h3 class="text-xl font-semibold text-gray-900 mb-2">Start your search</h3>
            <p class="text-gray-600 mb-6">Enter a query above to search through your knowledge graph</p>
            
            <!-- Quick Actions -->
            <div class="flex justify-center space-x-4">
              <button
                @click="searchRecent"
                class="px-4 py-2 text-sm text-primary-600 hover:text-primary-700 font-medium"
              >
                View Recent Nodes
              </button>
              <button
                @click="searchPopular"
                class="px-4 py-2 text-sm text-primary-600 hover:text-primary-700 font-medium"
              >
                Most Connected
              </button>
            </div>
          </div>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { searchApi, type SearchResultDTO, type SearchResponseDTO } from '@/api/searchApi'
import { nodesApi } from '@/api/nodes'
import SearchBar from '@/components/search/SearchBar.vue'
import SearchResults from '@/components/search/SearchResults.vue'
import SearchFilters from '@/components/search/SearchFilters.vue'
import type { NodeType } from '@/types'
import type { SearchFilters as FilterType } from '@/components/search/SearchFilters.vue'

const route = useRoute()
const router = useRouter()

// Search state
const searchQuery = ref('')
const searchType = ref<'hybrid' | 'fulltext' | 'vector' | 'adaptive'>('hybrid')
const searchResults = ref<SearchResultDTO[]>([])
const totalElements = ref(0)
const totalPages = ref(0)
const currentPage = ref(0)
const searchTime = ref<number>()
const sortBy = ref('relevance')
const isLoading = ref(false)
const hasSearched = ref(false)
const searchHistory = ref<string[]>([])

// Filters
const filters = ref<FilterType>({
  nodeTypes: [],
  dateRange: '',
  minScore: 0,
  minConnections: 0,
  hasCitations: false,
  sourceUri: ''
})

// Node type counts for filter display
const nodeTypeCounts = ref<Record<NodeType, number>>({
  PERSON: 0,
  ORGANIZATION: 0,
  EVENT: 0,
  LOCATION: 0,
  PLACE: 0,
  CONCEPT: 0,
  DOCUMENT: 0,
  PROJECT: 0,
  SYSTEM: 0
})

// Load search history from localStorage
onMounted(() => {
  const stored = localStorage.getItem('searchHistory')
  if (stored) {
    searchHistory.value = JSON.parse(stored).slice(0, 5)
  }
  
  // Check for query in route
  const query = route.query.q as string
  if (query) {
    searchQuery.value = query
    handleSearch(query, searchType.value)
  }
  
  // Load node type counts
  fetchNodeTypeCounts()
})

// Save search to history
const saveToHistory = (query: string) => {
  const history = [query, ...searchHistory.value.filter(q => q !== query)].slice(0, 5)
  searchHistory.value = history
  localStorage.setItem('searchHistory', JSON.stringify(history))
}

// Fetch node type counts for filters
const fetchNodeTypeCounts = async () => {
  try {
    // This would typically be a dedicated endpoint
    // For now, we'll simulate with multiple calls
    const types: NodeType[] = ['PERSON', 'ORGANIZATION', 'EVENT', 'LOCATION', 'PLACE', 'CONCEPT', 'DOCUMENT', 'PROJECT', 'SYSTEM']
    for (const type of types) {
      try {
        const response = await nodesApi.getByType(type, 0, 1)
        nodeTypeCounts.value[type] = response.totalElements || 0
      } catch (error) {
        nodeTypeCounts.value[type] = 0
      }
    }
  } catch (error) {
    console.error('Failed to fetch node counts:', error)
  }
}

// Simplified: Extract search method selection and use async/await pattern
const searchMethods = {
  vector: (query: string, params: any) => 
    searchApi.vectorSearch(query, filters.value.minScore || 0, 10),
  hybrid: (query: string, params: any) => 
    searchApi.hybridSearch(query, 0.5, 0.5, params.page, params.size),
  adaptive: (query: string, params: any) => 
    searchApi.adaptiveSearch(query, params.page, params.size),
  fulltext: (query: string, params: any) => 
    searchApi.search(query, 'fulltext', params.page, params.size, params.highlight)
}

const handleSearch = async (query: string, type: string) => {
  if (!query.trim()) return
  
  isLoading.value = true
  hasSearched.value = true
  saveToHistory(query)
  
  // Update URL
  router.replace({ query: { q: query, type, page: currentPage.value.toString() } })
  
  try {
    const startTime = Date.now()
    const searchParams = {
      page: currentPage.value,
      size: 10,
      highlight: true,
      ...buildFilterParams()
    }
    
    // Use search method map for cleaner selection
    const searchMethod = searchMethods[type as keyof typeof searchMethods] || searchMethods.fulltext
    const response = await searchMethod(query, searchParams)
    
    // Update results with defaults
    searchTime.value = Date.now() - startTime
    searchResults.value = response.results || []
    totalElements.value = response.totalElements || 0
    totalPages.value = response.totalPages || 0
  } catch (error) {
    console.error('Search failed:', error)
    // Reset results on error
    Object.assign(searchResults, { value: [] })
    Object.assign(totalElements, { value: 0 })
    Object.assign(totalPages, { value: 0 })
  } finally {
    isLoading.value = false
  }
}

// Simplified: Use map for date ranges and object spread
const dateRangeOffsets = new Map([
  ['today', () => { const d = new Date(); d.setHours(0, 0, 0, 0); return d }],
  ['week', () => new Date(Date.now() - 7 * 24 * 60 * 60 * 1000)],
  ['month', () => new Date(Date.now() - 30 * 24 * 60 * 60 * 1000)],
  ['year', () => { const d = new Date(); d.setFullYear(d.getFullYear() - 1); return d }]
])

const buildFilterParams = () => {
  const params: Record<string, any> = {}
  const { nodeTypes, dateRange, startDate, endDate, minScore, minConnections, hasCitations, sourceUri } = filters.value
  
  // Add simple filters using object spread
  Object.assign(params, {
    ...(nodeTypes?.length && { nodeTypes: nodeTypes.join(',') }),
    ...(minScore > 0 && { minScore }),
    ...(minConnections > 0 && { minConnections }),
    ...(hasCitations && { hasCitations: true }),
    ...(sourceUri && { sourceUri })
  })
  
  // Handle date ranges
  if (dateRange === 'custom') {
    Object.assign(params, {
      ...(startDate && { startDate }),
      ...(endDate && { endDate })
    })
  } else if (dateRange && dateRangeOffsets.has(dateRange)) {
    params.startDate = dateRangeOffsets.get(dateRange)!().toISOString()
    params.endDate = new Date().toISOString()
  }
  
  return params
}

// Handle search type change
const handleTypeChange = (type: string) => {
  searchType.value = type as typeof searchType.value
  if (searchQuery.value && hasSearched.value) {
    handleSearch(searchQuery.value, type)
  }
}

// Handle filter apply
const handleFiltersApply = (newFilters: FilterType) => {
  filters.value = newFilters
  if (searchQuery.value && hasSearched.value) {
    currentPage.value = 0 // Reset to first page
    handleSearch(searchQuery.value, searchType.value)
  }
}

// Handle result selection
const handleResultSelect = (result: SearchResultDTO) => {
  if (result.id) {
    router.push(`/node/${result.id}`)
  }
}

// Handle pagination
const handlePageChange = (page: number) => {
  currentPage.value = page
  handleSearch(searchQuery.value, searchType.value)
}

// Handle sort change
const handleSortChange = (sort: string) => {
  sortBy.value = sort
  if (searchQuery.value) {
    handleSearch(searchQuery.value, searchType.value)
  }
}

// Quick action: search recent nodes
const searchRecent = async () => {
  isLoading.value = true
  hasSearched.value = true
  
  try {
    const response = await nodesApi.getAll(0, 10, undefined, undefined, 'createdAt', 'DESC')
    
    // Convert to search results format
    searchResults.value = response.content.map(node => ({
      id: node.id,
      type: node.type,
      title: node.name,
      snippet: JSON.stringify(node.properties).substring(0, 150) + '...',
      score: 1.0,
      sourceUri: node.sourceUri,
      createdAt: node.createdAt,
      connectionCount: 0
    }))
    
    totalElements.value = response.totalElements
    totalPages.value = response.totalPages
    currentPage.value = 0
  } catch (error) {
    console.error('Failed to fetch recent nodes:', error)
  } finally {
    isLoading.value = false
  }
}

// Quick action: search most connected nodes
const searchPopular = async () => {
  isLoading.value = true
  hasSearched.value = true
  
  try {
    // This would ideally be a dedicated endpoint for most connected nodes
    const response = await nodesApi.getAll(0, 10, undefined, undefined, 'updatedAt', 'DESC')
    
    // Convert to search results format
    searchResults.value = response.content.map(node => ({
      id: node.id,
      type: node.type,
      title: node.name,
      snippet: `Node of type ${node.type}`,
      score: 1.0,
      sourceUri: node.sourceUri,
      createdAt: node.createdAt,
      connectionCount: 0
    }))
    
    totalElements.value = response.totalElements
    totalPages.value = response.totalPages
    currentPage.value = 0
  } catch (error) {
    console.error('Failed to fetch popular nodes:', error)
  } finally {
    isLoading.value = false
  }
}

// Watch for route changes
watch(() => route.query.q, (newQuery) => {
  if (newQuery && typeof newQuery === 'string') {
    searchQuery.value = newQuery
    handleSearch(newQuery, searchType.value)
  }
})
</script>