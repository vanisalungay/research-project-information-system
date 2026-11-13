/**
 * scripts/build-backend-location-catalog.mjs
 * ---------------------------------------------------------------------------
 * Builds the server-side copy of the Philippine administrative hierarchy that
 * the backend uses to validate the structured address of a proposal
 * (com.rpis.backend.service.PhilippineAddressCatalog).
 *
 * Why a second copy
 *   The browser reads the hierarchy from public/locations/ph (served by Vite),
 *   which is not part of the Spring Boot jar. The server needs the same
 *   relationships to reject an address whose parts do not belong together (a
 *   province of another region, a city of another province, a district of
 *   another area), so the extract is written a second time, trimmed to the
 *   fields that check needs:
 *
 *   backend/src/main/resources/locations/ph-address-catalog.json
 *     country    "Philippines"
 *     countries  ISO 3166-1 list, taken from src/utils/countries.js (the same
 *                list the Country dropdown is built from)
 *     regions    [{ code, name, regionName }]
 *     provinces  [{ code, name, regionCode }]
 *     districts  [{ code, name, regionCode }]            (NCR only)
 *     cities     [{ code, name, regionCode, provinceCode, districtCode }]
 *
 *   Barangays are not included: the 42,046 records are only ever needed to fill
 *   one dropdown, and the client already loads them per region. The server
 *   therefore validates the parent chain down to the municipality / city and
 *   only requires the barangay to be present.
 *
 * Source of truth: the bundled dataset written by scripts/fetch-psgc-data.mjs,
 * so run this script after refreshing that one:
 *
 *     node scripts/fetch-psgc-data.mjs
 *     node scripts/build-backend-location-catalog.mjs
 */
import { mkdir, readFile, writeFile } from 'node:fs/promises'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..')
const PH_DIR = join(ROOT, 'public', 'locations', 'ph')
const OUT = join(
  ROOT,
  'backend',
  'src',
  'main',
  'resources',
  'locations',
  'ph-address-catalog.json',
)

/** Reads one of the bundled datasets written by fetch-psgc-data.mjs. */
const read = async (name) => JSON.parse(await readFile(join(PH_DIR, name), 'utf8'))

const main = async () => {
  const [meta, regions, provinces, districts, cities] = await Promise.all([
    read('meta.json'),
    read('regions.json'),
    read('provinces.json'),
    read('districts.json'),
    read('cities-municipalities.json'),
  ])
  // The country list is shared with the form, never duplicated by hand.
  const { COUNTRIES } = await import(new URL('../src/utils/countries.js', import.meta.url).href)

  // Integrity checks: the server validates against this file, so a broken
  // relationship in the bundle must fail the build instead of shipping.
  const regionCodes = new Set(regions.map((region) => region.code))
  const provinceCodes = new Set(provinces.map((province) => province.code))
  const districtCodes = new Set(districts.map((district) => district.code))
  for (const province of provinces) {
    if (!regionCodes.has(province.regionCode)) {
      throw new Error(`province ${province.name} has no region`)
    }
  }
  for (const district of districts) {
    if (!regionCodes.has(district.regionCode)) {
      throw new Error(`district ${district.name} has no region`)
    }
  }
  for (const city of cities) {
    if (!regionCodes.has(city.regionCode)) throw new Error(`city ${city.name} has no region`)
    if (city.provinceCode && !provinceCodes.has(city.provinceCode)) {
      throw new Error(`city ${city.name} has an unknown province`)
    }
    if (city.districtCode && !districtCodes.has(city.districtCode)) {
      throw new Error(`city ${city.name} has an unknown district`)
    }
  }

  const catalog = {
    source:
      meta[0]?.source ??
      'Philippine Standard Geographic Code (PSGC), Philippine Statistics Authority',
    sourceUrl: meta[0]?.sourceUrl ?? 'https://psgc.gitlab.io/api/',
    generatedAt: new Date().toISOString(),
    country: 'Philippines',
    countries: COUNTRIES.map((country) => ({ code: country.code, name: country.name })),
    regions: regions.map((region) => ({
      code: region.code,
      name: region.name,
      regionName: region.regionName,
    })),
    provinces,
    districts,
    cities: cities.map((city) => ({
      code: city.code,
      name: city.name,
      regionCode: city.regionCode,
      provinceCode: city.provinceCode ?? null,
      districtCode: city.districtCode ?? null,
    })),
  }

  await mkdir(dirname(OUT), { recursive: true })
  await writeFile(OUT, `${JSON.stringify(catalog)}\n`, 'utf8')
  const bytes = Buffer.byteLength(JSON.stringify(catalog))
  console.log(
    `Wrote ${OUT}\n  ${catalog.countries.length} countries, ${catalog.regions.length} regions, ` +
      `${catalog.provinces.length} provinces, ${catalog.districts.length} districts, ` +
      `${catalog.cities.length} cities/municipalities (${(bytes / 1024).toFixed(1)} KB)`,
  )
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
