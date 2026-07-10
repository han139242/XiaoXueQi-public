import dayjs from 'dayjs'

export function createDashboardPreview() {
  return {
    summary: {
      productionOrderCount: 24,
      workOrderCount: 38,
      runningTaskCount: 12,
      todayQualifiedQuantity: 12860,
      todayDefectQuantity: 126,
      inventoryWarningCount: 4,
      equipmentFaultCount: 2
    },
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
    }
  }
}
