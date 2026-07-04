<template>
  <div style="padding: 40px; font-family: sans-serif;">
    <h1 style="color: #2c3e50;">Backend Connection Test</h1>
    <p>This page tests if your Vue frontend can talk to your Spring Boot backend.</p>

    <div style="margin-top: 20px; padding: 20px; border: 1px solid #ccc; border-radius: 8px;">
      <h2>1. Testing Server Health (/api/health)</h2>
      <button @click="testHealth" style="padding: 10px 20px; margin-bottom: 10px; cursor: pointer;">
        Check Backend Status
      </button>
      <p>
        <strong>Result:</strong> 
        <span :style="{ color: healthStatus === 'Connected!' ? 'green' : 'red' }">
          {{ healthMessage }}
        </span>
      </p>
    </div>

    <div style="margin-top: 20px; padding: 20px; border: 1px solid #ccc; border-radius: 8px;">
      <h2>2. Fetching Users (/api/users)</h2>
      <button @click="fetchUsers" style="padding: 10px 20px; margin-bottom: 10px; cursor: pointer;">
        Load Users from Database
      </button>
      
      <div v-if="users.length > 0">
        <ul>
          <li v-for="user in users" :key="user.id">
            <strong>{{ user.name }}</strong> ({{ user.role }}) - {{ user.email }}
          </li>
        </ul>
      </div>
      <p v-else style="color: gray; font-style: italic;">
        {{ userMessage }}
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import api from '@/utils/api';

const healthStatus = ref('');
const healthMessage = ref('Not tested yet.');

const users = ref([]);
const userMessage = ref('Click the button to load users.');

const testHealth = async () => {
  healthMessage.value = 'Testing...';
  try {
    const response = await api.get('/api/health');
    healthStatus.value = 'Connected!';
    healthMessage.value = response.data; // Should say "RPIS backend is running"
  } catch (error) {
    healthStatus.value = 'Error';
    healthMessage.value = 'Failed to connect. Make sure your Spring Boot application is running on port 8081.';
  }
};

const fetchUsers = async () => {
  userMessage.value = 'Loading...';
  try {
    const response = await api.get('/api/users');
    users.value = response.data;
    if (users.value.length === 0) {
      userMessage.value = 'Connected successfully, but there are no users in the database yet.';
    }
  } catch (error) {
    userMessage.value = 'Failed to load users. Is the backend running?';
  }
};
</script>
