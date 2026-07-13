const maps = {
  workOrder: {
    DRAFT: ['新建', 'neutral'],
    APPROVED: ['已审核', 'primary'],
    RELEASED: ['已下达', 'primary'],
    RUNNING: ['生产中', 'warning'],
    FINISHED: ['已完成', 'success'],
    CLOSED: ['已关闭', 'neutral']
  },
  task: {
    WAIT_START: ['待开工', 'warning'],
    RUNNING: ['生产中', 'primary'],
    PAUSED: ['暂停', 'warning'],
    FINISHED: ['已完工', 'success'],
    CLOSED: ['已关闭', 'neutral']
  },
  audit: {
    SUBMITTED: ['待审核', 'warning'],
    APPROVED: ['已通过', 'success'],
    REJECTED: ['已驳回', 'danger']
  }
}

function getStatus(type, value) {
  const result = maps[type] && maps[type][value]
  return result
    ? { text: result[0], tone: result[1] }
    : { text: value || '-', tone: 'neutral' }
}

module.exports = {
  getStatus
}
