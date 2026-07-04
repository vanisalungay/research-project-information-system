import { createRouter, createWebHistory } from 'vue-router'
import { useUserDataStore } from '../stores/userData'

import DefaultLayout from '../layouts/DefaultLayout.vue'
import NoNavLayout from '../layouts/NoNavLayout.vue'

import Dashboard from '../views/Dashboard/Index.vue'
import Notification from '../views/Notifications/Notification.vue'
import NotificationsHub from '../views/Notifications/Index.vue'
import NotificationPage from '../views/Notifications/NotificationPage.vue'

/* Auth */
import Login from '../views/Auth/Login.vue'
import ForgotPassword from '../views/Auth/ForgotPassword.vue'
import CreateAccount from '../views/Auth/CreateAccount.vue'
import ProponentLogin from '../views/Auth/ProponentLogin.vue'
import ProponentRegister from '../views/Auth/ProponentRegister.vue'
import ProponentForgotPassword from '../views/Auth/ProponentForgotPassword.vue'

/* Shared */
import About from '../views/Shared/About.vue'
import BackendTest from '../views/Shared/BackendTest.vue'
import FundViewProp from '../views/Shared/FundViewProp.vue'
import ProfileSettings from '../views/Shared/ProfileSettings.vue'
import ProfileInformation from '../views/Shared/ProfileInformation.vue'

/* Admin */
import UserAccounts from '@/views/Admin/UserAccounts.vue'
import CreateUserModal from '@/views/Admin/CreateUserModal.vue'

/* OVCRIGE */
import OvcReviewProposal from '../views/Ovc/OvcReviewProposal.vue'
import OvcSubmittedProposal from '../views/Ovc/OvcSubmittedProposal.vue'
import OvcEndorsedProposal from '../views/Ovc/OvcEndorsedProposal.vue'
import OvcAssignReviewer from '../views/Ovc/OvcAssignReviewer.vue'
import OvcReturn4Revision from '../views/Ovc/OvcReturn4Revision.vue'
import OvcNotifications from '../views/Notifications/OvcNotifications.vue'

/* RPS */
import RpsAdminDashboard from '@/views/Dashboard/RpsAdminDashboard.vue'
import RpsStaffDashboard from '@/views/Dashboard/RpsStaffDashboard.vue'
import RpsAdminVerification from '@/views/Rps/RpsAdminVerification.vue'
import RpsInbox from '@/views/Rps/RpsInbox.vue'
import RpsEndPro from '@/views/Rps/RpsEndPro.vue'
import RpsReturn4Rev from '@/views/Rps/RpsReturn4Rev.vue'
import RpsSubmittedProposals from '@/views/Rps/RpsSubmittedProposals.vue'
import RpsFundedPro from '@/views/Rps/RpsFundedPro.vue'
import RpsEndorsedPro from '@/views/Rps/RpsEndorsedPro.vue'
import RpsProposalDetails from '../views/Rps/RpsProposalDetails.vue'
import RpsNotificationDetails from '../views/Notifications/RpsNotificationDetails.vue'
import RpsProposalReview from '../views/Rps/RpsProposalReview.vue'
import RpsRevResultViewer from '../views/Rps/RpsRevResultViewer.vue'
import RpsBudgetProcessing from '../views/Rps/RpsBudgetProcessing.vue'
import RpsBudgetRelease from '../views/Rps/RpsBudgetRelease.vue'
import RpsProposalMonitoringDetails from '../views/Rps/RpsProposalMonitoringDetails.vue'
import RpsProposalMonitoring from '@/views/Rps/RpsProposalMonitoring.vue'
import RpsAdminNotification from '../views/Notifications/RpsAdminNotification.vue'
import RpsStaffNotification from '../views/Notifications/RpsStaffNotification.vue'

/* OVCAF */
import OvcafDashboard from '../views/Dashboard/OvcafDashboard.vue'
import OvcafValidate from '../views/Ovcaf/OvcafValidate.vue'
import OvcafNotification from '../views/Notifications/OvcafNotification.vue'

/* OC */
import OCDashboard from '../views/Dashboard/OCDashboard.vue'
import OCFinalApproval from '../views/Oc/OCFinalApproval.vue'
import OCFinalApproval2 from '../views/Oc/OCFinalApproval2.vue'
import OcDetailedProposal from '../views/Oc/OcDetailedProposal.vue'
import OCNotifications from '../views/Notifications/OCNotifications.vue'

/* REC */
import RecDashboard from '../views/Dashboard/RecDashboard.vue'
import RecAssignedProposal from '../views/Rec/RecAssignedProposal.vue'
import RecReviewForm from '../views/Rec/RecReviewForm.vue'
import RecMeetingDetails from '../views/Rec/RecMeetingDetails.vue'
import RecRejectProposal from '../views/Rec/RecRejectProposal.vue'
import RecReturn4Revision from '../views/Rec/RecReturn4Revision.vue'
import RecDetailedProp from '../views/Rec/RecDetailedProp.vue'
import RecNotification from '../views/Notifications/RecNotification.vue'

/* PROPONENT */
import ProponentDashboard from '../views/Dashboard/ProponentDashboard.vue'
import ProponentRevision from '@/views/Proponent/ProponentRevision.vue'
import ProponentProposals from '@/views/Proponent/ProponentProposals.vue'
import ProponentEvalCriteria from '@/views/Proponent/ProponentEvalCriteria.vue'
import ProponentFeedback from '@/views/Proponent/ProponentFeedback.vue'
import ProponentSubmitRev from '@/views/Proponent/ProponentSubmitRev.vue'
import ProponentPropDetails from '../views/Proponent/ProponentPropDetails.vue'
import ProponentDetiailedProp from '../views/Proponent/ProponentDetiailedProp.vue'
import ProponentNotifications from '../views/Notifications/ProponentNotifications.vue'

const routes = [
  {
    path: '/',
    component: DefaultLayout,
    meta: { requiresAuth: true },
    children: [
      { path: 'notifications', component: NotificationPage },
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
      { path: 'rpsadmin-notif', component: NotificationPage },

      { path: 'rpsstaff-dash', component: RpsStaffDashboard },
      { path: 'rpsstaff-notification', component: NotificationPage },

      { path: 'rps-endorse', component: RpsEndPro },
      { path: 'rps-return4rev', component: RpsReturn4Rev },
      { path: 'rps-subproposal', component: RpsSubmittedProposals },
      { path: 'rpsendorsed-prop', component: RpsEndorsedPro },
      { path: 'funded-prop', component: RpsFundedPro },

      { path: 'fundviewprop', component: FundViewProp },

      // ================= OVCAF =================
      { path: 'ovcaf-dash', component: OvcafDashboard },
      { path: 'ovcaf-validate/:id?', name: 'OvcafValidate', component: OvcafValidate },
      { path: 'ovcaf-notification', component: NotificationPage },

      // ================= OVCRIGE =================
      { path: 'review-prop', component: OvcReviewProposal },
      { path: 'review-prop/:id', name: 'OvcReviewProposal', component: OvcReviewProposal },
      { path: 'submit-proposals', component: OvcSubmittedProposal },
      { path: 'endorsed-proposals', component: OvcEndorsedProposal },
      { path: 'assign-reviewer', component: OvcAssignReviewer },
      { path: 'assign-reviewer/:id', name: 'OvcAssignReviewer', component: OvcAssignReviewer },
      { path: 'notifications', component: NotificationPage },
      { path: 'return4-revision', component: OvcReturn4Revision },
      { path: 'return4-revision/:id', name: 'OvcReturn4Revision', component: OvcReturn4Revision },

      // ================= REC =================
      { path: 'rec-dash', component: RecDashboard },
      { path: 'assigned-proposals', component: RecAssignedProposal },
      { path: 'review-form', component: RecReviewForm },
      { path: 'review-form/:id', name: 'RecReviewForm', component: RecReviewForm },
      { path: 'meeting-details', component: RecMeetingDetails },
      { path: 'meeting-details/:id', name: 'RecMeetingDetails', component: RecMeetingDetails },
      { path: 'reject-proposal', component: RecRejectProposal },
      { path: 'return-proposals', component: RecReturn4Revision },
      { path: 'rec-notif', component: NotificationPage },
      { path: 'rec-prop', component: RecDetailedProp },

      // ================= PROPONENT =================
      { path: 'proponent-dashboard', component: ProponentDashboard },
      { path: 'proponent-notification', component: NotificationPage },
      { path: 'proposals', component: ProponentProposals },
      { path: 'revisions', component: ProponentRevision },
      { path: 'evalCriteria', component: ProponentEvalCriteria },
      { path: 'proponent-feedback', component: ProponentFeedback },
      { path: 'submit-revision/:id?', component: ProponentSubmitRev },
      { path: 'proponent-prop-details/:id?', component: ProponentPropDetails },
      {
        path: 'detailed-proposal',
        name: 'ProponentDetiailedProp',
        component: ProponentDetiailedProp
      },

      // ================= CHANCELLOR =================
      { path: 'oc-dashboard', component: OCDashboard },
      { path: 'oc-notification', component: NotificationPage },
      { path: 'oc-detailed', component: OcDetailedProposal },
      { path: 'oc/final-approval/:id', name: 'OCFinalApproval', component: OCFinalApproval },
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