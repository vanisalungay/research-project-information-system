<template>
<div class="overlay">

<div class="modal">

<h2>Create User</h2>

<input
v-model="user.name"
placeholder="Full Name"
/>

<input
v-model="user.email"
placeholder="Email"
/>

<input
type="password"
v-model="user.password"
placeholder="Password"
/>

<select v-model="user.role">

<option value="RPS_STAFF">RPS STAFF</option>

<option value="RPS_ADMIN">RPS ADMIN</option>

<option value="OVCAF">OVCAF</option>

<option value="OVCRIGE">OVCRIGE</option>

<option value="REC">REC</option>

<option value="OC">OC</option>

</select>

<div class="buttons">

<button @click="$emit('close')">

Cancel

</button>

<button @click="createUser">

Create

</button>

</div>

</div>

</div>
</template>

<script setup>
import { ref } from 'vue'
import axios from 'axios'

const emit = defineEmits(['close','created'])

const user = ref({
    name:'',
    email:'',
    password:'',
    role:'RPS_STAFF'
})

const createUser = async()=>{

   await axios.post(
    "http://localhost:8081/api/users/admin/create",
    user.value
)
    alert("Account Created!")

    emit("created")
    emit("close")

}
</script>

<style scoped>
.overlay{
position:fixed;
top:0;
left:0;
width:100%;
height:100%;
background:rgba(0,0,0,.5);
display:flex;
justify-content:center;
align-items:center;
}

.modal{
background:white;
padding:25px;
width:350px;
display:flex;
flex-direction:column;
gap:10px;
border-radius:10px;
}

input,select{
padding:10px;
}

.buttons{
display:flex;
justify-content:flex-end;
gap:10px;
}
</style>