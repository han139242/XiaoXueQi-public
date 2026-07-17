import dayjs from 'dayjs'

// 创建开发预览模式使用的仪表盘数据。它只用于没有登录后端时展示页面结构，不会写入数据库。
export function createDashboardPreview() {
  // 排期围绕当前时间生成，保证框架预览不会因为固定日期产生虚假逾期。
  const scheduleStart = dayjs().subtract(2, 'hour').startOf('hour')
  return {
    // 顶部核心指标卡片。
    summary: {
      productionOrderCount: 24,
      workOrderCount: 38,
      activeProductionOrderCount: 7,
      activeWorkOrderCount: 6,
      runningTaskCount: 12,
      todayQualifiedQuantity: 12860,
      todayDefectQuantity: 126,
      inventoryWarningCount: 4,
      equipmentFaultCount: 2
    },
    // 最近七天合格数和不良数；offset 表示距离今天多少天。
    trend: [
      [6, 820, 18],
      [5, 1080, 21],
      [4, 960, 17],
      [3, 1420, 25],
      [2, 1310, 19],
      [1, 1680, 16],
      [0, 1820, 10]
    ].map(([offset, qualifiedQuantity, defectQuantity]) => ({
      date: dayjs().subtract(offset, 'day').format('YYYY-MM-DD'),
      qualifiedQuantity,
      defectQuantity
    })),
    // 质量统计和设备状态分别供饼图、指标卡使用。
    quality: {
      inspectionCount: 34,
      evaluatedCount: 33,
      pendingCount: 1,
      qualifiedCount: 31,
      unqualifiedCount: 2,
      qualifiedRate: 93.94
    },
    qualityClosure: {
      total: 2,
      closed: 2,
      rate: 100
    },
    equipment: {
      running: 42,
      idle: 2,
      stopped: 6,
      fault: 2,
      maintenance: 3
    },
    lines: [
      { lineId: 1, lineCode: 'LINE-TRAIN-01', lineName: '训练球1号线', dailyCapacity: 800, stationCount: 7, status: 'RUNNING' },
      { lineId: 2, lineCode: 'LINE-MATCH-01', lineName: '比赛球1号线', dailyCapacity: 500, stationCount: 7, status: 'RUNNING' },
      { lineId: 4, lineCode: 'LINE-TRAIN-02', lineName: '训练球2号线', dailyCapacity: 800, stationCount: 7, status: 'RUNNING' },
      { lineId: 5, lineCode: 'LINE-MATCH-02', lineName: '比赛球2号线', dailyCapacity: 500, stationCount: 7, status: 'RUNNING' },
      { lineId: 6, lineCode: 'LINE-MATCH-03', lineName: '比赛球3号线', dailyCapacity: 500, stationCount: 7, status: 'MAINTAINING' }
    ],
    // 生产计划时间轴示例，不同 status 用来演示各类状态颜色。
    schedule: [
      { planId: 1, planNo: 'PLAN-001', productionOrderId: 1, orderNo: 'MO-TRAIN-001', lineId: 1, lineName: '训练球1号线', productName: '雷尼克斯T300训练级羽毛球76速', plannedStartTime: scheduleStart.format('YYYY-MM-DDTHH:mm:ss'), plannedEndTime: scheduleStart.add(6, 'hour').format('YYYY-MM-DDTHH:mm:ss'), plannedQuantity: 600, status: 'RUNNING', statusLabel: '生产中', statusTone: 'running' },
      { planId: 2, planNo: 'PLAN-002', productionOrderId: 2, orderNo: 'MO-MATCH-001', lineId: 2, lineName: '比赛球1号线', productName: '雷尼克斯M700比赛级羽毛球77速', plannedStartTime: scheduleStart.add(7, 'hour').format('YYYY-MM-DDTHH:mm:ss'), plannedEndTime: scheduleStart.add(15, 'hour').format('YYYY-MM-DDTHH:mm:ss'), plannedQuantity: 500, status: 'RELEASED', statusLabel: '待执行', statusTone: 'released' },
      { planId: 3, planNo: 'PLAN-003', productionOrderId: 3, orderNo: 'MO-TRAIN-002', lineId: 4, lineName: '训练球2号线', productName: '雷尼克斯T300训练级羽毛球77速', plannedStartTime: scheduleStart.add(7, 'hour').format('YYYY-MM-DDTHH:mm:ss'), plannedEndTime: scheduleStart.add(15, 'hour').format('YYYY-MM-DDTHH:mm:ss'), plannedQuantity: 800, status: 'RELEASED', statusLabel: '待执行', statusTone: 'released' },
      { planId: 4, planNo: 'PLAN-004', productionOrderId: 4, orderNo: 'MO-MATCH-002', lineId: 5, lineName: '比赛球2号线', productName: '雷尼克斯M700比赛级羽毛球78速', plannedStartTime: scheduleStart.add(16, 'hour').format('YYYY-MM-DDTHH:mm:ss'), plannedEndTime: scheduleStart.add(24, 'hour').format('YYYY-MM-DDTHH:mm:ss'), plannedQuantity: 500, status: 'RELEASED', statusLabel: '待执行', statusTone: 'released' },
      { planId: 5, planNo: 'PLAN-005', productionOrderId: 5, orderNo: 'MO-TRAIN-003', lineId: 1, lineName: '训练球1号线', productName: '迅羽C500 OEM俱乐部级羽毛球77速', plannedStartTime: scheduleStart.add(16, 'hour').format('YYYY-MM-DDTHH:mm:ss'), plannedEndTime: scheduleStart.add(24, 'hour').format('YYYY-MM-DDTHH:mm:ss'), plannedQuantity: 800, status: 'RELEASED', statusLabel: '待执行', statusTone: 'released' }
    ]
  }
}
