<template>
  <div v-if="nodeDetail" class="min-h-screen bg-gray-50 py-8">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <!-- Header -->
      <div class="mb-8">
        <div class="flex items-start justify-between">
          <div class="flex items-start space-x-4">
            <!-- Back Button -->
            <button
              @click="$router.go(-1)"
              class="mt-1 p-2 rounded-lg text-gray-600 hover:text-gray-900 hover:bg-gray-100 transition-colors"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            
            <!-- Node Info -->
            <div>
              <div class="flex items-center space-x-3 mb-2">
                <span
                  :class="[
                    'px-3 py-1 text-sm font-medium rounded-full',
                    getTypeColor(nodeDetail.type)
                  ]"
                >
                  {{ nodeDetail.type }}
                </span>
                <span class="text-sm text-gray-500">ID: {{ nodeDetail.id }}</span>
                <span v-if="nodeDetail.totalConnections" class="text-sm text-gray-500">
                  • {{ nodeDetail.totalConnections }} connections
                </span>
              </div>
              <h1 class="text-3xl font-bold text-gray-900">{{ nodeDetail.name }}</h1>
            </div>
          </div>
          
          <!-- Actions -->
          <div class="flex items-center space-x-2">
            <button
              @click="editNode"
              class="px-4 py-2 text-sm text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50"
            >
              Edit
            </button>
            <button
              @click="deleteNode"
              class="px-4 py-2 text-sm text-red-600 bg-white border border-red-300 rounded-md hover:bg-red-50"
            >
              Delete
            </button>
          </div>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <!-- Main Content -->
        <div class="lg:col-span-2 space-y-6">
          <!-- Properties -->
          <div class="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
            <h2 class="text-xl font-semibold text-gray-900 mb-4">Properties</h2>
            <div v-if="Object.keys(nodeDetail.properties).length > 0" class="space-y-3">
              <div
                v-for="[key, value] in Object.entries(nodeDetail.properties)"
                :key="key"
                class="flex justify-between py-2 border-b border-gray-100 last:border-b-0"
              >
                <span class="font-medium text-gray-700">{{ formatPropertyKey(key) }}</span>
                <span class="text-gray-900 text-right max-w-md">{{ formatPropertyValue(value) }}</span>
              </div>
            </div>
            <div v-else class="text-gray-500 italic">No properties available</div>
          </div>

          <!-- Related Nodes -->
          <RelatedNodes
            :outgoing-edges="nodeDetail.outgoingEdges"
            :incoming-edges="nodeDetail.incomingEdges"
            :total-connections="nodeDetail.totalConnections"
            :connections-by-type="nodeDetail.connectionsByType"
            @select-node="navigateToNode"
          />

          <!-- Citations -->
          <CitationList
            :citations="nodeDetail.citations"
            :initial-display-count="3"
            @view-document="viewDocument"
            @view-context="viewContext"
          />

          <!-- Source Information -->
          <div v-if="nodeDetail.sourceUri" class="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
            <h2 class="text-xl font-semibold text-gray-900 mb-4">Source Information</h2>
            <div class="space-y-3">
              <div>
                <span class="font-medium text-gray-700">Source URI:</span>
                <a 
                  :href="nodeDetail.sourceUri" 
                  target="_blank" 
                  class="text-primary-600 hover:text-primary-700 ml-2 inline-flex items-center"
                >
                  {{ truncateUri(nodeDetail.sourceUri) }}
                  <svg class="w-4 h-4 ml-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
                  </svg>
                </a>
              </div>
              <div v-if="nodeDetail.capturedAt">
                <span class="font-medium text-gray-700">Captured:</span>
                <span class="text-gray-900 ml-2">{{ formatDate(nodeDetail.capturedAt) }}</span>
              </div>
            </div>
          </div>

          <!-- Embeddings Info -->
          <div v-if="nodeDetail.embeddings && nodeDetail.embeddings.length > 0" class="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
            <h2 class="text-xl font-semibold text-gray-900 mb-4">
              Vector Embeddings
              <span class="ml-2 text-sm font-normal text-gray-500">({{ nodeDetail.embeddings.length }})</span>
            </h2>
            <div class="space-y-3">
              <div
                v-for="embedding in nodeDetail.embeddings.slice(0, 3)"
                :key="embedding.id"
                class="p-3 bg-gray-50 rounded-md"
              >
                <div class="flex justify-between items-start mb-2">
                  <span class="text-xs font-medium text-gray-500">{{ embedding.modelVersion }}</span>
                  <span class="text-xs text-gray-500">{{ formatDate(embedding.createdAt) }}</span>
                </div>
                <p v-if="embedding.contentSnippet" class="text-sm text-gray-700 italic">
                  "{{ embedding.contentSnippet }}"
                </p>
              </div>
            </div>
          </div>
        </div>

        <!-- Sidebar -->
        <div class="space-y-6">
          <!-- Quick Actions -->
          <div class="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
            <h3 class="text-lg font-semibold text-gray-900 mb-4">Quick Actions</h3>
            <div class="space-y-2">
              <button
                @click="viewInGraph"
                class="w-full px-4 py-2 text-sm font-medium text-white bg-primary-600 rounded-md hover:bg-primary-700 flex items-center justify-center space-x-2"
              >
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
                </svg>
                <span>View in Graph</span>
              </button>
              
              <button
                @click="findSimilar"
                class="w-full px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 flex items-center justify-center space-x-2"
              >
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
                <span>Find Similar</span>
              </button>
              
              <button
                @click="findPath"
                class="w-full px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 flex items-center justify-center space-x-2"
              >
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7" />
                </svg>
                <span>Find Path to Node</span>
              </button>
            </div>
          </div>

          <!-- Metadata -->
          <div class="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
            <h3 class="text-lg font-semibold text-gray-900 mb-4">Metadata</h3>
            <div class="space-y-3 text-sm">
              <div>
                <span class="font-medium text-gray-700">Created:</span>
                <div class="text-gray-900">{{ formatDate(nodeDetail.createdAt) }}</div>
              </div>
              <div>
                <span class="font-medium text-gray-700">Updated:</span>
                <div class="text-gray-900">{{ formatDate(nodeDetail.updatedAt) }}</div>
              </div>
              <div v-if="nodeDetail.capturedAt">
                <span class="font-medium text-gray-700">Captured:</span>
                <div class="text-gray-900">{{ formatDate(nodeDetail.capturedAt) }}</div>
              </div>
            </div>
          </div>

          <!-- Connection Statistics -->
          <div class="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
            <h3 class="text-lg font-semibold text-gray-900 mb-4">Connection Statistics</h3>
            <div class="space-y-3">
              <div class="flex justify-between items-center">
                <span class="text-sm text-gray-700">Total Connections</span>
                <span class="text-lg font-semibold text-gray-900">{{ nodeDetail.totalConnections }}</span>
              </div>
              
              <div v-if="nodeDetail.connectionsByType && Object.keys(nodeDetail.connectionsByType).length > 0" class="pt-3 border-t border-gray-200">
                <h4 class="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">By Type</h4>
                <div class="space-y-2">
                  <div
                    v-for="(count, type) in nodeDetail.connectionsByType"
                    :key="type"
                    class="flex justify-between items-center"
                  >
                    <span class="text-sm text-gray-600">{{ type }}</span>
                    <span class="text-sm font-medium text-gray-900">{{ count }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- Loading State -->
  <div v-else-if="isLoading" class="min-h-screen flex items-center justify-center">
    <div class="text-center">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600 mx-auto"></div>
      <p class="mt-4 text-gray-600">Loading node details...</p>
    </div>
  </div>

  <!-- Error State -->
  <div v-else class="min-h-screen flex items-center justify-center">
    <div class="text-center">
      <svg class="w-16 h-16 text-gray-400 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.667-2.195-1.667-2.964 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
      </svg>
      <h3 class="text-lg font-semibold text-gray-900 mb-2">{{ error || 'Node not found' }}</h3>
      <p class="text-gray-600 mb-4">The requested node could not be loaded</p>
      <button @click="$router.push('/')" class="px-4 py-2 bg-primary-600 text-white rounded-md hover:bg-primary-700">
        Return Home
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { nodesApi, type NodeDetailDTO } from '@/api/nodes'
import { searchApi } from '@/api/searchApi'
import CitationList from '@/components/nodes/CitationList.vue'
import RelatedNodes from '@/components/nodes/RelatedNodes.vue'
import { getNodeTypeColor, formatRelativeDate, truncateString, formatPropertyKey, formatPropertyValue } from '@/utils/formatters'
import type { NodeType } from '@/types'

const route = useRoute()
const router = useRouter()

const nodeDetail = ref<NodeDetailDTO | null>(null)
const isLoading = ref(true)
const error = ref<string | null>(null)

// Simplified: Use imported utility functions
const getTypeColor = getNodeTypeColor

// Enhanced date formatting with fallback to full date for older items
const formatDate = (dateString: string): string => {
  const relativeDate = formatRelativeDate(dateString)
  
  // For dates older than a year, show full date instead
  if (relativeDate.includes('years ago')) {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  }
  
  return relativeDate
}

const truncateUri = (uri: string) => truncateString(uri, 60)

// Actions
const viewInGraph = () => {
  router.push(`/graph?node=${nodeDetail.value?.id}`)
}

const findSimilar = async () => {
  if (!nodeDetail.value) return
  
  try {
    const results = await searchApi.findSimilar(nodeDetail.value.id, 10)
    router.push(`/search?similar=${nodeDetail.value.id}`)
  } catch (error) {
    console.error('Failed to find similar nodes:', error)
  }
}

const findPath = () => {
  router.push(`/graph/path?from=${nodeDetail.value?.id}`)
}

const navigateToNode = (nodeId: string) => {
  router.push(`/node/${nodeId}`)
}

const viewDocument = (documentId: string) => {
  router.push(`/document/${documentId}`)
}

const viewContext = (citation: any) => {
  // Open modal or navigate to context view
  console.log('View context for citation:', citation)
}

const editNode = () => {
  router.push(`/node/${nodeDetail.value?.id}/edit`)
}

const deleteNode = async () => {
  if (!confirm('Are you sure you want to delete this node?')) return
  
  try {
    await nodesApi.delete(nodeDetail.value!.id)
    router.push('/')
  } catch (error) {
    console.error('Failed to delete node:', error)
  }
}

// Load node details
onMounted(async () => {
  const nodeId = route.params.id as string
  
  try {
    nodeDetail.value = await nodesApi.getById(nodeId)
  } catch (err: any) {
    console.error('Failed to load node:', err)
    error.value = err.message || 'Failed to load node'
  } finally {
    isLoading.value = false
  }
})
</script>