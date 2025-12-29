import { createRouter, createWebHistory } from 'vue-router'
import { useUserDataStore } from '../stores/userData'

import DefaultLayout from '../layouts/DefaultLayout.vue'
import NoNavLayout from '../layouts/NoNavLayout.vue'
import Login from '../views/Login.vue'
import About from '../views/About.vue'
import ProfileSettings from '../views/ProfileSettings.vue'
import ProfileInformation from '../views/ProfileInformation.vue'
import Notification from '../views/Notification.vue'

import CreateAccount from '../views/CreateAccount.vue'
import Dashboard from '../views/Dashboard/Index.vue'

import OvcReviewProposal from '../views/OvcReviewProposal.vue'
import OvcSubmittedProposal from '../views/OvcSubmittedProposal.vue'
import OvcEndorsedProposal from '../views/OvcEndorsedProposal.vue'
import OvcAssignReviewer from '../views/OvcAssignReviewer.vue'
//import OCFinalApproval from '../views/OCFinalApproval.vue'
//import OCFinalApproval2 from '../views/OCFinalApproval2.vue'

import RiiAdminDashboard from '@/views/Dashboard/RiiAdminDashboard.vue'
import RiiStaffNotification from '@/views/RiiStaffNotification.vue'
import RiiAdminVerification from '@/views/RiiAdminVerification.vue'
import RiiStaffDashboard from '@/views/Dashboard/RiiStaffDashboard.vue'
import RiiEndPro from '@/views/RiiEndPro.vue'
import RiiReturn4Rev from '@/views/RiiReturn4Rev.vue'
import RiiSubmittedProposals from '@/views/RiiSubmittedProposals.vue'
import RiiFundedPro from '@/views/RiiFundedPro.vue'
import RiiEndorsedPro from '@/views/RiiEndorsedPro.vue'
import RiiAdminNotification from '@/views/RiiAdminNotification.vue'


import OvcafDashboard from '../views/Dashboard/OvcafDashboard.vue'
import OvcafValidate from '../views/OvcafValidate.vue'
import OvcafNotification from '../views/OvcafNotification.vue'

import FundedProposals from '../views/FundedProposals.vue'
import FundViewProp from '../views/FundViewProp.vue'
import FundRelease from '../views/FundRelease.vue'

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
      { path: 'rii-subproposal', component: RiiSubmittedProposals },
      { path: 'funded-prop', component: FundedProposals },
      { path: 'riiendorsed-prop', component: RiiEndorsedPro },
      { path: 'fundviewprop', component: FundViewProp },
      { path: 'fundrelease', component: FundRelease },
      { path: 'riifunded', component: RiiFundedPro },
      { path: 'manage-notifications', component: Notification },
      // { path: 'riistaff-dash', component: RiiStaffDashboard },
      { path: 'riiadmin-ver', component: RiiAdminVerification },
      { path: 'riiadmin-dash', component: RiiAdminDashboard },
      { path: 'riiadmin-notif', component: RiiAdminNotification },

      { path: 'ovcaf-validate', component: OvcafValidate },
      { path: 'ovcaf-notification', component: OvcafNotification },
      { path: 'ovcaf-dash', component: OvcafDashboard },

      // { path: 'radmin-dash', component: RAdminDashboard },

      { path: 'create-account', component: CreateAccount },
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

      { path: 'proponent-notification', component: ProponentNotifications },
      { path: 'revisions', component: ProponentRevision },
      { path: 'proposals', component: ProponentProposals },
      { path: 'evalCriteria', component: ProponentEvalCriteria },
      { path: 'proponent-feedback', component: ProponentFeedback },
      { path: 'submit-revision', component: ProponentSubmitRev },
      { path: 'proponent-dashboard', component: ProponentDashboard },
      { path: 'proponent-prop-details', component: ProponentPropDetails },
      { path: 'detailed-proposal', name: 'ProponentDetiailedProp', component: ProponentDetiailedProp },

      { path: 'oc/final-approval/:id', component: OCFinalApproval },
      { path: 'oc/final-approval/:id/approved', component: OCFinalApproval2 },
      { path: 'oc-dashboard', component: OCDashboard },
      { path: 'oc-notification', component: OCNotifications },



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

  // public pages (login, about, etc.)
  if (to.matched.some((record) => record.meta.public)) {
    return next()
  }

  // protected pages
  if (to.matched.some((record) => record.meta.requiresAuth)) {
    if (!userStore.isLoggedIn) {
      return next({ path: 'login' })
    }
  }

  next()
})

export default router
