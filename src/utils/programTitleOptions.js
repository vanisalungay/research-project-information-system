/**
 * programTitleOptions.js
 * ---------------------------------------------------------------------------
 * The choices of the Program Title field of the Create Proposal form.
 *
 * The field is optional: the proponent may leave it empty, pick one of their own
 * previously used titles, or create a new one. The three kinds of choice are built
 * in one place so the control that renders them (components/ProgramTitleSelect.vue)
 * and the form that owns the value agree on their order and meaning:
 *
 *   1. the placeholder - no selection at all (the field stays empty),
 *   2. the "create a new program title" action, flagged with `create` so the control
 *      can give it the application's blue and white,
 *   3. the proponent's own previously used titles, alphabetically as they arrive.
 *
 * `selected` marks the option that matches the current value, so the control can show
 * it on the closed field and highlight it in the list.
 */

/** Value of the "create a new program title" choice (never a real title). */
export const CREATE_NEW_PROGRAM_TITLE = '__CREATE_NEW_PROGRAM_TITLE__'

/** Text of the empty option; it is also the placeholder of the closed field. */
export const PROGRAM_TITLE_PLACEHOLDER = 'Select a program title or create a new program title'

/** Label of the create action (unchanged from the previous dropdown). */
export const CREATE_NEW_PROGRAM_TITLE_LABEL = 'Create New Program Title'

/**
 * The options of the field, in the order they are listed.
 * `titles` are the proponent's own reusable titles; `selected` is the current value.
 */
export function programTitleOptions({
  titles = [],
  selected = '',
  placeholder = PROGRAM_TITLE_PLACEHOLDER,
  createLabel = CREATE_NEW_PROGRAM_TITLE_LABEL,
} = {}) {
  const value = typeof selected === 'string' ? selected : ''
  return [
    { value: '', label: placeholder, selected: value === '' },
    {
      value: CREATE_NEW_PROGRAM_TITLE,
      label: createLabel,
      create: true,
      selected: value === CREATE_NEW_PROGRAM_TITLE,
    },
    ...(Array.isArray(titles) ? titles : []).map((title) => ({
      value: title,
      label: title,
      selected: value === title,
    })),
  ]
}

/**
 * What the closed field shows: the label of the selected option, or the placeholder
 * while nothing is selected (the field is optional, so an empty value is normal).
 */
export function programTitleLabel(options, placeholder = PROGRAM_TITLE_PLACEHOLDER) {
  return (Array.isArray(options) ? options : []).find((option) => option.selected)?.label || placeholder
}

/** True when the value means "the proponent is creating a new title". */
export function isCreatingProgramTitle(value) {
  return value === CREATE_NEW_PROGRAM_TITLE
}

/**
 * What a click on the field means: `'open'`, `'close'` or `'create'`.
 *
 * A single click opens the list (or closes it again). A double click - the second
 * click of a pair - asks for the "create a new program title" flow instead, so the
 * pair of clicks can never end up selecting an unrelated title.
 */
export function programTitleClickAction(clickCount, isOpen) {
  if (Number(clickCount) >= 2) return 'create'
  return isOpen ? 'close' : 'open'
}
