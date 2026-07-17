// 通用图标组件：统一加载 assets/icons 下的业务图标。
Component({
  properties: {
    name: { type: String, value: 'info' },
    size: { type: Number, value: 36 },
    inverse: { type: Boolean, value: false },
    muted: { type: Boolean, value: false }
  }
})
