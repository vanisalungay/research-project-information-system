<template>
  <div class="login-wrapper">
    <div class="left-panel">
      <img src="@/assets/images/logo.png" class="school-logo" />

      <h1 class="title">
        Research <span>Project</span><br />
        Information<br />
        System
      </h1>

      <p class="school-name">
        RESEARCH AND INNOVATION INSTITUTE<br />
        Mindanao State University at Naawan
      </p>
    </div>

    <div class="right-panel">
      <h2 class="welcome">Forgot Password</h2>

      <div class="contact-admin-card">
        <div class="card-icon">🔑</div>

        <h3>Need to reset your password?</h3>

        <p class="instruction">
          Please contact the <strong>Research and Publication Services (RPS) Admin</strong> to request a password reset for your account.
        </p>

        <div class="admin-details">
          <div class="detail-row">
            <span class="detail-label">Office:</span>
            <span class="detail-value">Research and Publication Services (RPS)</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">Institution:</span>
            <span class="detail-value">MSU-Naawan</span>
          </div>
        </div>

        <p class="note">
          The RPS Admin will reset your password and provide you with a temporary one. You can then change it after logging in.
        </p>
      </div>

      <p class="back">Remember your password?</p>
      <BackButton label="Back to Sign In" to="/" class="auth-back" />
    </div>
  </div>
</template>

<script setup>
import BackButton from '@/components/BackButton.vue'
</script>

<style scoped>
.login-wrapper {
  display: flex;
  height: 100vh;
  background: #f8f8f8;
}

.left-panel {
  width: 40%;
  background: #1f1a3d;
  padding: 40px;
  color: white;
}

.school-logo {
  width: 90px;
  margin-bottom: 20px;
}

.title {
  font-size: var(--ui-font-display);
  font-weight: var(--ui-weight-bold);
  line-height: var(--ui-line-display);
}

.title span {
  color: #ffdf00;
}

.school-name {
  margin-top: 40px;
  font-size: var(--ui-font-body);
}

.right-panel {
  width: 60%;
  padding: 50px;
}

.welcome {
  font-size: var(--ui-font-display);
  font-weight: var(--ui-weight-bold);
  margin-bottom: 25px;
}

.contact-admin-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 32px;
  max-width: 500px;
  text-align: center;
}

.card-icon {
  font-size: var(--ui-font-display);
  margin-bottom: 16px;
}

.contact-admin-card h3 {
  font-size: var(--ui-font-card-title);
  color: #1f2937;
  margin-bottom: 12px;
}

.instruction {
  color: #4b5563;
  font-size: var(--ui-font-body);
  line-height: var(--ui-line-body);
  margin-bottom: 20px;
}

.admin-details {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 20px;
  text-align: left;
}

.detail-row {
  display: flex;
  gap: 8px;
  padding: 6px 0;
}

.detail-label {
  font-weight: var(--ui-weight-semibold);
  color: var(--ui-text-muted);
  min-width: 100px;
  font-size: var(--ui-font-label);
}

.detail-value {
  color: #1e293b;
  font-size: var(--ui-font-body);
}

.note {
  color: #6b7280;
  font-size: var(--ui-font-caption);
  line-height: var(--ui-line-body);
}

.back {
  margin-top: 24px;
}

.auth-back {
  margin-top: 10px;
}
</style>