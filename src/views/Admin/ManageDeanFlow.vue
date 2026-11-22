<template>
  <!-- One Manage Dean flow, used by both administration screens. The details dialog is the
       same ManageDeanModal everywhere; Replace Dean opens the same locked Create Dean
       Account form and completes the succession in one backend call. -->
  <ManageDeanModal
    v-if="!replacing"
    :assignment="assignment"
    @close="$emit('close')"
    @replace="startReplacement"
  />

  <CreateUserModal
    v-else
    :key="`replacement-${assignment.id}`"
    initial-role="DEAN"
    lock-role
    :initial-designation="assignment.designation || 'Dean'"
    :initial-college="assignment.assignedCollege || ''"
    lock-college
    :replacement-assignment-id="assignment.id"
    college-authority
    @close="$emit('close')"
    @created="onCreated"
  />
</template>

<script setup>
/**
 * The shared Manage Dean workflow.
 *
 * <p>Dean Role Management and User Accounts used to open two different screens for the
 * same account: one showed the appointment and offered Replace Dean, the other jumped
 * straight to a candidate picker. Both now mount this component, so the details, the
 * action, the validation and the replacement result are identical.
 *
 * <p>Replace Dean keeps the existing workflow: the Create Dean Account form opens with
 * the appointment's college locked, and the backend creates (or reuses) the account,
 * transfers the pending endorsement requests and closes the outgoing appointment in one
 * transaction. The form stays open after success so the administrator can read and copy
 * the credentials; a reused account shows no temporary password.
 */
import { ref } from 'vue'
import ManageDeanModal from '@/views/Admin/ManageDeanModal.vue'
import CreateUserModal from '@/views/Admin/CreateUserModal.vue'

const props = defineProps({
  /** The ACTIVE appointment being managed. */
  assignment: { type: Object, required: true },
})

const emit = defineEmits(['close', 'changed'])

const replacing = ref(false)

function startReplacement() {
  if (String(props.assignment?.status || '').toUpperCase() !== 'ACTIVE') return
  replacing.value = true
}

/** The replacement completed; the page reloads while the credentials stay on screen. */
function onCreated(info) {
  emit('changed', info)
}
</script>
