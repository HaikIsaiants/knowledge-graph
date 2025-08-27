<template>
  <div class="bg-white rounded-lg shadow-sm border border-gray-200">
    <div class="px-4 py-3 border-b border-gray-200">
      <h3 class="text-lg font-semibold text-gray-900">
        Citations
        <span class="ml-2 text-sm font-normal text-gray-500">({{ citations.length }})</span>
      </h3>
    </div>
    
    <div v-if="citations.length > 0" class="divide-y divide-gray-200">
      <div
        v-for="(citation, index) in displayedCitations"
        :key="index"
        class="p-4 hover:bg-gray-50 transition-colors"
      >
        <!-- Citation Content -->
        <div class="mb-2">
          <p class="text-sm text-gray-700 leading-relaxed">
            <span v-if="citation.contentSnippet" class="italic">
              "{{ citation.contentSnippet }}"
            </span>
            <span v-else class="text-gray-500">
              [Citation content not available]
            </span>
          </p>
        </div>
        
        <!-- Citation Metadata -->
        <div class="flex items-center space-x-4 text-xs text-gray-500">
          <!-- Document Link -->
          <div v-if="citation.documentUri" class="flex items-center space-x-1">
            <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            <a
              :href="citation.documentUri"
              target="_blank"
              class="hover:text-primary-600 underline"
              @click.stop
            >
              {{ formatDocumentUri(citation.documentUri) }}
            </a>
          </div>
          
          <!-- Page Number -->
          <div v-if="citation.pageNumber" class="flex items-center space-x-1">
            <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
            </svg>
            <span>Page {{ citation.pageNumber }}</span>
          </div>
          
          <!-- Text Position -->
          <div v-if="citation.startOffset !== undefined && citation.endOffset !== undefined">
            <span>Characters {{ citation.startOffset }}-{{ citation.endOffset }}</span>
          </div>
          
          <!-- Extraction Date -->
          <div v-if="citation.extractedAt" class="flex items-center space-x-1">
            <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span>{{ formatDate(citation.extractedAt) }}</span>
          </div>
        </div>
        
        <!-- Actions -->
        <div class="mt-3 flex items-center space-x-2">
          <button
            v-if="citation.documentId"
            @click="$emit('viewDocument', citation.documentId)"
            class="text-xs text-primary-600 hover:text-primary-700 font-medium"
          >
            View Document
          </button>
          <button
            @click="$emit('viewContext', citation)"
            class="text-xs text-primary-600 hover:text-primary-700 font-medium"
          >
            View Context
          </button>
        </div>
      </div>
    </div>
    
    <!-- Empty State -->
    <div v-else class="p-8 text-center">
      <svg class="w-12 h-12 text-gray-400 mx-auto mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
      </svg>
      <p class="text-gray-500">No citations available</p>
    </div>
    
    <!-- Show More/Less -->
    <div v-if="citations.length > initialDisplayCount" class="px-4 py-3 border-t border-gray-200">
      <button
        @click="toggleShowAll"
        class="text-sm text-primary-600 hover:text-primary-700 font-medium"
      >
        {{ showAll ? 'Show Less' : `Show ${citations.length - initialDisplayCount} More` }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { CitationDTO } from '@/api/nodes'
import { formatRelativeDate, extractFilename } from '@/utils/formatters'

const props = defineProps<{
  citations: CitationDTO[]
  initialDisplayCount?: number
}>()

const emit = defineEmits<{
  viewDocument: [documentId: string]
  viewContext: [citation: CitationDTO]
}>()

const showAll = ref(false)
const initialDisplayCount = props.initialDisplayCount || 3

const displayedCitations = computed(() => {
  if (showAll.value || props.citations.length <= initialDisplayCount) {
    return props.citations
  }
  return props.citations.slice(0, initialDisplayCount)
})

const toggleShowAll = () => {
  showAll.value = !showAll.value
}

// Simplified: Use shared utility functions
const formatDocumentUri = extractFilename

const formatDate = (dateStr: string) => {
  const relativeDate = formatRelativeDate(dateStr)
  
  // For dates older than a month, show actual date
  if (relativeDate.includes('months ago') || relativeDate.includes('years ago')) {
    return new Date(dateStr).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    })
  }
  
  return relativeDate
}
</script>