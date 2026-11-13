/**
 * institutionalEmail.js
 * ---------------------------------------------------------------------------
 * The institution's address rule for accounts created by an administrator:
 *
 *     firstname + middlename + "." + lastname + "@msunawaan.edu.ph"
 *
 * e.g. "Xarzha Maurine Bagares" -> xarzhamaurine.bagares@msunawaan.edu.ph
 *
 * The given names are run together (never a space), the surname follows a single dot, and
 * every part is lower-cased with accents and punctuation stripped so the result is always a
 * valid address. The administrator never types the address; it is derived from the name and
 * shown read-only, and the backend re-derives it authoritatively on create.
 *
 * Verifiable without a browser - see institutionalEmail.test.js.
 */

export const INSTITUTIONAL_EMAIL_DOMAIN = '@msunawaan.edu.ph'

/** Lower case, no diacritics, nothing but a-z and 0-9 left. */
export function normalizeEmailPart(value) {
  return String(value ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]/g, '')
}

/**
 * The local part of the address: the given names run together, a dot, then the surname.
 * One name is used as-is; two or more treat the last word as the surname. Returns '' when
 * the name carries nothing usable, so a caller can tell "no name yet" from a real address.
 */
export function institutionalEmailLocalPart(fullName) {
  const parts = String(fullName ?? '')
    .trim()
    .split(/\s+/)
    .map(normalizeEmailPart)
    .filter(Boolean)

  if (parts.length === 0) return ''
  if (parts.length === 1) return parts[0]

  const surname = parts[parts.length - 1]
  const given = parts.slice(0, -1).join('')
  return given + '.' + surname
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