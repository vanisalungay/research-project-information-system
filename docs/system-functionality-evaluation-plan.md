# SYSTEM FUNCTIONALITY EVALUATION PLAN

**Name of System:** Research Project Information System (RPIS) with Rule-Based Decision Support
**Institution:** Mindanao State University at Naawan — Research and Publication Services (RPS)

---

## Document Management

| Field | Entry |
|---|---|
| Document Summary | A role-based functionality test plan for RPIS covering the eight implemented user roles, their modules, the end-to-end proposal workflow, and the reporting workflow. |
| Type of Document | Functionality Test |
| Date of Evaluation | ____________________ |
| Name and Profession of Evaluator | ____________________ |
| Evaluators per Role | See Deliverable 4, Section 4.1 |

### Developers

| Full Name | Role |
|---|---|
| ____________________ | ____________________ |
| ____________________ | ____________________ |
| ____________________ | ____________________ |
| ____________________ | ____________________ |

> The developer names are left blank for the team to complete. No name has been invented.

### How to Use This Document

1. Each role has its own set of tables. A table is grouped under the **Parent Module** it tests.
2. Perform each task in the order shown. Use the test accounts and fixtures in Deliverable 4.
3. Record **Pass** or **Fail** in the result column. Leave it blank if the task was not performed.
4. Sign the row you tested. Use **Remarks** for the observed message, a defect number, or a reason a task was skipped.
5. Tasks marked **VERIFY** have behaviour that could not be confirmed from the source. Record what actually happens instead of assuming an expected result.

### Scope and Evidence

Every scenario below was derived from a read-only inspection of the implemented system: Vue routes, views, components and stores; Spring Boot controllers, services, entities and security configuration. No source code, database schema, database record, or application setting was changed. Scenarios describe only implemented functionality. Items that could not be confirmed are marked **VERIFY**. Pages that exist but are stubs, mock-ups, or unreachable are excluded and listed in Deliverable 3.

---

## Verified Workflow Tested by This Plan

```
Proponent DRAFT → FOR REVIEW
  → RPS Staff: ENDORSED (to OVCRIGE) | REJECTED | return to Proponent (RPS_RETURNED)
OVCRIGE ENDORSED → UNDER_REVIEW (forward to REC)
REC UNDER_REVIEW → REC_APPROVED | RETURNED_TO_RPS | REJECTED
OVCRIGE REC_APPROVED → FOR_OC_APPROVAL
OC FOR_OC_APPROVAL → FOR_OVCAF_APPROVAL (With Budget) | APPROVED (Without Budget)
OVCAF FOR_OVCAF_APPROVAL → READY_FOR_RELEASE (Budget Approved) → APPROVED (Fund Release recorded)
APPROVED / RELEASED + issued Special Order → Q1 → Q2 → Q3 → TERMINAL → COMPLETED
```

**APPROVED means "Implementation Approved". COMPLETED means reporting has finished.** They are distinct states and are tested separately.

---

# DELIVERABLE 1 — SYSTEM FUNCTIONALITY INVENTORY

## 1.1 Roles Actually Implemented

| Role Code | UI Label | Independent? | Data Scope |
|---|---|---|---|
| `PROPONENT` | Proponent | Yes | Own proposals, endorsements, quarterly and terminal reports |
| `RPS_ADMIN` | No login card | Yes | Accounts, Deans, announcements, priority agendas, activity log |
| `RPS_STAFF` | "ORPS" on the login page | Yes | Proposal review and endorsement, revision forwarding, monitoring |
| `DEAN` | Dean | Yes — not privileged | Own college's endorsement requests only |
| `OVCRIGE` | OVCRIGE | Privileged | University-wide proposals |
| `REC` | REC | Privileged | University-wide proposals; no per-member assignment |
| `OVCAF` | OVCAF | Privileged | Proposals at the finance stage |
| `OC` | Chancellor (OC) | Privileged | Proposals awaiting final approval |

## 1.2 Modules and Functionality per Role

### Proponent

| Module | Implemented Functionality |
|---|---|
| Authentication | Login with role card; self-registration (`PENDING`); no password-reset flow (contact RPS Admin) |
| Profile | Personal information, contact, specialization, notification preferences, electronic signature |
| Dashboard | Own project statistics, recent proposals, notifications |
| My Projects | List of own proposals; start a new proposal; resume a draft; read-only submitted view |
| Submit Proposal | Four-step wizard: Call → 27-section proposal details (6 substeps) → Certification & Endorsement → Review & Submit; attachments (technology map, line-item budget, endorsement document, submitted-by signature) |
| Endorsements | Request Dean endorsement; pending/approved/rejected; issued certification view and download |
| Revisions | RPS-returned proposals, section and field remarks, revision deadline, submit revision |
| Monitoring | Own proposals with stage and status |
| Quarterly Reports | Eligible projects; Q1–Q3 forms (objectives, 6Ps outputs, 12-month workplan); save draft; submit; stage progression |
| Terminal Report | 14-section report; abstract limited to 350 words; submitting completes the project |
| Notifications | Shared notification page, deep links, mark read and mark all |

### RPS Administrator

| Module | Implemented Functionality |
|---|---|
| Dashboard | Account statistics; pending registrations; approve and reject |
| User Accounts | Search list; create account with server-generated email and temporary password; reset password |
| Dean Role Management | Create Dean (designation and college); replace Dean; filter ACTIVE/REVOKED/ALL |
| Priority Agendas | Create framework and items; activate framework |
| Proposal Announcements | Create, edit, activate, and delete calls for proposals |
| Activity Log | Own actions; search, module, status, and date filters; pagination; detail view |
| Profile | Personal information, notification preferences, electronic signature |

### RPS Staff

| Module | Implemented Functionality |
|---|---|
| Dashboard | Pending-review statistics; submitted proposals |
| Submitted Proposals | Review proposal; endorse to OVCRIGE; return to Proponent; reject |
| Revision Requests | List `RETURNED_TO_RPS`; set deadline and forward to Proponent |
| Budget Processing | Read-only list of proposals; no fund-release action |
| Proposal Monitoring | University-wide proposal list and detail |
| Quarterly Reports | Read-only master list and report viewer |
| Announcements | View proposal announcements |
| Priority Agendas | Sidebar link exists but the route is RPS Admin only — dead link |
| Notifications | Shared notification page |

### Dean

| Module | Implemented Functionality |
|---|---|
| Dashboard | Pending, approved, and rejected counts; pending list; recently decided |
| Endorsement Requests | Status-filtered list, scoped to the Dean's college |
| Endorsement Review | Approve and issue PDF letter using the saved electronic signature; reject with reason; reissue letter; view and download letter; audit history |
| Profile | Electronic signature management (required to issue letters) |

### OVCRIGE

| Module | Implemented Functionality |
|---|---|
| Dashboard | Status statistics; latest proposals; notifications |
| Endorsed Proposals | Proposals endorsed by RPS; search and filter; open review |
| Proposals for OC Endorsement | `REC_APPROVED`, `OVC_APPROVED`, and `FOR_OC_APPROVAL` queue |
| Review Proposal | Forward to REC; forward to OC; return for revision (comments and checklist); reject; field remarks; document download |
| Quarterly Reports | Read-only master list and three-page report viewer |
| Notifications | Shared notification page |

### REC

| Module | Implemented Functionality |
|---|---|
| Dashboard | Proposal statistics; latest proposals; notifications |
| Assigned Proposals | Status-filtered university-wide list (not per-member assignment); search |
| Proposal Detail | Read-only 27-section view; field remarks |
| Review Form | 15 scored criteria in four weighted groups (1–5, optional); overall comments; Approve → `REC_APPROVED` |
| Reject Proposal | Reject with confirmation; typed reason is not submitted |
| Return for Revision | Comments and four-item checklist required → `RETURNED_TO_RPS` |
| Meeting Details | Read-only project profile and attachments; no meeting scheduling |
| Notifications | Shared notification page; proposal deep links |

### Office of the Chancellor (OC)

| Module | Implemented Functionality |
|---|---|
| Dashboard | Pending final approval; approved and released; sent to OVCAF |
| Final Approval | Review proposal and review trail; Chancellor's notes; budget classification With/Without; Special Order number and document; approve and issue SO; save SO draft; return to OVCRIGE; download proposal details |
| Notifications | Shared notification page |

### OVCAF

| Module | Implemented Functionality |
|---|---|
| Dashboard | For review; endorsed to OC; approved and released |
| Inbox | Proposal list with search, status and college filters, sorting, pagination |
| Financial Validation | Line-item budget preview and download; six-item financial checklist; decision Approved, Returned for Revision, or Rejected with remarks |
| Fund Release | Record fund release on a budget-approved proposal → Implementation Approved |
| Financial Reports | Counts and budgets by status; endorsement rate |
| Notifications | Shared notification page |

## 1.3 Implemented vs Incomplete or Unavailable

| Area | Status | Evidence |
|---|---|---|
| RPS Admin login | **BROKEN** — no RPS Admin role card; login requires an exact role match | `src/views/Auth/Login.vue:128-138`; `UserService.login` |
| Google login | Not usable — handler exists but no button; server audience placeholder | `Login.vue:169-190`; `UserController` google-login |
| Forgot password | Not implemented — static "contact RPS Admin" page; `/register` shows it | `ForgotPassword.vue`; `router/index.js:295` |
| RPS Staff Priority Agendas | Dead link — sidebar item; route is RPS Admin only | `NavMenu.vue`; `router/index.js`; `SecurityConfig` |
| Endorsement letter templates | Only the IFP funding program has a template; other programs are blocked at approval | `EndorsementLetterTemplates.java:30`; `requireImplemented` |
| OVCAF inbox table | Seven headers against eight body cells — the college cell shifts budget, date, status, and action columns | `OvcafProposalInbox.vue:72-105` |
| OC approval trail | Static placeholder timeline, not derived from proposal history | `OCFinalApproval.vue:89-123` |
| Proposal status history | Only OC transitions and QPR completion are recorded; no history endpoint or UI for other stages | `ProposalService.recordStatusChange`; `ProposalStatusHistory` |
| Terminal Report GAD checklist | Does not exist — the GAD checklist is proposal section 19 only | `terminalReport.js`; `GadChecklist.vue` |
| Submit Revision "Save Draft" | Not persisted — alert only | `SubmitRev` view |
| OVCRIGE/REC rejection reason | Reason fields are not submitted; only the status changes | `OvcReviewProposal.vue`; `RecRejectProposal.vue` |
| OVCRIGE return redirect | Redirects to `/review-prop` without an id and renders an error state | `OvcReturn4Revision.vue` |
| REC meeting scheduling | No scheduling UI or backend fields | `RecMeetingDetails.vue` |
| RPS Budget Release / RPS Return4Rev | Local-storage mocks, not backend actions | `RpsBudgetRelease.vue`; `RpsReturn4Rev.vue` |
| Proponent Feedback / role notification pages | Stubs and dead code; the shared notification page is used | `ProponentFeedback.vue`; `Notifications/Index.vue` |
| Unrouted pages | `OvcAssignReviewer` (mock), `ProposalReviewPage` (unused), `DeanAccountsModal` (orphan) | Router and imports |
| Revision attachments | The file pickers record file names only; new files are not uploaded | `ProponentSubmitRev.vue` |
| QPR and Terminal review | No review, approval, remarks, or delete functions exist; RPS and OVCRIGE can only view | `QuarterlyProgressReportController`; `QuarterlyReportsMaster.vue` |
| QPR deadlines | No due dates, late flags, or quarter-selection windows | Quarterly reporting module |
| Terminal Report project team | Marked as required in the form but not enforced at submission | `terminalReport.js:768-774`, `:858` |
| Submission certifications | The form requires signature and endorsement fields; the backend does not enforce them | `ProposalService.java:567-586` |

## 1.4 Findings to Report, Not to Test as Working

1. RPS Admin cannot sign in through the UI, which blocks that role's evaluation.
2. RPS Staff sidebar shows Priority Agendas but the route and API reject the role.
3. The Terminal Report has no GAD checklist.
4. OVCRIGE and REC rejection does not store a reason.
5. OVCRIGE return-for-revision ends on an error page.
6. Submit Revision "Save Draft" does not save.
7. Public registration accepts an arbitrary role through a direct API call, and RPS Staff can create RPS Admin accounts.
8. Admin password reset and Profile Settings password change bypass policy and current-password checks.
9. Non-proponents can mark any notification id as read.
10. Forward-to-REC, forward-to-OC, and reject endpoints have no explicit role matcher; any authenticated user can call them.
11. `/api/ovcaf/**` and `/api/oc/**` have no role matcher; any signed-in user can call them.
12. `POST /api/proposals/{id}/reviews` trusts a caller-supplied `reviewerId`; the caller's role is not checked.
13. OC has no decline path; the only negative action is Return to OVCRIGE.
14. OVCAF fund release writes `APPROVED`, so the `RELEASED` state and its notification are unreachable.
15. Endorsement approval is limited to the IFP letter template; other funding programs cannot be endorsed until a template exists.
16. Revision attachment pickers submit file names only; replacement files are never uploaded.
17. Quarterly and Terminal Reports have no review, approval, remarks, or delete functions at any role.
18. The form marks the Terminal Report project team as required, but submission does not enforce it.
19. The proposal submission form requires certification and endorsement fields, but the backend does not enforce them.

---

# DELIVERABLE 2 — ROLE-BASED TEST SCENARIOS

> The `Pass / Fail`, `Tested By`, and `Remarks` columns are intentionally blank. The evaluator completes them.

## USER ROLE: Proponent

### Parent Module: Authentication & Account

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 1 | Sign in with a valid proponent email, password, and the "Proponent" role card. | The Proponent dashboard opens, the account name is shown, and the sidebar lists only Proponent menus. | | | |
| 2 | Sign in with correct proponent credentials while the "Dean" role card is selected. | Sign-in is refused with "Invalid email, password, or role."; no session opens. | | | |
| 3 | From Sign up, register using a non-MSU email address. | The message "Only MSU Naawan email addresses (@msunaawan.edu.ph) are allowed." appears; no account is created. | | | |
| 4 | Register with a valid `@msunaawan.edu.ph` address, then try to sign in before approval. | Registration reports success; sign-in is refused with "Your account is pending RII ADMIN approval." | | | |
| 5 | Open Profile Settings and update name, department or office, contact number, and specialization. | The values are saved and remain after reloading the page. | | | |
| 6 | Upload a PNG electronic signature, then upload a 3 MB image. | The PNG is accepted and shown; the oversized image is rejected with the 2 MB limit message. | | | |

### Parent Module: Proponent Dashboard

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 7 | Open the dashboard with existing proposals in different stages. | Project counts and recent proposals reflect only the signed-in proponent's records; each shows its current status. | | | |
| 8 | Open the notification icon or card on the dashboard. | Unread notifications are listed; selecting one marks it read and opens the related record. | | | |

### Parent Module: My Projects

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 9 | Open My Projects with more than one proponent in the system. | Only the signed-in proponent's proposals are listed. | | | |
| 10 | Open a saved draft and continue editing it. | The previously entered values are restored and the form opens for continued editing. | | | |
| 11 | Try to submit a proposal when there is no active call for proposals. | Submission is blocked with "Submissions are currently closed. There is no active proposal announcement at this time." | | | |

### Parent Module: Submit New Proposal

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 12 | On Step 1, view a call whose start date is in the future. | The call is listed as scheduled; "Apply to This Call" is disabled and the opening date is shown. | | | |
| 13 | On Step 1, select an open call. | The call is selected and Step 2 (Proposal Details) opens for that call. | | | |
| 14 | On Step 2, leave the project title blank and try to continue. | A required-field message names the project title; the wizard does not advance. | | | |
| 15 | Set a project duration other than 12, 24, or 36 months. | The value is rejected and only 12, 24, or 36 months can be submitted. | | | |
| 16 | Answer only part of the GAD checklist in section 19. | Submission is blocked until all checklist items are answered. | | | |
| 17 | Reach Step 4 without uploading a line-item budget file. | Submission is blocked and the line-item budget file is required. | | | |
| 18 | Complete a valid proposal and submit it. | A confirmation appears; the proposal receives a reference or document ID and its status becomes "For RPS Review". | | | |
| 19 | Open the submitted proposal's details. | The technology map, line-item budget, endorsement document, and submitted-by signature are listed and downloadable. | | | |
| 20 | Try to edit the proposal after it has been submitted. | The proposal is read-only; no editing control is available. | | | |

### Parent Module: Endorsements

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 21 | Create an endorsement request with required fields left blank. | Validation identifies the missing field and the request is not created. | | | |
| 22 | Submit a complete endorsement request. | The request is created with status Pending and its reference number is shown. | | | |
| 23 | Open an endorsement request the Dean has approved. | The status is Approved, the endorsement letter details are shown, and an issued certification is available. | | | |
| 24 | Download the issued certification. | The certification document opens or downloads with the correct reference. | | | |
| 25 | Open an endorsement request the Dean rejected. | The rejection reason is displayed and the proponent can create a new request. | | | |

### Parent Module: Revisions

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 26 | Open a proposal returned by RPS. | The return remarks, flagged sections and fields, and revision deadline are shown. | | | |
| 27 | Submit a corrected revision before the deadline. | The revision is accepted; the proposal returns to "For RPS Review" and the revision number increases. | | | |
| 28 | Submit a revision after the deadline. | Submission is refused with "The revision deadline has passed. Revision submission is no longer available." | | | |

### Parent Module: Proposal Monitoring

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 29 | Open Proposal Monitoring. | Own proposals are listed with their current stage and status. | | | |
| 30 | Open one proposal's detail or timeline. | The current stage and status are shown. **VERIFY:** a full dated history is only recorded for OC transitions and QPR completion; record what the page actually displays. | | | |

### Parent Module: Quarterly Reports

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 31 | Open Quarterly Reports for a project that is not Implementation Approved or has no issued Special Order. | The project is not offered for reporting; the eligibility reason is shown. | | | |
| 32 | Open Quarterly Reports for an eligible project. | The project is listed and the next required stage (Q1) is shown. | | | |
| 33 | Create a Q1 report with a workplan objective that has no description. | The report cannot be submitted until at least one objective is described. | | | |
| 34 | Enter a percentage above 100 in an objective row. | The value is rejected or limited to 100; it cannot be saved as entered. | | | |
| 35 | Submit a completed Q1 report. | The report status becomes Submitted and the next stage becomes Q2. | | | |
| 36 | Try to create a Q2 report before Q1 is submitted. | The action is refused and only Q1 can be created. | | | |
| 37 | Save a Q1 report as a draft, close it, and reopen it. | All entered values are restored exactly as saved. | | | |

### Parent Module: Terminal Report

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 38 | Open the Terminal Report before Q1–Q3 are all submitted. | The Terminal Report is not yet available and the remaining quarterly stage is shown. | | | |
| 39 | Enter an abstract longer than 350 words. | The abstract is rejected with the 350-word limit message. | | | |
| 40 | Submit a terminal report whose Introduction or Results and Discussion is blank. | Submission is blocked and the missing required section is identified. | | | |
| 41 | Submit a complete Terminal Report after Q1–Q3. | The report is submitted and the project status becomes Completed. | | | |
| 42 | Reopen the project after completion. | The project shows Completed and the reporting page is read-only with no option to create another report. | | | |

### Parent Module: Notifications

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 43 | Trigger a status change on a proposal, for example an endorsement forwarded. | A notification with the correct title and message appears for the proponent. | | | |
| 44 | Select an unread notification. | It is marked read and opens the related proposal or endorsement page. | | | |
| 45 | Use "Mark All as Read". | All of the proponent's notifications become read and the unread count becomes zero. | | | |

---

## USER ROLE: RPS Administrator

### Parent Module: Authentication & Dashboard

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 1 | Sign in with an RPS Admin account. | The RPS Admin dashboard opens. **Known issue:** no RPS Admin role card exists on the login page; record the actual outcome. | | | |
| 2 | Open the dashboard with pending registrations. | Total, pending, active, and rejected account counts are shown with the pending-account list. The dashboard is account-focused and has no proposal widgets. | | | |
| 3 | Approve a pending registration. | The account becomes Approved, the user is notified by email, and the user can sign in. | | | |
| 4 | Reject a pending registration. | The account becomes Rejected and the user cannot sign in. | | | |

### Parent Module: User Accounts

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 5 | Search for an account by name or email. | Only matching accounts are listed. | | | |
| 6 | Create a staff account, for example OVCRIGE, with a full name and role. | An institutional email and temporary password are generated and shown once. | | | |
| 7 | Sign in with the newly created account. | The user is forced to change the temporary password before reaching a dashboard. | | | |
| 8 | Reset another user's password. | The confirmation appears and the new password is accepted at the next sign-in. | | | |
| 9 | Open Create Account and inspect the role choices. | No Proponent role is offered, and Dean requires designation and assigned college. | | | |

### Parent Module: Dean Role Management

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 10 | Create a Dean account with designation and assigned college. | The Dean account is created and appears as an Active assignment. | | | |
| 11 | Replace an active Dean with another account. | The old assignment becomes Revoked, the new one becomes Active, and the history is preserved. | | | |
| 12 | Filter assignments by ACTIVE, REVOKED, and ALL. | Each filter shows only assignments in that state. | | | |

### Parent Module: Priority Agendas

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 13 | Create a priority agenda framework with an end year earlier than the start year. | The framework is rejected with a coverage-year validation message. | | | |
| 14 | Add an agenda item and activate the framework. | The item is saved and the framework becomes the active set used by new proposals. | | | |

### Parent Module: Proposal Announcements

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 15 | Create an announcement with an end date before its start date. | The announcement is rejected with the date validation message. | | | |
| 16 | Activate a call for proposals. | The call becomes Open and all approved proponents are notified. | | | |
| 17 | Try to delete an active announcement. | Deletion is refused with the message that active announcements must be closed first. | | | |

### Parent Module: Activity Log

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 18 | Open the Activity Log after performing account and announcement actions. | The administrator's own actions are listed, newest first, with module and result. | | | |
| 19 | Filter by module, result, and date range, then open a detail entry. | The list narrows correctly and the detail shows the full entry. | | | |

---

## USER ROLE: RPS Staff

### Parent Module: Dashboard & Submitted Proposals

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 1 | Sign in as RPS Staff using the "ORPS" card. | The RPS Staff dashboard opens with proposal statistics. | | | |
| 2 | Open Submitted Proposals. | Proposals in "For RPS Review" are listed with proponent, date, and status. | | | |
| 3 | Open a submitted proposal. | All proposal sections, uploaded attachments, and the workflow timeline are shown. | | | |

### Parent Module: Proposal Review

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 4 | Endorse a valid proposal to OVCRIGE. | The status becomes "Endorsed to OVCRIGE" and OVCRIGE receives a notification. | | | |
| 5 | Reject a proposal. | The status becomes Rejected and the proponent is notified. | | | |
| 6 | Return a proposal for revision with remarks for specific sections or fields. | The proposal becomes "Returned to RPS" internally with remarks recorded, ready to forward to the proponent. | | | |
| 7 | Forward a returned proposal to the proponent with a deadline and notes. | The status becomes "Returned to Proponent" and the proponent receives a revision notification with the deadline. | | | |

### Parent Module: Revision Requests

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 8 | Open Revision Requests after a reviewing office returns a proposal. | The returned proposal is listed with the office that returned it. | | | |
| 9 | Forward the revision without entering a deadline. | A validation message requires the deadline; the forward action does not complete. | | | |
| 10 | Forward the revision with a deadline and notes. | The proponent sees the deadline and the notes, and the status becomes "Returned to Proponent". | | | |

### Parent Module: Budget Processing

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 11 | Open Budget Processing. | Proposals awaiting budget action are listed; the page provides viewing only, with no fund-release control. | | | |

### Parent Module: Proposal Monitoring

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 12 | Open Proposal Monitoring. | Proposals across all workflow stages are listed with their current status. | | | |
| 13 | Open a proposal from monitoring. | The read-only details and status history are shown. | | | |

### Parent Module: Quarterly Reports

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 14 | Open Quarterly Reports. | Projects that are Implementation Approved with an issued Special Order are listed. | | | |
| 15 | Open a submitted quarterly report. | The report opens read-only; there is no submit or approval control. | | | |

### Parent Module: Announcements & Notifications

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 16 | Open Proposal Announcements. | Active and closed calls are listed with their dates and derived status. | | | |
| 17 | Select "Priority Agendas" in the sidebar. | **Known issue:** the route is restricted to RPS Admin; record whether the page opens or the user is redirected. | | | |
| 18 | Open Notifications after a new proposal submission. | A submission notification is listed; selecting it opens the proposal. | | | |

---

## USER ROLE: Dean

> **Fixture prerequisite:** use a request whose call uses the **IFP** funding program. IFP is the only program with an implemented endorsement-letter template. Any other program is blocked at approval with "No official endorsement letter is configured for {funding program} yet, so it cannot be endorsed." Do not treat that block as a failure of the Dean's action.

### Parent Module: Dean Dashboard

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 1 | Sign in as Dean. | The Dean dashboard opens with pending, approved, and rejected counts. | | | |
| 2 | Open the dashboard with pending requests in the Dean's college. | Pending requests are listed with proponent, project title, college, and date; "Review" opens the request. | | | |
| 3 | Open "See all requests" and filter by Pending, Approved, and Rejected. | Each filter shows only requests in that state. | | | |

### Parent Module: Endorsement Requests

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 4 | Open an endorsement request from another college by URL. | Access is refused; the request is not shown or decidable. | | | |
| 5 | Open a pending request. | Proponent, program, project title, call, funding program, college, description, and notes are displayed. | | | |

### Parent Module: Endorsement Review

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 6 | Approve a request with endorser name, designation, and date while no electronic signature is saved. | Approval is blocked with the message to save an electronic signature in Manage Profile; no letter is issued. | | | |
| 7 | Approve a request with a required field blank. | The missing field is identified and approval does not proceed. | | | |
| 8 | Approve a request with complete details and a saved signature. | The request becomes Approved; the official letter is generated with the signature and appears in the proponent's Certifications. | | | |
| 9 | View and download the issued letter. | The letter opens or downloads as a PDF with the correct reference and letter version. | | | |
| 10 | Reissue an approved letter. | A new letter version is generated; the previous version remains recorded. | | | |
| 11 | Reject a request without a reason. | A reason is required and the rejection does not proceed. | | | |
| 12 | Reject a request with a reason. | The request becomes Rejected and the proponent sees the reason. | | | |
| 13 | Open the audit history of a decided request. | Created, assigned, approved or rejected, letter generated, and certification events appear in order. | | | |

### Parent Module: Profile

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 14 | Save a drawn or uploaded PNG signature. | The signature is saved and used on subsequently issued letters. | | | |
| 15 | Upload a non-image file as the signature. | The file is rejected with the PNG/JPEG-only message and is not saved. | | | |

### Parent Module: Endorsement Review — Letter Template

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 16 | Approve a request whose funding program has no configured letter template. | Approval is blocked with "No official endorsement letter is configured for {funding program} yet, so it cannot be endorsed." | | | |

---

## USER ROLE: OVCRIGE

### Parent Module: OVCRIGE Dashboard

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 1 | Sign in as OVCRIGE. | The OVCRIGE dashboard opens with endorsed, under-REC-review, REC-approved, and for-revision counts. | | | |
| 2 | Open the notifications card. | Recent notifications are listed; selecting one opens the related proposal. | | | |

### Parent Module: Endorsed Proposals

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 3 | Open Endorsed Proposals (from RPS). | Proposals endorsed by RPS are listed with ID, title, proponent, date, status, and a Review action. | | | |
| 4 | Search by project title and filter by status. | The list narrows to matching records. | | | |

### Parent Module: Review Proposal

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 5 | Forward an "Endorsed to OVCRIGE" proposal to REC. | The status becomes "Under Review by REC" and REC receives the evaluation notification. | | | |
| 6 | Forward an "Approved by REC" proposal to the Office of the Chancellor. | The status becomes "For OC Approval" and the OC receives the notification. | | | |
| 7 | Return a proposal for revision without checking all checklist items. | The action is blocked until all four confirmation items are checked. | | | |
| 8 | Return a proposal for revision with comments and all four items checked. | The proposal becomes "Returned to RPS" and RPS receives the notification. | | | |
| 9 | Reject a proposal and confirm. | The proposal becomes Rejected and the proponent is notified. | | | |
| 10 | Add a field remark to a specific proposal field, then edit and remove it. | The remark is saved, shown with reviewer and date, updated, and removed successfully. | | | |
| 11 | Download the complete proposal document. | A document containing the proposal record is prepared and downloaded. | | | |
| 12 | Open the status timeline. | Submitted, RPS-endorsed, forwarded-to-REC, REC-approved, forwarded-to-Chancellor, and final-approval stages show their correct state. | | | |

### Parent Module: Quarterly Reports

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 13 | Open Quarterly Reports. | Approved projects with an issued Special Order are listed with report counts. | | | |
| 14 | Open a project's reports. | Submitted Q1–Q3 reports and the Terminal Report are listed. | | | |
| 15 | Open a quarterly report in the viewer. | The three-page report opens read-only; no submit, edit, or approve control is present. | | | |

---

## USER ROLE: REC

### Parent Module: REC Dashboard & Assigned Proposals

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 1 | Sign in as REC. | The REC dashboard opens with proposal statistics and the latest assigned proposals. | | | |
| 2 | Open Assigned Proposals. | Proposals under REC review and completed REC reviews are listed with title, proponent, date, and status. | | | |
| 3 | Search by title or proponent and filter by status. | The list narrows to matching records. | | | |

### Parent Module: Proposal Evaluation

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 4 | Open a proposal under review. | All proposal sections and attachments are shown read-only, with field-remark controls. | | | |
| 5 | Add and save a field remark. | The remark is persisted and appears with the reviewer role and date. | | | |
| 6 | Open the Review Form for an under-review proposal. | The 15 criteria in the four weighted groups and the overall comments box are shown. | | | |
| 7 | Enter scores of 6 or 0 in a criterion. | The score is not accepted; only 1–5 is selectable. | | | |
| 8 | Enter scores and overall comments, then approve. | A confirmation appears; on confirm the status becomes "Approved by REC", OVCRIGE is notified, and the user is returned to the dashboard. | | | |
| 9 | Approve without entering any score or comment. | **VERIFY:** the interface currently allows it; record whether approval succeeds and whether that matches policy. | | | |

### Parent Module: Reject Proposal

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 10 | Reject a proposal and confirm the warning. | The proposal becomes Rejected; the confirmation states the action is permanent. **VERIFY:** the typed reason is not sent to the server. | | | |

### Parent Module: Return for Revision

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 11 | Return a proposal without comments. | The action is blocked with the required-fields message. | | | |
| 12 | Return a proposal with comments and all four checklist items. | The proposal becomes "Returned to RPS" and RPS is notified with the remarks. | | | |

### Parent Module: Meeting Details

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 13 | Open Meeting Details for a reviewed proposal. | Project profile, cooperating agencies, sites, summary, methodology, outputs, and attachments are shown read-only. | | | |

---

## USER ROLE: Office of the Chancellor (OC)

> **Known limitation:** the "Approval Trail" on the final-approval page is a fixed placeholder, not a history read from the proposal record. Do not score it as a working audit trail; record what is displayed. There is no decline or reject action at OC; the only negative action is Return to OVCRIGE.

### Parent Module: OC Dashboard

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 1 | Sign in as OC. | The OC dashboard opens with pending final approval, approved and released, and sent-to-OVCAF counts. | | | |
| 2 | Open a proposal awaiting final approval from the dashboard. | The final approval page opens for that proposal. | | | |

### Parent Module: Final Approval

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 3 | Open a proposal for final approval. | Proposal details, field review, Chancellor's notes, approval trail, and download option are shown. | | | |
| 4 | Try to approve without selecting a budget classification. | Approval is disabled and the page states that one classification must be selected. | | | |
| 5 | Select "With Budget" and approve with an SO number. | The status becomes "For OVCAF Approval"; the SO number is saved and OVCAF is notified. | | | |
| 6 | Select "Without Budget" and approve with an SO number. | The status becomes "Implementation Approved"; the SO is recorded and the proposal is not sent to OVCAF. | | | |
| 7 | Approve without entering an SO number. | The SO number is required and approval does not proceed. | | | |
| 8 | Upload a Special Order document in an unsupported format or over 25 MB. | The file is rejected with the format or size message and is not attached. | | | |
| 9 | Save the SO form as a draft with only an SO number. | The draft is saved, the proposal status is unchanged, and no SO date is recorded. | | | |
| 10 | Return a proposal to OVCRIGE. | The proposal returns to the "Approved by REC" stage and OVCRIGE is notified. | | | |
| 11 | Download the proposal details. | The complete proposal document is prepared and downloaded. | | | |

---

## USER ROLE: OVCAF

> **Known display quirks to record, not to mistake for evaluator error:** the dashboard applies a completed-OC-approval filter (SO number and issued timestamp) before counting, so "Endorsed to OC" normally reads 0; and "Approved / Released" also counts proposals still awaiting OVCAF. Report the observed values without judging them as correct.

### Parent Module: OVCAF Dashboard & Inbox

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 1 | Sign in as OVCAF. | The dashboard opens with for-review, endorsed-to-OC, and approved/released counts. | | | |
| 2 | Open the Proposal Inbox. | Proposals for financial validation are listed, and each value appears under its correct column: ID, title, leader, budget, date received, status. | | | |
| 3 | Search, filter by status and college, sort, and paginate. | Each control narrows or reorders the list correctly. | | | |

### Parent Module: Financial Validation

| No. | Test Scenario / Task | Expected Results | Pass / Fail | Tested By (Name & Signature) | Remarks |
|---|---|---|---|---|---|
| 4 | Open a proposal for validation and preview the line-item budget. | The uploaded budget opens in a preview without leaving the validation page. | | | |
| 5 | Try to approve after checking only some checklist items. | Approval remains unavailable until all six financial checklist items are checked. | | | |
| 6 | Check all six items, choose "Budget Approved", and submit. | The status becomes "Ready for Fund Release"; the checklist and remarks are saved and OC is notified. | | | |
| 7 | Choose "Returned for Revision" without remarks. | Remarks are required and the decision does not proceed. | | | |
| 8 | Return a proposal with remarks. | The proposal becomes "Returned for Revision" and the remarks are visible. | | | |
| 9 | Reject a proposal with remarks. | The proposal becomes Rejected and the decision is recorded with the remarks. | | | |
| 10 | Open a budget-approved proposal and record the fund release. | The status becomes "Implementation Approved"; repeating the action does not change it again. | | | |
| 11 | Open Financial Reports. | Validated budget totals, status counts, and the endorsement rate are shown. | | | |

---

# DELIVERABLE 3 — TESTING COVERAGE SUMMARY

## 3.1 Coverage Map

| Workflow or Module | Roles Covered | Scenarios |
|---|---|---|
| Authentication, registration, password change | Proponent, RPS Admin | P1–P6, A1–A4 |
| Account creation and approval | RPS Admin | A3–A9 |
| Dean provisioning and assignment | RPS Admin, Dean | A10–A12, D1–D3 |
| Call for proposals and announcements | RPS Admin, Proponent | A15–A17, P11–P13 |
| Proposal submission (27 sections) | Proponent | P12–P20 |
| Dean endorsement and letter issuance | Proponent, Dean | P21–P25, D4–D16 |
| RPS review and endorsement | RPS Staff | S3–S10 |
| Revision loop | Proponent, RPS Staff, OVCRIGE, REC | P26–P28, S6–S10, O7–O8, R11–R12 |
| REC evaluation | REC | R4–R9 |
| OC final approval and Special Order | OC | C3–C11 |
| OVCAF budget validation and release | OVCAF | V4–V10 |
| QPR and Terminal reporting | Proponent, RPS Staff, OVCRIGE | P31–P42, S14–S15, O13–O15 |
| Notifications and deep links | All roles | P43–P45, A18–A19, S18, O2, R1 |
| Role-based access control | All roles | P2, P9, P20, D4, S17, plus negative checks in each role |
| Document upload and download | Proponent, Dean, OVCRIGE, OC | P6, P19, D9–D10, D15, O10–O11, C8–C11 |
| Activity log | RPS Admin | A18–A19 |

## 3.2 Must-Not-Miss Scenarios

1. Full happy path: Proponent submit → RPS endorse → OVCRIGE forward → REC approve → OVCRIGE forward → OC issue SO → OVCAF approve → OVCAF release → Q1–Q3 → Terminal → Completed.
2. The "Without Budget" branch at OC must skip OVCAF and become Implementation Approved.
3. OVCAF approval must not be possible before the OC decision and the issued Special Order.
4. QPR must not be available before Implementation Approved plus an issued SO, and OVCAF approval where budget applies.
5. Terminal submission must be the action that changes the project to Completed.
6. RPS must be the only role that forwards a return to the Proponent with a deadline.
7. A Dean must be unable to see or decide another college's request.
8. A Proponent must not see another proponent's proposals or files.
9. A role mismatch at login must be refused.
10. Revision-deadline enforcement must block late submissions.

## 3.3 Untestable or Blocked Areas

| Item | Reason | Action |
|---|---|---|
| RPS Admin login | No role card on the login page | Report as a defect; the evaluator may record the observed failure; role scenarios remain valid if an account is provisioned and a workaround is agreed |
| Google sign-in | No button; server client-id placeholder | Exclude |
| Forgot password | Static page, no reset endpoint | Exclude; note as unavailable |
| Terminal Report GAD checklist | Not implemented | Do not test; correct the brief |
| REC meeting scheduling | No scheduling implementation | Do not test |
| RPS Budget Release / RPS Return4Rev | Local-storage mocks | Do not test as workflow actions |
| OVCRIGE reject reason | Not submitted | Test the status change only; record the missing reason |
| Submit Revision "Save Draft" | Does not persist | Report as a defect |
| Email delivery | Fallback logging only | Verify UI state, not inbox delivery |
| OC decline or reject | Not implemented; only return to OVCRIGE | Exclude |
| Intermediate status history | Not recorded outside OC transitions and completion | Do not assert a dated audit trail |
| OVCAF legacy `/ovcaf-validate` | Routed without an id; unreachable | Exclude |
| Endorsement for non-IFP programs | No letter template implemented | Use IFP fixtures only; record the block as correct |
| QPR and Terminal review or approval | Not implemented at any role | Exclude; only view and download are testable |
| QPR deadlines and late reporting | No due dates or late flags | Exclude |
| Revision file replacement | Files are not uploaded | Test the text revision only; record the defect |
| External dependencies | Google OAuth and mail server | Require test-environment configuration |

---

# DELIVERABLE 4 — RECOMMENDED EVALUATION SETUP

## 4.1 Evaluator Profile per Role

| Role | Evaluator Profile |
|---|---|
| Proponent | Faculty researcher who has prepared a proposal |
| RPS Admin | RPS administrative staff familiar with account and call management |
| RPS Staff | RPS technical staff who screen proposals |
| Dean | College Dean or officer familiar with endorsement letters |
| OVCRIGE | OVCRIGE staff involved in research coordination |
| REC | REC member who evaluates proposals |
| OC | Office of the Chancellor staff |
| OVCAF | Finance or OVCAF staff who validate budgets |

## 4.2 Test Accounts

Required dedicated accounts, never production accounts:

| Role | Account Needed |
|---|---|
| Proponent | 2 accounts (P1 and P2) to prove isolation |
| RPS Admin | 1, provisioned directly; the login-card issue applies |
| RPS Staff | 1 |
| Dean | 1 per college under test, created by RPS Admin |
| OVCRIGE, REC, OC, OVCAF | 1 each |
| Pending account | 1 for approval and rejection tests |

Seed accounts exist only for the `rpis_test` database and must never be used against production data.

## 4.3 Sample Fixtures

| Fixture | Purpose |
|---|---|
| Open call for proposals | Submission tests |
| Scheduled call | Disabled Apply test |
| Closed call | Submission-blocked test |
| Approved Dean endorsement and certification | Proposal submission prerequisite |
| Proposal in `FOR REVIEW` | RPS review |
| Proposal in `ENDORSED` | OVCRIGE forward-to-REC |
| Proposal in `UNDER_REVIEW` | REC evaluation |
| Proposal in `REC_APPROVED` | OVCRIGE forward-to-OC |
| Proposal in `FOR_OC_APPROVAL` | OC approval |
| Proposal in `RETURNED_TO_RPS` with deadline | RPS forward-revision |
| Proposal in `RPS_RETURNED` past deadline | Late-revision block |
| Proposal in `FOR_OVCAF_APPROVAL` with issued SO | OVCAF validation |
| Proposal in `READY_FOR_RELEASE` | Fund release |
| Proposal in `APPROVED` with SO and OVCAF approval | Q1 reporting |
| Proposal with Q1–Q3 submitted | Terminal Report |
| Proposal in `COMPLETED` | Read-only completed state |

## 4.4 How to Test Without Damaging Existing Records

1. Use a restored copy of the database on an isolated environment. The application's seed endpoint itself refuses non-test databases.
2. Create dedicated test accounts and test proposals. Never reuse real project records.
3. Where a status fixture is unavailable, reach the status on a test record through the legitimate workflow, not by direct SQL.
4. Do not run scenarios against the live database. Do not modify schema, application settings, or production records.
5. Record the proposal or document ID for every scenario so results are traceable.
6. Restore the environment from the snapshot between evaluator groups.

---

# OVERALL COMMENTS / SUGGESTIONS

**General observations of the evaluator:**

______________________________________________________________________________

______________________________________________________________________________

______________________________________________________________________________

______________________________________________________________________________

**Functionality issues found during testing:**

______________________________________________________________________________

______________________________________________________________________________

______________________________________________________________________________

**Suggestions for improvement:**

______________________________________________________________________________

______________________________________________________________________________

______________________________________________________________________________

---

# APPROVAL

| Party | Name | Signature | Date |
|---|---|---|---|
| Development Team | ____________________ | ____________________ | __________ |
| Development Team | ____________________ | ____________________ | __________ |
| Evaluator | ____________________ | ____________________ | __________ |
