/**
 * budgetTemplate.js
 * ---------------------------------------------------------------------------
 * Official Line-Item Budget template of the Create Proposal form (section 25 -
 * "Line-Item Budget Requirement").
 *
 * The official template is the PDF supplied by RPS/ORPS and shipped with the
 * application in `public/templates`, so it is served as a static asset at
 * `LINE_ITEM_BUDGET_TEMPLATE_URL`. The proponent never has to download the file
 * to read it: the Create Proposal form shows that very file inside a modal
 * (components/LineItemBudgetTemplateModal.vue) through the browser's own PDF
 * viewer, and the modal still offers the Download action.
 *
 * The constants are kept here (and not inline in the form) so the instruction
 * text, the button label and the file that is actually shipped can be verified
 * without a browser - see budgetTemplate.test.js.
 *
 * Run the rules with: npm test
 */

/** Exact instruction shown above the "View Line-Item Budget Template" button. */
export const LINE_ITEM_BUDGET_INSTRUCTION =
  'Please use and follow the official Line-Item Budget Template provided above when preparing your proposed budget. Once completed, upload your filled-out budget file below.'

/** Label of the button that opens the template viewer modal. */
export const LINE_ITEM_BUDGET_TEMPLATE_BUTTON_LABEL = 'View Line-Item Budget Template'

/** Title of the template viewer modal. */
export const LINE_ITEM_BUDGET_TEMPLATE_TITLE = 'Line-Item Budget Template'

/**
 * Name of the official template file as it is shipped in `public/templates`
 * (a PDF - the browser's own viewer renders it inside the modal).
 */
export const LINE_ITEM_BUDGET_TEMPLATE_FILE_NAME = 'Project-Line-Item-Budget.pdf'

/** URL of the official template, served as a static asset from `public/templates`. */
export const LINE_ITEM_BUDGET_TEMPLATE_URL = `/templates/${LINE_ITEM_BUDGET_TEMPLATE_FILE_NAME}`
