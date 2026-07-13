function display(value, fallback) {
  if (value === null || value === undefined || value === '') {
    return fallback || '-'
  }
  return String(value)
}

function formatDateTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ')
}

function toNumber(value) {
  if (value === '' || value === null || value === undefined) return null
  const number = Number(value)
  return Number.isFinite(number) ? number : null
}

function compactQuery(source) {
  const result = {}
  Object.keys(source || {}).forEach((key) => {
    const value = source[key]
    if (value !== '' && value !== null && value !== undefined) {
      result[key] = value
    }
  })
  return result
}

module.exports = {
  display,
  formatDateTime,
  toNumber,
  compactQuery
}
