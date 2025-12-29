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
      <p class="subtitle">
        Enter your registered email address and we’ll send you a password reset link.
      </p>

      <form class="forgot-form" @submit.prevent="handleReset">
        <label>Email Address</label>
        <input type="email" v-model="email" placeholder="you@msunaawan.edu.ph" required />

        <button class="login-btn">Send Reset Link</button>

        <p v-if="successMessage" class="success">{{ successMessage }}</p>
        <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

        <p class="back">
          Remember your password?
          <a href="/">Back to Sign In</a>
        </p>
      </form>
    </div>
  </div>
</template>
