export interface Node {
  id: string
  type: NodeType
  name: string
  properties: Record<string, any>
  sourceUri?: string
  capturedAt?: string
  createdAt: string
  updatedAt: string
}

export interface Edge {
  id: string
  sourceId: string
  targetId: string
  type: EdgeType
  properties: Record<string, any>
  sourceUri?: string
  capturedAt?: string
  createdAt: string
  updatedAt: string
}

export interface Document {
  id: string
  uri: string
  content?: string
  contentType?: string
  metadata: Record<string, any>
  lastModified?: string
  etag?: string
  contentHash?: string
  createdAt: string
  updatedAt: string
}

export interface Embedding {
  id: string
  nodeId?: string
  documentId?: string
  contentSnippet?: string
  vector?: number[]
  modelVersion: string
  createdAt: string
}

export enum NodeType {
  PERSON = 'PERSON',
  ORGANIZATION = 'ORGANIZATION',
  EVENT = 'EVENT',
  PLACE = 'PLACE',
  ITEM = 'ITEM',
  CONCEPT = 'CONCEPT',
  DOCUMENT = 'DOCUMENT'
}

export enum EdgeType {
  AFFILIATED_WITH = 'AFFILIATED_WITH',
  PARTICIPATED_IN = 'PARTICIPATED_IN',
  LOCATED_IN = 'LOCATED_IN',
  PART_OF = 'PART_OF',
  REFERENCES = 'REFERENCES',
  PRODUCED_BY = 'PRODUCED_BY',
  SIMILAR_TO = 'SIMILAR_TO'
}

export interface SearchResult {
  node?: Node
  document?: Document
  snippet?: string
  score: number
  highlights?: string[]
}

export interface GraphData {
  nodes: Node[]
  edges: Edge[]
}

export interface ApiResponse<T> {
  data: T
  message?: string
  status: 'success' | 'error'
}

export interface PaginatedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
}