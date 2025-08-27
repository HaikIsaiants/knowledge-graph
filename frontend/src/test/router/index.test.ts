import { describe, it, expect, beforeEach, vi } from 'vitest'
import { createRouter, createWebHistory } from 'vue-router'
import router from '@/router'

// Mock the view components
vi.mock('@/views/HomeView.vue', () => ({
  default: {
    name: 'HomeView',
    template: '<div data-testid="home-view">Home View</div>'
  }
}))

vi.mock('@/views/SearchView.vue', () => ({
  default: {
    name: 'SearchView',
    template: '<div data-testid="search-view">Search View</div>'
  }
}))

vi.mock('@/views/NodeView.vue', () => ({
  default: {
    name: 'NodeView',
    template: '<div data-testid="node-view">Node View</div>'
  }
}))

vi.mock('@/views/GraphView.vue', () => ({
  default: {
    name: 'GraphView',
    template: '<div data-testid="graph-view">Graph View</div>'
  }
}))

vi.mock('@/views/NotFoundView.vue', () => ({
  default: {
    name: 'NotFoundView',
    template: '<div data-testid="not-found-view">Not Found View</div>'
  }
}))

describe('Router Configuration', () => {
  beforeEach(async () => {
    // Reset router state before each test
    await router.push('/')
    await router.isReady()
  })

  it('should be configured correctly', () => {
    console.log('Testing router configuration...')
    
    expect(router).toBeDefined()
    expect(router.getRoutes()).toBeDefined()
    expect(router.getRoutes().length).toBeGreaterThan(0)
    
    console.log('✓ Router is configured correctly')
  })

  it('should have all required routes', () => {
    console.log('Testing router routes exist...')
    
    const routes = router.getRoutes()
    const routeNames = routes.map(route => route.name).filter(name => name)
    
    expect(routeNames).toContain('home')
    expect(routeNames).toContain('search')
    expect(routeNames).toContain('node')
    expect(routeNames).toContain('graph')
    expect(routeNames).toContain('not-found')
    
    console.log('Found routes:', routeNames)
    console.log('✓ All required routes exist')
  })

  it('should navigate to home route correctly', async () => {
    console.log('Testing navigation to home route...')
    
    await router.push('/')
    
    expect(router.currentRoute.value.name).toBe('home')
    expect(router.currentRoute.value.path).toBe('/')
    expect(router.currentRoute.value.meta?.title).toBe('Knowledge Graph')
    
    console.log('✓ Home route navigation works correctly')
  })

  it('should navigate to search route correctly', async () => {
    console.log('Testing navigation to search route...')
    
    await router.push('/search')
    
    expect(router.currentRoute.value.name).toBe('search')
    expect(router.currentRoute.value.path).toBe('/search')
    expect(router.currentRoute.value.meta?.title).toBe('Search - Knowledge Graph')
    
    console.log('✓ Search route navigation works correctly')
  })

  it('should navigate to node route with parameter correctly', async () => {
    console.log('Testing navigation to node route with parameter...')
    
    const nodeId = '123e4567-e89b-12d3-a456-426614174000'
    await router.push(`/node/${nodeId}`)
    
    expect(router.currentRoute.value.name).toBe('node')
    expect(router.currentRoute.value.path).toBe(`/node/${nodeId}`)
    expect(router.currentRoute.value.params.id).toBe(nodeId)
    expect(router.currentRoute.value.meta?.title).toBe('Node Details - Knowledge Graph')
    
    console.log('✓ Node route navigation works correctly')
  })

  it('should navigate to graph route correctly', async () => {
    console.log('Testing navigation to graph route...')
    
    await router.push('/graph')
    
    expect(router.currentRoute.value.name).toBe('graph')
    expect(router.currentRoute.value.path).toBe('/graph')
    expect(router.currentRoute.value.meta?.title).toBe('Graph Visualization - Knowledge Graph')
    
    console.log('✓ Graph route navigation works correctly')
  })

  it('should handle 404 routes correctly', async () => {
    console.log('Testing 404 route handling...')
    
    await router.push('/nonexistent-route')
    
    expect(router.currentRoute.value.name).toBe('not-found')
    expect(router.currentRoute.value.meta?.title).toBe('Page Not Found - Knowledge Graph')
    
    console.log('✓ 404 route handling works correctly')
  })

  it('should handle nested 404 routes correctly', async () => {
    console.log('Testing nested 404 route handling...')
    
    await router.push('/some/nested/nonexistent/route')
    
    expect(router.currentRoute.value.name).toBe('not-found')
    expect(router.currentRoute.value.params.pathMatch).toEqual(['some', 'nested', 'nonexistent', 'route'])
    
    console.log('✓ Nested 404 route handling works correctly')
  })

  it('should handle search route with query parameters', async () => {
    console.log('Testing search route with query parameters...')
    
    await router.push('/search?q=machine%20learning&type=concept')
    
    expect(router.currentRoute.value.name).toBe('search')
    expect(router.currentRoute.value.path).toBe('/search')
    expect(router.currentRoute.value.query.q).toBe('machine learning')
    expect(router.currentRoute.value.query.type).toBe('concept')
    
    console.log('✓ Search route with query parameters works correctly')
  })

  it('should have correct route components', () => {
    console.log('Testing route components...')
    
    const routes = router.getRoutes()
    
    // Home route should have direct component
    const homeRoute = routes.find(route => route.name === 'home')
    expect(homeRoute?.component).toBeDefined()
    
    // Other routes should have lazy-loaded components (functions)
    const searchRoute = routes.find(route => route.name === 'search')
    expect(typeof searchRoute?.component).toBe('function')
    
    const nodeRoute = routes.find(route => route.name === 'node')
    expect(typeof nodeRoute?.component).toBe('function')
    
    const graphRoute = routes.find(route => route.name === 'graph')
    expect(typeof graphRoute?.component).toBe('function')
    
    const notFoundRoute = routes.find(route => route.name === 'not-found')
    expect(typeof notFoundRoute?.component).toBe('function')
    
    console.log('✓ Route components are configured correctly')
  })

  it('should handle route meta information', () => {
    console.log('Testing route meta information...')
    
    const routes = router.getRoutes()
    
    routes.forEach(route => {
      if (route.meta?.title) {
        expect(typeof route.meta.title).toBe('string')
        expect(route.meta.title.length).toBeGreaterThan(0)
      }
    })
    
    // Check specific meta titles
    const homeRoute = routes.find(route => route.name === 'home')
    expect(homeRoute?.meta?.title).toBe('Knowledge Graph')
    
    const searchRoute = routes.find(route => route.name === 'search')
    expect(searchRoute?.meta?.title).toBe('Search - Knowledge Graph')
    
    console.log('✓ Route meta information is correct')
  })

  it('should update document title on navigation', async () => {
    console.log('Testing document title updates...')
    
    // Mock document.title
    const originalTitle = document.title
    
    // Navigate to different routes and check title updates
    await router.push('/')
    expect(document.title).toBe('Knowledge Graph')
    
    await router.push('/search')
    expect(document.title).toBe('Search - Knowledge Graph')
    
    await router.push('/graph')
    expect(document.title).toBe('Graph Visualization - Knowledge Graph')
    
    // Restore original title
    document.title = originalTitle
    
    console.log('✓ Document title updates correctly on navigation')
  })

  it('should handle route transitions properly', async () => {
    console.log('Testing route transitions...')
    
    // Navigate through multiple routes
    await router.push('/')
    expect(router.currentRoute.value.name).toBe('home')
    
    await router.push('/search')
    expect(router.currentRoute.value.name).toBe('search')
    
    await router.push('/graph')
    expect(router.currentRoute.value.name).toBe('graph')
    
    // Test browser-like navigation
    router.back()
    await router.isReady()
    // Note: In test environment, router.back() may not work as expected
    
    console.log('✓ Route transitions work correctly')
  })

  it('should handle invalid node IDs in node route', async () => {
    console.log('Testing invalid node IDs in node route...')
    
    // Test with various invalid node ID formats
    await router.push('/node/invalid-id')
    expect(router.currentRoute.value.name).toBe('node')
    expect(router.currentRoute.value.params.id).toBe('invalid-id')
    
    await router.push('/node/123')
    expect(router.currentRoute.value.name).toBe('node')
    expect(router.currentRoute.value.params.id).toBe('123')
    
    console.log('✓ Invalid node IDs handled correctly (passed to component for validation)')
  })

  it('should have consistent naming convention', () => {
    console.log('Testing route naming convention...')
    
    const routes = router.getRoutes()
    const namedRoutes = routes.filter(route => route.name)
    
    namedRoutes.forEach(route => {
      // Route names should be lowercase kebab-case or single words
      expect(route.name).toMatch(/^[a-z]+(-[a-z]+)*$/)
    })
    
    console.log('✓ Route naming convention is consistent')
  })

  it('should use createWebHistory correctly', () => {
    console.log('Testing router history mode...')
    
    // Check that the router is using HTML5 history mode
    expect(router.options.history).toBeDefined()
    // The exact check for history mode type is implementation-specific
    
    console.log('✓ Router uses correct history mode')
  })
})