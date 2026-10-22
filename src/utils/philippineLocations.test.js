/**
 * philippineLocations.test.js
 * ---------------------------------------------------------------------------
 * Covers the pure half of the cascading-address module used by the Create
 * Proposal form: parent changes clearing dependent selections, the PSGC
 * filtering rules (region -> province -> district -> city -> barangay), option
 * labels, the combined legacy "Address / Contact" text and the required-field
 * rules - including the NCR case, where a region has no provinces but does have
 * districts.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

import {
  ADDRESS_LEVELS,
  PHILIPPINES,
  addressLevelPlaceholders,
  barangaysOfCity,
  citiesForArea,
  citiesOfDistrict,
  citiesOfProvince,
  citiesOfRegion,
  cityLabel,
  clearDependentLevels,
  codeOf,
  districtsOfRegion,
  emptyAddressDetails,
  formatAddress,
  formatAddressContact,
  independentCitiesOfRegion,
  isIndependentCity,
  isPhilippines,
  loadProvinces,
  loadRegions,
  provincesOfRegion,
  regionLabel,
  setAddressLevel,
  sortedByName,
  validateAddress,
} from './philippineLocations.js'

const regions = [
  { code: '080000000', name: 'Eastern Visayas', regionName: 'Region VIII' },
  { code: '100000000', name: 'Northern Mindanao', regionName: 'Region X' },
  { code: '130000000', name: 'National Capital Region', regionName: 'National Capital Region' },
]
const provinces = [
  { code: '083700000', name: 'Leyte', regionCode: '080000000' },
  { code: '101300000', name: 'Misamis Oriental', regionCode: '100000000' },
]
const districts = [{ code: '133900000', name: 'First District', regionCode: '130000000' }]
const cities = [
  {
    code: '083740000',
    name: 'Ormoc City',
    regionCode: '080000000',
    provinceCode: '083700000',
    districtCode: null,
    isCity: true,
    isMunicipality: false,
  },
  {
    code: '101301000',
    name: 'Naawan',
    regionCode: '100000000',
    provinceCode: '101300000',
    districtCode: null,
    isCity: false,
    isMunicipality: true,
  },
  {
    code: '133900000',
    name: 'City of Manila',
    regionCode: '130000000',
    provinceCode: null,
    districtCode: '133900000',
    isCity: true,
    isMunicipality: false,
  },
]
const barangays = [
  { code: '083740001', name: 'San Miguel', city: '083740000' },
  { code: '101301001', name: 'Balintad', city: '101301000' },
  { code: '133901001', name: 'Barangay 1', city: '133900000' },
]

/**
 * Region IX has one city that belongs to no province and no district (City of
 * Isabela) next to the municipalities of its provinces - the case a region-wide
 * city list would get wrong. Codes, names and parent codes are the real PSGC
 * values of the bundled dataset.
 */
const citiesWithIndependent = [
  {
    code: '099701000',
    name: 'City of Isabela',
    regionCode: '090000000',
    provinceCode: null,
    districtCode: null,
    isCity: true,
    isMunicipality: false,
  },
  {
    code: '097202000',
    name: 'City of Dipolog',
    regionCode: '090000000',
    provinceCode: '097200000',
    districtCode: null,
    isCity: true,
    isMunicipality: false,
  },
  {
    code: '098305000',
    name: 'Ipil',
    regionCode: '090000000',
    provinceCode: '098300000',
    districtCode: null,
    isCity: false,
    isMunicipality: true,
  },
]

test('a new address starts on the Philippines with every level below empty', () => {
  const details = emptyAddressDetails()
  assert.deepEqual(Object.keys(details), ADDRESS_LEVELS)
  // The Philippines is the default for a new proposal, so its region dropdown is
  // available immediately; nothing below it is preselected.
  assert.equal(details.country, 'Philippines')
  assert.ok(
    ADDRESS_LEVELS.filter((level) => level !== 'country').every((level) => details[level] === ''),
  )
})

test('changing a parent clears every dependent selection', () => {
  const filled = {
    country: 'Philippines',
    region: 'Region X \u2013 Northern Mindanao',
    province: 'Misamis Oriental',
    district: 'First District',
    municipality: 'Naawan',
    barangay: 'Balintad',
  }

  // Changing the region must not leave a province, district, city or barangay
  // from the previous region behind.
  const afterRegion = setAddressLevel(filled, 'region', 'Region VIII \u2013 Eastern Visayas')
  assert.deepEqual(afterRegion, {
    country: 'Philippines',
    region: 'Region VIII \u2013 Eastern Visayas',
    province: '',
    district: '',
    municipality: '',
    barangay: '',
  })

  // Changing the province keeps the country and region, drops the rest.
  const afterProvince = setAddressLevel(filled, 'province', 'Leyte')
  assert.equal(afterProvince.region, 'Region X \u2013 Northern Mindanao')
  assert.equal(afterProvince.province, 'Leyte')
  assert.equal(afterProvince.municipality, '')

  // Changing the city only drops the barangay.
  const afterCity = setAddressLevel(filled, 'municipality', 'Ormoc City')
  assert.equal(afterCity.barangay, '')
  assert.equal(afterCity.province, 'Misamis Oriental')

  // Clearing a parent is the same operation.
  assert.equal(clearDependentLevels(filled, 'district').municipality, '')
  assert.equal(setAddressLevel(filled, 'country', '').region, '')
})

test('the Philippines is the only country with structured data', () => {
  assert.equal(PHILIPPINES.name, 'Philippines')
  assert.equal(isPhilippines('Philippines'), true)
  assert.equal(isPhilippines('Malaysia'), false)
  assert.equal(isPhilippines(''), false)
})

test('province and district options follow the selected region', () => {
  assert.deepEqual(
    provincesOfRegion(provinces, '080000000').map((p) => p.name),
    ['Leyte'],
  )
  assert.deepEqual(provincesOfRegion(provinces, '130000000'), [])
  assert.deepEqual(
    districtsOfRegion(districts, '130000000').map((d) => d.name),
    ['First District'],
  )
  assert.deepEqual(districtsOfRegion(districts, '080000000'), [])
  // No parent selected yet -> nothing to show.
  assert.deepEqual(provincesOfRegion(provinces, ''), [])
  assert.deepEqual(districtsOfRegion(districts, ''), [])
})

test('municipalities and cities come from the province, district or region', () => {
  assert.deepEqual(
    citiesOfProvince(cities, '101300000').map((c) => c.name),
    ['Naawan'],
  )
  assert.deepEqual(
    citiesOfDistrict(cities, '133900000').map((c) => c.name),
    ['City of Manila'],
  )
  assert.deepEqual(
    citiesOfRegion(cities, '080000000').map((c) => c.name),
    ['Ormoc City'],
  )

  // Province wins over district and region, district over region.
  assert.deepEqual(
    citiesForArea(cities, { provinceCode: '101300000', regionCode: '100000000' }).map(
      (c) => c.name,
    ),
    ['Naawan'],
  )
  assert.deepEqual(
    citiesForArea(cities, { districtCode: '133900000', regionCode: '130000000' }).map(
      (c) => c.name,
    ),
    ['City of Manila'],
  )
  assert.deepEqual(
    citiesForArea(cities, { regionCode: '130000000' }).map((c) => c.name),
    ['City of Manila'],
  )
  assert.deepEqual(citiesForArea(cities, {}), [])
  // A district-divided area offers nothing until the district is chosen.
  assert.deepEqual(citiesForArea(cities, { regionCode: '130000000', districtRequired: true }), [])
  assert.deepEqual(
    citiesForArea(cities, {
      districtCode: '133900000',
      regionCode: '130000000',
      districtRequired: true,
    }).map((c) => c.name),
    ['City of Manila'],
  )
})

test('a region with provinces offers no city of another province', () => {
  // Region IX has provinces: until one is chosen only its own independent city
  // (which belongs to no province) is offered - never the municipalities of all
  // of its provinces at once, which could be submitted under the wrong province.
  assert.deepEqual(
    citiesForArea(citiesWithIndependent, {
      regionCode: '090000000',
      provinceRequired: true,
    }).map((c) => c.name),
    ['City of Isabela'],
  )
  assert.deepEqual(
    citiesForArea(citiesWithIndependent, {
      regionCode: '090000000',
      provinceCode: '097200000',
      provinceRequired: true,
    }).map((c) => c.name),
    ['City of Dipolog'],
  )
  assert.deepEqual(
    citiesForArea(citiesWithIndependent, {
      regionCode: '090000000',
      provinceCode: '098300000',
      provinceRequired: true,
    }).map((c) => c.name),
    ['Ipil'],
  )

  // A region with provinces and without independent cities offers nothing yet.
  assert.deepEqual(citiesForArea(cities, { regionCode: '100000000', provinceRequired: true }), [])
  assert.deepEqual(
    citiesForArea(cities, {
      regionCode: '100000000',
      provinceCode: '101300000',
      provinceRequired: true,
    }).map((c) => c.name),
    ['Naawan'],
  )

  // Independent cities are only recognised for the region they belong to.
  assert.deepEqual(
    independentCitiesOfRegion(citiesWithIndependent, '090000000').map((c) => c.name),
    ['City of Isabela'],
  )
  assert.deepEqual(independentCitiesOfRegion(citiesWithIndependent, '080000000'), [])
  assert.deepEqual(independentCitiesOfRegion(citiesWithIndependent, ''), [])
})

test('an independent city is a complete address without a province', () => {
  assert.equal(isIndependentCity(citiesWithIndependent, 'City of Isabela'), true)
  assert.equal(isIndependentCity(citiesWithIndependent, 'City of Dipolog'), false)
  assert.equal(isIndependentCity(citiesWithIndependent, 'Nowhere'), false)
  assert.equal(isIndependentCity(citiesWithIndependent, ''), false)

  const address = {
    country: 'Philippines',
    region: 'Zamboanga Peninsula',
    province: '',
    district: '',
    municipality: 'City of Isabela',
    barangay: 'Balintad',
  }
  // The city belongs to no province, so the province level does not apply to it.
  assert.equal(validateAddress(address, { hasProvinces: true, independentCity: true }), '')
  // Without that knowledge the same address is missing its province.
  assert.match(validateAddress(address, { hasProvinces: true }), /province/i)
})

test('barangays come from the selected municipality or city only', () => {
  assert.deepEqual(
    barangaysOfCity(barangays, '101301000').map((b) => b.name),
    ['Balintad'],
  )
  assert.deepEqual(
    barangaysOfCity(barangays, '133900000').map((b) => b.name),
    ['Barangay 1'],
  )
  assert.deepEqual(barangaysOfCity(barangays, ''), [])
})
test('lookups resolve names to PSGC codes and sort options by name', () => {
  assert.equal(codeOf(regions, 'Eastern Visayas'), '080000000')
  assert.equal(codeOf(regions, 'Region VIII'), '')
  assert.equal(codeOf(regions, ''), '')
  assert.deepEqual(
    sortedByName(cities).map((c) => c.name),
    ['City of Manila', 'Naawan', 'Ormoc City'],
  )
})

test('option labels name the administrative unit', () => {
  assert.equal(regionLabel(regions[0]), 'Region VIII \u2013 Eastern Visayas')
  assert.equal(regionLabel(regions[2]), 'National Capital Region')
  assert.equal(regionLabel(null), '')
  assert.equal(cityLabel(cities[0]), 'Ormoc City (City)')
  assert.equal(cityLabel(cities[1]), 'Naawan (Municipality)')
  assert.equal(cityLabel(null), '')
})

test('the combined legacy address keeps the old field order', () => {
  const details = {
    country: 'Philippines',
    region: 'Region X \u2013 Northern Mindanao',
    province: 'Misamis Oriental',
    district: '',
    municipality: 'Naawan',
    barangay: 'Balintad',
  }
  assert.equal(
    formatAddress(details),
    'Balintad, Naawan, Misamis Oriental, Region X \u2013 Northern Mindanao, Philippines',
  )
  assert.equal(formatAddress({}), '')

  // The legacy "Address / Contact" value still carries both halves.
  assert.equal(
    formatAddressContact(details, { type: 'EMAIL_ADDRESS', value: 'a@b.ph' }),
    'Balintad, Naawan, Misamis Oriental, Region X \u2013 Northern Mindanao, Philippines / a@b.ph',
  )
  assert.equal(formatAddressContact({}, { value: '09171234567' }), '09171234567')
  assert.equal(formatAddressContact({ country: 'Philippines' }, {}), 'Philippines')
})

test('an incomplete address is rejected with a message for the missing level', () => {
  const complete = {
    country: 'Philippines',
    region: 'Region X \u2013 Northern Mindanao',
    province: 'Misamis Oriental',
    district: '',
    municipality: 'Naawan',
    barangay: 'Balintad',
  }
  assert.equal(validateAddress(complete, { hasProvinces: true }), '')
  assert.match(validateAddress({}, {}), /country/i)
  assert.match(validateAddress({ country: 'Philippines' }, { hasProvinces: true }), /region/i)
  assert.match(validateAddress({ ...complete, province: '' }, { hasProvinces: true }), /province/i)
  assert.match(
    validateAddress({ ...complete, municipality: '' }, { hasProvinces: true }),
    /municipality/i,
  )
  assert.match(validateAddress({ ...complete, barangay: '' }, { hasProvinces: true }), /barangay/i)
})

test('a region without provinces (NCR) needs a district instead', () => {
  const ncr = {
    country: 'Philippines',
    region: 'National Capital Region',
    province: '',
    district: 'First District',
    municipality: 'City of Manila',
    barangay: 'Barangay 1',
  }
  assert.equal(validateAddress(ncr, { hasProvinces: false, hasDistricts: true }), '')
  assert.match(
    validateAddress({ ...ncr, district: '' }, { hasProvinces: false, hasDistricts: true }),
    /district/i,
  )
  // A province-less region that also has no districts only needs the city.
  assert.equal(
    validateAddress({ ...ncr, district: '' }, { hasProvinces: false, hasDistricts: false }),
    '',
  )
})

test('a foreign address is typed in and never needs Philippine units', () => {
  const foreign = {
    country: 'Japan',
    region: 'Kanto',
    province: 'Tokyo',
    district: '',
    municipality: 'Shinjuku',
    barangay: 'Nishi-Shinjuku',
  }
  assert.equal(validateAddress(foreign, { manual: true }), '')
  assert.match(validateAddress({ ...foreign, region: '' }, { manual: true }), /state \/ region/i)
  assert.match(validateAddress({ ...foreign, barangay: '' }, { manual: true }), /barangay/i)
})

test('each dropdown reports whether it is waiting, loading, ready or not applicable', () => {
  // Nothing selected yet: only the country can be answered.
  const fresh = addressLevelPlaceholders({})
  assert.equal(fresh.country, 'Select Country')
  assert.equal(fresh.region, 'Select a country first')
  assert.equal(fresh.province, 'Select a region first')
  assert.equal(fresh.municipality, 'Select a region first')
  assert.equal(fresh.barangay, 'Select a municipality / city first')

  // A country is chosen and its regions are still being read.
  assert.equal(
    addressLevelPlaceholders({ country: 'Philippines', loadingBase: true }).region,
    'Loading regions...',
  )
  assert.equal(
    addressLevelPlaceholders({ country: 'Philippines', loadingBase: false }).region,
    'No regions available',
  )

  // Region chosen: the levels that do not apply say so instead of inventing data.
  const region = addressLevelPlaceholders({
    country: 'Philippines',
    region: 'Northern Mindanao',
    regionCount: 17,
    provinceCount: 5,
    districtCount: 0,
    cityCount: 0,
  })
  assert.equal(region.region, 'Select Region')
  assert.equal(region.province, 'Select Province')
  assert.equal(region.district, 'Not applicable - no districts in this area')
  assert.equal(region.municipality, 'Select the area above first')

  // A region without provinces (NCR) is answered through its districts.
  const ncr = addressLevelPlaceholders({
    country: 'Philippines',
    region: 'National Capital Region',
    regionCount: 17,
    provinceCount: 0,
    districtCount: 4,
  })
  assert.equal(ncr.province, 'Not applicable - this region has no provinces')
  assert.equal(ncr.district, 'Select District')

  // City chosen: the barangay level is loading, empty or ready.
  assert.equal(
    addressLevelPlaceholders({ municipality: 'Naawan', loadingBarangays: true }).barangay,
    'Loading barangays...',
  )
  assert.equal(addressLevelPlaceholders({ municipality: 'Naawan' }).barangay, 'No barangays found')
  assert.equal(
    addressLevelPlaceholders({ municipality: 'Naawan', barangayCount: 12 }).barangay,
    'Select Barangay',
  )
})

test('the municipality level reports its parent, its loading and its failure', () => {
  // A region with provinces: the city waits for the province.
  assert.equal(
    addressLevelPlaceholders({
      country: 'Philippines',
      region: 'Northern Mindanao',
      provinceRequired: true,
      regionCount: 17,
      provinceCount: 5,
    }).municipality,
    'Select a province first',
  )
  // ...unless the region has independent cities of its own, which are offered.
  assert.equal(
    addressLevelPlaceholders({
      country: 'Philippines',
      region: 'Zamboanga Peninsula',
      provinceRequired: true,
      cityCount: 1,
    }).municipality,
    'Select Municipality / City',
  )
  assert.equal(
    addressLevelPlaceholders({
      country: 'Philippines',
      region: 'Northern Mindanao',
      province: 'Misamis Oriental',
      provinceRequired: true,
      cityCount: 24,
    }).municipality,
    'Select Municipality / City',
  )
  // The 242 KB city file is still being read, and a failed read says so.
  assert.equal(
    addressLevelPlaceholders({
      country: 'Philippines',
      region: 'Northern Mindanao',
      loadingCities: true,
    }).municipality,
    'Loading municipalities / cities...',
  )
  assert.equal(
    addressLevelPlaceholders({
      country: 'Philippines',
      region: 'Northern Mindanao',
      citiesFailed: true,
    }).municipality,
    'Municipalities / cities could not be loaded',
  )
})

test('the bundled PSGC extract keeps its real administrative relationships', async () => {
  const read = async (name) =>
    JSON.parse(
      await readFile(new URL(`../../public/locations/ph/${name}`, import.meta.url), 'utf8'),
    )
  const [meta, regions, provinces, districts, cities] = await Promise.all([
    read('meta.json'),
    read('regions.json'),
    read('provinces.json'),
    read('districts.json'),
    read('cities-municipalities.json'),
  ])

  // The extract is the PSGC release; its counts must match what was fetched.
  const counts = meta[0].counts
  assert.equal(regions.length, counts.regions)
  assert.equal(provinces.length, counts.provinces)
  assert.equal(districts.length, counts.districts)
  assert.equal(cities.length, counts.citiesMunicipalities)
  assert.equal(regions.length, 17)
  assert.equal(districts.length, 4)

  // Every unit points at a parent that exists.
  const regionCodes = new Set(regions.map((region) => region.code))
  const provinceCodes = new Set(provinces.map((province) => province.code))
  const districtCodes = new Set(districts.map((district) => district.code))
  const cityCodes = new Set(cities.map((city) => city.code))
  assert.ok(provinces.every((province) => regionCodes.has(province.regionCode)))
  assert.ok(districts.every((district) => regionCodes.has(district.regionCode)))
  assert.ok(cities.every((city) => regionCodes.has(city.regionCode)))
  assert.ok(
    cities.every(
      (city) =>
        (!city.provinceCode || provinceCodes.has(city.provinceCode)) &&
        (!city.districtCode || districtCodes.has(city.districtCode)),
    ),
  )

  // Region X: provinces, no districts - a city waits for its province, and the
  // municipalities offered for one province are that province's own.
  const regionX = regions.find((region) => region.name === 'Northern Mindanao').code
  const regionXProvinces = provincesOfRegion(provinces, regionX)
  assert.equal(regionXProvinces.length, 5)
  assert.deepEqual(districtsOfRegion(districts, regionX), [])
  assert.deepEqual(citiesForArea(cities, { regionCode: regionX, provinceRequired: true }), [])
  const misamisOriental = codeOf(regionXProvinces, 'Misamis Oriental')
  const regionXCities = citiesForArea(cities, {
    regionCode: regionX,
    provinceCode: misamisOriental,
    provinceRequired: true,
  })
  assert.ok(regionXCities.some((city) => city.name === 'Naawan'))
  assert.ok(regionXCities.every((city) => city.provinceCode === misamisOriental))
  const regionXBarangays = await read('barangays/100000000.json')
  assert.ok(barangaysOfCity(regionXBarangays, codeOf(regionXCities, 'Naawan')).length > 0)

  // NCR: no provinces, four districts - the district decides which cities apply.
  const ncr = regions.find((region) => region.name === 'NCR').code
  assert.deepEqual(provincesOfRegion(provinces, ncr), [])
  const ncrDistricts = districtsOfRegion(districts, ncr)
  assert.equal(ncrDistricts.length, 4)
  assert.deepEqual(citiesForArea(cities, { regionCode: ncr, districtRequired: true }), [])
  assert.deepEqual(
    citiesForArea(cities, {
      regionCode: ncr,
      districtCode: codeOf(ncrDistricts, 'First District'),
      districtRequired: true,
    }).map((city) => city.name),
    ['City of Manila'],
  )

  // Region IX: City of Isabela belongs to no province and no district, so it is
  // the only city offered before a province is chosen.
  const regionIX = regions.find((region) => region.name === 'Zamboanga Peninsula').code
  assert.equal(provincesOfRegion(provinces, regionIX).length, 3)
  assert.deepEqual(
    citiesForArea(cities, { regionCode: regionIX, provinceRequired: true }).map(
      (city) => city.name,
    ),
    ['City of Isabela'],
  )
  assert.equal(isIndependentCity(cities, 'City of Isabela'), true)
  assert.equal(isIndependentCity(cities, 'Naawan'), false)
  assert.ok(cityCodes.has(codeOf(cities, 'City of Isabela')))
})

test('the bundled dataset is requested once and cached per session', async () => {
  const requested = []
  const originalFetch = globalThis.fetch
  globalThis.fetch = async (url) => {
    requested.push(url)
    return { ok: true, json: async () => [{ code: '1', name: 'Test Region' }] }
  }
  try {
    const first = await loadRegions()
    const second = await loadRegions()
    assert.deepEqual(first, [{ code: '1', name: 'Test Region' }])
    assert.equal(second, first)
    assert.deepEqual(requested, ['/locations/ph/regions.json'])
  } finally {
    globalThis.fetch = originalFetch
  }
})

test('a failed dataset request reports the problem and can be retried', async () => {
  const originalFetch = globalThis.fetch
  try {
    globalThis.fetch = async () => ({ ok: false, status: 404 })
    await assert.rejects(() => loadProvinces(), /HTTP 404/)

    // The rejected entry must not stay cached, so a retry can succeed.
    globalThis.fetch = async () => ({ ok: true, json: async () => [{ code: '2', name: 'Leyte' }] })
    assert.deepEqual(await loadProvinces(), [{ code: '2', name: 'Leyte' }])
  } finally {
    globalThis.fetch = originalFetch
  }
})
