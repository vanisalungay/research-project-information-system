<template>
  <div class="proposal-view">
    <div class="back" @click="$router.go(-1)">← Back</div>
    <br />
    <h1>-</h1>
    <p class="subtitle">Proposal ID -</p>

    <!-- 1. Project Profile -->
    <section>
      <h2>(1) PROJECT PROFILE</h2>
      <p><strong>Program Title:</strong> -</p>
      <p><strong>Project Title:</strong> -</p>
      <p><strong>Project Leader / Sex:</strong> -</p>

      <div class="grid-3">
        <p><strong>Duration:</strong> -</p>
        <p><strong>Start Date:</strong> -</p>
        <p><strong>End Date:</strong> -</p>
      </div>

      <p><strong>Implementing College / Department:</strong> -</p>
      <p><strong>Address:</strong> -</p>
    </section>

    <!-- 2. Cooperating Agencies -->
    <section>
      <h2>(2) COOPERATING AGENCY / IES</h2>
      <p>-</p>
    </section>

    <!-- 3. Sites of Implementation -->
    <section>
      <h2>(3) SITE(S) OF IMPLEMENTATION</h2>

      <table>
        <thead>
          <tr>
            <th>No.</th>
            <th>Country</th>
            <th>Region</th>
            <th>Province</th>
            <th>District</th>
            <th>Municipality</th>
            <th>Barangay</th>
          </tr>
        </thead>

        <tbody>
          <tr>
            <td>-</td>
            <td>-</td>
            <td>-</td>
            <td>-</td>
            <td>-</td>
            <td>-</td>
            <td>-</td>
          </tr>
        </tbody>
      </table>
    </section>

    <!-- 4. Type of Research -->
    <section>
      <h2>(4) TYPE OF RESEARCH</h2>
      <p>-</p>
    </section>

    <!-- 5. Priority Agenda -->
    <section>
      <h2>(5) PRIORITY AGENDA</h2>
      <ul>
        <li>-</li>
      </ul>
    </section>

    <!-- 6 -->
    <section>
      <h2>(6) INNOVATION GOALS</h2>
      <p>-</p>
    </section>

    <!-- 7 -->
    <section>
      <h2>(7) SECTOR RELEVANCE</h2>
      <p>-</p>
    </section>

    <!-- 8 -->
    <section>
      <h2>(8) APPLICABLE SUSTAINABLE DEVELOPMENT GOAL (SDG)</h2>
      <p>-</p>
    </section>

    <!-- 9 -->
    <section>
      <h2>(9) EXECUTIVE SUMMARY</h2>
      <p>-</p>
    </section>

    <!-- 10 -->
    <section>
      <h2>(10) INTRODUCTION</h2>

      <h3>10.1 Rationale / Significance</h3>
      <p>-</p>

      <h3>10.2 Scientific Basis / Theoretical Framework</h3>
      <p>-</p>

      <h3>10.3 Objectives</h3>

      <p><strong>General Objective:</strong> -</p>

      <p><strong>Specific Objectives:</strong> -</p>
    </section>

    <!-- 11 -->
    <section>
      <h2>(11) REVIEW OF LITERATURE</h2>
      <p>-</p>
    </section>

    <!-- 12 -->
    <section>
      <h2>(12) METHODOLOGY</h2>
      <p>-</p>
    </section>

    <!-- 13 -->
    <section>
      <h2>(13) TECHNOLOGY ROADMAP</h2>
      <p>-</p>
    </section>

    <!-- 14 -->
    <section>
      <h2>(14) EXPECTED OUTPUTS</h2>
      <p>-</p>
    </section>

    <!-- 15 -->
    <section>
      <h2>(15) POTENTIAL OUTCOMES</h2>
      <p>-</p>
    </section>

    <!-- 16 -->
    <section>
      <h2>(16) POTENTIAL IMPACTS</h2>
      <p><strong>Economic:</strong> -</p>
      <p><strong>Social / Ethical:</strong> -</p>
    </section>

    <!-- 17 -->
    <section>
      <h2>(17) TARGET BENEFICIARIES</h2>
      <p>-</p>
    </section>

    <!-- 18 -->
    <section>
      <h2>(18) SUSTAINABILITY PLAN</h2>
      <p>-</p>
    </section>

    <!-- 19 -->
    <section>
      <h2>(19) GENDER AND DEVELOPMENT (GAD) SCORE</h2>
      <p>-</p>
    </section>

    <!-- 20 -->
    <section>
      <h2>(20) LIMITATIONS</h2>
      <p>-</p>
    </section>

    <!-- 21 -->
    <section>
      <h2>(21) RISKS & ASSUMPTIONS</h2>
      <p>-</p>
    </section>

    <!-- 22 -->
    <section>
      <h2>(22) LOGICAL FRAMEWORK</h2>

      <table>
        <thead>
          <tr>
            <th>Outcome Indicators</th>
            <th>Output Indicators</th>
          </tr>
        </thead>

        <tbody>
          <tr>
            <td>-</td>
            <td>-</td>
          </tr>
        </tbody>
      </table>
    </section>

    <!-- 23 -->
    <section>
      <h2>(23) LITERATURE CITED</h2>
      <p>-</p>
    </section>

    <!-- 24 -->
    <section>
      <h2>(24) PERSONNEL REQUIREMENT</h2>
      <p>-</p>
    </section>

    <!-- 25 -->
    <section>
      <h2>(25) LINE-ITEM BUDGET</h2>
      <p>-</p>
    </section>

    <!-- 26 -->
    <section>
      <h2>(26) OTHER ONGOING PROJECTS</h2>
      <p>-</p>
    </section>
  </div>
</template>

<script setup>
// This component is read-only, no props or reactive data needed.
</script>

<style scoped>
.proposal-view {
  max-width: 900px;
  margin: 24px auto;
  padding: 16px;
  font-family: sans-serif;
  line-height: 1.6;
}

h1 {
  font-size: 22px;
  margin-bottom: 4px;
}
.subtitle {
  font-size: 14px;
  color: #555;
  margin-bottom: 16px;
}
h2 {
  font-size: 16px;
  margin-top: 16px;
  margin-bottom: 8px;
  border-bottom: 1px solid #ccc;
  padding-bottom: 2px;
  font-weight: bold;
}
h3 {
  font-size: 14px;
  margin-top: 8px;
  margin-bottom: 4px;
}
p {
  margin: 2px 0 6px 0;
}
.grid-3 {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 4px;
  margin-bottom: 8px;
}
table th,
table td {
  border: 1px solid #ccc;
  padding: 6px;
  text-align: left;
}
</style>
