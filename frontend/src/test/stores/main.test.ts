import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useMainStore } from '@/stores/main'
import type { Node, Edge, SearchResult } from '@/types'

describe('Main Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('should initialize with default state', () => {
    console.log('Testing main store initialization...')
    
    const store = useMainStore()
    
    // Check initial state
    expect(store.isLoading).toBe(false)
    expect(store.error).toBe(null)
    expect(store.search.query).toBe('')
    expect(store.search.results).toEqual([])
    expect(store.selectedNode).toBe(null)
    expect(store.graphData.nodes).toEqual([])
    expect(store.graphData.edges).toEqual([])
    
    console.log('✓ Store initializes with correct default state')
  })

  it('should update loading state correctly', () => {
    console.log('Testing loading state management...')
    
    const store = useMainStore()
    
    expect(store.isLoading).toBe(false)
    
    store.setLoading(true)
    expect(store.isLoading).toBe(true)
    
    store.setLoading(false)
    expect(store.isLoading).toBe(false)
    
    console.log('✓ Loading state management works correctly')
  })

  it('should update error state correctly', () => {
    console.log('Testing error state management...')
    
    const store = useMainStore()
    
    expect(store.error).toBe(null)
    
    store.setError('Test error message')
    expect(store.error).toBe('Test error message')
    
    store.setError(null)
    expect(store.error).toBe(null)
    
    console.log('✓ Error state management works correctly')
  })

  it('should update search state correctly', () => {
    console.log('Testing search state management...')
    
    const store = useMainStore()
    const mockResults: SearchResult[] = [
      {
        id: '1',
        type: 'node',
        title: 'Test Node',
        snippet: 'Test snippet',
        score: 0.95
      },
      {
        id: '2',
        type: 'document',
        title: 'Test Document',
        snippet: 'Document snippet',
        score: 0.87
      }
    ]
    
    expect(store.search.query).toBe('')
    expect(store.search.results).toEqual([])
    
    store.updateSearch('machine learning', mockResults)
    
    expect(store.search.query).toBe('machine learning')
    expect(store.search.results).toEqual(mockResults)
    expect(store.search.results.length).toBe(2)
    
    console.log('✓ Search state management works correctly')
  })

  it('should clear search state correctly', () => {
    console.log('Testing search state clearing...')
    
    const store = useMainStore()
    const mockResults: SearchResult[] = [
      {
        id: '1',
        type: 'node',
        title: 'Test Node',
        snippet: 'Test snippet',
        score: 0.95
      }
    ]
    
    // Set some search data
    store.updateSearch('test query', mockResults)
    expect(store.search.query).toBe('test query')
    expect(store.search.results.length).toBe(1)
    
    // Clear search
    store.clearSearch()
    expect(store.search.query).toBe('')
    expect(store.search.results).toEqual([])
    
    console.log('✓ Search state clearing works correctly')
  })

  it('should update selected node correctly', () => {
    console.log('Testing selected node management...')
    
    const store = useMainStore()
    const mockNode: Node = {
      id: 'node-1',
      type: 'PERSON',
      name: 'John Doe',
      properties: { occupation: 'Engineer' },
      createdAt: '2023-12-01T10:00:00Z',
      updatedAt: '2023-12-01T10:00:00Z'
    }
    
    expect(store.selectedNode).toBe(null)
    
    store.setSelectedNode(mockNode)
    expect(store.selectedNode).toEqual(mockNode)
    
    store.setSelectedNode(null)
    expect(store.selectedNode).toBe(null)
    
    console.log('✓ Selected node management works correctly')
  })

  it('should update graph data correctly', () => {
    console.log('Testing graph data management...')
    
    const store = useMainStore()
    const mockNodes: Node[] = [
      {
        id: 'node-1',
        type: 'PERSON',
        name: 'John Doe',
        properties: {},
        createdAt: '2023-12-01T10:00:00Z',
        updatedAt: '2023-12-01T10:00:00Z'
      },
      {
        id: 'node-2',
        type: 'ORGANIZATION',
        name: 'ACME Corp',
        properties: {},
        createdAt: '2023-12-01T10:00:00Z',
        updatedAt: '2023-12-01T10:00:00Z'
      }
    ]
    const mockEdges: Edge[] = [
      {
        id: 'edge-1',
        sourceId: 'node-1',
        targetId: 'node-2',
        type: 'AFFILIATED_WITH',
        properties: {},
        createdAt: '2023-12-01T10:00:00Z',
        updatedAt: '2023-12-01T10:00:00Z'
      }
    ]
    
    expect(store.graphData.nodes).toEqual([])
    expect(store.graphData.edges).toEqual([])
    
    store.updateGraphData(mockNodes, mockEdges)
    
    expect(store.graphData.nodes).toEqual(mockNodes)
    expect(store.graphData.edges).toEqual(mockEdges)
    expect(store.graphData.nodes.length).toBe(2)
    expect(store.graphData.edges.length).toBe(1)
    
    console.log('✓ Graph data management works correctly')
  })

  it('should update individual graph nodes correctly', () => {
    console.log('Testing individual graph node updates...')
    
    const store = useMainStore()
    const initialNode: Node = {
      id: 'node-1',
      type: 'PERSON',
      name: 'John Doe',
      properties: { age: 30 },
      createdAt: '2023-12-01T10:00:00Z',
      updatedAt: '2023-12-01T10:00:00Z'
    }
    
    // Set initial graph data
    store.updateGraphData([initialNode], [])
    expect(store.graphData.nodes.length).toBe(1)
    expect(store.graphData.nodes[0].name).toBe('John Doe')
    
    // Update existing node
    const updatedNode: Node = {
      ...initialNode,
      name: 'John Smith',
      properties: { age: 31 }
    }
    
    store.updateGraphNode(updatedNode)
    expect(store.graphData.nodes.length).toBe(1)
    expect(store.graphData.nodes[0].name).toBe('John Smith')
    expect(store.graphData.nodes[0].properties.age).toBe(31)
    
    console.log('✓ Individual graph node updates work correctly')
  })

  it('should add new graph nodes correctly', () => {
    console.log('Testing new graph node addition...')
    
    const store = useMainStore()
    const existingNode: Node = {
      id: 'node-1',
      type: 'PERSON',
      name: 'John Doe',
      properties: {},
      createdAt: '2023-12-01T10:00:00Z',
      updatedAt: '2023-12-01T10:00:00Z'
    }
    
    // Set initial graph data
    store.updateGraphData([existingNode], [])
    expect(store.graphData.nodes.length).toBe(1)
    
    // Add new node
    const newNode: Node = {
      id: 'node-2',
      type: 'ORGANIZATION',
      name: 'ACME Corp',
      properties: {},
      createdAt: '2023-12-01T10:00:00Z',
      updatedAt: '2023-12-01T10:00:00Z'
    }
    
    store.updateGraphNode(newNode)
    expect(store.graphData.nodes.length).toBe(2)
    expect(store.graphData.nodes.find(n => n.id === 'node-2')).toEqual(newNode)
    
    console.log('✓ New graph node addition works correctly')
  })

  it('should remove graph nodes correctly', () => {
    console.log('Testing graph node removal...')
    
    const store = useMainStore()
    const mockNodes: Node[] = [
      {
        id: 'node-1',
        type: 'PERSON',
        name: 'John Doe',
        properties: {},
        createdAt: '2023-12-01T10:00:00Z',
        updatedAt: '2023-12-01T10:00:00Z'
      },
      {
        id: 'node-2',
        type: 'ORGANIZATION',
        name: 'ACME Corp',
        properties: {},
        createdAt: '2023-12-01T10:00:00Z',
        updatedAt: '2023-12-01T10:00:00Z'
      }
    ]
    const mockEdges: Edge[] = [
      {
        id: 'edge-1',
        sourceId: 'node-1',
        targetId: 'node-2',
        type: 'AFFILIATED_WITH',
        properties: {},
        createdAt: '2023-12-01T10:00:00Z',
        updatedAt: '2023-12-01T10:00:00Z'
      }
    ]
    
    // Set initial graph data
    store.updateGraphData(mockNodes, mockEdges)
    expect(store.graphData.nodes.length).toBe(2)
    expect(store.graphData.edges.length).toBe(1)
    
    // Remove node
    store.removeGraphNode('node-1')
    
    // Node should be removed
    expect(store.graphData.nodes.length).toBe(1)
    expect(store.graphData.nodes[0].id).toBe('node-2')
    
    // Connected edges should also be removed
    expect(store.graphData.edges.length).toBe(0)
    
    console.log('✓ Graph node removal works correctly')
  })

  it('should handle readonly state correctly', () => {
    console.log('Testing readonly state access...')
    
    const store = useMainStore()
    
    // These should be readonly and not throw errors when accessed
    expect(store.search).toBeDefined()
    expect(store.graphData).toBeDefined()
    
    // The actual reactive refs should not be directly accessible
    expect(store.search.query).toBe('')
    expect(store.graphData.nodes).toEqual([])
    
    console.log('✓ Readonly state access works correctly')
  })

  it('should handle empty search results correctly', () => {
    console.log('Testing empty search results handling...')
    
    const store = useMainStore()
    
    // Update with empty results
    store.updateSearch('no results query', [])
    
    expect(store.search.query).toBe('no results query')
    expect(store.search.results).toEqual([])
    expect(store.search.results.length).toBe(0)
    
    console.log('✓ Empty search results handled correctly')
  })

  it('should handle empty graph data correctly', () => {
    console.log('Testing empty graph data handling...')
    
    const store = useMainStore()
    
    // Set some data first
    const mockNode: Node = {
      id: 'node-1',
      type: 'PERSON',
      name: 'John Doe',
      properties: {},
      createdAt: '2023-12-01T10:00:00Z',
      updatedAt: '2023-12-01T10:00:00Z'
    }
    
    store.updateGraphData([mockNode], [])
    expect(store.graphData.nodes.length).toBe(1)
    
    // Clear with empty arrays
    store.updateGraphData([], [])
    
    expect(store.graphData.nodes).toEqual([])
    expect(store.graphData.edges).toEqual([])
    
    console.log('✓ Empty graph data handled correctly')
  })

  it('should maintain state consistency across actions', () => {
    console.log('Testing state consistency across multiple actions...')
    
    const store = useMainStore()
    
    // Perform multiple actions
    store.setLoading(true)
    store.setError('Initial error')
    store.updateSearch('test', [])
    
    expect(store.isLoading).toBe(true)
    expect(store.error).toBe('Initial error')
    expect(store.search.query).toBe('test')
    
    // Update loading and error
    store.setLoading(false)
    store.setError(null)
    
    expect(store.isLoading).toBe(false)
    expect(store.error).toBe(null)
    expect(store.search.query).toBe('test') // Search should remain
    
    console.log('✓ State consistency maintained across actions')
  })
})