const MIN_COLUMN_WIDTH = 30
const RESIZE_HANDLE_WIDTH = 8
const CLICK_SUPPRESSION_MS = 400
const directiveState = new WeakMap()

function resolveTable(root, vnode) {
  const component = root.__vueParentComponent || vnode.component
  return component?.exposed || component?.proxy
}

function findColumn(table, headerCell) {
  return table?.store?.states?.columns?.value?.find((column) =>
    headerCell.classList.contains(column.id))
}

function canResize(event, root, table) {
  const target = event.target instanceof Element ? event.target.closest('th.is-leaf') : null
  if (!target || !root.contains(target)) return null

  const column = findColumn(table, target)
  if (!column || column.resizable === false || column.children?.length) return null

  const rect = target.getBoundingClientRect()
  const isLastColumn = target.parentElement?.lastElementChild === target
  if (rect.width <= 12 || rect.right - event.clientX >= RESIZE_HANDLE_WIDTH
    || (isLastColumn && !vnodeProps(table).allowDragLastColumn)) return null

  return { column, rect }
}

function vnodeProps(table) {
  return table?.$?.props || {}
}

export default {
  mounted(root, _binding, vnode) {
    let activeResize = null
    let animationFrame = 0
    let previousCursor = ''
    let previousUserSelect = ''
    let suppressHeaderClickUntil = 0

    const applyWidth = () => {
      animationFrame = 0
      if (!activeResize) return
      const width = Math.max(MIN_COLUMN_WIDTH, Math.round(
        activeResize.startWidth + activeResize.currentX - activeResize.startX))
      if (width === activeResize.column.width && width === activeResize.column.realWidth) return
      activeResize.column.width = width
      activeResize.column.realWidth = width
      activeResize.didResize = true
      activeResize.table.doLayout()
    }

    const scheduleWidthUpdate = () => {
      if (!animationFrame) animationFrame = requestAnimationFrame(applyWidth)
    }

    const stopResize = (event) => {
      if (!activeResize) return
      if (animationFrame) {
        cancelAnimationFrame(animationFrame)
        animationFrame = 0
        applyWidth()
      }
      const didResize = activeResize.didResize
      activeResize = null
      document.body.style.cursor = previousCursor
      document.body.style.userSelect = previousUserSelect
      document.removeEventListener('mousemove', handleMouseMove, true)
      document.removeEventListener('mouseup', stopResize, true)
      if (didResize) {
        // The mouseup still produces a click on the sortable table header.
        event?.preventDefault()
        event?.stopPropagation()
        suppressHeaderClickUntil = Date.now() + CLICK_SUPPRESSION_MS
      }
    }

    const handleMouseMove = (event) => {
      if (!activeResize) return
      activeResize.currentX = event.clientX
      scheduleWidthUpdate()
    }

    const handleMouseDown = (event) => {
      if (event.button !== 0 || activeResize) return
      const table = resolveTable(root, vnode)
      if (!table?.store || !table?.doLayout) return
      const candidate = canResize(event, root, table)
      if (!candidate) return

      activeResize = {
        table,
        column: candidate.column,
        startX: event.clientX,
        currentX: event.clientX,
        startWidth: candidate.rect.width,
        didResize: false
      }
      previousCursor = document.body.style.cursor
      previousUserSelect = document.body.style.userSelect
      document.body.style.cursor = 'col-resize'
      document.body.style.userSelect = 'none'
      event.preventDefault()
      event.stopPropagation()
      document.addEventListener('mousemove', handleMouseMove, true)
      document.addEventListener('mouseup', stopResize, true)
    }

    const suppressHeaderClick = (event) => {
      if (Date.now() > suppressHeaderClickUntil) return
      const target = event.target instanceof Element ? event.target.closest('th.is-leaf') : null
      if (!target || !root.contains(target)) return

      suppressHeaderClickUntil = 0
      event.preventDefault()
      event.stopImmediatePropagation()
    }

    root.addEventListener('mousedown', handleMouseDown, true)
    root.addEventListener('click', suppressHeaderClick, true)
    directiveState.set(root, () => {
      root.removeEventListener('mousedown', handleMouseDown, true)
      root.removeEventListener('click', suppressHeaderClick, true)
      stopResize()
    })
  },
  unmounted(root) {
    directiveState.get(root)?.()
    directiveState.delete(root)
  }
}
