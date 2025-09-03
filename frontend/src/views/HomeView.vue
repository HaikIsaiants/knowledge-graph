<template>
  <div class="max-w-4xl mx-auto">
    <!-- Hero Section -->
    <div class="text-center mb-12">
      <h1 class="text-4xl font-bold text-gray-900 mb-4">
        Personal Knowledge Graph
      </h1>
      <p class="text-xl text-gray-600 mb-8 max-w-2xl mx-auto">
        Discover connections, explore relationships, and navigate your knowledge with hybrid search capabilities.
      </p>
      
      <div class="flex flex-col sm:flex-row gap-4 justify-center">
        <RouterLink to="/search" class="btn btn-primary text-lg px-8 py-3">
          Start Searching
        </RouterLink>
        <RouterLink to="/graph" class="btn btn-secondary text-lg px-8 py-3">
          Explore Graph
        </RouterLink>
      </div>
    </div>

    <!-- Stats Cards -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-12">
      <div class="card text-center">
        <div class="text-3xl font-bold text-primary-600 mb-2">{{ stats.nodes }}</div>
        <div class="text-gray-600">Nodes</div>
      </div>
      <div class="card text-center">
        <div class="text-3xl font-bold text-primary-600 mb-2">{{ stats.edges }}</div>
        <div class="text-gray-600">Relationships</div>
      </div>
      <div class="card text-center">
        <div class="text-3xl font-bold text-primary-600 mb-2">{{ stats.documents }}</div>
        <div class="text-gray-600">Documents</div>
      </div>
    </div>

    <!-- Features -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-8 mb-12">
      <div class="card">
        <div class="flex items-start space-x-4">
          <div class="flex-shrink-0">
            <div class="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
              <svg class="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </div>
          </div>
          <div>
            <h3 class="text-lg font-semibold text-gray-900 mb-2">Hybrid Search</h3>
            <p class="text-gray-600">
              Combine full-text search with vector similarity for powerful, contextual results.
            </p>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="flex items-start space-x-4">
          <div class="flex-shrink-0">
            <div class="w-10 h-10 bg-green-100 rounded-lg flex items-center justify-center">
              <svg class="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
              </svg>
            </div>
          </div>
          <div>
            <h3 class="text-lg font-semibold text-gray-900 mb-2">Relationship Mapping</h3>
            <p class="text-gray-600">
              Visualize and explore complex relationships between entities in your knowledge base.
            </p>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="flex items-start space-x-4">
          <div class="flex-shrink-0">
            <div class="w-10 h-10 bg-purple-100 rounded-lg flex items-center justify-center">
              <svg class="w-6 h-6 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
            </div>
          </div>
          <div>
            <h3 class="text-lg font-semibold text-gray-900 mb-2">Document Processing</h3>
            <p class="text-gray-600">
              Ingest and process multiple file formats including PDF, CSV, JSON, and Markdown.
            </p>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="flex items-start space-x-4">
          <div class="flex-shrink-0">
            <div class="w-10 h-10 bg-orange-100 rounded-lg flex items-center justify-center">
              <svg class="w-6 h-6 text-orange-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
              </svg>
            </div>
          </div>
          <div>
            <h3 class="text-lg font-semibold text-gray-900 mb-2">Citation Tracking</h3>
            <p class="text-gray-600">
              Every fact and relationship includes full provenance with source citations.
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- Health Check -->
    <div v-if="healthStatus" class="card">
      <div class="flex items-center justify-between">
        <div>
          <h3 class="text-lg font-semibold text-gray-900 mb-1">System Status</h3>
          <p class="text-sm text-gray-600">Backend connection: {{ healthStatus.status }}</p>
        </div>
        <div class="flex items-center space-x-2">
          <div :class="[
            'w-3 h-3 rounded-full',
            healthStatus.status === 'UP' ? 'bg-green-500' : 'bg-red-500'
          ]"></div>
          <span :class="[
            'text-sm font-medium',
            healthStatus.status === 'UP' ? 'text-green-600' : 'text-red-600'
          ]">
            {{ healthStatus.status === 'UP' ? 'Healthy' : 'Unavailable' }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { healthApi } from '@/api/client'
import { apiCall } from '@/api/client'

const stats = ref({
  nodes: '0',
  edges: '0',
  documents: '0'
})

const healthStatus = ref<any>(null)

onMounted(async () => {
  try {
    const health = await healthApi.check()
    healthStatus.value = health
    
    // Fetch actual stats from backend
    const graphStats = await apiCall<any>('GET', '/graph/stats')
    
    // Count documents from nodeTypes
    const documentCount = graphStats.nodeTypes?.DOCUMENT || 0
    
    stats.value = {
      nodes: String(graphStats.totalNodes || 0),
      edges: String(graphStats.totalEdges || 0),
      documents: String(documentCount)
    }
  } catch (error) {
    console.error('Failed to fetch stats:', error)
    healthStatus.value = { status: 'DOWN' }
  }
})
</script>