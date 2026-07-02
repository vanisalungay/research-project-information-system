  <template>
  <div class="page">

  <div class="page-header">
  <h2>Budget Processing</h2>
  <p>Process returned budgets and release approved research funds.</p>
  </div>

  <div class="toolbar">
  <input
  v-model="search"
  class="search"
  type="text"
  placeholder="Search proposal..."
  >

  </div>
    <div class="filters">
    <button
      v-for="filter in filters"
      :key="filter"
      :class="{active:activeFilter===filter}"
      @click="activeFilter=filter"
      >
      {{filter}}
    </button>
  </div>

  <div class="table-container">

  <table class="table">

  <thead>
  <tr>
  <th>Proposal ID</th>
  <th>Proposal Title</th>
  <th>Project Leader</th>
  <th>Budget</th>
  <th>Status</th>
  </tr>
  </thead>

  <tbody>

  <tr
    v-for="proposal in filteredProposals"
    :key="proposal.id"
    class="clickable-row"
    @click="openProposal(proposal)"
    >

    <td>{{proposal.code}}</td>
    <td>{{proposal.title}}</td>
    <td>{{proposal.leader}}</td>
    <td>₱ {{proposal.budget}}</td>

    <td>
    <span
    class="status"
    :class="{
    ready:proposal.status=='Ready for Release',
    returned:proposal.status=='Returned',
    released:proposal.status=='Released'
    }"
    >
    {{proposal.status}}
    </span>
    </td>
  </tr>

  <tr v-if="filteredProposals.length==0">
  <td colspan="5" class="empty">
  No budget records found.
  </td>
  </tr>


  </tbody>

  </table>

  </div>

  </div>
  </template>

  <script>

  export default{

  name:"RpsBudgetProcessing",

  data(){
    return{

      search:"",

      proposals:[],
        
        activeFilter:"All",

        filters:[
          "All",
          "Ready for Release",
          "Returned",
          "Released"
        ],
      }

      
    },

    computed:{

    filteredProposals(){

      const keyword=this.search.toLowerCase().trim()
      return this.proposals.filter(proposal=>{
      const matchSearch=

      proposal.code.toLowerCase().includes(keyword)||
      proposal.title.toLowerCase().includes(keyword)||
      proposal.leader.toLowerCase().includes(keyword)

      const matchStatus=

      this.activeFilter==="All"||
      proposal.status===this.activeFilter

      return matchSearch&&matchStatus
      })
      }

    },

    methods:{

      openProposal(proposal){

      this.$router.push({
      name:"BudgetRelease",
      params:{id:proposal.id,status:proposal.status}
      })

      }
  },
  mounted(){

    const saved=localStorage.getItem("budgetProposal");

    if(saved){

      const proposal=JSON.parse(saved);

      this.proposals=[

      {
      id:proposal.id,
      code:proposal.code,
      title:proposal.project_title,
      leader:proposal.project_leader,
      budget:proposal.approved_budget,
      status:proposal.release_status || "Ready for Release"
      }

      ];

    }

}

}

</script>

<style scoped>

  .page{
  padding:25px;
  font-family:Segoe UI,sans-serif;
  background:#f5f7fb;
  min-height:100vh;
  width:136%;
  }

  .page-header{
  margin-bottom:20px;
  }

  .page-header h2{
  margin:0;
  font-size:28px;
  font-weight:700;
  color:#1e293b;
  }

  .page-header p{
  margin-top:5px;
  font-size:14px;
  color:#64748b;
  }

  .toolbar{
  margin-bottom:20px;
  }

  .search{
  width:100%;
  padding:12px 15px;
  border:1px solid #d1d5db;
  border-radius:8px;
  font-size:14px;
  outline:none;
  box-sizing:border-box;
  }

  .search:focus{
  border-color:#2563eb;
  }

  .table-container{
  background:#fff;
  border-radius:12px;
  overflow:hidden;
  box-shadow:0 4px 12px rgba(0,0,0,.08);
  }

  .table{
  width:100%;
  border-collapse:collapse;
  }

  .table thead{
  background:#1e40af;
  color:#fff;
  }

  .table th,
  .table td{
  padding:15px;
  text-align:left;
  font-size:14px;
  }

  .table td{
  border-bottom:1px solid #e5e7eb;
  color:#374151;
  }

  .table tbody tr:hover{
  background:#f8fafc;
  }

  .status{
  padding:6px 12px;
  border-radius:20px;
  font-size:12px;
  font-weight:600;
  display:inline-block;
  text-align:center;
  min-width:140px;
  }

  .ready{
  background:#dcfce7;
  color:#166534;
  }

  .returned{
  background:#fef3c7;
  color:#92400e;
  }

  .released{
  background:#dbeafe;
  color:#1d4ed8;
  }

  .view-btn{
  background:#2563eb;
  color:#fff;
  border:none;
  padding:8px 18px;
  border-radius:6px;
  cursor:pointer;
  font-size:13px;
  }

  .view-btn:hover{
  background:#1d4ed8;
  }

  .empty{
  padding:25px;
  text-align:center;
  color:#94a3b8;
  }

  .filters{
  display:flex;
  gap:10px;
  margin-bottom:20px;
  }

  .filters button{
  padding:8px 18px;
  border:none;
  border-radius:8px;
  background:#e2e8f0;
  cursor:pointer;
  font-size:13px;
  font-weight:600;
  }

  .filters button.active{
  background:#1e40af;
  color:#fff;
  }

  .clickable-row{
  cursor:pointer;
  transition:.2s;
  }

  .clickable-row:hover{
  background:#eff6ff!important;
  }

</style>