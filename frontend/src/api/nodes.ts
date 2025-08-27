import { apiCall } from './client'
import type { Node, NodeType, PaginatedResponse, Edge } from '@/types'

export interface NodeDetailDTO {
  id: string
  type: NodeType
  name: string
  properties: Record<string, any>
  sourceUri?: string
  capturedAt?: string
  createdAt: string
  updatedAt: string
  outgoingEdges: EdgeDTO[]
  incomingEdges: EdgeDTO[]
  citations: CitationDTO[]
  embeddings: EmbeddingDTO[]
  totalConnections: number
  connectionsByType: Record<string, number>
}

export interface EdgeDTO {
  id: string
  nodeId: string
  nodeName: string
  nodeType: NodeType
  edgeType: string
  properties: Record<string, any>
  direction: 'outgoing' | 'incoming'
}

export interface CitationDTO {
  documentId?: string
  documentUri?: string
  contentSnippet?: string
  startOffset?: number
  endOffset?: number
  pageNumber?: number
  extractedAt?: string
}

export interface EmbeddingDTO {
  id: string
  contentSnippet?: string
  modelVersion: string
  createdAt: string
}

export const nodesApi = {
  // Get all nodes with pagination
  getAll: (page = 0, size = 20, type?: NodeType, name?: string, sortBy = 'createdAt', direction = 'DESC') => {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
      sortBy,
      direction
    })
    if (type) params.append('type', type)
    if (name) params.append('name', name)
    
    return apiCall<PaginatedResponse<Node>>('GET', `/nodes?${params}`)
  },

  // Get node by ID with full details
  getById: (id: string) =>
    apiCall<NodeDetailDTO>('GET', `/nodes/${id}`),

  // Get nodes by type
  getByType: (type: NodeType, page = 0, size = 20) =>
    apiCall<PaginatedResponse<Node>>('GET', `/nodes?type=${type}&page=${page}&size=${size}`),

  // Get connected nodes (related)
  getRelated: (nodeId: string, limit = 10) =>
    apiCall<Node[]>('GET', `/nodes/${nodeId}/related?limit=${limit}`),

  // Get node citations
  getCitations: (nodeId: string) =>
    apiCall<CitationDTO[]>('GET', `/nodes/${nodeId}/citations`),

  // Get node edges
  getEdges: (nodeId: string) =>
    apiCall<{ outgoing: EdgeDTO[], incoming: EdgeDTO[] }>('GET', `/nodes/${nodeId}/edges`),

  // Create node
  create: (node: Omit<Node, 'id' | 'createdAt' | 'updatedAt'>) =>
    apiCall<Node>('POST', '/nodes', node),

  // Update node
  update: (id: string, node: Partial<Node>) =>
    apiCall<Node>('PUT', `/nodes/${id}`, node),

  // Delete node
  delete: (id: string) =>
    apiCall<void>('DELETE', `/nodes/${id}`)
}