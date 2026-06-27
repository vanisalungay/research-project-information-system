<template>
  <div class="pending-accounts">
    <!-- Header -->
    <header class="page-header">
      <h2>User Accounts</h2>
      <p>Manage all registered accounts</p>
    </header>

    <!-- Search -->
    <div class="search-box">
      <input
        type="text"
        placeholder="Search by name, email, or role..."
        v-model="search"
      />
    </div>

    <!-- Table -->
    <div class="table-container">
      <table>
        <thead>
          <tr>
            <th>Name</th>
            <th>Email</th>
            <th>Role</th>
            <th>Status</th>
            <th class="actions-col">Actions</th>
          </tr>
        </thead>

        <tbody>
          <tr v-for="user in filteredUsers" :key="user.id">
            <td>{{ user.name }}</td>

            <td>{{ user.email }}</td>

            <td>
              <span class="role-badge">
                {{ user.role }}
              </span>
            </td>

            <td>
              <span
                :class="{
                  'status-badge pending':
                    user.status === 'PENDING',

                  'status-badge approved':
                    user.status === 'APPROVED',

                  'status-badge rejected':
                    user.status === 'REJECTED'
                }"
              >
                {{ user.status }}
              </span>
            </td>

            <td class="actions">
              <button
                class="btn view"
                @click="viewUser(user)"
              >
                View
              </button>
            </td>
          </tr>

          <tr v-if="filteredUsers.length === 0">
            <td colspan="5" class="empty">
              No accounts found.
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()

const search = ref('')
const users = ref([])

const fetchUsers = async () => {
  try {
    const response = await axios.get(
      'http://localhost:8081/api/users'
    )

    users.value = response.data.map(user => ({
      ...user,
      status: (user.status || '').toUpperCase()
    }))

    console.log('Loaded users:', users.value)
  } catch (error) {
    console.error(error)

    console.warn(
      'Backend offline. Loading offline users.'
    )

    const offlineUsers = JSON.parse(
      localStorage.getItem('offline_users') || '[]'
    )

    users.value = offlineUsers
  }
}

onMounted(() => {
  fetchUsers()
})

const filteredUsers = computed(() => {
  return users.value.filter(user =>
    user.name.toLowerCase().includes(
      search.value.toLowerCase()
    ) ||
    user.email.toLowerCase().includes(
      search.value.toLowerCase()
    ) ||
    user.role.toLowerCase().includes(
      search.value.toLowerCase()
    )
  )
})

const viewUser = user => {
  router.push({
    path: '/rpsadmin-ver',
    query: {
      id: user.id
    }
  })
}
</script>

<style scoped>
.pending-accounts {
  padding: 24px;
  width: 500%;
  max-width: 135%;
}

/* Header */
.page-header h2 {
  margin: 0;
}

.page-header p {
  color: #6b7280;
  margin-bottom: 20px;
}

/* Search */
.search-box {
  margin-bottom: 16px;
}

.search-box input {
  width: 100%;
  padding: 10px 14px;
  border-radius: 8px;
  border: 1px solid #d1d5db;
}

/* Table */
.table-container {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
}

table {
  width: 100%;
  border-collapse: collapse;
}

thead {
  background: linear-gradient(
    to right,
    #2563eb,
    #16a34a
  );
  color: white;
}

th,
td {
  padding: 14px;
  text-align: left;
}

tbody tr {
  border-bottom: 1px solid #e5e7eb;
}

/* Role Badge */
.role-badge {
  background: #e0ecff;
  color: #2563eb;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
}

/* Status Badge */
.status-badge {
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.pending {
  background: #fef3c7;
  color: #92400e;
}

.approved {
  background: #dcfce7;
  color: #166534;
}

.rejected {
  background: #fee2e2;
  color: #991b1b;
}

/* Actions */
.actions {
  display: flex;
  gap: 6px;
}

.btn {
  padding: 6px 10px;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  border: none;
}

.btn.view {
  background: #2563eb;
  color: white;
}

.empty {
  text-align: center;
  color: #6b7280;
  padding: 20px;
}
</style>