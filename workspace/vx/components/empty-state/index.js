// 空状态组件：列表无数据或查询无结果时统一展示。
Component({
  properties: {
    title: {
      type: String,
      value: '暂无数据'
    },
    description: {
      type: String,
      value: ''
    },
    actionText: {
      type: String,
      value: ''
    }
  },
  methods: {
    handleAction() {
      this.triggerEvent('action')
    }
  }
})
