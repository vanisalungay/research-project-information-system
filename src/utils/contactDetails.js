/**
 * contactDetails.js
 * ---------------------------------------------------------------------------
 * Contact rules of the Create Proposal form (section 01, "Contact *").
 *
 * The old form had a single free-text "Address / Contact" field. The contact is
 * now a method + value pair: the proponent picks Contact Number or Email Address
 * from the dropdown under the "Contact *" heading, so the input can be filtered
 * and validated for what it actually is before the proposal is submitted.
 */

/** Values persisted on the proposal (`contactType`). */
export const CONTACT_NUMBER = 'CONTACT_NUMBER'
export const EMAIL_ADDRESS = 'EMAIL_ADDRESS'

/** Contact types offered by the dropdown, in order. */
export const CONTACT_TYPES = [
  { value: CONTACT_NUMBER, label: 'Contact Number' },
  { value: EMAIL_ADDRESS, label: 'Email Address' },
]

/**
 * First option of the contact-method dropdown: the unselected placeholder. The
 * dropdown sits directly under the "Contact *" heading, so it carries no label
 * of its own and this text tells the proponent what the field is for.
 */
export const CONTACT_METHOD_PLACEHOLDER = 'Select Contact Method'

/**
 * Contact state for a freshly chosen method: the type is kept and the value is
 * always empty, so a value typed for the previous method (a phone number is not
 * an email address) can never be submitted as the new one.
 */
export const contactDetailsForType = (type) => ({ type: type || '', value: '' })

/** A blank contact state; also used when the proposal form is reset. */
export const emptyContactDetails = () => contactDetailsForType('')

/**
 * Name of a stored contact type ("Contact Number" / "Email Address"). The value
 * input has no visible label of its own, so this is used as its accessible name.
 */
export const contactTypeLabel = (type) =>
  CONTACT_TYPES.find((entry) => entry.value === type)?.label || ''

/** Input placeholder for the selected contact type. */
export const contactPlaceholder = (type) => {
  if (type === EMAIL_ADDRESS) return 'Enter email address'
  if (type === CONTACT_NUMBER) return 'Enter contact number'
  return ''
}

/**
 * Attributes for the contact input of the selected type. Both are plain text
 * inputs - a phone number keeps its leading "+" and leading zeroes and gets the
 * telephone keyboard on touch devices, an email gets the email keyboard - and
 * neither is a numeric input, which would strip them.
 */
export const contactInputAttributes = (type) => {
  if (type === EMAIL_ADDRESS) {
    return { type: 'email', inputmode: 'email', autocomplete: 'email' }
  }
  if (type === CONTACT_NUMBER) {
    return { type: 'text', inputmode: 'tel', autocomplete: 'tel' }
  }
  return { type: 'text' }
}

/** Everything a contact number may contain; a "+" is only valid as the prefix. */
const PHONE_ALLOWED = /^\+?[()\d\s.-]*$/

/** A general (provider-independent) email shape for the contact address. */
const EMAIL_PATTERN = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}$/

/** Keeps only characters that can appear in a contact number (letters are dropped). */
export const sanitizeContactNumber = (value) => String(value ?? '').replace(/[^+()\d\s.-]/g, '')

/** 7-15 digits, with an optional leading "+" and the usual separators. */
export function isValidContactNumber(value) {
  const trimmed = String(value ?? '').trim()
  if (!PHONE_ALLOWED.test(trimmed)) return false
  const digits = trimmed.replace(/\D/g, '')
  return digits.length >= 7 && digits.length <= 15
}

/** Email format check used before the proposal can be submitted. */
export const isValidEmail = (value) => EMAIL_PATTERN.test(String(value ?? '').trim())

/**
 * Required-field check for the Contact section. Nothing is required before a
 * contact type is chosen - the value input is not rendered until then.
 */
export function validateContact(contact = {}) {
  const value = String(contact.value ?? '').trim()
  if (!contact.type) return 'Please select a contact type.'
  if (!value) {
    return contact.type === EMAIL_ADDRESS
      ? 'Email address is required.'
      : 'Contact number is required.'
  }
  if (contact.type === EMAIL_ADDRESS) {
    return isValidEmail(value) ? '' : 'Please enter a valid email address (e.g. name@example.com).'
  }
  if (contact.type === CONTACT_NUMBER) {
    return isValidContactNumber(value)
      ? ''
      : 'Please enter a valid contact number (digits, spaces, dashes or parentheses, with an optional + country code).'
  }
  return 'Please select a contact type.'
}
