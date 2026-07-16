import dayjs from 'dayjs'

// 创建开发预览模式使用的仪表盘数据。它只用于没有登录后端时展示页面结构，不会写入数据库。
export function createDashboardPreview() {
  // 所有排产时间以今天 07:00 为基准生成，保证预览甘特图每天打开都落在当前日期。
  const scheduleStart = dayjs().startOf('day').hour(7).minute(0).second(0)
  return {
    // 顶部核心指标卡片。
    summary: {
      productionOrderCount: 24,
      workOrderCount: 38,
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
      qualifiedCount: 31,
      unqualifiedCount: 3,
      qualifiedRate: 91.18
    },
    equipment: {
      running: 42,
      stopped: 6,
      fault: 2,
      maintenance: 3
    },
    lines: [
      { lineId: 1, lineCode: 'LINE-TRAIN-01', lineName: '训练球1号线', dailyCapacity: 800, status: 'RUNNING' },
      { lineId: 2, lineCode: 'LINE-MATCH-01', lineName: '比赛球1号线', dailyCapacity: 500, status: 'RUNNING' },
      { lineId: 4, lineCode: 'LINE-TRAIN-02', lineName: '训练球2号线', dailyCapacity: 800, status: 'IDLE' },
      { lineId: 5, lineCode: 'LINE-MATCH-02', lineName: '比赛球2号线', dailyCapacity: 500, status: 'MAINTENANCE' },
      { lineId: 6, lineCode: 'LINE-MATCH-03', lineName: '比赛球3号线', dailyCapacity: 500, status: 'STOPPED' }
    ],
    // 生产计划时间轴示例，不同 status 用来演示各类状态颜色。
    schedule: [
      { planId: 1, planNo: 'PLAN-001', productionOrderId: 1, orderNo: 'MO-20260713-001', lineId: 1, lineName: '训练球自动线', productName: '训练级羽毛球', plannedStartTime: scheduleStart.format('YYYY-MM-DDTHH:mm:ss'), plannedEndTime: scheduleStart.add(4, 'hour').format('YYYY-MM-DDTHH:mm:ss'), plannedQuantity: 500, status: 'RELEASED', statusLabel: '已生效', statusTone: 'released' },
      { planId: 2, planNo: 'PLAN-002', productionOrderId: 2, orderNo: 'MO-20260713-002', lineId: 2, lineName: '比赛球自动线', productName: '比赛级羽毛球', plannedStartTime: scheduleStart.add(1.5, 'hour').format('YYYY-MM-DDTHH:mm:ss'), plannedEndTime: scheduleStart.add(6.5, 'hour').format('YYYY-MM-DDTHH:mm:ss'), plannedQuantity: 300, status: 'RELEASED', statusLabel: '已生效', statusTone: 'released' },
      { planId: 3, planNo: 'PLAN-003', productionOrderId: 3, orderNo: 'MO-20260713-003', lineId: 1, lineName: '训练球自动线', productName: '训练级羽毛球', plannedStartTime: scheduleStart.add(4.5, 'hour').format('YYYY-MM-DDTHH:mm:ss'), plannedEndTime: scheduleStart.add(9, 'hour').format('YYYY-MM-DDTHH:mm:ss'), plannedQuantity: 420, status: 'DRAFT', statusLabel: '待生效', statusTone: 'draft' },
      { planId: 4, planNo: 'PLAN-004', productionOrderId: 4, orderNo: 'MO-20260713-004', lineId: 2, lineName: '比赛球自动线', productName: '比赛级羽毛球', plannedStartTime: scheduleStart.add(7, 'hour').format('YYYY-MM-DDTHH:mm:ss'), plannedEndTime: scheduleStart.add(11.5, 'hour').format('YYYY-MM-DDTHH:mm:ss'), plannedQuantity: 260, status: 'DRAFT', statusLabel: '待生效', statusTone: 'draft' },
      { planId: 5, planNo: 'PLAN-005', productionOrderId: 5, orderNo: 'MO-20260713-005', lineId: 1, lineName: '训练球自动线', productName: '训练级羽毛球', plannedStartTime: scheduleStart.add(9.5, 'hour').format('YYYY-MM-DDTHH:mm:ss'), plannedEndTime: scheduleStart.add(13, 'hour').format('YYYY-MM-DDTHH:mm:ss'), plannedQuantity: 360, status: 'FINISHED', statusLabel: '已完成', statusTone: 'finished' },
      { planId: 6, planNo: 'PLAN-006', productionOrderId: 6, orderNo: 'MO-20260713-006', lineId: 2, lineName: '比赛球自动线', productName: '比赛级羽毛球', plannedStartTime: scheduleStart.add(12, 'hour').format('YYYY-MM-DDTHH:mm:ss'), plannedEndTime: scheduleStart.add(15, 'hour').format('YYYY-MM-DDTHH:mm:ss'), plannedQuantity: 180, status: 'CLOSED', statusLabel: '已取消', statusTone: 'closed' }
    ]
  }
}
