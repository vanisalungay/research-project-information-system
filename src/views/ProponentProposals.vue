<template>
  <div class="proposals-content">
    <div class="top-bar">
      <h2>Proposals</h2>
      <button class="btn-new" @click="showModal = true">+ New Proposal</button>    
    </div>

    <!-- Modal -->
    <ProponentSubmitProp
        v-model="showModal"
        @save="handleSave"
        @next="goToStep2"  
    />

    <ProponentSubmitProp2
        v-if="showModal2"
        :open="showModal2"
        @close="showModal2 = false"
        @openCriteria="openCriteriaModal"
    />


    <ProponentSubmitProp3
        v-model="showCriteria"
        @update:modelValue="val => showCriteria = val"
    />

    <p class="subtitle">Manage and track all your proposal submissions</p>

    <div class="filters">
      <input type="text" placeholder="Search proposals by title or category..." />
      <select>
        <option>All Status</option>
      </select>
      <button class="btn-filter">Filters</button>
    </div>

    <table class="proposals-table">
        <thead>
            <tr>
                <th>Title</th>
                <th>Category</th>
                <th>Status</th>
                <th>Submitted</th>
                <th>Reviewer</th>
                <th>Actions</th>
            </tr>
        </thead>

      <tbody>
        <tr>
          <td>Community Development Program 2024</td>
          <td>Punla/Dagat</td>
          <td><span class="status pending">Pending</span></td>
          <td>2024-12-10</td>
          <td>Jane Smith</td>
          <td><button class="btn-view" @click="viewProposal(index)">View</button></td>
        </tr>

        <tr>
          <td>Healthcare Facility Upgrade</td>
          <td>Tanglaw</td>
          <td><span class="status approved">Approved</span></td>
          <td>2024-12-08</td>
          <td>Mike Johnson</td>
          <td><button class="btn-view">View</button></td>
        </tr>

      </tbody>
    </table>
  </div>
</template>

<script>
export default {
  name: "ProposalsContent"
}
</script>

<script setup>
import { ref } from "vue";
import ProponentSubmitProp from "./ProponentSubmitProp.vue";
import ProponentSubmitProp2 from "./ProponentSubmitProp2.vue";
import ProponentSubmitProp3 from './ProponentSubmitProp3.vue';
import { useRouter } from 'vue-router';

const router = useRouter();

const viewProposal = (index) => {
  // Navigate to the detailed proposal view
  router.push({ 
    name: 'ProponentDetiailedProp', 
    query: { id: index } // pass the proposal index or ID
  });
};



const showModal = ref(false);
const showModal2 = ref(false);
const currentModal = ref(null); 
const showCriteria = ref(false);

const handleSave = (formData) => {
  console.log("Saved Proposal:", formData);
  showModal.value = false;
};

const goToStep2 = (formData) => {
  showModal.value = false;
  showModal2.value = true;
};

const openCriteriaModal = () => {
  showModal2.value = false; // close Step 2 modal
  showCriteria.value = true; // open Step 3 modal
};

const openPreviousModal = () => {
  showCriteria.value = false; // close Step 3 modal
  showModal2.value = true;    // go back to Step 2
};

const closeModal = () => {
  showModal.value = false;
  showModal2.value = false;
  currentModal.value = null;
};
</script>


<style scoped>
.proposals-content {
  padding: 24px;
  background: #ffffff;
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.subtitle {
  color: #6b7280;
  margin-bottom: 16px;
}

.btn-new {
  background: #facc15;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  font-weight: 600;
}

.filters {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.filters input,
.filters select {
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.btn-filter {
  border: 1px solid #e5e7eb;
  background: #fff;
  padding: 8px 14px;
  border-radius: 6px;
}

.proposals-table {
  width: 100%;
  border-collapse: collapse;
}

.proposals-table th {
  text-align: left;
  padding: 12px;
  background: #4b3f72;
  color: #ffffff;
}

.proposals-table td {
  padding: 12px;
  border-bottom: 1px solid #e5e7eb;
}

.status {
  padding: 4px 10px;
  border-radius: 999px;
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

.revision {
  background: #e0e7ff;
  color: #3730a3;
}

.submitted {
  background: #f3e8ff;
  color: #6b21a8;
}

.draft {
  background: #e5e7eb;
  color: #374151;
}

.rejected {
  background: #fee2e2;
  color: #991b1b;
}

.btn-view {
  background: #60a5fa;
  border: none;
  color: #ffffff;
  padding: 6px 12px;
  border-radius: 6px;
}
</style>
