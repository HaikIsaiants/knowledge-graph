import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'

// Mock the API client
vi.mock('@/api/client', () => ({
  healthApi: {
    check: vi.fn()
  }
}))

import { healthApi } from '@/api/client'

// Mock router
const mockRoutes = [
  { path: '/', component: { template: '<div>Home</div>' } },
  { path: '/search', component: { template: '<div>Search</div>' } },
  { path: '/graph', component: { template: '<div>Graph</div>' } }
]

const mockRouter = createRouter({
  history: createWebHistory(),
  routes: mockRoutes
})

describe('HomeView Component', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should render the home view correctly', async () => {
    console.log('Testing HomeView component rendering...')
    
    // Mock successful health check
    vi.mocked(healthApi.check).mockResolvedValue({
      status: 'UP',
      timestamp: '2023-12-01T10:00:00Z',
      application: 'Knowledge Graph Backend',
      version: '0.0.1-SNAPSHOT'
    })
    
    const wrapper = mount(HomeView, {
      global: {
        plugins: [mockRouter]
      }
    })

    // Check hero section
    expect(wrapper.find('h1').text()).toBe('Personal Knowledge Graph')
    expect(wrapper.text()).toContain('Discover connections, explore relationships')
    
    // Check action buttons
    const buttons = wrapper.findAll('a')
    const buttonTexts = buttons.map(btn => btn.text())
    expect(buttonTexts).toContain('Start Searching')
    expect(buttonTexts).toContain('Explore Graph')
    
    console.log('✓ HomeView renders correctly')
  })

  it('should display stats cards with default values', async () => {
    console.log('Testing HomeView stats cards...')
    
    vi.mocked(healthApi.check).mockResolvedValue({ status: 'UP' })
    
    const wrapper = mount(HomeView, {
      global: {
        plugins: [mockRouter]
      }
    })

    // Wait for component to mount and health check to complete
    await wrapper.vm.$nextTick()
    await new Promise(resolve => setTimeout(resolve, 50))

    // Check stats cards
    const statCards = wrapper.findAll('.card')
    expect(statCards.length).toBeGreaterThan(2)
    
    // Should display sample stats after health check
    expect(wrapper.text()).toContain('24')  // nodes
    expect(wrapper.text()).toContain('56')  // edges  
    expect(wrapper.text()).toContain('12')  // documents
    
    expect(wrapper.text()).toContain('Nodes')
    expect(wrapper.text()).toContain('Relationships') 
    expect(wrapper.text()).toContain('Documents')
    
    console.log('✓ Stats cards display correctly')
  })

  it('should display feature cards', async () => {
    console.log('Testing HomeView feature cards...')
    
    vi.mocked(healthApi.check).mockResolvedValue({ status: 'UP' })
    
    const wrapper = mount(HomeView, {
      global: {
        plugins: [mockRouter]
      }
    })

    // Check feature headings
    expect(wrapper.text()).toContain('Hybrid Search')
    expect(wrapper.text()).toContain('Relationship Mapping')
    expect(wrapper.text()).toContain('Document Processing')
    expect(wrapper.text()).toContain('Citation Tracking')
    
    // Check feature descriptions
    expect(wrapper.text()).toContain('Combine full-text search with vector similarity')
    expect(wrapper.text()).toContain('Visualize and explore complex relationships')
    expect(wrapper.text()).toContain('Ingest and process multiple file formats')
    expect(wrapper.text()).toContain('Every fact and relationship includes full provenance')
    
    console.log('✓ Feature cards display correctly')
  })

  it('should handle successful health check', async () => {
    console.log('Testing HomeView successful health check...')
    
    const mockHealthResponse = {
      status: 'UP',
      timestamp: '2023-12-01T10:00:00Z',
      application: 'Knowledge Graph Backend',
      version: '0.0.1-SNAPSHOT'
    }
    
    vi.mocked(healthApi.check).mockResolvedValue(mockHealthResponse)
    
    const wrapper = mount(HomeView, {
      global: {
        plugins: [mockRouter]
      }
    })

    // Wait for health check to complete
    await wrapper.vm.$nextTick()
    await new Promise(resolve => setTimeout(resolve, 50))

    // Check health status display
    expect(wrapper.text()).toContain('System Status')
    expect(wrapper.text()).toContain('Backend connection: UP')
    expect(wrapper.text()).toContain('Healthy')
    
    // Check for green status indicator
    const statusIndicator = wrapper.find('.bg-green-500')
    expect(statusIndicator.exists()).toBe(true)
    
    const healthyText = wrapper.find('.text-green-600')
    expect(healthyText.exists()).toBe(true)
    expect(healthyText.text()).toBe('Healthy')
    
    console.log('✓ Successful health check handled correctly')
  })

  it('should handle failed health check', async () => {
    console.log('Testing HomeView failed health check...')
    
    vi.mocked(healthApi.check).mockRejectedValue(new Error('Network error'))
    
    const wrapper = mount(HomeView, {
      global: {
        plugins: [mockRouter]
      }
    })

    // Wait for health check to complete
    await wrapper.vm.$nextTick()
    await new Promise(resolve => setTimeout(resolve, 50))

    // Check error health status display
    expect(wrapper.text()).toContain('System Status')
    expect(wrapper.text()).toContain('Backend connection: DOWN')
    expect(wrapper.text()).toContain('Unavailable')
    
    // Check for red status indicator
    const statusIndicator = wrapper.find('.bg-red-500')
    expect(statusIndicator.exists()).toBe(true)
    
    const unavailableText = wrapper.find('.text-red-600')
    expect(unavailableText.exists()).toBe(true)
    expect(unavailableText.text()).toBe('Unavailable')
    
    console.log('✓ Failed health check handled correctly')
  })

  it('should have correct navigation links', async () => {
    console.log('Testing HomeView navigation links...')
    
    vi.mocked(healthApi.check).mockResolvedValue({ status: 'UP' })
    
    const wrapper = mount(HomeView, {
      global: {
        plugins: [mockRouter]
      }
    })

    // Check search button link
    const searchButton = wrapper.find('a[href="/search"]')
    expect(searchButton.exists()).toBe(true)
    expect(searchButton.text()).toBe('Start Searching')
    
    // Check graph button link  
    const graphButton = wrapper.find('a[href="/graph"]')
    expect(graphButton.exists()).toBe(true)
    expect(graphButton.text()).toBe('Explore Graph')
    
    console.log('✓ Navigation links are correct')
  })

  it('should have proper responsive layout classes', async () => {
    console.log('Testing HomeView responsive layout...')
    
    vi.mocked(healthApi.check).mockResolvedValue({ status: 'UP' })
    
    const wrapper = mount(HomeView, {
      global: {
        plugins: [mockRouter]
      }
    })

    // Check responsive grid classes
    const statsGrid = wrapper.find('.grid.grid-cols-1.md\\:grid-cols-3')
    expect(statsGrid.exists()).toBe(true)
    
    const featuresGrid = wrapper.find('.grid.grid-cols-1.md\\:grid-cols-2')
    expect(featuresGrid.exists()).toBe(true)
    
    // Check responsive button layout
    const buttonContainer = wrapper.find('.flex.flex-col.sm\\:flex-row')
    expect(buttonContainer.exists()).toBe(true)
    
    console.log('✓ Responsive layout classes are correct')
  })

  it('should call health API on component mount', async () => {
    console.log('Testing HomeView health API call on mount...')
    
    vi.mocked(healthApi.check).mockResolvedValue({ status: 'UP' })
    
    mount(HomeView, {
      global: {
        plugins: [mockRouter]
      }
    })

    // Verify health API was called
    expect(healthApi.check).toHaveBeenCalledTimes(1)
    
    console.log('✓ Health API called on component mount')
  })

  it('should update stats after successful health check', async () => {
    console.log('Testing HomeView stats update after health check...')
    
    vi.mocked(healthApi.check).mockResolvedValue({ status: 'UP' })
    
    const wrapper = mount(HomeView, {
      global: {
        plugins: [mockRouter]
      }
    })

    // Initially stats should be '0'
    expect(wrapper.text()).toContain('0')
    
    // Wait for health check and stats update
    await wrapper.vm.$nextTick()
    await new Promise(resolve => setTimeout(resolve, 50))
    
    // Stats should be updated to sample values
    expect(wrapper.text()).toContain('24')
    expect(wrapper.text()).toContain('56') 
    expect(wrapper.text()).toContain('12')
    
    console.log('✓ Stats updated after health check')
  })

  it('should have accessible structure', async () => {
    console.log('Testing HomeView accessibility structure...')
    
    vi.mocked(healthApi.check).mockResolvedValue({ status: 'UP' })
    
    const wrapper = mount(HomeView, {
      global: {
        plugins: [mockRouter]
      }
    })

    // Check for proper heading hierarchy
    const h1 = wrapper.find('h1')
    expect(h1.exists()).toBe(true)
    expect(h1.text()).toBe('Personal Knowledge Graph')
    
    const h3Elements = wrapper.findAll('h3')
    expect(h3Elements.length).toBeGreaterThan(0)
    
    // Check for proper button elements (links with button styling)
    const actionLinks = wrapper.findAll('a.btn')
    expect(actionLinks.length).toBeGreaterThan(0)
    
    console.log('✓ Accessibility structure is correct')
  })
})