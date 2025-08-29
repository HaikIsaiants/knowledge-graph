import { ref, customRef } from 'vue'
import type { Ref } from 'vue'

export function useDebounce<T>(value: Ref<T>, delay = 300) {
  let timeout: NodeJS.Timeout | undefined

  return customRef((track, trigger) => {
    return {
      get() {
        track()
        return value.value
      },
      set(newValue: T) {
        clearTimeout(timeout)
        timeout = setTimeout(() => {
          value.value = newValue
          trigger()
        }, delay)
      }
    }
  })
}

export function useDebouncedFn<T extends (...args: any[]) => any>(
  fn: T,
  delay = 300
): (...args: Parameters<T>) => void {
  let timeout: NodeJS.Timeout | undefined

  return (...args: Parameters<T>) => {
    clearTimeout(timeout)
    timeout = setTimeout(() => {
      fn(...args)
    }, delay)
  }
}