/**
 * siteLocations.js
 * ---------------------------------------------------------------------------
 * The site location state used by section 03 (Sites of Implementation) of the
 * Create Proposal form.
 *
 * The dropdowns themselves reuse the same PSGC extract and the same
 * `useAddressCascade` composable as the section 01 Address, so there is one
 * location dataset and one set of parent/child rules. This module only shapes
 * one site row.
 *
 * The country is the Philippines by default: it is the only country with
 * structured data, and defaulting an old draft (or a new row) to it keeps the
 * site editable instead of showing six empty controls that can never cascade.
 * A site that already stored a foreign country keeps it, exactly like the
 * section 01 address keeps a legacy foreign country.
 */
import { PHILIPPINES } from './philippineLocations.js'

const SITE_PART_KEYS = ['region', 'province', 'district', 'municipality', 'barangay']

const text = (value) => (value == null ? '' : String(value))

/** A blank site, with the country the dropdown is seeded to. */
export function emptySiteLocation() {
  return {
    country: PHILIPPINES.name,
    region: '',
    province: '',
    district: '',
    municipality: '',
    barangay: '',
  }
}

/**
 * One site loaded from a saved proposal. The stored country wins; an empty one
 * becomes the Philippines, which is what the form stores for every new site and
 * what the backend treats an address without a country as.
 */
export function siteLocationFromSaved(saved = {}) {
  return {
    country: text(saved?.country).trim() || PHILIPPINES.name,
    region: text(saved?.region),
    province: text(saved?.province),
    district: text(saved?.district),
    municipality: text(saved?.municipality),
    barangay: text(saved?.barangay),
  }
}

/** True when every part except the country is empty. */
export function isEmptySiteLocation(site) {
  return SITE_PART_KEYS.every((key) => text(site?.[key]).trim() === '')
}

/**
 * Whether a site's chosen city still needs a province. Cities recorded directly
 * under a region (NCR, and independent cities) carry no `provinceCode`, so leaving
 * Province blank there is valid. District is optional and never part of this rule.
 *
 * The full city list is passed in so this stays a pure rule and does not load the
 * PSGC data itself; when the data is unavailable, nothing is reported here (step 1
 * and the backend still enforce the normal requirements).
 */
export function siteNeedsProvince(cities, site) {
  const municipality = text(site?.municipality).trim()
  if (!municipality) return false
  // `some`, not `find`: a municipality name can occur more than once. If any match
  // belongs to a province, the province is required.
  return (Array.isArray(cities) ? cities : []).some(
    (row) => row?.name === municipality && Boolean(row?.provinceCode),
  )
}
