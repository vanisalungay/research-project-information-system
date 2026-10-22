/**
 * philippineLocations.js
 * ---------------------------------------------------------------------------
 * Cascading address data for the Create Proposal form (section 01 - Project
 * Profile, "Address *").
 *
 * Data source
 *   The bundled extract of the Philippine Standard Geographic Code (PSGC) of
 *   the Philippine Statistics Authority that lives in public/locations/ph and
 *   is (re)generated with `node scripts/fetch-psgc-data.mjs`. The files ship
 *   with the app, so the dropdowns keep working without internet access and the
 *   proposal form never depends on a third-party service being reachable.
 *
 * Bundled shape
 *   regions.json                 [{ code, name, regionName }]
 *   provinces.json               [{ code, name, regionCode }]
 *   districts.json               [{ code, name, regionCode }]     (NCR only)
 *   cities-municipalities.json   [{ code, name, regionCode, provinceCode,
 *                                    districtCode, isCity, isMunicipality }]
 *   barangays/<regionCode>.json  [{ code, name, city }]
 *
 * The form stores the *names* of the selected units (not their PSGC codes) so
 * that the values persisted on the proposal - and the combined legacy
 * "Address / Contact" text derived from them - stay readable on every existing
 * screen (proposal details, reviewer views, exports). Codes are used only
 * internally, to filter the next level down.
 */
import { COUNTRIES, PHILIPPINES_COUNTRY_CODE } from './countries.js'

/** Base path of the bundled PSGC extract (Vite serves public/ from the root). */
const DATA_BASE = `${import.meta.env?.BASE_URL ?? '/'}locations/ph/`

/** Address levels of section 01, widest first. */
export const ADDRESS_LEVELS = [
  'country',
  'region',
  'province',
  'district',
  'municipality',
  'barangay',
]

/** Country entry of the Philippines - the only country with structured data. */
export const PHILIPPINES = COUNTRIES.find((country) => country.code === PHILIPPINES_COUNTRY_CODE)

/** True when the selected country name is the Philippines. */
export const isPhilippines = (countryName) => countryName === PHILIPPINES.name

/**
 * A fresh address state, also used when the proposal form is reset.
 *
 * The Philippines is preselected: it is the only country RPIS holds a structured
 * hierarchy for, so a new proposal opens with its Region dropdown already
 * available (the levels below start empty and are cleared again whenever a parent
 * changes).
 */
export const emptyAddressDetails = () => ({
  country: PHILIPPINES.name,
  region: '',
  province: '',
  district: '',
  municipality: '',
  barangay: '',
})

// ------------------------------------------------------------------ loading --

/** Session cache of the bundled files; a rejected entry is dropped so retry works. */
const cache = new Map()

const load = (path) => {
  if (!cache.has(path)) {
    const request = fetch(`${DATA_BASE}${path}`).then((response) => {
      if (!response.ok) throw new Error(`Location data request failed (HTTP ${response.status}).`)
      return response.json()
    })
    request.catch(() => cache.delete(path))
    cache.set(path, request)
  }
  return cache.get(path)
}

export const loadRegions = () => load('regions.json')
export const loadProvinces = () => load('provinces.json')
export const loadDistricts = () => load('districts.json')
export const loadCitiesMunicipalities = () => load('cities-municipalities.json')
export const loadBarangays = (regionCode) => load(`barangays/${regionCode}.json`)

// ---------------------------------------------------------------- selection --

/**
 * Clears every level below `level`. Used whenever a parent selection changes so
 * no stale child value survives (e.g. changing the region empties the province,
 * district, municipality and barangay).
 */
export function clearDependentLevels(details, level) {
  const next = { ...details }
  const index = ADDRESS_LEVELS.indexOf(level)
  if (index < 0) return next
  for (const key of ADDRESS_LEVELS.slice(index + 1)) next[key] = ''
  return next
}

/** Applies one level and clears everything below it. */
export function setAddressLevel(details, level, value) {
  const next = clearDependentLevels(details, level)
  if (ADDRESS_LEVELS.includes(level)) next[level] = value == null ? '' : String(value)
  return next
}

// ------------------------------------------------------------------ lookups --

export const findByName = (items, name) => (items || []).find((item) => item.name === name) || null

/** PSGC code of a unit selected by name ('' when it is not in the loaded list). */
export const codeOf = (items, name) => findByName(items, name)?.code || ''

// ------------------------------------------------------------------ filters --

export const provincesOfRegion = (provinces, regionCode) =>
  regionCode ? (provinces || []).filter((province) => province.regionCode === regionCode) : []

export const districtsOfRegion = (districts, regionCode) =>
  regionCode ? (districts || []).filter((district) => district.regionCode === regionCode) : []

export const citiesOfProvince = (cities, provinceCode) =>
  provinceCode ? (cities || []).filter((city) => city.provinceCode === provinceCode) : []

export const citiesOfDistrict = (cities, districtCode) =>
  districtCode ? (cities || []).filter((city) => city.districtCode === districtCode) : []

export const citiesOfRegion = (cities, regionCode) =>
  regionCode ? (cities || []).filter((city) => city.regionCode === regionCode) : []

export const barangaysOfCity = (barangays, cityCode) =>
  cityCode ? (barangays || []).filter((barangay) => barangay.city === cityCode) : []

/**
 * Cities / municipalities that belong to no province and no district: the
 * independent (highly urbanised) cities such as City of Isabela and City of
 * Cotabato, which the PSGC records directly under their region. Their address
 * has no province level, so they are offered even before a province is chosen.
 */
export const independentCitiesOfRegion = (cities, regionCode) =>
  regionCode
    ? (cities || []).filter(
        (city) => city.regionCode === regionCode && !city.provinceCode && !city.districtCode,
      )
    : []

/** True when a city/municipality selected by name belongs to no province and no district. */
export const isIndependentCity = (cities, cityName) => {
  const city = findByName(cities, cityName)
  return Boolean(city) && !city.provinceCode && !city.districtCode
}

/**
 * Municipalities / cities of the selected administrative area: the district
 * when the area is divided into districts (NCR, where cities hang off the
 * district), the province when the region has provinces, otherwise the region
 * itself (regions whose cities hang directly off the region).
 *
 * `districtRequired` must be true when the selected region has districts: then
 * nothing is offered until a district is chosen, because the district decides
 * which cities belong to the address.
 *
 * `provinceRequired` must be true when the selected region has provinces: the
 * city must then come from the chosen province, so nothing but the region's own
 * independent cities (which belong to no province) is offered before one is
 * chosen - a region-wide list would let a municipality of one province be
 * submitted under another.
 */
export function citiesForArea(
  cities,
  {
    provinceCode,
    districtCode,
    regionCode,
    districtRequired = false,
    provinceRequired = false,
  } = {},
) {
  if (districtRequired) return citiesOfDistrict(cities, districtCode)
  if (provinceRequired) {
    return provinceCode
      ? citiesOfProvince(cities, provinceCode)
      : independentCitiesOfRegion(cities, regionCode)
  }
  if (provinceCode) return citiesOfProvince(cities, provinceCode)
  if (districtCode) return citiesOfDistrict(cities, districtCode)
  return citiesOfRegion(cities, regionCode)
}

// ------------------------------------------------------------------- labels --

/** "Region VIII - Eastern Visayas" (falls back to the plain PSGC name). */
export function regionLabel(region) {
  if (!region) return ''
  const { name, regionName } = region
  return regionName && regionName !== name ? `${regionName} \u2013 ${name}` : name
}

/** "Naawan (Municipality)" / "City of Manila (City)". */
export function cityLabel(city) {
  if (!city) return ''
  if (city.isCity) return `${city.name} (City)`
  if (city.isMunicipality) return `${city.name} (Municipality)`
  return city.name
}

/** Alphabetical copy of a PSGC list, for the dropdowns. */
export const sortedByName = (items) =>
  [...(items || [])].sort((a, b) => a.name.localeCompare(b.name))

// --------------------------------------------------------------- formatting --

/**
 * Combined address text, narrowest first - the same order the old free-text
 * "Address / Contact" field asked for (Barangay, Municipality, District,
 * Province, Region).
 */
export function formatAddress(details = {}) {
  return [
    details.barangay,
    details.municipality,
    details.district,
    details.province,
    details.region,
    details.country,
  ]
    .map((part) => (part || '').trim())
    .filter(Boolean)
    .join(', ')
}

/**
 * The legacy combined "Address / Contact" value. Every existing screen
 * (proposal details, reviewer views, exports, print) reads the single `address`
 * column, so the structured address and the selected contact are written back
 * into it as well - the old data is never silently dropped.
 */
export function formatAddressContact(details, contact) {
  const address = formatAddress(details)
  const value = (contact?.value || '').trim()
  if (!address) return value
  return value ? `${address} / ${value}` : address
}

// -------------------------------------------------------------- validation --

/**
 * Required-field check for the Address section.
 *
 * `hasProvinces` / `hasDistricts` come from the loaded hierarchy: a region
 * without provinces (NCR) needs no province, and a district is only required
 * where the PSGC actually defines districts. `manual` marks the states in which
 * the dropdowns cannot be used (a country other than the Philippines, or the
 * bundled data being unavailable) - then the parts are typed in and the province
 * is required again. `independentCity` marks a selected municipality / city that
 * belongs to no province and no district (City of Isabela, City of Cotabato), so
 * neither level applies to that address.
 */
export function validateAddress(
  details = {},
  {
    hasProvinces = true,
    hasDistricts = false,
    manual = false,
    manualBarangay = false,
    independentCity = false,
  } = {},
) {
  if (!details.country) return 'Please select a country.'
  const philippines = isPhilippines(details.country)
  if (!details.region)
    return philippines ? 'Please select a region.' : 'Please enter the state / region.'
  if (manual) {
    if (!details.province) return 'Please enter the province / administrative area.'
    if (!details.municipality) return 'Please enter the city / municipality.'
    if (!details.barangay) return 'Please enter the barangay / district.'
    return ''
  }
  if (hasProvinces && !independentCity && !details.province) return 'Please select a province.'
  if (hasDistricts && !independentCity && !details.district) return 'Please select a district.'
  if (!details.municipality) return 'Please select a municipality / city.'
  if (!details.barangay)
    return manualBarangay ? 'Please enter the barangay.' : 'Please select a barangay.'
  return ''
}

// ------------------------------------------------------------ placeholders --

/** Labels of the six Address controls, in cascade order. */
export const ADDRESS_LEVEL_LABELS = {
  country: 'Country',
  region: 'Region',
  province: 'Province',
  district: 'District',
  municipality: 'Municipality / City',
  barangay: 'Barangay',
}

/**
 * First option of each Address dropdown. It doubles as the status line of the
 * level: "Select ..." while the level can be answered, and an explicit
 * explanation when it is still waiting for a parent, still loading, or simply
 * does not apply to the selected area (a region without provinces such as NCR,
 * an area without districts). No level is ever filled with invented records, and
 * a level that does not apply is never treated as a missing selection.
 *
 * `provinceRequired` is true when the selected region has provinces: the
 * municipality / city level then waits for one - unless the region has
 * independent cities of its own, which are offered without a province.
 */
export function addressLevelPlaceholders({
  country = '',
  region = '',
  province = '',
  municipality = '',
  loadingBase = false,
  loadingCities = false,
  loadingBarangays = false,
  citiesFailed = false,
  provinceRequired = false,
  regionCount = 0,
  provinceCount = 0,
  districtCount = 0,
  cityCount = 0,
  barangayCount = 0,
} = {}) {
  return {
    country: 'Select Country',
    region: !country
      ? 'Select a country first'
      : loadingBase
        ? 'Loading regions...'
        : regionCount
          ? 'Select Region'
          : 'No regions available',
    province: !region
      ? 'Select a region first'
      : provinceCount
        ? 'Select Province'
        : 'Not applicable - this region has no provinces',
    district: !region
      ? 'Select a region first'
      : districtCount
        ? 'Select District'
        : 'Not applicable - no districts in this area',
    municipality: !region
      ? 'Select a region first'
      : loadingCities
        ? 'Loading municipalities / cities...'
        : citiesFailed
          ? 'Municipalities / cities could not be loaded'
          : provinceRequired && !province && !cityCount
            ? 'Select a province first'
            : cityCount
              ? 'Select Municipality / City'
              : 'Select the area above first',
    barangay: !municipality
      ? 'Select a municipality / city first'
      : loadingBarangays
        ? 'Loading barangays...'
        : barangayCount
          ? 'Select Barangay'
          : 'No barangays found',
  }
}
