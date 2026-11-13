/**
 * institutionalEmail.test.js
 * ---------------------------------------------------------------------------
 * Verifies the institution's address rule for administrator-created accounts.
 *
 * The administrator never types an address, so the rule has to be right: the given names run
 * together, the surname follows one dot, nothing but a-z0-9 survives, and a clash is
 * resolved with a number rather than by reusing someone else's address.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  INSTITUTIONAL_EMAIL_DOMAIN,
  availableInstitutionalEmail,
  institutionalEmail,
  institutionalEmailLocalPart,
  normalizeEmailPart,
} from './institutionalEmail.js'

test('the given names run together and the surname follows one dot', () => {
  assert.equal(institutionalEmail('Xarzha Maurine Bagares'), 'xarzhamaurine.bagares@msunawaan.edu.ph')
  assert.equal(institutionalEmail('Jane Doe'), 'jane.doe@msunawaan.edu.ph')
  assert.equal(institutionalEmail('Ana Maria Clara Reyes'), 'anamariaclara.reyes@msunawaan.edu.ph')
  // A single name is used as it is.
  assert.equal(institutionalEmail('Madonna'), 'madonna@msunawaan.edu.ph')
})

test('the address never contains a space', () => {
  for (const name of ['Xarzha Maurine Bagares', '  Jane   Doe  ', 'Juan Dela Cruz']) {
    assert.ok(!institutionalEmail(name).includes(' '), name + ' must produce a space-free address')
  }
})

test('case, accents and punctuation are normalized away', () => {
  assert.equal(normalizeEmailPart('  Bañares '), 'banares')
  assert.equal(institutionalEmail('MARÍA Bañares'), 'maria.banares@msunawaan.edu.ph')
  assert.equal(institutionalEmail("René O'Brien"), 'rene.obrien@msunawaan.edu.ph')
  assert.equal(institutionalEmail('Ana-Maria Reyes'), 'anamaria.reyes@msunawaan.edu.ph')
})

test('a name with nothing usable yields no address', () => {
  assert.equal(institutionalEmail(''), '')
  assert.equal(institutionalEmail('   '), '')
  assert.equal(institutionalEmail('***'), '')
  assert.equal(institutionalEmail(null), '')
  assert.equal(institutionalEmailLocalPart(''), '')
})

test('a taken address is numbered before the domain', () => {
  const taken = ['xarzhamaurine.bagares@msunawaan.edu.ph']

  const first = availableInstitutionalEmail('Xarzha Maurine Bagares', taken)
  assert.equal(first.email, 'xarzhamaurine.bagares2@msunawaan.edu.ph')
  assert.equal(first.suffix, 2)
  assert.equal(first.adjusted, true)

  // And it keeps counting while the numbered ones are taken too.
  const next = availableInstitutionalEmail('Xarzha Maurine Bagares', [...taken, first.email])
  assert.equal(next.email, 'xarzhamaurine.bagares3@msunawaan.edu.ph')
})

test('a free address is used unchanged', () => {
  const result = availableInstitutionalEmail('Jane Doe', ['someone.else@msunawaan.edu.ph'])
  assert.equal(result.email, 'jane.doe@msunawaan.edu.ph')
  assert.equal(result.suffix, 1)
  assert.equal(result.adjusted, false)
  assert.ok(result.email.endsWith(INSTITUTIONAL_EMAIL_DOMAIN))
})