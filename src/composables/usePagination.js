import { computed, ref, watch } from 'vue'

/** The default page size used by every list that does not ask for another one. */
export const DEFAULT_PAGE_SIZE = 10

/** The page-size choices offered to the user. */
export const PAGE_SIZE_OPTIONS = [10, 25, 50, 100]

/** How many items a list of `total` items has at `pageSize`. At least one page. */
export function pageCount(total, pageSize) {
  const size = Math.max(1, Number(pageSize) || 1)
  return Math.max(1, Math.ceil(Math.max(0, Number(total) || 0) / size))
}

/** The 1-based index of the first item on the page (0 for an empty list). */
export function pageStart(page, pageSize, total) {
  if (!total) return 0
  return (clampPage(page, pageCount(total, pageSize)) - 1) * pageSize + 1
}

/** The 1-based index of the last item on the page (0 for an empty list). */
export function pageEnd(page, pageSize, total) {
  if (!total) return 0
  return Math.min(clampPage(page, pageCount(total, pageSize)) * pageSize, total)
}

/** Keeps a requested page inside 1..pages. */
export function clampPage(page, pages) {
  const value = Math.trunc(Number(page)) || 1
  return Math.min(Math.max(1, value), Math.max(1, pages))
}

/**
 * The page numbers to render: always the first and last page, the current page and one
 * neighbour on each side. A gap is represented by the string '…'.
 */
export function buildPageTokens(page, pages) {
  const last = Math.max(1, pages)
  const current = clampPage(page, last)
  const wanted = new Set([1, last, current - 1, current, current + 1])
  const numbers = [...wanted].filter((n) => n >= 1 && n <= last).sort((a, b) => a - b)
  const tokens = []
  numbers.forEach((number, index) => {
    if (index > 0 && number - numbers[index - 1] > 1) tokens.push('…')
    tokens.push(number)
  })
  return tokens
}

/** A slice of `items` for the requested page. */
export function paginate(items, page, pageSize) {
  const list = Array.isArray(items) ? items : []
  const size = Math.max(1, Number(pageSize) || 1)
  const current = clampPage(page, pageCount(list.length, size))
  const start = (current - 1) * size
  return list.slice(start, start + size)
}

/**
 * Client-side pagination state for a computed list.
 *
 * The caller owns the filtering: `items` is the already filtered/sorted list, so the
 * pagination always shows the real filtered total. When the list shrinks (a filter was
 * changed, a row was removed) the current page is clamped instead of showing an empty
 * page. Call `reset()` whenever the search/filter criteria change.
 */
export function usePagination(items, { initialPageSize = DEFAULT_PAGE_SIZE } = {}) {
  const page = ref(1)
  const pageSize = ref(initialPageSize)
  const total = computed(() => (Array.isArray(items.value) ? items.value.length : 0))
  const pages = computed(() => pageCount(total.value, pageSize.value))
  const pageItems = computed(() => paginate(items.value, page.value, pageSize.value))
  const rangeStart = computed(() => pageStart(page.value, pageSize.value, total.value))
  const rangeEnd = computed(() => pageEnd(page.value, pageSize.value, total.value))

  watch(total, () => {
    page.value = clampPage(page.value, pages.value)
  })

  const setPage = (value) => {
    page.value = clampPage(value, pages.value)
  }
  const setPageSize = (value) => {
    pageSize.value = Math.max(1, Number(value) || DEFAULT_PAGE_SIZE)
    page.value = 1
  }
  const reset = () => {
    page.value = 1
  }

  return {
    page,
    pageSize,
    total,
    pages,
    pageItems,
    rangeStart,
    rangeEnd,
    setPage,
    setPageSize,
    reset,
  }
}
