import { createRouter, createWebHistory } from 'vue-router'
import { useUserDataStore } from '../stores/userData'

import DefaultLayout from '../layouts/DefaultLayout.vue'
import NoNavLayout from '../layouts/NoNavLayout.vue'

import Login from '../views/Login.vue'
import About from '../views/About.vue'
import ForgotPassword from '../views/ForgotPassword.vue'
import CreateAccount from '../views/CreateAccount.vue'
import BackendTest from '../views/BackendTest.vue'

import ProfileSettings from '../views/ProfileSettings.vue'
import ProfileInformation from '../views/ProfileInformation.vue'
import Notification from '../views/Notification.vue'
import Dashboard from '../views/Dashboard/Index.vue'
import UserAccounts from '@/views/UserAccounts.vue'
import CreateUserModal from '@/views/CreateUserModal.vue'

/* OVCRIGE */
import OvcReviewProposal from '../views/OvcReviewProposal.vue'
import OvcSubmittedProposal from '../views/OvcSubmittedProposal.vue'
import OvcEndorsedProposal from '../views/OvcEndorsedProposal.vue'
import OvcAssignReviewer from '../views/OvcAssignReviewer.vue'
import OvcNotifications from '../views/OvcNotifications.vue'
import OvcReturn4Revision from '../views/OvcReturn4Revision.vue'

/* RPS */
import RpsAdminDashboard from '@/views/Dashboard/RpsAdminDashboard.vue'
import RpsStaffDashboard from '@/views/Dashboard/RpsStaffDashboard.vue'
import RpsAdminVerification from '@/views/RpsAdminVerification.vue'
import RpsAdminNotification from '@/views/RpsAdminNotification.vue'
import RpsStaffNotification from '@/views/RpsStaffNotification.vue'
import RpsInbox from '@/views/RpsInbox.vue'
import RpsEndPro from '@/views/RpsEndPro.vue'
import RpsReturn4Rev from '@/views/RpsReturn4Rev.vue'
import RpsSubmittedProposals from '@/views/RpsSubmittedProposals.vue'
import RpsFundedPro from '@/views/RpsFundedPro.vue'
import RpsEndorsedPro from '@/views/RpsEndorsedPro.vue'
import RpsProposalDetails from '../views/RpsProposalDetails.vue'
import RpsNotificationDetails from '../views/RpsNotificationDetails.vue'
import RpsProposalReview from '../views/RpsProposalReview.vue'
import RpsRevResultViewer from '../views/RpsRevResultViewer.vue'
import RpsBudgetProcessing from '../views/RpsBudgetProcessing.vue'
import RpsBudgetRelease from '../views/RpsBudgetRelease.vue'
import RpsProposalMonitoringDetails from '../views/RpsProposalMonitoringDetails.vue'
import RpsProposalMonitoring from '@/views/RpsProposalMonitoring.vue'

/* OVCAF */
import OvcafFinancialValidation from '../views/OvcafFinancialValidation.vue'
import OvcafProposalInbox from '../views/OvcafProposalInbox.vue'
import OvcafProposalDetails from '../views/OvcafProposalDetails.vue'
import OvcafReports from '../views/OvcafReports.vue'

/* OC */
import OCDashboard from '../views/Dashboard/OCDashboard.vue'
import OCNotifications from '../views/OCNotifications.vue'
import OCFinalApproval from '../views/OCFinalApproval.vue'
import OCFinalApproval2 from '../views/OCFinalApproval2.vue'
import OcDetailedProposal from '../views/OcDetailedProposal.vue'

/* REC */
import RecDashboard from '../views/Dashboard/RecDashboard.vue'
import RecAssignedProposal from '../views/RecAssignedProposal.vue'
import RecReviewForm from '../views/RecReviewForm.vue'
import RecMeetingDetails from '../views/RecMeetingDetails.vue'
import RecRejectProposal from '../views/RecRejectProposal.vue'
import RecReturn4Revision from '../views/RecReturn4Revision.vue'
import RecNotification from '../views/RecNotification.vue'
import RecDetailedProp from '../views/RecDetailedProp.vue'

/* PROPONENT */
import ProponentDashboard from '../views/Dashboard/ProponentDashboard.vue'
import ProponentNotifications from '@/views/ProponentNotifications.vue'
import ProponentRevision from '@/views/ProponentRevision.vue'
import ProponentProposals from '@/views/ProponentProposals.vue'
import ProponentEvalCriteria from '@/views/ProponentEvalCriteria.vue'
import ProponentFeedback from '@/views/ProponentFeedback.vue'
import ProponentSubmitRev from '@/views/ProponentSubmitRev.vue'
import ProponentPropDetails from '../views/ProponentPropDetails.vue'
import ProponentDetiailedProp from '../views/ProponentDetiailedProp.vue'

/* Authentication */
import ProponentLogin from '../views/ProponentLogin.vue'
import ProponentRegister from '../views/ProponentRegister.vue'
import ProponentForgotPassword from '../views/ProponentForgotPassword.vue'

/* Shared */
import FundViewProp from '../views/FundViewProp.vue'
import OvcafDashboard from '../views/Dashboard/OvcafDashboard.vue'


const routes = [
  {
    path: '/',
    component: DefaultLayout,
    meta: { requiresAuth: true },
    children: [
      { path: 'home', component: Dashboard },
      { path: 'manage-profile', component: ProfileSettings },
      { path: 'profile-info', component: ProfileInformation },
      { path: 'manage-notifications', component: Notification },

      { path: '/user-accounts', name: 'UserAccounts', component: UserAccounts },
      { path: '/create-user-modal', name: 'CreateUserModal', component: CreateUserModal },

      // ================= RPS =================
      { path: 'proposal/:id', name: 'ProposalDetails', component: RpsProposalDetails },
      { path: 'notification/:id', name: 'NotificationDetails', component: RpsNotificationDetails },
      { path: 'proposal/:id/review', name: 'ProposalReview', component: RpsProposalReview },
      { path: 'review-viewer/:id', name: 'ReviewResultViewer', component: RpsRevResultViewer },

      { path: 'inbox', component: RpsInbox },
      { path: 'budget-processing', name: 'BudgetProcessing', component: RpsBudgetProcessing },
      { path: 'budgetrelease', name: 'BudgetRelease', component: RpsBudgetRelease },

      {
        path: 'proposal-monitoring-details/:id',
        name: 'RpsProposalMonitoringDetails',
        component: RpsProposalMonitoringDetails
      },

      { path: 'rps-proposal-monitoring', component: RpsProposalMonitoring },

      { path: 'rpsadmin-ver', component: RpsAdminVerification },
      { path: 'rpsadmin-dash', component: RpsAdminDashboard },
      { path: 'rpsadmin-notif', component: RpsAdminNotification },

      { path: 'rpsstaff-dash', component: RpsStaffDashboard },
      { path: 'rpsstaff-notification', component: RpsStaffNotification },

      { path: 'rps-endorse', component: RpsEndPro },
      { path: 'rps-return4rev', component: RpsReturn4Rev },
      { path: 'rps-subproposal', component: RpsSubmittedProposals },
      { path: 'rpsendorsed-prop', component: RpsEndorsedPro },
      { path: 'rpsfunded', component: RpsFundedPro },

      { path: 'fundviewprop', component: FundViewProp },

      // ================= OVCAF =================
      { path: '/ovcaf/dash', component: OvcafDashboard},
      { path: '/ovcaf/inbox', name: 'ProposalInbox', component: OvcafProposalInbox, }, 
      { path: '/ovcaf/proposals/:id', name: 'ProposalDetails', component: OvcafProposalDetails, },
      { path: '/ovcaf/validate/:id', name: 'FinancialValidation', component: OvcafFinancialValidation, },
      { path: '/ovcaf/reports', name: 'Reports', component: OvcafReports, },



      // ================= OVCRIGE =================
      { path: 'review-prop', component: OvcReviewProposal },
      { path: 'submit-proposals', component: OvcSubmittedProposal },
      { path: 'endorsed-proposals', component: OvcEndorsedProposal },
      { path: 'assign-reviewer', component: OvcAssignReviewer },
      { path: 'notifications', component: OvcNotifications },
      { path: 'return4-revision', component: OvcReturn4Revision },

      // ================= REC =================
      { path: 'rec-dash', component: RecDashboard },
      { path: 'assigned-proposals', component: RecAssignedProposal },
      { path: 'review-form', component: RecReviewForm },
      { path: 'meeting-details', component: RecMeetingDetails },
      { path: 'reject-proposal', component: RecRejectProposal },
      { path: 'return-proposals', component: RecReturn4Revision },
      { path: 'rec-notif', component: RecNotification },
      { path: 'rec-prop', component: RecDetailedProp },

      // ================= PROPONENT =================
      { path: 'proponent-dashboard', component: ProponentDashboard },
      { path: 'proponent-notification', component: ProponentNotifications },
      { path: 'proposals', component: ProponentProposals },
      { path: 'revisions', component: ProponentRevision },
      { path: 'evalCriteria', component: ProponentEvalCriteria },
      { path: 'proponent-feedback', component: ProponentFeedback },
      { path: 'submit-revision', component: ProponentSubmitRev },
      { path: 'proponent-prop-details', component: ProponentPropDetails },
      {
        path: 'detailed-proposal',
        name: 'ProponentDetiailedProp',
        component: ProponentDetiailedProp
      },

      // ================= CHANCELLOR =================
      { path: 'oc-dashboard', component: OCDashboard },
      { path: 'oc-notification', component: OCNotifications },
      { path: 'oc-detailed', component: OcDetailedProposal },
      { path: 'oc/final-approval/:id', component: OCFinalApproval },
      { path: 'oc/final-approval/:id/approved', component: OCFinalApproval2 },
    ],
  },

  {
    path: '/',
    component: NoNavLayout,
    meta: { public: true },
    children: [
      { path: 'login', component: Login },
      { path: 'about', component: About },
      { path: 'create-account', component: CreateAccount },
      { path: 'register', component: ForgotPassword },

      { path: 'proponent-login', component: ProponentLogin },
      { path: 'proponent-register', component: ProponentRegister },
      { path: 'proponent-forgot-password', component: ProponentForgotPassword },

      { path: 'test-backend', component: BackendTest },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const userStore = useUserDataStore()
  userStore.hydrate()

  if (to.matched.some(record => record.meta.public)) {
    return next()
  }

  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!userStore.isLoggedIn) {
      return next('/login')
    }
  }

  next()
})

export default router