// Shared formatting utilities to reduce code duplication across components

import type { NodeType } from '@/types'

// Node type color mapping - centralized to avoid duplication
const NODE_TYPE_COLORS = new Map<NodeType | string, string>([
  ['PERSON', 'bg-blue-100 text-blue-800'],
  ['ORGANIZATION', 'bg-green-100 text-green-800'],
  ['EVENT', 'bg-purple-100 text-purple-800'],
  ['LOCATION', 'bg-orange-100 text-orange-800'],
  ['PLACE', 'bg-orange-100 text-orange-800'],
  ['CONCEPT', 'bg-indigo-100 text-indigo-800'],
  ['DOCUMENT', 'bg-gray-100 text-gray-800'],
  ['PROJECT', 'bg-pink-100 text-pink-800'],
  ['SYSTEM', 'bg-yellow-100 text-yellow-800']
])

export const getNodeTypeColor = (type: NodeType | string): string => 
  NODE_TYPE_COLORS.get(type) || 'bg-gray-100 text-gray-800'

// Simplified relative date formatting
export const formatRelativeDate = (dateInput: string | Date): string => {
  const date = typeof dateInput === 'string' ? new Date(dateInput) : dateInput
  const diffMs = Date.now() - date.getTime()
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))
  
  // Use array of thresholds for cleaner logic
  const thresholds = [
    { days: 0, label: 'Today' },
    { days: 1, label: 'Yesterday' },
    { days: 7, label: (d: number) => `${d} days ago` },
    { days: 30, label: (d: number) => `${Math.floor(d / 7)} weeks ago` },
    { days: 365, label: (d: number) => `${Math.floor(d / 30)} months ago` }
  ]
  
  for (const { days, label } of thresholds) {
    if (diffDays <= days) {
      return typeof label === 'string' ? label : label(diffDays)
    }
  }
  
  return `${Math.floor(diffDays / 365)} years ago`
}

// Simplified string truncation
export const truncateString = (str: string, maxLength = 50): string => 
  str.length <= maxLength ? str : `${str.substring(0, maxLength)}...`

// Format property key from camelCase to Title Case
export const formatPropertyKey = (key: string): string =>
  key
    .replace(/([A-Z])/g, ' $1')
    .replace(/^./, str => str.toUpperCase())
    .trim()

// Format complex property values for display
export const formatPropertyValue = (value: any, maxLength = 100): string => {
  if (Array.isArray(value)) return value.join(', ')
  if (typeof value === 'object' && value !== null) return JSON.stringify(value, null, 2)
  
  const stringValue = String(value)
  return truncateString(stringValue, maxLength)
}

// Format edge type from SNAKE_CASE to Title Case
export const formatEdgeType = (type: string): string =>
  type
    .split('_')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
    .join(' ')

// Extract filename from URI
export const extractFilename = (uri: string, maxLength = 40): string => {
  const filename = uri.split('/').pop() || uri
  return truncateString(filename, maxLength)
}