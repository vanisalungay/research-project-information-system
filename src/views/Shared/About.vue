<template>
  <div class="about">
    <div class="text">
      <h1 class="bold white">Research <span class="bold yellow">Project</span></h1>
      <h1 class="bold yellow">Information</h1>
      <h1 class="white normal">System</h1>
      <div class="description-container">
        <p class="white normal poppins-thin description">
          An institutional web-based information system designed <br />
          to support the management of project proposals<br />
          from submission to completion <br />
          under the Research and Innovation Institute <br />
          of Mindanao State University at Naawan.
        </p>
        <div class="cta-container">
          <button class="cta get-started" @click="gotoLoginPage">Get Started</button>
          <button class="cta learn-more" @click="gotoLoginPage">Learn More</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const gotoLoginPage = () => {
  router.push({ path: 'login' })
}
</script>

<style scoped>
.about {
  min-height: 100vh;
  display: flex;
  align-items: center;

  background-image:
    linear-gradient(rgba(0, 0, 0, 0.6), rgba(0, 0, 0, 0.6)), url('../assets/images/background.png');

  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  padding: 50px 70px;
}

.text h1 {
  margin: 0;
  font-size: 70px;
}

h1 {
  line-height: 1.2;
}

.bold {
  font-weight: 700;
}

.normal {
  font-weight: 400;
}

.white {
  color: #ffffff;
}

.yellow {
  color: #ffd803;
}

.description {
  font-size: 25px;
  margin-top: 50px;
}

.description-container {
  display: flex;
  justify-content: center;
  align-items: center;
}

.cta {
  padding: 10px 20px;
  border-radius: 5px;
  width: 150px;
  height: 66px;
  border-radius: 80px;
  font-size: 18px;
  font-weight: bold;
  cursor: pointer;
}

.get-started {
  background-color: #ffd803;
}

.learn-more {
  background-color: #ffffff;
}

.cta-container {
  margin-left: 30px;
}
</style>
