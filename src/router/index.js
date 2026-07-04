import { createRouter, createWebHistory } from 'vue-router'
import { useUserDataStore } from '../stores/userData'

import DefaultLayout from '../layouts/DefaultLayout.vue'
import NoNavLayout from '../layouts/NoNavLayout.vue'
import Login from '../views/Login.vue'
import About from '../views/About.vue'
import ProfileSettings from '../views/ProfileSettings.vue'
import ProfileInformation from '../views/ProfileInformation.vue'
import Notification from '../views/Notification.vue'
import ForgotPassword from '../views/ForgotPassword.vue'

import CreateAccount from '../views/CreateAccount.vue'
import Dashboard from '../views/Dashboard/Index.vue'
import BackendTest from '../views/BackendTest.vue'

import OvcReviewProposal from '../views/OvcReviewProposal.vue'
import OvcSubmittedProposal from '../views/OvcSubmittedProposal.vue'
import OvcEndorsedProposal from '../views/OvcEndorsedProposal.vue'
import OvcAssignReviewer from '../views/OvcAssignReviewer.vue'

import RiiAdminDashboard from '@/views/Dashboard/RiiAdminDashboard.vue'
import RiiStaffNotification from '@/views/RiiStaffNotification.vue'
import RiiAdminVerification from '@/views/RiiAdminVerification.vue'
import RiiStaffDashboard from '@/views/Dashboard/RiiStaffDashboard.vue'
import RiiEndPro from '@/views/RiiEndPro.vue'
import RiiReturn4Rev from '@/views/RiiReturn4Rev.vue'
import RiiFundedPro from '@/views/RiiFundedPro.vue'
import RiiEndorsedPro from '@/views/RiiEndorsedPro.vue'
import RiiAdminNotification from '@/views/RiiAdminNotification.vue'
import RpsInbox from '@/views/RpsInbox.vue'
import RpsAdminDashboard from '@/views/Dashboard/RpsAdminDashboard.vue'
import RpsStaffNotification from '@/views/RpsStaffNotification.vue'
import RpsAdminVerification from '@/views/RpsAdminVerification.vue'
import RpsStaffDashboard from '@/views/Dashboard/RpsStaffDashboard.vue'
import RpsEndPro from '@/views/RpsEndPro.vue'
import RpsReturn4Rev from '@/views/RpsReturn4Rev.vue'
import RpsSubmittedProposals from '@/views/RpsSubmittedProposals.vue'
import RpsFundedPro from '@/views/RpsFundedPro.vue'
import RpsEndorsedPro from '@/views/RpsEndorsedPro.vue'
import RpsAdminNotification from '@/views/RpsAdminNotification.vue'

import OvcafDashboard from '../views/Dashboard/OvcafDashboard.vue'
import OvcafValidate from '../views/OvcafValidate.vue'
import OvcafNotification from '../views/OvcafNotification.vue'


import FundViewProp from '../views/FundViewProp.vue'
import OCFinalApproval from '../views/OCFinalApproval.vue'
import OCFinalApproval2 from '../views/OCFinalApproval2.vue'

import OvcNotifications from '../views/OvcNotifications.vue'
import OvcReturn4Revision from '../views/OvcReturn4Revision.vue'

import RecDashboard from '../views/Dashboard/RecDashboard.vue'
import RecAssignedProposal from '../views/RecAssignedProposal.vue'
import RecReviewForm from '../views/RecReviewForm.vue'
import RecMeetingDetails from '../views/RecMeetingDetails.vue'
import RecRejectProposal from '../views/RecRejectProposal.vue'
import RecReturn4Revision from '../views/RecReturn4Revision.vue'
import RecNotification from '../views/RecNotification.vue'
import RecDetailedProp from '../views/RecDetailedProp.vue'

import ProponentNotifications from '@/views/ProponentNotifications.vue'
import ProponentRevision from '@/views/ProponentRevision.vue'
import ProponentProposals from '@/views/ProponentProposals.vue'
import ProponentEvalCriteria from '@/views/ProponentEvalCriteria.vue'
import ProponentFeedback from '@/views/ProponentFeedback.vue'
import ProponentSubmitRev from '@/views/ProponentSubmitRev.vue'
import ProponentDashboard from '../views/Dashboard/ProponentDashboard.vue'
import OCDashboard from '../views/Dashboard/OCDashboard.vue'
import OCNotifications from '../views/OCNotifications.vue'
import ProponentPropDetails from '../views/ProponentPropDetails.vue'
import ProponentDetiailedProp from '../views/ProponentDetiailedProp.vue'
import OcDetailedProposal from '../views/OcDetailedProposal.vue'
import ProponentLogin from '../views/ProponentLogin.vue'
import ProponentRegister from '../views/ProponentRegister.vue'
import ProponentForgotPassword from '../views/ProponentForgotPassword.vue'



import RpsProposalDetails from '../views/RpsProposalDetails.vue'
import RpsNotificationDetails from '../views/RpsNotificationDetails.vue'
import RpsProposalReview from '../views/RpsProposalReview.vue'
import RpsRevResultViewer from '../views/RpsRevResultViewer.vue'
import RpsBudgetProcessing from '../views/RpsBudgetProcessing.vue'
import RpsBudgetRelease from '../views/RpsBudgetRelease.vue'
import RpsProposalMonitoringDetails from '../views/RpsProposalMonitoringDetails.vue'
import RpsProposalMonitoring from '@/views/RpsProposalMonitoring.vue'



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

      { path: 'riiadmin-ver', component: RiiAdminVerification },
      { path: 'riiadmin-dash', component: RiiAdminDashboard },
      { path: 'riistaff-dash', component: RiiStaffDashboard },
      { path: 'riistaff-notification', component: RiiStaffNotification },
      { path: 'rii-endorse', component: RiiEndPro },
      { path: 'rii-return4rev', component: RiiReturn4Rev },
      { path: 'riiendorsed-prop', component: RiiEndorsedPro },
      { path: 'fundviewprop', component: FundViewProp },
      { path: 'riifunded', component: RiiFundedPro },
      { path: 'manage-notifications', component: Notification },


      { path: 'proposal/:id', name: 'ProposalDetails', component: RpsProposalDetails },
      { path: 'notification/:id', name: 'NotificationDetails', component: RpsNotificationDetails },
      { path: 'proposal/:id/review', name: 'ProposalReview', component: RpsProposalReview },
      { path: 'inbox', component: RpsInbox },
      { path: "/review-viewer/:id", name: "ReviewResultViewer", component: RpsRevResultViewer },
      { path: '/budget-processing', name: 'BudgetProcessing', component: RpsBudgetProcessing },
      { path: '/budgetrelease', name: 'BudgetRelease', component: RpsBudgetRelease },
      { path: 'proposal-monitoring-details/:id', name: 'RpsProposalMonitoringDetails', component: RpsProposalMonitoringDetails },
      { path: 'rii-subproposal', component: RpsProposalMonitoring },



      { path: 'riiadmin-ver', component: RiiAdminVerification },
      { path: 'riiadmin-dash', component: RiiAdminDashboard },
      { path: 'riiadmin-notif', component: RiiAdminNotification },
      { path: 'rpsadmin-ver', component: RpsAdminVerification },
      { path: 'rpsadmin-dash', component: RpsAdminDashboard },
      { path: 'rpsstaff-dash', component: RpsStaffDashboard },
      { path: 'rpsstaff-notification', component: RpsStaffNotification },
      { path: 'rps-endorse', component: RpsEndPro },
      { path: 'rps-return4rev', component: RpsReturn4Rev },
      { path: 'rps-subproposal', component: RpsSubmittedProposals },
      { path: 'funded-prop', component: FundedProposals },
      { path: 'rpsendorsed-prop', component: RpsEndorsedPro },
      { path: 'fundviewprop', component: FundViewProp },
      { path: 'fundrelease', component: FundRelease },
      { path: 'rpsfunded', component: RpsFundedPro },
      { path: 'manage-notifications', component: Notification },

      { path: 'rpsadmin-ver', component: RpsAdminVerification },
      { path: 'rpsadmin-dash', component: RpsAdminDashboard },
      { path: 'rpsadmin-notif', component: RpsAdminNotification },

      { path: 'ovcaf-validate', component: OvcafValidate },
      { path: 'ovcaf-notification', component: OvcafNotification },
      { path: 'ovcaf-dash', component: OvcafDashboard },

      { path: 'review-prop', component: OvcReviewProposal },
      { path: 'submit-proposals', component: OvcSubmittedProposal },
      { path: 'endorsed-proposals', component: OvcEndorsedProposal },
      { path: 'assign-reviewer', component: OvcAssignReviewer },
      { path: 'notifications', component: OvcNotifications },
      { path: 'return4-revision', component: OvcReturn4Revision },

      { path: 'rec-dash', component: RecDashboard },
      { path: 'assigned-proposals', component: RecAssignedProposal },
      { path: 'review-form', component: RecReviewForm },
      { path: 'meeting-details', component: RecMeetingDetails },
      { path: 'reject-proposal', component: RecRejectProposal },
      { path: 'return-proposals', component: RecReturn4Revision },
      { path: 'rec-notif', component: RecNotification },
      { path: 'rec-prop', component: RecDetailedProp },

      { path: 'proponent-notification', component: ProponentNotifications },
      { path: 'revisions', component: ProponentRevision },
      { path: 'proposals', component: ProponentProposals },
      { path: 'evalCriteria', component: ProponentEvalCriteria },
      { path: 'proponent-feedback', component: ProponentFeedback },
      { path: 'submit-revision', component: ProponentSubmitRev },
      { path: 'proponent-dashboard', component: ProponentDashboard },
      { path: 'proponent-prop-details', component: ProponentPropDetails },
      { path: 'detailed-proposal', component: ProponentDetiailedProp },
      { path: 'detailed-proposal', component: ProponentDetiailedProp, name: 'ProponentDetiailedProp' },

      { path: 'oc/final-approval/:id', component: OCFinalApproval },
      { path: 'oc/final-approval/:id/approved', component: OCFinalApproval2 },
      { path: 'oc-dashboard', component: OCDashboard },
      { path: 'oc-notification', component: OCNotifications },
      { path: 'oc-detailed', component: OcDetailedProposal },
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
<<<<<<< HEAD

      { path: 'proponent-login', component: ProponentLogin},
=======
      { path: 'proponent-login', component: ProponentLogin },
>>>>>>> 4dca1a44a15ed023945701cda40f73ce74839a50
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

  if (to.matched.some((record) => record.meta.public)) {
    return next()
  }

  if (to.matched.some((record) => record.meta.requiresAuth)) {
    if (!userStore.isLoggedIn) {
      return next({ path: 'login' })
    }
  }

  next()
})

export default router
