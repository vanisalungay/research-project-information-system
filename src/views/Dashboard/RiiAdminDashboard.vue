<template>
  <div class="pending-accounts">
    <!-- Header -->
    <header class="page-header">
      <h2>Pending Accounts</h2>
      <p>Review and approve new user registrations</p>
    </header>

    <!-- Search -->
    <div class="search-box">
      <input type="text" placeholder="Search by name, email, or role..." v-model="search" />
    </div>

    <!-- Table -->
    <div class="table-container">
      <table>
        <thead>
          <tr>
            <th>Name</th>
            <th>Email</th>
            <th>Role</th>
            <th>Date Registered</th>
            <th>Status</th>
            <th class="actions-col">Actions</th>
          </tr>
        </thead>

        <tbody>
          <tr v-for="user in filteredUsers" :key="user.id">
            <td>{{ user.name }}</td>
            <td>{{ user.email }}</td>
            <td>
              <span class="role-badge">{{ user.role }}</span>
            </td>
            <td>{{ user.date }}</td>
            <td>
              <span
                :class="{
                  'status-badge pending': user.status === 'Pending',
                  'status-badge rejected': user.status === 'Rejected',
                  'status-badge approved': user.status === 'Approved',
                }"
                >{{ user.status }}</span
              >
            </td>

            <td class="actions">
              <button class="btn view" @click="viewUser(user)">View</button>
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

const router = useRouter()

const search = ref('')
const defaultUsers = ref([
  {
    id: 1,
    name: 'Dr. Maria Santos',
    email: 'maria.santos@msunawan.edu.ph',
    role: 'Faculty',
    date: 'Dec 15, 2024',
    status: 'Pending',
  },
  {
    id: 2,
    name: 'John Doe',
    email: 'john.doe@university.edu',
    role: 'REC Member',
    date: 'Dec 14, 2024',
    status: 'Pending',
  },
  {
    id: 3,
    name: 'Dr. Lisa Garcia',
    email: 'lisa.garcia@university.edu',
    role: 'OVCRIGE',
    date: 'Dec 13, 2024',
    status: 'Pending',
  },
  {
    id: 4,
    name: 'Robert Chen',
    email: 'robert.chen@university.edu',
    role: 'Faculty',
    date: 'Dec 12, 2024',
    status: 'Pending',
  },
])

const users = ref(JSON.parse(localStorage.getItem('riiAdminUsers') || 'null') || defaultUsers)

localStorage.setItem('riiAdminUsers', JSON.stringify(users.value))

const filteredUsers = computed(() => {
  return users.value.filter(
    (user) =>
      user.name.toLowerCase().includes(search.value.toLowerCase()) ||
      user.email.toLowerCase().includes(search.value.toLowerCase()) ||
      user.role.toLowerCase().includes(search.value.toLowerCase()),
  )
})

const viewUser = (user) => {
  router.push({
    path: 'riiadmin-ver',
    query: {
      id: user.id,
    },
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
  background: linear-gradient(to right, #2563eb, #16a34a);
  color: #fff;
}

th,
td {
  padding: 14px;
  text-align: left;
}

tbody tr {
  border-bottom: 1px solid #e5e7eb;
}

/* Badges */
.role-badge {
  background: #e0ecff;
  color: #2563eb;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
}

.status-badge {
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
}

.pending {
  background: #fef3c7;
  color: #92400e;
}

.rejected {
  background: #fef3c7;
  color: #92400e;
}

.approved {
  background: #069214;
  color: #ffffff;
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

.btn.approve {
  background: #16a34a;
  color: white;
}

.btn.reject {
  background: #dc2626;
  color: white;
}
</style>
