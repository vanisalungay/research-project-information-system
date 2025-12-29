<template>
  <div class="criteria-content">
    <h3 class="section-title">A. Criteria for Evaluating Proposals</h3>

    <table class="criteria-table">
      <thead>
        <tr>
          <th>Criterion</th>
          <th>Definition</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td>Relevance or Significance</td>
          <td>
            Aligned with the priorities of the OVCROG
            the approach ensures strategic relevance to the university objectives
            while being mindful of the political cultural and traditional context
            as well as gender and development considerations
          </td>
        </tr>
        <tr>
          <td>Technical or Scientific Merit</td>
          <td>
            Sound scientific basis to generate new knowledge
            or apply existing knowledge in innovative manner
          </td>
        </tr>
        <tr>
          <td>Budget Appropriateness</td>
          <td>
            The proposed budget is commensurate to the proposed work plan
            and deliverables
          </td>
        </tr>
        <tr>
          <td>Competence of Proponent</td>
          <td>
            Proponents expertise is relevant to the proposal
            and with proven competence in implementing managed and completed
            RnD projects within the approved duration and budget
          </td>
        </tr>
      </tbody>
    </table>

    <h3 class="section-title">B. REC Evaluation Criteria</h3>

    <table class="rec-table">
      <thead>
        <tr>
          <th>Criteria</th>
          <th>Indicators</th>
          <th>Raw Score</th>
        </tr>
      </thead>

      <tbody>
        <tr class="group">
          <td rowspan="4">A Soundness of Proposal 20%</td>
          <td>R and D addresses relevant sectoral need applicable to pressing concern</td>
          <td>5</td>
        </tr>
        <tr>
          <td>Solution provided is most effective compared to other proposed solutions</td>
          <td>5</td>
        </tr>
        <tr>
          <td>Proposed budget is reasonable vis a vis work plan</td>
          <td>5</td>
        </tr>
        <tr>
          <td>Work plan is doable in a given timeframe</td>
          <td>5</td>
        </tr>

        <tr class="group">
          <td rowspan="4">B Suitability of Output 30%</td>
          <td>R and D output is cost effective competitive and responsive</td>
          <td>5</td>
        </tr>
        <tr>
          <td>Has identified partners to adopt technology with letter of support</td>
          <td>5</td>
        </tr>
        <tr>
          <td>Output can be commercialized through an existing manufacturer</td>
          <td>5</td>
        </tr>
        <tr>
          <td>R and D utilization is simple and ready for extension</td>
          <td>5</td>
        </tr>

        <tr class="group">
          <td rowspan="4">C Significance of Outcomes 30%</td>
          <td>Economic increased productivity income jobs and ROI</td>
          <td>5</td>
        </tr>
        <tr>
          <td>Social partnerships gender inclusive education and public service</td>
          <td>5</td>
        </tr>
        <tr>
          <td>Environmental minimal impact and no adverse effect</td>
          <td>5</td>
        </tr>
        <tr>
          <td>Sustainability financial and institutional mechanisms established</td>
          <td>5</td>
        </tr>

        <tr class="group">
          <td rowspan="3">D Completeness of Proponent 20%</td>
          <td>Collaboration with agencies and industry partners</td>
          <td>5</td>
        </tr>
        <tr>
          <td>Clear understanding of proposal deliverables</td>
          <td>5</td>
        </tr>
        <tr>
          <td>Good experience in the proposed field</td>
          <td>5</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
export default {
  name: "EvaluationCriteriaContent"
}
</script>

<style scoped>
.criteria-content {
  padding: 24px;
  background: #ffffff;
}

.section-title {
  margin: 16px 0;
  font-weight: 700;
}

.criteria-table,
.rec-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 24px;
}

.criteria-table th,
.rec-table th {
  background: #aab2c0;
  border: 2px solid #1e3a8a;
  padding: 10px;
  text-align: left;
  font-weight: bold;
}

.criteria-table td,
.rec-table td {
  border: 2px solid #1e3a8a;
  padding: 10px;
  vertical-align: top;
}

.rec-table td:last-child,
.rec-table th:last-child {
  text-align: center;
  width: 80px;
}

.group {
  background: #f9fafb;
  font-weight: 600;
}
</style>
