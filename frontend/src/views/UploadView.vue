<template>
  <div class="container mx-auto px-4 py-8">
    <div class="max-w-4xl mx-auto">
      <h1 class="text-3xl font-bold mb-8">File Upload</h1>
      
      <!-- Upload Area -->
      <div class="bg-white rounded-lg shadow-md p-8 mb-6">
        <div
          @drop="handleDrop"
          @dragover.prevent
          @dragenter.prevent
          @dragleave="isDragging = false"
          @dragenter="isDragging = true"
          :class="[
            'border-2 border-dashed rounded-lg p-12 text-center transition-colors',
            isDragging ? 'border-blue-500 bg-blue-50' : 'border-gray-300 hover:border-gray-400'
          ]"
        >
          <svg class="mx-auto h-12 w-12 text-gray-400 mb-4" stroke="currentColor" fill="none" viewBox="0 0 48 48">
            <path d="M28 8H12a4 4 0 00-4 4v20m32-12v8m0 0v8a4 4 0 01-4 4H12a4 4 0 01-4-4v-4m32-4l-3.172-3.172a4 4 0 00-5.656 0L28 28M8 32l9.172-9.172a4 4 0 015.656 0L28 28m0 0l4 4m4-24h8m-4-4v8m-12 4h.02" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          
          <p class="text-gray-600 mb-2">
            Drag and drop files here, or click to browse
          </p>
          <p class="text-sm text-gray-500 mb-4">
            Supported formats: CSV, JSON, PDF, Markdown, TXT, HTML, XML
          </p>
          
          <input
            ref="fileInput"
            type="file"
            multiple
            :accept="acceptedFormats"
            @change="handleFileSelect"
            class="hidden"
          />
          
          <button
            @click="$refs.fileInput.click()"
            class="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
          >
            Select Files
          </button>
        </div>
      </div>

      <!-- Selected Files -->
      <div v-if="selectedFiles.length > 0" class="bg-white rounded-lg shadow-md p-6 mb-6">
        <h2 class="text-xl font-semibold mb-4">Selected Files</h2>
        <div class="space-y-2">
          <div
            v-for="(file, index) in selectedFiles"
            :key="index"
            class="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
          >
            <div class="flex items-center space-x-3">
              <component :is="getFileIcon(file.type)" class="h-5 w-5 text-gray-500" />
              <div>
                <p class="text-sm font-medium text-gray-900">{{ file.name }}</p>
                <p class="text-xs text-gray-500">{{ formatFileSize(file.size) }}</p>
              </div>
            </div>
            <button
              @click="removeFile(index)"
              class="text-red-500 hover:text-red-700"
            >
              <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>
        
        <div class="mt-4 flex justify-end space-x-3">
          <button
            @click="clearFiles"
            class="px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
          >
            Clear All
          </button>
          <button
            @click="uploadFiles"
            :disabled="isUploading"
            class="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-green-600 hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <span v-if="!isUploading">Upload Files</span>
            <span v-else class="flex items-center">
              <svg class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              Uploading...
            </span>
          </button>
        </div>
      </div>

      <!-- Upload Results -->
      <div v-if="uploadResults.length > 0" class="bg-white rounded-lg shadow-md p-6">
        <h2 class="text-xl font-semibold mb-4">Upload Results</h2>
        <div class="space-y-2">
          <div
            v-for="result in uploadResults"
            :key="result.jobId"
            :class="[
              'flex items-center justify-between p-3 rounded-lg',
              result.status === 'SUCCESS' ? 'bg-green-50' : 'bg-red-50'
            ]"
          >
            <div class="flex items-center space-x-3">
              <svg
                v-if="result.status === 'SUCCESS'"
                class="h-5 w-5 text-green-500"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
              </svg>
              <svg
                v-else
                class="h-5 w-5 text-red-500"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
              <div>
                <p class="text-sm font-medium text-gray-900">{{ result.filename }}</p>
                <p class="text-xs text-gray-500">
                  Job ID: {{ result.jobId }}
                  <span v-if="result.message"> - {{ result.message }}</span>
                </p>
              </div>
            </div>
            <span
              :class="[
                'px-2 py-1 text-xs font-semibold rounded-full',
                result.status === 'SUCCESS' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
              ]"
            >
              {{ result.status }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import axios from 'axios'

interface FileUploadResult {
  jobId: string
  filename: string
  status: 'SUCCESS' | 'ERROR'
  message?: string
}

const selectedFiles = ref<File[]>([])
const uploadResults = ref<FileUploadResult[]>([])
const isDragging = ref(false)
const isUploading = ref(false)

const acceptedFormats = '.csv,.json,.pdf,.md,.txt,.html,.xml'

const handleDrop = (e: DragEvent) => {
  e.preventDefault()
  isDragging.value = false
  
  const files = Array.from(e.dataTransfer?.files || [])
  addFiles(files)
}

const handleFileSelect = (e: Event) => {
  const target = e.target as HTMLInputElement
  const files = Array.from(target.files || [])
  addFiles(files)
  target.value = '' // Reset input
}

const addFiles = (files: File[]) => {
  const validFiles = files.filter(file => {
    const extension = '.' + file.name.split('.').pop()?.toLowerCase()
    return acceptedFormats.includes(extension)
  })
  
  selectedFiles.value = [...selectedFiles.value, ...validFiles]
  
  if (validFiles.length < files.length) {
    alert('Some files were skipped because they are not in a supported format.')
  }
}

const removeFile = (index: number) => {
  selectedFiles.value.splice(index, 1)
}

const clearFiles = () => {
  selectedFiles.value = []
}

const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 Bytes'
  const k = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

const getFileIcon = (type: string) => {
  // Return icon component based on file type
  // This is simplified - you'd normally import actual icon components
  return 'svg'
}

const uploadFiles = async () => {
  if (selectedFiles.value.length === 0) return
  
  isUploading.value = true
  uploadResults.value = []
  
  try {
    for (const file of selectedFiles.value) {
      const formData = new FormData()
      formData.append('file', file)
      
      try {
        const response = await axios.post('http://localhost:8080/api/files/upload', formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        })
        
        uploadResults.value.push({
          jobId: response.data.jobId,
          filename: file.name,
          status: 'SUCCESS',
          message: response.data.message
        })
      } catch (error) {
        uploadResults.value.push({
          jobId: 'error-' + Date.now(),
          filename: file.name,
          status: 'ERROR',
          message: error.response?.data?.message || 'Upload failed'
        })
      }
    }
    
    // Clear selected files after upload
    selectedFiles.value = []
  } finally {
    isUploading.value = false
  }
}
</script>