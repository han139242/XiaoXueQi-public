// 格式化工具：统一处理空值、日期时间和数字转换。
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

function validateRemark(value, maxLength) {
  const text = String(value || '').trim()
  if (!text) return { value: '', error: '' }
  if (maxLength && text.length > maxLength) {
    return { value: text, error: '备注不能超过 ' + maxLength + ' 个字' }
  }
  if (/[\u0000-\u0008\u000B\u000C\u000E-\u001F\u007F]/.test(text)
    || /\?{3,}/.test(text)
    || /�|锟斤拷|ï¿½/i.test(text)) {
    return { value: text, error: '备注包含乱码或不可识别字符，请重新填写' }
  }
  return { value: text, error: '' }
}

module.exports = {
  display,
  formatDateTime,
  toNumber,
  compactQuery,
  validateRemark
}
