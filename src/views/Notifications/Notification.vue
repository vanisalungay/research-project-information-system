<template>
  <div class="manage-container">
    <h1 class="page-title">MANAGE ACCOUNT</h1>

    <button class="tab-button">Notification Settings</button>

    <div class="form-box">
      <div class="section">
        <h3 class="section-title">Proposal Notifications</h3>
        <label><input type="checkbox" /> New Proposal Assigned</label>
        <label><input type="checkbox" /> Proponent Submitted Revision</label>
        <label><input type="checkbox" /> New Attachment Uploaded</label>
        <label><input type="checkbox" /> Proposal Status Updated</label>
      </div>

      <div class="section">
        <h3 class="section-title">Deadline Notifications</h3>
        <label><input type="checkbox" /> Evaluation Deadline Reminders</label>
        <label><input type="checkbox" /> Revision Deadline Reminders</label>
      </div>

      <div class="section">
        <h3 class="section-title">System Notifications</h3>
        <label><input type="checkbox" /> Email Reminders</label>
        <label><input type="checkbox" /> System Notifications</label>
      </div>

      <button class="btn-save">Save Settings</button>
    </div>
  </div>
</template>

<style scoped>
.page-title {
  background: #1c1a3c;
  padding: 18px;
  color: #ffd429;
  font-size: 28px;
  font-weight: bold;
}

.tab-button {
  background: #ffd429;
  border: none;
  padding: 6px 14px;
  margin-top: 18px;
  border-radius: 6px;
}

.form-box {
  background: #1c1a3c;
  padding: 40px;
  border-radius: 8px;
  color: white;
  margin-top: 20px;
}

.section {
  margin-bottom: 25px;
}

.section-title {
  color: #ffd429;
  font-weight: bold;
  margin-bottom: 10px;
}

label {
  display: block;
  margin-bottom: 6px;
}

.btn-save {
  background: #ffd429;
  border: none;
  padding: 8px 18px;
  border-radius: 6px;
  margin-top: 20px;
}
</style>
