import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'

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

describe('AppHeader Component', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should render the component correctly', async () => {
    console.log('Testing AppHeader component rendering...')
    
    const wrapper = mount(AppHeader, {
      global: {
        plugins: [mockRouter]
      }
    })

    // Check that the header renders
    expect(wrapper.find('header').exists()).toBe(true)
    expect(wrapper.find('nav').exists()).toBe(true)
    
    // Check logo and title
    expect(wrapper.text()).toContain('Knowledge Graph')
    
    console.log('✓ AppHeader component renders correctly')
  })

  it('should render navigation links correctly', async () => {
    console.log('Testing AppHeader navigation links...')
    
    const wrapper = mount(AppHeader, {
      global: {
        plugins: [mockRouter]
      }
    })

    // Check navigation links
    const navLinks = wrapper.findAll('a')
    const linkTexts = navLinks.map(link => link.text())
    
    expect(linkTexts).toContain('Home')
    expect(linkTexts).toContain('Search')
    expect(linkTexts).toContain('Graph')
    
    // Check href attributes
    const homeLink = wrapper.find('a[href="/"]')
    const searchLink = wrapper.find('a[href="/search"]')
    const graphLink = wrapper.find('a[href="/graph"]')
    
    expect(homeLink.exists()).toBe(true)
    expect(searchLink.exists()).toBe(true)
    expect(graphLink.exists()).toBe(true)
    
    console.log('✓ Navigation links render correctly')
  })

  it('should handle search input correctly', async () => {
    console.log('Testing AppHeader search input functionality...')
    
    const mockPush = vi.fn()
    vi.spyOn(mockRouter, 'push').mockImplementation(mockPush)
    
    const wrapper = mount(AppHeader, {
      global: {
        plugins: [mockRouter]
      }
    })

    const searchInput = wrapper.find('input[type="text"]')
    expect(searchInput.exists()).toBe(true)
    expect(searchInput.attributes('placeholder')).toContain('Quick search')
    
    // Test search input value
    await searchInput.setValue('machine learning')
    expect((searchInput.element as HTMLInputElement).value).toBe('machine learning')
    
    // Test Enter key handler
    await searchInput.trigger('keyup.enter')
    
    // Verify router navigation was called
    expect(mockPush).toHaveBeenCalledWith('/search?q=machine%20learning')
    
    console.log('✓ Search input functionality works correctly')
  })

  it('should handle mobile menu toggle', async () => {
    console.log('Testing AppHeader mobile menu functionality...')
    
    const wrapper = mount(AppHeader, {
      global: {
        plugins: [mockRouter]
      }
    })

    // Mobile menu should be hidden initially
    expect(wrapper.find('[data-testid="mobile-menu"]').exists()).toBe(false)
    
    // Find and click mobile menu button
    const mobileMenuButton = wrapper.find('button')
    expect(mobileMenuButton.exists()).toBe(true)
    
    await mobileMenuButton.trigger('click')
    
    // Mobile menu should be visible after click
    const mobileMenu = wrapper.find('.md\\:hidden .flex-col')
    expect(mobileMenu.exists()).toBe(true)
    
    // Check mobile menu contains navigation links
    const mobileLinks = mobileMenu.findAll('a')
    const mobileLinkTexts = mobileLinks.map(link => link.text())
    expect(mobileLinkTexts).toContain('Home')
    expect(mobileLinkTexts).toContain('Search')
    expect(mobileLinkTexts).toContain('Graph')
    
    console.log('✓ Mobile menu functionality works correctly')
  })

  it('should handle empty search query correctly', async () => {
    console.log('Testing AppHeader empty search handling...')
    
    const mockPush = vi.fn()
    vi.spyOn(mockRouter, 'push').mockImplementation(mockPush)
    
    const wrapper = mount(AppHeader, {
      global: {
        plugins: [mockRouter]
      }
    })

    const searchInput = wrapper.find('input[type="text"]')
    
    // Test empty search
    await searchInput.setValue('')
    await searchInput.trigger('keyup.enter')
    
    // Router push should not be called for empty search
    expect(mockPush).not.toHaveBeenCalled()
    
    // Test whitespace-only search
    await searchInput.setValue('   ')
    await searchInput.trigger('keyup.enter')
    
    // Router push should not be called for whitespace-only search
    expect(mockPush).not.toHaveBeenCalled()
    
    console.log('✓ Empty search handling works correctly')
  })

  it('should clear search input after successful search', async () => {
    console.log('Testing AppHeader search input clearing...')
    
    const mockPush = vi.fn()
    vi.spyOn(mockRouter, 'push').mockImplementation(mockPush)
    
    const wrapper = mount(AppHeader, {
      global: {
        plugins: [mockRouter]
      }
    })

    const searchInput = wrapper.find('input[type="text"]')
    
    // Set search query and trigger search
    await searchInput.setValue('test query')
    await searchInput.trigger('keyup.enter')
    
    // Input should be cleared after search
    expect((searchInput.element as HTMLInputElement).value).toBe('')
    
    console.log('✓ Search input clearing works correctly')
  })

  it('should have correct CSS classes for styling', async () => {
    console.log('Testing AppHeader CSS classes...')
    
    const wrapper = mount(AppHeader, {
      global: {
        plugins: [mockRouter]
      }
    })

    // Check header styling
    const header = wrapper.find('header')
    expect(header.classes()).toContain('bg-white')
    expect(header.classes()).toContain('shadow-sm')
    
    // Check nav styling
    const nav = wrapper.find('nav')
    expect(nav.classes()).toContain('container')
    expect(nav.classes()).toContain('mx-auto')
    
    // Check search input styling
    const searchInput = wrapper.find('input[type="text"]')
    expect(searchInput.classes()).toContain('border')
    expect(searchInput.classes()).toContain('rounded-lg')
    
    console.log('✓ CSS classes are applied correctly')
  })

  it('should encode search queries properly', async () => {
    console.log('Testing AppHeader search query encoding...')
    
    const mockPush = vi.fn()
    vi.spyOn(mockRouter, 'push').mockImplementation(mockPush)
    
    const wrapper = mount(AppHeader, {
      global: {
        plugins: [mockRouter]
      }
    })

    const searchInput = wrapper.find('input[type="text"]')
    
    // Test special characters encoding
    await searchInput.setValue('test & query with spaces')
    await searchInput.trigger('keyup.enter')
    
    expect(mockPush).toHaveBeenCalledWith('/search?q=test%20%26%20query%20with%20spaces')
    
    console.log('✓ Search query encoding works correctly')
  })
})