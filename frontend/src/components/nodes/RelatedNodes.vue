<template>
  <div class="bg-white rounded-lg shadow-sm border border-gray-200">
    <div class="px-4 py-3 border-b border-gray-200">
      <h3 class="text-lg font-semibold text-gray-900">
        Related Nodes
        <span class="ml-2 text-sm font-normal text-gray-500">({{ totalConnections }})</span>
      </h3>
    </div>
    
    <!-- Tabs for Incoming/Outgoing -->
    <div class="flex border-b border-gray-200">
      <button
        @click="activeTab = 'outgoing'"
        :class="[
          'flex-1 px-4 py-2 text-sm font-medium',
          activeTab === 'outgoing'
            ? 'text-primary-600 border-b-2 border-primary-600'
            : 'text-gray-600 hover:text-gray-900'
        ]"
      >
        Outgoing ({{ outgoingEdges.length }})
      </button>
      <button
        @click="activeTab = 'incoming'"
        :class="[
          'flex-1 px-4 py-2 text-sm font-medium',
          activeTab === 'incoming'
            ? 'text-primary-600 border-b-2 border-primary-600'
            : 'text-gray-600 hover:text-gray-900'
        ]"
      >
        Incoming ({{ incomingEdges.length }})
      </button>
    </div>
    
    <!-- Edge List -->
    <div v-if="currentEdges.length > 0" class="divide-y divide-gray-200 max-h-96 overflow-y-auto">
      <div
        v-for="edge in currentEdges"
        :key="edge.id"
        class="p-4 hover:bg-gray-50 transition-colors cursor-pointer"
        @click="$emit('selectNode', edge.nodeId)"
      >
        <div class="flex items-start justify-between">
          <div class="flex-1">
            <!-- Relationship Type -->
            <div class="mb-2">
              <span class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-800">
                {{ formatEdgeType(edge.edgeType) }}
              </span>
            </div>
            
            <!-- Node Info -->
            <div class="flex items-center space-x-2 mb-1">
              <span
                :class="[
                  'px-2 py-0.5 text-xs font-medium rounded-full',
                  getTypeColor(edge.nodeType)
                ]"
              >
                {{ edge.nodeType }}
              </span>
              <h4 class="text-sm font-semibold text-gray-900">
                {{ edge.nodeName }}
              </h4>
            </div>
            
            <!-- Edge Properties -->
            <div v-if="edge.properties && Object.keys(edge.properties).length > 0" class="mt-2">
              <div class="flex flex-wrap gap-2">
                <span
                  v-for="(value, key) in edge.properties"
                  :key="key"
                  class="text-xs text-gray-600"
                >
                  <span class="font-medium">{{ key }}:</span> {{ formatEdgePropertyValue(value) }}
                </span>
              </div>
            </div>
          </div>
          
          <!-- Arrow Indicator -->
          <div class="ml-4">
            <svg
              :class="[
                'w-5 h-5 text-gray-400',
                activeTab === 'incoming' ? 'rotate-180' : ''
              ]"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
            </svg>
          </div>
        </div>
      </div>
    </div>
    
    <!-- Empty State -->
    <div v-else class="p-8 text-center">
      <svg class="w-12 h-12 text-gray-400 mx-auto mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
      </svg>
      <p class="text-gray-500">
        No {{ activeTab }} connections
      </p>
    </div>
    
    <!-- Connection Statistics -->
    <div v-if="connectionsByType && Object.keys(connectionsByType).length > 0" class="px-4 py-3 border-t border-gray-200 bg-gray-50">
      <h4 class="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">
        Connection Types
      </h4>
      <div class="flex flex-wrap gap-2">
        <span
          v-for="(count, type) in connectionsByType"
          :key="type"
          class="inline-flex items-center px-2 py-1 rounded-md text-xs font-medium bg-white border border-gray-200"
        >
          {{ type }}: {{ count }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { EdgeDTO } from '@/api/nodes'
import type { NodeType } from '@/types'
import { getNodeTypeColor, formatEdgeType, formatPropertyValue } from '@/utils/formatters'

const props = defineProps<{
  outgoingEdges: EdgeDTO[]
  incomingEdges: EdgeDTO[]
  totalConnections: number
  connectionsByType?: Record<string, number>
}>()

const emit = defineEmits<{
  selectNode: [nodeId: string]
}>()

const activeTab = ref<'outgoing' | 'incoming'>('outgoing')

const currentEdges = computed(() => {
  return activeTab.value === 'outgoing' ? props.outgoingEdges : props.incomingEdges
})

// Simplified: Use imported utility functions
const getTypeColor = getNodeTypeColor

// Override formatPropertyValue to use smaller max length for edge properties
const formatEdgePropertyValue = (value: any) => formatPropertyValue(value, 50)
</script>