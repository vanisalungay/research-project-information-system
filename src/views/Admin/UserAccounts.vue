<template>
  <div class="page">
    <div class="header">
      <h2>User Accounts</h2>

      <button class="create-btn" @click="showModal = true">
        + Create Account
      </button>
    </div>

    <table>
      <thead>
        <tr>
          <th>Name</th>
          <th>Email</th>
          <th>Role</th>
        </tr>
      </thead>

      <tbody>
        <tr v-for="user in users" :key="user.id">
          <td>{{ user.name }}</td>
          <td>{{ user.email }}</td>
          <td>{{ user.role }}</td>
        </tr>
      </tbody>
    </table>

    <CreateUserModal
      v-if="showModal"
      @close="showModal = false"
      @created="loadUsers"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/utils/api'

// CHANGE THIS
import CreateUserModal from '@/views/Admin/CreateUserModal.vue'

const users = ref([])
const showModal = ref(false)

const loadUsers = async () => {
  const res = await api.get('/api/users')

  users.value = res.data
}

onMounted(loadUsers)
</script>

<style scoped>
.page{
    padding:20px;
}

.header{
    display:flex;
    justify-content:space-between;
    margin-bottom:20px;
}

.create-btn{
    padding:10px 18px;
    background:#2452ff;
    color:white;
    border:none;
    border-radius:6px;
    cursor:pointer;
}

table{
    width:100%;
    border-collapse:collapse;
}

th,td{
    border:1px solid #ddd;
    padding:12px;
}
</style>