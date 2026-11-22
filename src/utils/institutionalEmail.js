/**
 * institutionalEmail.js
 * ---------------------------------------------------------------------------
 * The institution's address rule for accounts created by an administrator:
 *
 *     firstname + middlename + "." + lastname + "@msunaawan.edu.ph"
 *
 * e.g. "Xarzha Maurine Bagares" -> xarzhamaurine.bagares@msunaawan.edu.ph
 *
 * The given names are run together (never a space), the surname follows a single dot, and
 * every part is lower-cased with accents and punctuation stripped so the result is always a
 * valid address. The administrator never types the address; it is derived from the name and
 * shown read-only, and the backend re-derives it authoritatively on create.
 *
 * The name may be entered in the standard RPIS order ("Dela Cruz, Juan, M., Jr.") or as the
 * Dean form's plain "First Name Last Name" ("Juan Dela Cruz"); both resolve to the same
 * address, and an existing account's address is never re-derived.
 *
 * Verifiable without a browser - see institutionalEmail.test.js.
 */

export const INSTITUTIONAL_EMAIL_DOMAIN = '@msunaawan.edu.ph'

/**
 * The normalized form of an address the user typed or pasted into a sign-in field.
 *
 * Auto-capitalization, a trailing space or a non-breaking space copied from the account
 * table made a typed address fail against the same address pasted. The backend normalizes
 * the same way; doing it here too means the request carries one form for both inputs.
 * Only insignificant formatting is removed - no valid address character is changed.
 */
export function normalizeEmailInput(value) {
  return String(value ?? '')
    .replace(/[   ]/g, ' ')
    .replace(/[​-‍﻿⁠]/g, '')
    .trim()
    .toLowerCase()
}

/** Lower case, no diacritics, nothing but a-z and 0-9 left. */
export function normalizeEmailPart(value) {
  return String(value ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]/g, '')
}

/**
 * A name split into the given names and the surname, understanding both the standard
 * "Dela Cruz, Juan, M., Jr." order and the Dean form's plain "First Name Last Name" order.
 * The surname words are kept together (Dela Cruz is one surname).
 *
 * With a comma the first component is the surname and the second is the given names;
 * without one the surname is the last word plus any particles directly in front of it
 * ("Juan Dela Cruz" is Juan + Dela Cruz, matching the temporary-password rule). Punctuation
 * is normalized away later by {@link normalizeEmailPart}.
 */
/** Jr, Sr, II, III ... are suffixes, not middle names. */
const SUFFIX = /^(jr|sr|ii|iii|iv|v|vi|vii|viii|ix|x)\.?$/i

/**
 * Words that belong to the surname rather than to the given names. Mirrors
 * SURNAME_PARTICLES in InstitutionalCredentialsService, so the address and the temporary
 * password always agree on what the surname is.
 */
const SURNAME_PARTICLES = new Set([
  'de', 'del', 'dela', 'delas', 'dele', 'la', 'las', 'los', 'van', 'von',
  'der', 'den', 'san', 'santa', 'sta', 'mac',
])

export function personNameParts(fullName) {
  const text = String(fullName ?? '').replace(/\s+/g, ' ').trim()
  if (!text) return { given: [], surname: [] }

  if (text.includes(',')) {
    const [surnamePart = '', givenPart = '', middleOrSuffix = ''] = text.split(',')
    const surname = surnamePart.trim().split(/\s+/).filter(Boolean)
    const given = givenPart.trim().split(/\s+/).filter(Boolean)
    // The third component is the middle initial, or the suffix when the middle initial
    // was left out. Only a middle initial belongs in the address.
    const middle = middleOrSuffix.trim()
    if (middle && !SUFFIX.test(middle)) {
      given.push(...middle.split(/\s+/).filter(Boolean))
    }
    return { given, surname }
  }

  const words = text.split(' ').filter(Boolean)
  if (words.length === 1) return { given: [], surname: words }

  // The surname is the last word plus any particles directly before it, so a compound
  // surname is never split into a "middle name" and a one-word surname.
  let surnameFrom = words.length - 1
  while (surnameFrom > 0 && SURNAME_PARTICLES.has(words[surnameFrom - 1].toLowerCase())) {
    surnameFrom -= 1
  }
  return { given: words.slice(0, surnameFrom), surname: words.slice(surnameFrom) }
}

/**
 * The local part of the address: the given names run together, a dot, then the surname.
 * One name is used as-is; the standard "Last, First Middle, Suffix" order is understood,
 * and a legacy "First Middle Last" name keeps the last word as the surname. Returns ''
 * when the name carries nothing usable, so a caller can tell "no name yet" from a real
 * address.
 */
export function institutionalEmailLocalPart(fullName) {
  const { given, surname } = personNameParts(fullName)
  const givenPart = given.map(normalizeEmailPart).filter(Boolean).join('')
  const surnamePart = surname.map(normalizeEmailPart).filter(Boolean).join('')

  if (!givenPart && !surnamePart) return ''
  if (!givenPart) return surnamePart
  if (!surnamePart) return givenPart
  return givenPart + '.' + surnamePart
}

/** The full address, or '' when the name has nothing usable in it. */
export function institutionalEmail(fullName) {
  const local = institutionalEmailLocalPart(fullName)
  return local ? local + INSTITUTIONAL_EMAIL_DOMAIN : ''
}

/**
 * The first address that is free: the plain one, or the same local part with 2, 3, ... before
 * the domain when it is already registered. `taken` is the set of addresses already in use
 * (the backend owns the authoritative check; this keeps the field honest while typing).
 */
export function availableInstitutionalEmail(fullName, taken) {
  const local = institutionalEmailLocalPart(fullName)
  if (!local) return { email: '', suffix: 1, adjusted: false }

  const used =
    taken instanceof Set
      ? taken
      : new Set((Array.isArray(taken) ? taken : []).map((value) => String(value).toLowerCase()))

  let suffix = 1
  while (used.has((suffix === 1 ? local : local + suffix) + INSTITUTIONAL_EMAIL_DOMAIN)) {
    suffix += 1
  }

  return {
    email: (suffix === 1 ? local : local + suffix) + INSTITUTIONAL_EMAIL_DOMAIN,
    suffix,
    adjusted: suffix > 1,
  }
}