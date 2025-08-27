import { apiCall } from './client'
import type { Node, Document } from '@/types'

export interface SearchResultDTO {
  id: string
  type?: string
  title: string
  snippet?: string
  highlightedSnippet?: string
  score: number
  sourceUri?: string
  metadata?: Record<string, any>
  createdAt: string
  updatedAt: string
  documentId?: string
  connectionCount?: number
}

export interface SearchResponseDTO {
  results: SearchResultDTO[]
  totalElements: number
  totalPages: number
  currentPage: number
  pageSize: number
  query: string
  searchType: 'FULL_TEXT' | 'VECTOR' | 'HYBRID' | 'GRAPH'
  searchTimeMs?: number
  typeFacets?: Record<string, number>
  minScore?: number
  maxScore?: number
  suggestedQueries?: string[]
}

export const searchApi = {
  // Full-text search
  search: (query: string, type?: string, page = 0, size = 10, highlight = true) => {
    const params = new URLSearchParams({
      q: query,
      page: page.toString(),
      size: size.toString(),
      highlight: highlight.toString()
    })
    if (type) params.append('type', type)
    
    return apiCall<SearchResponseDTO>('GET', `/search?${params}`)
  },

  // Vector similarity search
  vectorSearch: (query: string, threshold?: number, limit = 10) => {
    const params = new URLSearchParams({
      q: query,
      limit: limit.toString()
    })
    if (threshold !== undefined) params.append('threshold', threshold.toString())
    
    return apiCall<SearchResponseDTO>('GET', `/search/vector?${params}`)
  },

  // Hybrid search combining FTS and vector
  hybridSearch: (
    query: string, 
    ftsWeight?: number, 
    vectorWeight?: number, 
    page = 0, 
    size = 10
  ) => {
    const params = new URLSearchParams({
      q: query,
      page: page.toString(),
      size: size.toString()
    })
    if (ftsWeight !== undefined) params.append('ftsWeight', ftsWeight.toString())
    if (vectorWeight !== undefined) params.append('vectorWeight', vectorWeight.toString())
    
    return apiCall<SearchResponseDTO>('GET', `/search/hybrid?${params}`)
  },

  // Adaptive hybrid search
  adaptiveSearch: (query: string, page = 0, size = 10) => {
    const params = new URLSearchParams({
      q: query,
      page: page.toString(),
      size: size.toString()
    })
    
    return apiCall<SearchResponseDTO>('GET', `/search/adaptive?${params}`)
  },

  // Find similar nodes
  findSimilar: (nodeId: string, limit = 10) => {
    const params = new URLSearchParams({
      limit: limit.toString()
    })
    
    return apiCall<SearchResponseDTO>('GET', `/search/similar/${nodeId}?${params}`)
  },

  // Get search suggestions
  getSuggestions: (query: string) => {
    const params = new URLSearchParams({ q: query })
    return apiCall<string[]>('GET', `/search/suggest?${params}`)
  },

  // Search documents
  searchDocuments: (query: string, page = 0, size = 10) => {
    const params = new URLSearchParams({
      q: query,
      page: page.toString(),
      size: size.toString()
    })
    
    return apiCall<SearchResponseDTO>('GET', `/search/documents?${params}`)
  }
}