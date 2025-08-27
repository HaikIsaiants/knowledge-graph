import { apiCall } from './client'
import type { Node, Edge } from '@/types'

export interface GraphNode {
  id: string
  type: string
  name: string
  properties: Record<string, any>
  hopLevel: number
  centrality?: number
}

export interface GraphEdge {
  id: string
  sourceId: string
  targetId: string
  type: string
  properties: Record<string, any>
  hopLevel: number
}

export interface GraphNeighborhoodDTO {
  centerNodeId: string
  requestedHops: number
  actualHops: number
  nodes: GraphNode[]
  edges: GraphEdge[]
  nodesPerHop: Record<number, number>
  totalNodes: number
  totalEdges: number
}

export interface PathResult {
  from: string
  to: string
  path: string[]
  distance: number
  found: boolean
}

export interface ComponentResult {
  nodeId: string
  componentSize: number
  nodeIds: string[]
}

export interface GraphStats {
  totalNodes: number
  totalEdges: number
  nodeTypes: Record<string, number>
  edgeTypes: Record<string, number>
  avgConnectionsPerNode: number
}

export const graphApi = {
  // Get node neighborhood
  getNeighborhood: (nodeId: string, hops = 1) => {
    const params = new URLSearchParams({
      hops: hops.toString()
    })
    
    return apiCall<GraphNeighborhoodDTO>('GET', `/graph/neighborhood/${nodeId}?${params}`)
  },

  // Find path between nodes
  findPath: (fromId: string, toId: string, maxHops = 5) => {
    const params = new URLSearchParams({
      from: fromId,
      to: toId,
      maxHops: maxHops.toString()
    })
    
    return apiCall<PathResult>('GET', `/graph/path?${params}`)
  },

  // Extract subgraph
  extractSubgraph: (nodeIds: string[]) => {
    return apiCall<GraphNeighborhoodDTO>('POST', '/graph/subgraph', nodeIds)
  },

  // Get connected component
  getConnectedComponent: (nodeId: string) => {
    return apiCall<ComponentResult>('GET', `/graph/component/${nodeId}`)
  },

  // Calculate centrality
  calculateCentrality: (nodeIds: string[]) => {
    return apiCall<Record<string, number>>('POST', '/graph/centrality', nodeIds)
  },

  // Get graph statistics
  getStatistics: () => {
    return apiCall<GraphStats>('GET', '/graph/stats')
  }
}