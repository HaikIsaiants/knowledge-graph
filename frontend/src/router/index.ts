import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
      meta: {
        title: 'Knowledge Graph'
      }
    },
    {
      path: '/search',
      name: 'search',
      component: () => import('@/views/SearchView.vue'),
      meta: {
        title: 'Search - Knowledge Graph'
      }
    },
    {
      path: '/node/:id',
      name: 'node',
      component: () => import('@/views/NodeView.vue'),
      meta: {
        title: 'Node Details - Knowledge Graph'
      }
    },
    {
      path: '/graph',
      name: 'graph',
      component: () => import('@/views/GraphView.vue'),
      meta: {
        title: 'Graph Visualization - Knowledge Graph'
      }
    },
    {
      path: '/upload',
      name: 'upload',
      component: () => import('@/views/UploadView.vue'),
      meta: {
        title: 'File Upload - Knowledge Graph'
      }
    },
    {
      path: '/jobs',
      name: 'jobs',
      component: () => import('@/views/JobsView.vue'),
      meta: {
        title: 'Ingestion Jobs - Knowledge Graph'
      }
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue'),
      meta: {
        title: 'Page Not Found - Knowledge Graph'
      }
    }
  ]
})

// Update document title
router.beforeEach((to) => {
  document.title = to.meta?.title as string || 'Knowledge Graph'
})

export default router