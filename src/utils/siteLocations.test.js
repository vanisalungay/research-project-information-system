/**
 * siteLocations.test.js
 * ---------------------------------------------------------------------------
 * The one-row shape of a Site of Implementation. The dropdown data and cascade
 * are the section-01 ones; this only checks the defaults and the saved values.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import {
  emptySiteLocation,
  isEmptySiteLocation,
  siteLocationFromSaved,
  siteNeedsProvince,
} from './siteLocations.js'

test('a new site starts with the Philippines and no other selection', () => {
  assert.deepEqual(emptySiteLocation(), {
    country: 'Philippines',
    region: '',
    province: '',
    district: '',
    municipality: '',
    barangay: '',
  })
  assert.equal(isEmptySiteLocation(emptySiteLocation()), true)
})

test('a saved site keeps its country and every part', () => {
  assert.deepEqual(
    siteLocationFromSaved({
      country: 'Philippines',
      region: 'Northern Mindanao',
      province: 'Misamis Oriental',
      district: '',
      municipality: 'Naawan',
      barangay: 'San Miguel',
    }),
    {
      country: 'Philippines',
      region: 'Northern Mindanao',
      province: 'Misamis Oriental',
      district: '',
      municipality: 'Naawan',
      barangay: 'San Miguel',
    },
  )
})

test('a legacy site without a country becomes a Philippine one', () => {
  const site = siteLocationFromSaved({
    region: 'Northern Mindanao',
    municipality: 'Naawan',
  })
  assert.equal(site.country, 'Philippines')
  assert.equal(site.region, 'Northern Mindanao')
})

test('a legacy foreign country is preserved', () => {
  const site = siteLocationFromSaved({
    country: 'Japan',
    region: 'Kanto',
    municipality: 'Shinjuku',
  })
  assert.equal(site.country, 'Japan')
  assert.equal(site.region, 'Kanto')
})

test('province is required only when the city belongs to a province', () => {
  const cities = [
    { name: 'City of Caloocan', provinceCode: null, districtCode: '137500000' },
    { name: 'Akbar', provinceCode: '150700000', districtCode: null },
  ]
  assert.equal(
    siteNeedsProvince(cities, { municipality: 'City of Caloocan', district: '' }),
    false,
    'NCR cities have no province level, so a blank province is valid',
  )
  assert.equal(
    siteNeedsProvince(cities, { municipality: 'Akbar', province: 'Basilan' }),
    true,
  )
  assert.equal(siteNeedsProvince(cities, { municipality: 'Unknown' }), false)
  assert.equal(siteNeedsProvince([], { municipality: 'Akbar' }), false)
})

test('a duplicate municipality name requires a province if any match has one', () => {
  const cities = [
    { name: 'San Isidro', provinceCode: null },
    { name: 'San Isidro', provinceCode: '123' },
  ]
  assert.equal(siteNeedsProvince(cities, { municipality: 'San Isidro' }), true)
})

test('missing parts are empty strings, never null', () => {
  const site = siteLocationFromSaved({})
  for (const value of Object.values(site)) assert.equal(typeof value, 'string')
  assert.equal(site.country, 'Philippines')
})
