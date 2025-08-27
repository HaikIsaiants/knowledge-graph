import { defineStore } from 'pinia'
import { ref, readonly } from 'vue'
import type { Node, Edge, SearchResult } from '@/types'

export const useMainStore = defineStore('main', () => {
  // Simplified state - fewer separate pieces
  const isLoading = ref(false)
  const error = ref<string | null>(null)
  const search = ref({
    query: '',
    results: [] as SearchResult[]
  })
  const selectedNode = ref<Node | null>(null)
  const graphData = ref<{ nodes: Node[]; edges: Edge[] }>({
    nodes: [],
    edges: []
  })

  // Simplified actions - combine related operations
  function updateSearch(query: string, results: SearchResult[] = []) {
    search.value = { query, results }
  }

  function clearSearch() {
    search.value = { query: '', results: [] }
  }

  function updateGraphData(nodes: Node[], edges: Edge[]) {
    graphData.value = { nodes, edges }
  }

  function updateGraphNode(node: Node) {
    const index = graphData.value.nodes.findIndex(n => n.id === node.id)
    if (index >= 0) {
      graphData.value.nodes[index] = node
    } else {
      graphData.value.nodes.push(node)
    }
  }

  function removeGraphNode(nodeId: string) {
    graphData.value.nodes = graphData.value.nodes.filter(n => n.id !== nodeId)
    graphData.value.edges = graphData.value.edges.filter(
      e => e.sourceId !== nodeId && e.targetId !== nodeId
    )
  }

  return {
    // State
    isLoading,
    error,
    search: readonly(search),
    selectedNode,
    graphData: readonly(graphData),
    
    // Actions
    updateSearch,
    clearSearch,
    updateGraphData,
    updateGraphNode,
    removeGraphNode,
    
    // Simple setters for remaining state
    setLoading: (loading: boolean) => isLoading.value = loading,
    setError: (errorMessage: string | null) => error.value = errorMessage,
    setSelectedNode: (node: Node | null) => selectedNode.value = node
  }
})