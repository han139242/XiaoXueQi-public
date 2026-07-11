<script setup>
const model = defineModel()

defineProps({
  field: { type: Object, required: true },
  options: { type: Array, default: () => [] },
  compact: { type: Boolean, default: false }
})
</script>

<template>
  <el-select
    v-if="field.type === 'select' || field.type === 'reference'"
    v-model="model"
    :multiple="field.multiple"
    :collapse-tags="field.multiple"
    :max-collapse-tags="2"
    :placeholder="field.placeholder || `请选择${field.label}`"
    :disabled="field.disabled"
    clearable
    filterable
  >
    <el-option
      v-for="item in options"
      :key="String(item.value)"
      :label="item.label"
      :value="item.value"
    />
  </el-select>

  <el-input-number
    v-else-if="field.type === 'number'"
    v-model="model"
    :min="field.min"
    :max="field.max"
    :precision="field.precision"
    :step="field.step || 1"
    :controls="!compact"
    controls-position="right"
    :placeholder="field.placeholder || `请输入${field.label}`"
    :disabled="field.disabled"
  />

  <el-date-picker
    v-else-if="field.type === 'date'"
    v-model="model"
    type="date"
    value-format="YYYY-MM-DD"
    format="YYYY-MM-DD"
    :placeholder="field.placeholder || `请选择${field.label}`"
    :disabled="field.disabled"
  />

  <el-date-picker
    v-else-if="field.type === 'datetime'"
    v-model="model"
    type="datetime"
    value-format="YYYY-MM-DDTHH:mm:ss"
    format="YYYY-MM-DD HH:mm"
    :placeholder="field.placeholder || `请选择${field.label}`"
    :disabled="field.disabled"
  />

  <el-input
    v-else-if="field.type === 'textarea'"
    v-model="model"
    type="textarea"
    :rows="compact ? 2 : 3"
    :maxlength="field.maxLength || 500"
    show-word-limit
    resize="none"
    :placeholder="field.placeholder || `请输入${field.label}`"
    :disabled="field.disabled"
  />

  <el-input
    v-else
    v-model="model"
    :type="field.type === 'password' ? 'password' : 'text'"
    :show-password="field.type === 'password'"
    :maxlength="field.maxLength || 255"
    clearable
    :placeholder="field.placeholder || `请输入${field.label}`"
    :disabled="field.disabled"
  />
</template>

<style scoped>
.el-select,
.el-input,
.el-input-number,
.el-date-editor {
  width: 100%;
}
</style>
