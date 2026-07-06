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

      <p class="back">
        Remember your password?
        <a href="/">Back to Sign In</a>
      </p>
    </div>
  </div>
</template>

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
  font-size: 32px;
  font-weight: 700;
  line-height: 1.2;
}

.title span {
  color: #ffdf00;
}

.school-name {
  margin-top: 40px;
  font-size: 14px;
}

.right-panel {
  width: 60%;
  padding: 50px;
}

.welcome {
  font-size: 36px;
  font-weight: 700;
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
  font-size: 48px;
  margin-bottom: 16px;
}

.contact-admin-card h3 {
  font-size: 20px;
  color: #1f2937;
  margin-bottom: 12px;
}

.instruction {
  color: #4b5563;
  font-size: 15px;
  line-height: 1.6;
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
  font-weight: 600;
  color: #64748b;
  min-width: 100px;
  font-size: 14px;
}

.detail-value {
  color: #1e293b;
  font-size: 14px;
}

.note {
  color: #6b7280;
  font-size: 13px;
  line-height: 1.5;
}

.back {
  margin-top: 24px;
}

.back a {
  color: #2452ff;
  text-decoration: none;
  font-weight: 600;
}

.back a:hover {
  text-decoration: underline;
}
</style>