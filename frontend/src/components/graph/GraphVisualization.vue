<template>
  <div class="relative h-full">
    <!-- Graph Container -->
    <div ref="graphContainer" class="w-full h-full bg-gray-50"></div>
    
    <!-- Controls Panel -->
    <div class="absolute top-4 left-4 bg-white rounded-lg shadow-lg p-4 space-y-3">
      <h3 class="text-sm font-semibold text-gray-700 mb-2">Graph Controls</h3>
      
      <!-- Layout Options -->
      <div>
        <label class="text-xs font-medium text-gray-600">Layout</label>
        <select
          v-model="currentLayout"
          @change="applyLayout"
          class="mt-1 w-full text-sm border border-gray-300 rounded-md px-2 py-1"
        >
          <option value="cose">Force Directed</option>
          <option value="circle">Circle</option>
          <option value="grid">Grid</option>
          <option value="breadthfirst">Hierarchical</option>
          <option value="concentric">Concentric</option>
        </select>
      </div>
      
      <!-- Zoom Controls -->
      <div class="flex items-center space-x-2">
        <button
          @click="zoomIn"
          class="p-1.5 text-gray-600 hover:text-gray-900 bg-gray-100 rounded hover:bg-gray-200"
          title="Zoom In"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0zM10 7v6m3-3H7" />
          </svg>
        </button>
        <button
          @click="zoomOut"
          class="p-1.5 text-gray-600 hover:text-gray-900 bg-gray-100 rounded hover:bg-gray-200"
          title="Zoom Out"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0zM13 10H7" />
          </svg>
        </button>
        <button
          @click="fitToScreen"
          class="p-1.5 text-gray-600 hover:text-gray-900 bg-gray-100 rounded hover:bg-gray-200"
          title="Fit to Screen"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 8V4m0 0h4M4 4l5 5m11-1V4m0 0h-4m4 0l-5 5M4 16v4m0 0h4m-4 0l5-5m11 5l-5-5m5 5v-4m0 4h-4" />
          </svg>
        </button>
      </div>
      
      <!-- Filter by Node Type -->
      <div>
        <label class="text-xs font-medium text-gray-600">Filter by Type</label>
        <div class="mt-1 space-y-1 max-h-32 overflow-y-auto">
          <label
            v-for="nodeType in nodeTypes"
            :key="nodeType"
            class="flex items-center text-xs"
          >
            <input
              type="checkbox"
              :checked="visibleNodeTypes.has(nodeType)"
              @change="toggleNodeType(nodeType)"
              class="mr-1.5 h-3 w-3 text-primary-600 rounded border-gray-300"
            />
            <span>{{ nodeType }}</span>
          </label>
        </div>
      </div>
      
      <!-- Display Options -->
      <div class="space-y-1">
        <label class="flex items-center text-xs">
          <input
            v-model="showLabels"
            type="checkbox"
            class="mr-1.5 h-3 w-3 text-primary-600 rounded border-gray-300"
          />
          <span>Show Labels</span>
        </label>
        <label class="flex items-center text-xs">
          <input
            v-model="showEdgeLabels"
            type="checkbox"
            class="mr-1.5 h-3 w-3 text-primary-600 rounded border-gray-300"
          />
          <span>Show Edge Labels</span>
        </label>
      </div>
      
      <!-- Actions -->
      <div class="pt-2 border-t border-gray-200 space-y-2">
        <button
          @click="exportImage"
          class="w-full px-3 py-1.5 text-xs font-medium text-gray-700 bg-gray-100 rounded hover:bg-gray-200"
        >
          Export as Image
        </button>
        <button
          @click="resetView"
          class="w-full px-3 py-1.5 text-xs font-medium text-gray-700 bg-gray-100 rounded hover:bg-gray-200"
        >
          Reset View
        </button>
      </div>
    </div>
    
    <!-- Info Panel -->
    <div v-if="selectedNode" class="absolute top-4 right-4 bg-white rounded-lg shadow-lg p-4 w-64">
      <div class="flex justify-between items-start mb-3">
        <h3 class="text-sm font-semibold text-gray-900">Node Details</h3>
        <button
          @click="selectedNode = null"
          class="text-gray-400 hover:text-gray-600"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>
      
      <div class="space-y-2">
        <div>
          <span class="text-xs font-medium text-gray-500">Name</span>
          <p class="text-sm text-gray-900">{{ selectedNode.name }}</p>
        </div>
        <div>
          <span class="text-xs font-medium text-gray-500">Type</span>
          <p class="text-sm text-gray-900">{{ selectedNode.type }}</p>
        </div>
        <div v-if="selectedNode.connections">
          <span class="text-xs font-medium text-gray-500">Connections</span>
          <p class="text-sm text-gray-900">{{ selectedNode.connections }}</p>
        </div>
        
        <div class="pt-2 border-t border-gray-200">
          <button
            @click="viewNodeDetails(selectedNode.id)"
            class="w-full px-3 py-1.5 text-xs font-medium text-white bg-primary-600 rounded hover:bg-primary-700"
          >
            View Full Details
          </button>
        </div>
      </div>
    </div>
    
    <!-- Legend -->
    <div class="absolute bottom-4 left-4 bg-white rounded-lg shadow-lg p-3">
      <h4 class="text-xs font-semibold text-gray-700 mb-2">Legend</h4>
      <div class="space-y-1">
        <div
          v-for="(color, type) in nodeColorMap"
          :key="type"
          class="flex items-center space-x-2"
        >
          <div
            :style="{ backgroundColor: color }"
            class="w-3 h-3 rounded-full"
          ></div>
          <span class="text-xs text-gray-600">{{ type }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import cytoscape, { Core, NodeSingular, EdgeSingular } from 'cytoscape'
import type { GraphNode, GraphEdge } from '@/api/graphApi'

const props = defineProps<{
  nodes: GraphNode[]
  edges: GraphEdge[]
  centerNodeId?: string
  highlightPath?: string[]
}>()

const emit = defineEmits<{
  nodeClick: [nodeId: string]
  edgeClick: [edgeId: string]
}>()

const router = useRouter()
const graphContainer = ref<HTMLElement>()
let cy: Core | null = null

// State
const selectedNode = ref<any>(null)
const currentLayout = ref('cose')
const showLabels = ref(true)
const showEdgeLabels = ref(false)
const visibleNodeTypes = ref(new Set<string>())

// Node color mapping
const nodeColorMap = {
  PERSON: '#3B82F6',
  ORGANIZATION: '#10B981',
  EVENT: '#8B5CF6',
  LOCATION: '#F97316',
  PLACE: '#F97316',
  CONCEPT: '#6366F1',
  DOCUMENT: '#6B7280',
  PROJECT: '#EC4899',
  SYSTEM: '#F59E0B'
}

// Get unique node types from data
const nodeTypes = computed(() => {
  const types = new Set<string>()
  props.nodes.forEach(node => types.add(node.type))
  return Array.from(types).sort()
})

// Initialize Cytoscape
const initializeGraph = () => {
  if (!graphContainer.value) return
  
  // Prepare elements for Cytoscape
  const elements = [
    // Nodes
    ...props.nodes.map(node => ({
      data: {
        id: node.id,
        label: node.name,
        type: node.type,
        hopLevel: node.hopLevel,
        centrality: node.centrality
      },
      classes: node.id === props.centerNodeId ? 'center' : ''
    })),
    // Edges
    ...props.edges.map(edge => ({
      data: {
        id: edge.id,
        source: edge.sourceId,
        target: edge.targetId,
        label: edge.type,
        hopLevel: edge.hopLevel
      }
    }))
  ]
  
  // Initialize Cytoscape
  cy = cytoscape({
    container: graphContainer.value,
    elements,
    style: [
      // Node styles
      {
        selector: 'node',
        style: {
          'background-color': (ele: any) => nodeColorMap[ele.data('type')] || '#6B7280',
          'label': showLabels.value ? 'data(label)' : '',
          'text-valign': 'center',
          'text-halign': 'center',
          'font-size': '12px',
          'color': '#fff',
          'text-outline-width': 2,
          'text-outline-color': (ele: any) => nodeColorMap[ele.data('type')] || '#6B7280',
          'width': (ele: any) => 30 + (ele.data('centrality') || 0) * 20,
          'height': (ele: any) => 30 + (ele.data('centrality') || 0) * 20
        }
      },
      // Center node style
      {
        selector: '.center',
        style: {
          'border-width': 3,
          'border-color': '#DC2626',
          'background-color': '#DC2626'
        }
      },
      // Selected node style
      {
        selector: ':selected',
        style: {
          'border-width': 3,
          'border-color': '#FBBF24',
          'background-color': '#F59E0B'
        }
      },
      // Edge styles
      {
        selector: 'edge',
        style: {
          'width': 2,
          'line-color': '#9CA3AF',
          'target-arrow-color': '#9CA3AF',
          'target-arrow-shape': 'triangle',
          'curve-style': 'bezier',
          'label': showEdgeLabels.value ? 'data(label)' : '',
          'font-size': '10px',
          'text-rotation': 'autorotate',
          'text-margin-y': -10
        }
      },
      // Highlighted edge style
      {
        selector: '.highlighted',
        style: {
          'line-color': '#EF4444',
          'target-arrow-color': '#EF4444',
          'width': 3
        }
      }
    ],
    layout: {
      name: currentLayout.value,
      animate: true,
      animationDuration: 500,
      fit: true,
      padding: 50
    },
    minZoom: 0.1,
    maxZoom: 5,
    wheelSensitivity: 0.2
  })
  
  // Initialize visible node types
  nodeTypes.value.forEach(type => visibleNodeTypes.value.add(type))
  
  // Event handlers
  cy.on('tap', 'node', (event: any) => {
    const node = event.target
    selectedNode.value = {
      id: node.data('id'),
      name: node.data('label'),
      type: node.data('type'),
      connections: node.degree()
    }
    emit('nodeClick', node.data('id'))
  })
  
  cy.on('tap', 'edge', (event: any) => {
    const edge = event.target
    emit('edgeClick', edge.data('id'))
  })
  
  cy.on('tap', (event: any) => {
    if (event.target === cy) {
      selectedNode.value = null
    }
  })
  
  // Highlight path if provided
  if (props.highlightPath && props.highlightPath.length > 1) {
    highlightPath(props.highlightPath)
  }
}

// Layout functions
const applyLayout = () => {
  if (!cy) return
  
  const layout = cy.layout({
    name: currentLayout.value,
    animate: true,
    animationDuration: 500,
    fit: true,
    padding: 50
  })
  
  layout.run()
}

// Zoom functions
const zoomIn = () => {
  if (!cy) return
  cy.zoom(cy.zoom() * 1.2)
}

const zoomOut = () => {
  if (!cy) return
  cy.zoom(cy.zoom() * 0.8)
}

const fitToScreen = () => {
  if (!cy) return
  cy.fit(undefined, 50)
}

const resetView = () => {
  if (!cy) return
  cy.reset()
  applyLayout()
}

// Filter functions
const toggleNodeType = (type: string) => {
  if (visibleNodeTypes.value.has(type)) {
    visibleNodeTypes.value.delete(type)
  } else {
    visibleNodeTypes.value.add(type)
  }
  updateNodeVisibility()
}

const updateNodeVisibility = () => {
  if (!cy) return
  
  cy.nodes().forEach((node: NodeSingular) => {
    const nodeType = node.data('type')
    if (visibleNodeTypes.value.has(nodeType)) {
      node.style('display', 'element')
    } else {
      node.style('display', 'none')
    }
  })
}

// Highlight path
const highlightPath = (nodePath: string[]) => {
  if (!cy) return
  
  // Reset all edges
  cy.edges().removeClass('highlighted')
  
  // Highlight edges in path
  for (let i = 0; i < nodePath.length - 1; i++) {
    const sourceId = nodePath[i]
    const targetId = nodePath[i + 1]
    
    const edge = cy.edges(`[source = "${sourceId}"][target = "${targetId}"], [source = "${targetId}"][target = "${sourceId}"]`)
    edge.addClass('highlighted')
  }
}

// Export functions
const exportImage = () => {
  if (!cy) return
  
  const png = cy.png({
    output: 'blob',
    bg: 'white',
    scale: 2
  })
  
  // Create download link
  const url = URL.createObjectURL(png as Blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `graph-${Date.now()}.png`
  link.click()
  URL.revokeObjectURL(url)
}

// Navigation
const viewNodeDetails = (nodeId: string) => {
  router.push(`/node/${nodeId}`)
}

// Watch for label changes
watch(showLabels, () => {
  if (!cy) return
  cy.style()
    .selector('node')
    .style('label', showLabels.value ? 'data(label)' : '')
    .update()
})

watch(showEdgeLabels, () => {
  if (!cy) return
  cy.style()
    .selector('edge')
    .style('label', showEdgeLabels.value ? 'data(label)' : '')
    .update()
})

// Lifecycle
onMounted(() => {
  initializeGraph()
})

onUnmounted(() => {
  if (cy) {
    cy.destroy()
  }
})
</script>

<style scoped>
/* Ensure the container takes full height */
.h-full {
  height: 100%;
  min-height: 600px;
}
</style>