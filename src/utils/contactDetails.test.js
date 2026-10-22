/**
 * contactDetails.test.js
 * ---------------------------------------------------------------------------
 * Covers the Contact section rules of the Create Proposal form: the single
 * contact-method dropdown under the "Contact *" heading (placeholder first,
 * then Contact Number / Email Address), the empty value a newly chosen method
 * starts with, the phone-number filter/format check (alphabetic characters and
 * malformed numbers must be rejected, an optional international prefix must be
 * accepted) and the email format check, plus the required-field rules that run
 * before the proposal can be submitted.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  CONTACT_METHOD_PLACEHOLDER,
  CONTACT_NUMBER,
  CONTACT_TYPES,
  EMAIL_ADDRESS,
  contactDetailsForType,
  contactInputAttributes,
  contactPlaceholder,
  contactTypeLabel,
  emptyContactDetails,
  isValidContactNumber,
  isValidEmail,
  sanitizeContactNumber,
  validateContact,
} from './contactDetails.js'

test('the two contact types are offered in order', () => {
  assert.deepEqual(
    CONTACT_TYPES.map((entry) => entry.value),
    [CONTACT_NUMBER, EMAIL_ADDRESS],
  )
  assert.deepEqual(
    CONTACT_TYPES.map((entry) => entry.label),
    ['Contact Number', 'Email Address'],
  )
  assert.equal(contactTypeLabel(CONTACT_NUMBER), 'Contact Number')
  assert.equal(contactTypeLabel(EMAIL_ADDRESS), 'Email Address')
  assert.equal(contactTypeLabel(''), '')
})

test('the contact dropdown starts on its placeholder and offers only the two methods', () => {
  // The dropdown sits directly under the "Contact *" heading, so the placeholder
  // is what identifies the field before a method is chosen.
  assert.equal(CONTACT_METHOD_PLACEHOLDER, 'Select Contact Method')
  assert.deepEqual(
    [CONTACT_METHOD_PLACEHOLDER, ...CONTACT_TYPES.map((entry) => entry.label)],
    ['Select Contact Method', 'Contact Number', 'Email Address'],
  )
})

test('a blank contact has no type and no value', () => {
  assert.deepEqual(emptyContactDetails(), { type: '', value: '' })
})

test('choosing a contact method always starts with an empty value', () => {
  assert.deepEqual(contactDetailsForType(CONTACT_NUMBER), { type: CONTACT_NUMBER, value: '' })
  assert.deepEqual(contactDetailsForType(EMAIL_ADDRESS), { type: EMAIL_ADDRESS, value: '' })
  assert.deepEqual(contactDetailsForType(''), { type: '', value: '' })
  assert.deepEqual(contactDetailsForType(), { type: '', value: '' })
  assert.deepEqual(emptyContactDetails(), contactDetailsForType(''))
})

test('switching methods clears the value so nothing stale can be submitted', () => {
  // A filled-in Contact Number is valid ...
  assert.equal(
    validateContact({ ...contactDetailsForType(CONTACT_NUMBER), value: '0917 123 4567' }),
    '',
  )
  // ... and switching to Email Address drops the number, so the empty email is
  // rejected instead of the phone number being submitted as the address.
  const switchedToEmail = contactDetailsForType(EMAIL_ADDRESS)
  assert.deepEqual(switchedToEmail, { type: EMAIL_ADDRESS, value: '' })
  assert.match(validateContact(switchedToEmail), /email address is required/i)

  // The same holds the other way round.
  assert.equal(
    validateContact({ ...contactDetailsForType(EMAIL_ADDRESS), value: 'juan@example.com' }),
    '',
  )
  const switchedToNumber = contactDetailsForType(CONTACT_NUMBER)
  assert.deepEqual(switchedToNumber, { type: CONTACT_NUMBER, value: '' })
  assert.match(validateContact(switchedToNumber), /contact number is required/i)
})

test('the contact input is labelled and hinted per selected type', () => {
  assert.equal(contactPlaceholder(CONTACT_NUMBER), 'Enter contact number')
  assert.equal(contactPlaceholder(EMAIL_ADDRESS), 'Enter email address')
  assert.equal(contactPlaceholder(''), '')
})

test('the contact input is a text input tuned for the selected type', () => {
  // A phone number keeps its leading "+" and leading zeroes, so it must not be a
  // numeric input; it gets the telephone keyboard instead.
  assert.deepEqual(contactInputAttributes(CONTACT_NUMBER), {
    type: 'text',
    inputmode: 'tel',
    autocomplete: 'tel',
  })
  assert.deepEqual(contactInputAttributes(EMAIL_ADDRESS), {
    type: 'email',
    inputmode: 'email',
    autocomplete: 'email',
  })
  // No contact type chosen yet: nothing is rendered for a value.
  assert.deepEqual(contactInputAttributes(''), { type: 'text' })
})

test('letters are stripped from a contact number as it is typed', () => {
  assert.equal(sanitizeContactNumber('0917abc1234567'), '09171234567')
  assert.equal(sanitizeContactNumber('+63 (912) 345-6789'), '+63 (912) 345-6789')
  assert.equal(sanitizeContactNumber('abc'), '')
  assert.equal(sanitizeContactNumber(null), '')
})

test('valid contact numbers are accepted, malformed ones are not', () => {
  assert.equal(isValidContactNumber('09171234567'), true)
  assert.equal(isValidContactNumber('+63 912 345 6789'), true)
  assert.equal(isValidContactNumber('(088) 555-1234'), true)
  assert.equal(isValidContactNumber('088-555-12-34'), true)
  // Letters, too few or too many digits and a misplaced prefix are rejected.
  assert.equal(isValidContactNumber('0917ABC4567'), false)
  assert.equal(isValidContactNumber('12345'), false)
  assert.equal(isValidContactNumber('12345678901234567'), false)
  assert.equal(isValidContactNumber('63+9123456789'), false)
  assert.equal(isValidContactNumber(''), false)
})

test('valid email addresses are accepted, malformed ones are not', () => {
  assert.equal(isValidEmail('juan.delaacruz@msunaawan.edu.ph'), true)
  assert.equal(isValidEmail('juan+research@example.com'), true)
  assert.equal(isValidEmail('juan@example'), false)
  assert.equal(isValidEmail('juan.example.com'), false)
  assert.equal(isValidEmail('juan @example.com'), false)
  assert.equal(isValidEmail(''), false)
})

test('the contact type must be chosen before anything else', () => {
  assert.match(validateContact(emptyContactDetails()), /select a contact type/i)
  assert.match(validateContact({ type: CONTACT_NUMBER, value: '' }), /contact number is required/i)
  assert.match(validateContact({ type: EMAIL_ADDRESS, value: '   ' }), /email address is required/i)
})

test('the contact value is validated for the selected type only', () => {
  assert.equal(validateContact({ type: CONTACT_NUMBER, value: '0917 123 4567' }), '')
  assert.equal(validateContact({ type: EMAIL_ADDRESS, value: 'juan@example.com' }), '')
  // An email-shaped value is not a contact number, and vice versa.
  assert.match(
    validateContact({ type: CONTACT_NUMBER, value: 'juan@example.com' }),
    /valid contact number/i,
  )
  assert.match(
    validateContact({ type: EMAIL_ADDRESS, value: '09171234567' }),
    /valid email address/i,
  )
  assert.match(validateContact({ type: 'SOMETHING_ELSE', value: 'x' }), /select a contact type/i)
})
