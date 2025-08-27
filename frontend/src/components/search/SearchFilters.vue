<template>
  <div class="bg-white rounded-lg shadow-sm border border-gray-200 p-4">
    <h3 class="text-lg font-semibold mb-4">Filters</h3>
    
    <!-- Node Type Filter -->
    <div class="mb-6">
      <label class="block text-sm font-medium text-gray-700 mb-2">
        Node Type
      </label>
      <div class="space-y-2">
        <label
          v-for="nodeType in nodeTypes"
          :key="nodeType.value"
          class="flex items-center"
        >
          <input
            type="checkbox"
            :value="nodeType.value"
            :checked="filters.nodeTypes?.includes(nodeType.value)"
            @change="toggleNodeType(nodeType.value)"
            class="h-4 w-4 text-primary-600 rounded border-gray-300 focus:ring-primary-500"
          />
          <span class="ml-2 text-sm text-gray-700">
            {{ nodeType.label }}
            <span class="text-gray-500">({{ nodeType.count }})</span>
          </span>
        </label>
      </div>
    </div>

    <!-- Date Range Filter -->
    <div class="mb-6">
      <label class="block text-sm font-medium text-gray-700 mb-2">
        Date Range
      </label>
      <select
        v-model="filters.dateRange"
        @change="updateFilters"
        class="w-full text-sm border border-gray-300 rounded-md px-3 py-2"
      >
        <option value="">All time</option>
        <option value="today">Today</option>
        <option value="week">Last 7 days</option>
        <option value="month">Last 30 days</option>
        <option value="year">Last year</option>
        <option value="custom">Custom range</option>
      </select>
      
      <!-- Custom Date Range -->
      <div v-if="filters.dateRange === 'custom'" class="mt-2 space-y-2">
        <input
          v-model="filters.startDate"
          type="date"
          @change="updateFilters"
          class="w-full text-sm border border-gray-300 rounded-md px-3 py-2"
          placeholder="Start date"
        />
        <input
          v-model="filters.endDate"
          type="date"
          @change="updateFilters"
          class="w-full text-sm border border-gray-300 rounded-md px-3 py-2"
          placeholder="End date"
        />
      </div>
    </div>

    <!-- Score Range Filter -->
    <div class="mb-6">
      <label class="block text-sm font-medium text-gray-700 mb-2">
        Minimum Score
      </label>
      <div class="flex items-center space-x-2">
        <input
          v-model.number="filters.minScore"
          type="range"
          min="0"
          max="1"
          step="0.1"
          @input="updateFilters"
          class="flex-1"
        />
        <span class="text-sm text-gray-600 w-12">{{ filters.minScore }}</span>
      </div>
    </div>

    <!-- Connection Count Filter -->
    <div class="mb-6">
      <label class="block text-sm font-medium text-gray-700 mb-2">
        Minimum Connections
      </label>
      <input
        v-model.number="filters.minConnections"
        type="number"
        min="0"
        @change="updateFilters"
        class="w-full text-sm border border-gray-300 rounded-md px-3 py-2"
        placeholder="0"
      />
    </div>

    <!-- Has Citations Filter -->
    <div class="mb-6">
      <label class="flex items-center">
        <input
          v-model="filters.hasCitations"
          type="checkbox"
          @change="updateFilters"
          class="h-4 w-4 text-primary-600 rounded border-gray-300 focus:ring-primary-500"
        />
        <span class="ml-2 text-sm text-gray-700">Has citations only</span>
      </label>
    </div>

    <!-- Source URI Filter -->
    <div class="mb-6">
      <label class="block text-sm font-medium text-gray-700 mb-2">
        Source URI
      </label>
      <input
        v-model="filters.sourceUri"
        type="text"
        @input="debouncedUpdateFilters"
        class="w-full text-sm border border-gray-300 rounded-md px-3 py-2"
        placeholder="Filter by source..."
      />
    </div>

    <!-- Actions -->
    <div class="flex space-x-2">
      <button
        @click="clearFilters"
        class="flex-1 px-4 py-2 text-sm text-gray-700 bg-gray-100 rounded-md hover:bg-gray-200 transition-colors"
      >
        Clear All
      </button>
      <button
        @click="applyFilters"
        class="flex-1 px-4 py-2 text-sm text-white bg-primary-600 rounded-md hover:bg-primary-700 transition-colors"
      >
        Apply
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { useDebouncedFn } from '@/composables/useDebounce'
import type { NodeType } from '@/types'

export interface SearchFilters {
  nodeTypes?: NodeType[]
  dateRange?: string
  startDate?: string
  endDate?: string
  minScore?: number
  minConnections?: number
  hasCitations?: boolean
  sourceUri?: string
}

const props = defineProps<{
  modelValue: SearchFilters
  nodeTypeCounts?: Record<NodeType, number>
}>()

const emit = defineEmits<{
  'update:modelValue': [filters: SearchFilters]
  'apply': [filters: SearchFilters]
}>()

const filters = reactive<SearchFilters>({
  nodeTypes: props.modelValue.nodeTypes || [],
  dateRange: props.modelValue.dateRange || '',
  startDate: props.modelValue.startDate || '',
  endDate: props.modelValue.endDate || '',
  minScore: props.modelValue.minScore || 0,
  minConnections: props.modelValue.minConnections || 0,
  hasCitations: props.modelValue.hasCitations || false,
  sourceUri: props.modelValue.sourceUri || ''
})

// Node types with counts
const nodeTypes = ref([
  { value: 'PERSON' as NodeType, label: 'Person', count: props.nodeTypeCounts?.PERSON || 0 },
  { value: 'ORGANIZATION' as NodeType, label: 'Organization', count: props.nodeTypeCounts?.ORGANIZATION || 0 },
  { value: 'EVENT' as NodeType, label: 'Event', count: props.nodeTypeCounts?.EVENT || 0 },
  { value: 'LOCATION' as NodeType, label: 'Location', count: props.nodeTypeCounts?.LOCATION || 0 },
  { value: 'PLACE' as NodeType, label: 'Place', count: props.nodeTypeCounts?.PLACE || 0 },
  { value: 'CONCEPT' as NodeType, label: 'Concept', count: props.nodeTypeCounts?.CONCEPT || 0 },
  { value: 'DOCUMENT' as NodeType, label: 'Document', count: props.nodeTypeCounts?.DOCUMENT || 0 },
  { value: 'PROJECT' as NodeType, label: 'Project', count: props.nodeTypeCounts?.PROJECT || 0 },
  { value: 'SYSTEM' as NodeType, label: 'System', count: props.nodeTypeCounts?.SYSTEM || 0 }
])

// Simplified: Use map for immutable update
watch(() => props.nodeTypeCounts, (newCounts) => {
  if (!newCounts) return
  
  nodeTypes.value = nodeTypes.value.map(nt => ({
    ...nt,
    count: newCounts[nt.value] || 0
  }))
})

// Sync with parent
watch(() => props.modelValue, (newVal) => {
  Object.assign(filters, newVal)
}, { deep: true })

// Simplified: Cleaner toggle logic using Set operations
const toggleNodeType = (type: NodeType) => {
  filters.nodeTypes = filters.nodeTypes || []
  const typeSet = new Set(filters.nodeTypes)
  
  // Toggle: add if not present, remove if present
  typeSet.has(type) ? typeSet.delete(type) : typeSet.add(type)
  filters.nodeTypes = Array.from(typeSet)
  
  updateFilters()
}

const updateFilters = () => {
  emit('update:modelValue', { ...filters })
}

const debouncedUpdateFilters = useDebouncedFn(updateFilters, 300)

const applyFilters = () => {
  emit('apply', { ...filters })
}

// Simplified: Use Object.assign for cleaner reset
const clearFilters = () => {
  Object.assign(filters, {
    nodeTypes: [],
    dateRange: '',
    startDate: '',
    endDate: '',
    minScore: 0,
    minConnections: 0,
    hasCitations: false,
    sourceUri: ''
  })
  updateFilters()
  applyFilters()
}
</script>