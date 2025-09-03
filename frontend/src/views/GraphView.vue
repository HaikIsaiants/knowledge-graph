<template>
  <div class="max-w-full">
    <div class="mb-6 flex items-center justify-between">
      <h1 class="text-3xl font-bold text-gray-900">Graph Visualization</h1>
      
      <!-- Graph Controls -->
      <div class="flex items-center space-x-4">
        <div class="flex items-center space-x-2">
          <label class="text-sm font-medium text-gray-700">Layout:</label>
          <select v-model="selectedLayout" @change="changeLayout" class="input text-sm">
            <option value="cose">Force Directed</option>
            <option value="grid">Grid</option>
            <option value="circle">Circle</option>
            <option value="breadthfirst">Hierarchical</option>
          </select>
        </div>
        
        <button @click="fitGraph" class="btn btn-secondary text-sm">
          <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 8V4m0 0h4M4 4l5 5m11-1V4m0 0h-4m4 0l-5 5M4 16v4m0 0h4m-4 0l5-5m11 5l-5-5m5 5v-4m0 4h-4" />
          </svg>
          Fit to Screen
        </button>
        
        <button @click="resetGraph" class="btn btn-secondary text-sm">
          <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
          </svg>
          Reset
        </button>
      </div>
    </div>

    <!-- Graph Container -->
    <div class="card p-0 overflow-hidden">
      <div
        ref="graphContainer"
        class="w-full bg-gray-50"
        style="height: 600px;"
      >
        <!-- Graph will be rendered here by Cytoscape -->
      </div>
      
      <!-- Graph Legend -->
      <div class="p-4 border-t border-gray-200 bg-white">
        <div class="flex items-center justify-between">
          <div class="flex items-center space-x-6">
            <div class="text-sm font-medium text-gray-700">Node Types:</div>
            <div class="flex items-center space-x-4">
              <div
                v-for="nodeType in nodeTypes"
                :key="nodeType.type"
                class="flex items-center space-x-2"
              >
                <div
                  :class="[
                    'w-4 h-4 rounded-full border-2',
                    nodeType.color
                  ]"
                ></div>
                <span class="text-sm text-gray-600">{{ nodeType.type }}</span>
              </div>
            </div>
          </div>
          
          <div class="flex items-center space-x-4 text-sm text-gray-600">
            <div>Nodes: {{ graphStats.nodeCount }}</div>
            <div>Edges: {{ graphStats.edgeCount }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Selected Node Panel -->
    <div v-if="selectedNode" class="mt-6 card">
      <div class="flex items-start justify-between">
        <div class="flex-1">
          <div class="flex items-center space-x-2 mb-2">
            <span
              :class="[
                'px-2 py-1 text-xs font-medium rounded-full',
                getTypeColor(selectedNode.type)
              ]"
            >
              {{ selectedNode.type }}
            </span>
            <h3 class="text-lg font-semibold text-gray-900">{{ selectedNode.name }}</h3>
          </div>
          
          <div class="text-sm text-gray-600 mb-4">
            {{ Object.keys(selectedNode.properties).length }} properties
          </div>
          
          <div class="flex items-center space-x-4">
            <button
              @click="expandNode(selectedNode.id)"
              class="btn btn-primary text-sm"
            >
              Expand Neighbors
            </button>
            <RouterLink
              :to="`/node/${selectedNode.id}`"
              class="btn btn-secondary text-sm"
            >
              View Details
            </RouterLink>
          </div>
        </div>
        
        <button
          @click="selectedNode = null"
          class="text-gray-400 hover:text-gray-600"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="isLoading" class="absolute inset-0 bg-white bg-opacity-75 flex items-center justify-center">
      <div class="text-center">
        <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600 mx-auto mb-4"></div>
        <div class="text-gray-600">Loading graph...</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import { useMainStore } from '@/stores/main'
import type { Node, NodeType } from '@/types'

const route = useRoute()
const store = useMainStore()

const graphContainer = ref<HTMLElement | null>(null)
const selectedLayout = ref('cose')
const selectedNode = ref<Node | null>(null)
const isLoading = ref(true)
let cy: any = null // Cytoscape instance

const nodeTypes = [
  { type: 'PERSON', color: 'bg-blue-500 border-blue-600' },
  { type: 'ORGANIZATION', color: 'bg-green-500 border-green-600' },
  { type: 'EVENT', color: 'bg-purple-500 border-purple-600' },
  { type: 'PLACE', color: 'bg-orange-500 border-orange-600' },
  { type: 'CONCEPT', color: 'bg-indigo-500 border-indigo-600' },
  { type: 'DOCUMENT', color: 'bg-gray-500 border-gray-600' }
]

const graphStats = ref({
  nodeCount: 0,
  edgeCount: 0
})

const getTypeColor = (type: NodeType) => {
  const colors: Record<NodeType, string> = {
    PERSON: 'bg-blue-100 text-blue-800',
    ORGANIZATION: 'bg-green-100 text-green-800',
    EVENT: 'bg-purple-100 text-purple-800',
    PLACE: 'bg-orange-100 text-orange-800',
    ITEM: 'bg-yellow-100 text-yellow-800',
    CONCEPT: 'bg-indigo-100 text-indigo-800',
    DOCUMENT: 'bg-gray-100 text-gray-800'
  }
  return colors[type]
}

const getNodeColor = (type: NodeType) => {
  const colors: Record<string, string> = {
    PERSON: '#3b82f6',      // Blue
    ORGANIZATION: '#10b981', // Green
    EVENT: '#8b5cf6',        // Purple
    PLACE: '#f97316',        // Orange
    LOCATION: '#f97316',     // Orange (same as PLACE)
    ITEM: '#eab308',         // Yellow
    CONCEPT: '#6366f1',      // Indigo
    DOCUMENT: '#6b7280',     // Gray
    DATE: '#ef4444',         // Red
    PRODUCT: '#14b8a6',      // Teal
    TERM: '#a855f7',         // Purple
    ENTITY: '#94a3b8',       // Slate
    OTHER: '#737373',        // Neutral gray
  }
  return colors[type] || '#94a3b8' // Default to slate if type not found
}

const initializeGraph = async () => {
  if (!graphContainer.value) return

  // Dynamically import Cytoscape to avoid SSR issues
  const cytoscape = (await import('cytoscape')).default

  // Fetch real data from backend
  const elements = []
  
  try {
    // Fetch all nodes
    const nodesResponse = await fetch('/api/nodes?page=0&size=1000')
    const nodesData = await nodesResponse.json()
    
    // Fetch all edges
    const edgesResponse = await fetch('/api/edges?page=0&size=1000')
    const edgesData = await edgesResponse.json()
    
    // Create a set of node IDs that have relationships
    const connectedNodeIds = new Set<string>()
    
    // Create edge elements and collect connected node IDs
    edgesData.content.forEach((edge: any) => {
      connectedNodeIds.add(edge.sourceId)
      connectedNodeIds.add(edge.targetId)
      
      // Convert SNAKE_CASE to readable format
      const readableType = (edge.type || 'RELATED_TO')
        .toLowerCase()
        .split('_')
        .map((word: string) => word.charAt(0).toUpperCase() + word.slice(1))
        .join(' ')
      
      elements.push({
        data: {
          id: edge.id,
          source: edge.sourceId,
          target: edge.targetId,
          relationship: readableType
        }
      })
    })
    
    // Only add nodes that have at least one relationship
    let connectedNodeCount = 0
    nodesData.content.forEach((node: any) => {
      if (connectedNodeIds.has(node.id)) {
        elements.push({
          data: { 
            id: node.id, 
            name: node.name, 
            type: node.type || 'ENTITY'
          }
        })
        connectedNodeCount++
      }
    })
    
    // Update stats to show actual displayed counts
    graphStats.value.nodeCount = connectedNodeCount
    graphStats.value.edgeCount = edgesData.totalElements
    
    console.log('Showing', connectedNodeCount, 'connected nodes (out of', nodesData.totalElements, 'total) and', edgesData.totalElements, 'edges')
    
  } catch (error) {
    console.error('Failed to fetch graph data:', error)
  }

  cy = cytoscape({
    container: graphContainer.value,
    elements,
    style: [
      {
        selector: 'node',
        style: {
          'background-color': (node: any) => getNodeColor(node.data('type')),
          'label': 'data(name)',
          'text-valign': 'center',
          'text-halign': 'center',
          'color': '#fff',
          'font-size': '10px',
          'font-weight': '500',
          'text-outline-width': 1.5,
          'text-outline-color': '#000',
          'width': 70,
          'height': 70,
          'border-width': 2,
          'border-color': '#fff',
          'text-wrap': 'wrap',
          'text-max-width': '65px',
          'padding': '5px'
        }
      },
      {
        selector: 'edge',
        style: {
          'width': 2,
          'line-color': '#cbd5e1',
          'target-arrow-color': '#94a3b8',
          'target-arrow-shape': 'triangle',
          'target-arrow-size': 6,
          'curve-style': 'bezier',
          'control-point-step-size': 40,
          'edge-text-rotation': 'autorotate',
          'label': 'data(relationship)',
          'font-size': '9px',
          'font-weight': '500',
          'text-rotation': 'autorotate',
          'text-margin-y': -12,
          'color': '#1e293b',
          'line-opacity': 0.5,
          'arrow-scale': 1,
          'text-outline-width': 3,
          'text-outline-color': '#ffffff',
          'text-outline-opacity': 0.9
        }
      },
      {
        selector: 'node:selected',
        style: {
          'border-color': '#3b82f6',
          'border-width': 4
        }
      },
      {
        selector: 'edge:selected',
        style: {
          'line-color': '#3b82f6',
          'target-arrow-color': '#3b82f6',
          'width': 4
        }
      }
    ],
    layout: {
      name: selectedLayout.value,
      animate: true,
      animationDuration: 500,
      // Force-directed (cose) layout options for much better spacing
      idealEdgeLength: 250,  // Much longer edges
      nodeOverlap: 100,      // More overlap prevention
      nodeRepulsion: 10000,  // Much stronger repulsion
      nestingFactor: 0.5,
      gravity: -100,         // Negative gravity to spread out
      numIter: 2000,         // More iterations for better layout
      padding: 50,           // Padding around the graph
      // Grid layout options
      spacingFactor: 3,      // Much more spacing in grid
      avoidOverlap: true,
      condense: false,
      // Circle layout options  
      radius: 400,           // Much larger circle
      avoidOverlap: true,
      // Breadthfirst layout options
      spacingFactor: 3,      // Much more spacing
      avoidOverlap: true
    },
    minZoom: 0.1,
    maxZoom: 3,
    wheelSensitivity: 0.2
  })

  // Event listeners
  cy.on('tap', 'node', (evt: any) => {
    const nodeData = evt.target.data()
    selectedNode.value = {
      id: nodeData.id,
      name: nodeData.name,
      type: nodeData.type as NodeType,
      properties: {},
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    }
  })

  cy.on('tap', (evt: any) => {
    if (evt.target === cy) {
      selectedNode.value = null
    }
  })

  // Update stats
  graphStats.value = {
    nodeCount: cy.nodes().length,
    edgeCount: cy.edges().length
  }

  isLoading.value = false
}

const changeLayout = () => {
  if (cy) {
    cy.layout({ name: selectedLayout.value, animate: true }).run()
  }
}

const fitGraph = () => {
  if (cy) {
    cy.fit()
  }
}

const resetGraph = () => {
  if (cy) {
    cy.reset()
    selectedNode.value = null
  }
}

const expandNode = async (nodeId: string) => {
  // TODO: Implement node expansion by fetching connected nodes
  console.log('Expanding node:', nodeId)
}

onMounted(async () => {
  await initializeGraph()
  
  // Handle URL parameters
  const nodeId = route.query.node as string
  if (nodeId && cy) {
    const node = cy.getElementById(nodeId)
    if (node.length > 0) {
      cy.center(node)
      node.trigger('tap')
    }
  }
})

onUnmounted(() => {
  if (cy) {
    cy.destroy()
  }
})
</script>