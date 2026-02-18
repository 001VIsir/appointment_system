<template>
  <div class="time-slot-picker">
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="3" animated />
    </div>

    <template v-else>
      <!-- 无时段提示 -->
      <el-empty v-if="slots.length === 0" description="暂无可用时段" />

      <!-- 时段列表 -->
      <div v-else class="slots-grid">
        <div
          v-for="slot in slots"
          :key="slot.id"
          class="slot-item"
          :class="{
            selected: selectedSlotId === slot.id,
            disabled: !slot.hasCapacity,
          }"
          @click="selectSlot(slot)"
        >
          <div class="slot-time">
            {{ formatTime(slot.startTime) }} - {{ formatTime(slot.endTime) }}
          </div>
          <div class="slot-capacity">
            <template v-if="slot.hasCapacity">
              <el-tag type="success" size="small">
                剩余 {{ slot.availableCapacity ?? (slot.capacity - slot.bookedCount) }} 位
              </el-tag>
            </template>
            <template v-else>
              <el-tag type="info" size="small">已满</el-tag>
            </template>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { bookingApi } from '@/api'
import type { AppointmentSlot } from '@/types'

const props = defineProps<{
  taskId: number
  onlyAvailable?: boolean
}>()

const emit = defineEmits<{
  (e: 'select', slot: AppointmentSlot | null): void
}>()

const loading = ref(false)
const slots = ref<AppointmentSlot[]>([])
const selectedSlotId = ref<number | null>(null)

const formatTime = (dateStr: string) => {
  if (!dateStr) return '-'
  // 提取时间部分 HH:mm
  return dateStr.substring(11, 16)
}

const loadSlots = async () => {
  if (!props.taskId) return

  loading.value = true
  selectedSlotId.value = null
  emit('select', null)

  try {
    const response = props.onlyAvailable
      ? await bookingApi.getAvailableSlots(props.taskId)
      : await bookingApi.getTaskSlots(props.taskId)
    slots.value = response.data || []

    // 计算是否有容量
    slots.value = slots.value.map((slot) => ({
      ...slot,
      hasCapacity: slot.hasCapacity ?? slot.bookedCount < slot.capacity,
      availableCapacity: slot.availableCapacity ?? slot.capacity - slot.bookedCount,
    }))

    // 按开始时间排序
    slots.value.sort((a, b) => {
      return new Date(a.startTime).getTime() - new Date(b.startTime).getTime()
    })
  } catch {
    slots.value = []
  } finally {
    loading.value = false
  }
}

const selectSlot = (slot: AppointmentSlot) => {
  if (!slot.hasCapacity) return

  if (selectedSlotId.value === slot.id) {
    selectedSlotId.value = null
    emit('select', null)
  } else {
    selectedSlotId.value = slot.id
    emit('select', slot)
  }
}

// 监听 taskId 变化
watch(
  () => props.taskId,
  () => {
    loadSlots()
  }
)

onMounted(() => {
  loadSlots()
})

// 暴露刷新方法
defineExpose({
  refresh: loadSlots,
})
</script>

<style scoped>
.time-slot-picker {
  min-height: 100px;
}

.loading-container {
  padding: 20px;
}

.slots-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px;
}

.slot-item {
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: center;
}

.slot-item:hover:not(.disabled) {
  border-color: #409eff;
  background-color: #ecf5ff;
}

.slot-item.selected {
  border-color: #409eff;
  background-color: #ecf5ff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.slot-item.disabled {
  background-color: #f5f7fa;
  cursor: not-allowed;
  opacity: 0.6;
}

.slot-time {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 8px;
}

.slot-capacity {
  font-size: 12px;
}
</style>
