/**
 * useAddressCascade.js
 * ---------------------------------------------------------------------------
 * Cascading state for the Create Proposal form's "Address *" section.
 *
 * Keeps the option lists, the loading / failure state and the parent-change
 * rules of the address controls (Region -> Province -> District -> Municipality /
 * City -> Barangay) in one place, so the form itself only binds the selects. The
 * data is the bundled PSGC extract (src/utils/philippineLocations.js, regenerated
 * by scripts/fetch-psgc-data.mjs) - nothing is fetched from a third party.
 *
 * The Address section is Philippine-only: there is no Country control, the country
 * is always "Philippines" (see `isPhilippineAddress`), and Region is therefore the
 * first dropdown the proponent answers.
 *
 * Rules implemented here
 *   - Region is usable as soon as the bundled hierarchy is available (the country
 *     is not a question any more), Province by the region (when the region has
 *     provinces), District by the region (when the PSGC defines districts for it),
 *     Municipality / City once its parent is answered and Barangay once a
 *     municipality or city is chosen;
 *   - Municipality / City always depends on the applicable parent: the district
 *     in a districted region (NCR), the province in a region that has provinces -
 *     only the region's own independent cities (which belong to no province) are
 *     offered before a province is chosen - and the region otherwise;
 *   - changing a parent clears every dependent value, so no stale child value
 *     can be submitted;
 *   - the barangays of a region are loaded on demand and cached, and the load is
 *     triggered as soon as the region code and the municipality are both known
 *     (also when a saved proposal is restored into the form);
 *   - for a legacy foreign address - and if the bundled data cannot be read at
 *     all - the same fields fall back to manual entry (with a clear notice and a
 *     Retry action) so the proposal can still be completed.
 *
 * `model` is a writable ref/computed holding the address details object.
 */
import { computed, reactive, ref, watch } from 'vue'

import {
  addressLevelPlaceholders,
  barangaysOfCity,
  citiesForArea,
  codeOf,
  districtsOfRegion,
  isIndependentCity,
  isPhilippineAddress,
  loadBarangays,
  loadCitiesMunicipalities,
  loadDistricts,
  loadProvinces,
  loadRegions,
  provincesOfRegion,
  setAddressLevel,
  sortedByName,
} from '@/utils/philippineLocations'

export function useAddressCascade(model) {
  const regions = ref([])
  const provinces = ref([])
  const districts = ref([])
  const cities = ref([])
  const barangays = ref([])

  /** Loading indicators, one per request that can still be running. */
  const loading = reactive({ base: false, cities: false, barangays: false })
  /** Failure messages ('' while everything is fine). */
  const failure = reactive({ base: '', cities: '', barangays: '' })
  /** Requests already issued; `retry()` clears one so a failed load can repeat. */
  const requested = reactive({ base: false, cities: false })
  /** Region whose barangays are currently loaded. */
  const barangayRegion = ref('')

  const details = computed(() => model.value || {})
  // The Address section is Philippine-only: an address with no stored country is a
  // Philippine one, so the dropdowns are used for it (and for every new proposal).
  const philippines = computed(() => isPhilippineAddress(details.value.country))

  // --------------------------------------------------------------- loading ---

  const run = async (key, task) => {
    loading[key] = true
    failure[key] = ''
    try {
      await task()
    } catch (error) {
      failure[key] = error?.message || 'Location data could not be loaded.'
    } finally {
      loading[key] = false
    }
  }

  const ensureBase = async () => {
    if (requested.base) return
    requested.base = true
    await run('base', async () => {
      const [regionList, provinceList, districtList] = await Promise.all([
        loadRegions(),
        loadProvinces(),
        loadDistricts(),
      ])
      regions.value = regionList
      provinces.value = provinceList
      districts.value = districtList
    })
  }

  const ensureCities = async () => {
    if (requested.cities) return
    requested.cities = true
    await run('cities', async () => {
      cities.value = await loadCitiesMunicipalities()
    })
  }

  const ensureBarangays = async (regionCode) => {
    if (!regionCode || barangayRegion.value === regionCode) return
    await run('barangays', async () => {
      barangays.value = await loadBarangays(regionCode)
      barangayRegion.value = regionCode
    })
  }

  /** Retries whatever failed, from the inline Retry action. */
  const retry = async () => {
    if (failure.base) {
      requested.base = false
      await ensureBase()
      return
    }
    if (failure.cities) {
      requested.cities = false
      await ensureCities()
      return
    }
    if (failure.barangays) {
      barangayRegion.value = ''
      await ensureBarangays(selectedRegionCode.value)
    }
  }

  // -------------------------------------------------------------- options ---

  const selectedRegionCode = computed(() => codeOf(regions.value, details.value.region))
  const selectedProvinceCode = computed(() => codeOf(provinceOptions.value, details.value.province))
  const selectedDistrictCode = computed(() => codeOf(districtOptions.value, details.value.district))
  const selectedCityCode = computed(() => codeOf(cityOptions.value, details.value.municipality))

  const regionOptions = computed(() => sortedByName(regions.value))
  const provinceOptions = computed(() =>
    sortedByName(provincesOfRegion(provinces.value, selectedRegionCode.value)),
  )
  const districtOptions = computed(() =>
    sortedByName(districtsOfRegion(districts.value, selectedRegionCode.value)),
  )
  const cityOptions = computed(() =>
    sortedByName(
      citiesForArea(cities.value, {
        provinceCode: selectedProvinceCode.value,
        districtCode: selectedDistrictCode.value,
        regionCode: selectedRegionCode.value,
        // Regions with districts (NCR) list cities only once a district is chosen.
        districtRequired: districtOptions.value.length > 0,
        // Regions with provinces list the cities of the chosen province only (plus
        // their own independent cities, which belong to no province).
        provinceRequired: provinceOptions.value.length > 0,
      }),
    ),
  )
  const barangayOptions = computed(() =>
    sortedByName(barangaysOfCity(barangays.value, selectedCityCode.value)),
  )

  /** True when the selected municipality / city belongs to no province and no district. */
  const independentCity = computed(() =>
    isIndependentCity(cities.value, details.value.municipality),
  )

  // ------------------------------------------------------------ behaviour ---

  /** The bundled hierarchy can be used for the country currently selected. */
  const dropdowns = computed(() => philippines.value && !failure.base)
  /** A stored foreign country: its address has to be typed in. */
  const manual = computed(() => Boolean(details.value.country) && !dropdowns.value)
  /** Only the barangay has to be typed in (its list could not be loaded). */
  const manualBarangay = computed(() => dropdowns.value && Boolean(failure.barangays))

  // Region is the first control of the section: it only waits for the bundled
  // hierarchy, never for a country (there is no country control any more).
  const regionEnabled = computed(() => dropdowns.value)
  const provinceEnabled = computed(() => regionEnabled.value && provinceOptions.value.length > 0)
  const districtEnabled = computed(() => regionEnabled.value && districtOptions.value.length > 0)
  const cityEnabled = computed(() => regionEnabled.value && cityOptions.value.length > 0)
  const barangayEnabled = computed(
    () =>
      regionEnabled.value &&
      Boolean(details.value.municipality) &&
      barangayOptions.value.length > 0,
  )

  /** First option of each dropdown; it also reports an empty or pending list. */
  const placeholders = computed(() =>
    addressLevelPlaceholders({
      region: details.value.region,
      province: details.value.province,
      municipality: details.value.municipality,
      loadingBase: loading.base,
      loadingCities: loading.cities,
      loadingBarangays: loading.barangays,
      citiesFailed: Boolean(failure.cities),
      provinceRequired: provinceOptions.value.length > 0,
      regionCount: regionOptions.value.length,
      provinceCount: provinceOptions.value.length,
      districtCount: districtOptions.value.length,
      cityCount: cityOptions.value.length,
      barangayCount: barangayOptions.value.length,
    }),
  )

  /** One-line notice under the Address fields ('' when there is nothing to say). */
  const notice = computed(() => {
    if (failure.base) {
      return 'The Philippine location data could not be loaded. Please type the address instead, or retry.'
    }
    if (details.value.country && !philippines.value) {
      return `No administrative location data is available for ${details.value.country} in RPIS. Please type the address parts instead.`
    }
    if (failure.cities) {
      return 'The municipalities / cities could not be loaded. Please retry.'
    }
    if (manualBarangay.value) {
      return 'The barangays could not be loaded. Please type the barangay instead, or retry.'
    }
    if (loading.barangays) return 'Loading barangays...'
    return ''
  })

  /** True while the notice can be acted on with `retry()`. */
  const retryable = computed(() => Boolean(failure.base || failure.cities || failure.barangays))

  // ------------------------------------------------------------- handlers ---

  /** Writes one level, clearing everything below it (no stale child values). */
  const select = (level, value) => {
    model.value = setAddressLevel(details.value, level, value)
  }

  /** Loads whatever the current selection needs (also restores a saved draft). */
  const sync = () => {
    if (!philippines.value) {
      // A foreign address is typed in: drop the Philippine options so they can
      // never be offered under another country.
      regions.value = []
      provinces.value = []
      districts.value = []
      cities.value = []
      barangays.value = []
      barangayRegion.value = ''
      return
    }
    ensureBase()
    if (details.value.region) ensureCities()
  }

  watch(details, sync, { immediate: true })

  /**
   * The barangays of a city live in the file of its region, so the list is loaded
   * only once both are known. Watching the resolved region code (and not only the
   * address itself) matters when a saved proposal is restored: the municipality is
   * already set while the region list is still being read, and the load has to
   * happen as soon as the region code becomes available.
   */
  watch(
    [selectedRegionCode, () => details.value.municipality],
    () => {
      if (dropdowns.value && selectedRegionCode.value && details.value.municipality) {
        ensureBarangays(selectedRegionCode.value)
      }
    },
    { immediate: true },
  )

  /**
   * Drops a selection that is no longer part of the loaded options (for example a
   * draft saved before the location data was refreshed). The value is cleared
   * together with its children, so an inconsistent address can never be
   * submitted. A level whose options have not been loaded yet is left untouched.
   */
  const pruneUnknownSelection = (level, options) => {
    const value = details.value[level]
    if (!value || !options.length) return
    if (options.some((option) => option.name === value)) return
    select(level, '')
  }

  watch([regionOptions, provinceOptions, districtOptions, cityOptions, barangayOptions], () => {
    pruneUnknownSelection('region', regionOptions.value)
    pruneUnknownSelection('province', provinceOptions.value)
    pruneUnknownSelection('district', districtOptions.value)
    pruneUnknownSelection('municipality', cityOptions.value)
    pruneUnknownSelection('barangay', barangayOptions.value)
  })

  // Load the small base files when the form opens, so the Region dropdown - the
  // first control of the section - is ready without a request of its own.
  ensureBase()

  /**
   * Returned as a `reactive` object on purpose. The form's template reads these
   * properties directly (`addressCascade.manual`, `addressCascade.regionOptions`,
   * `addressCascade.placeholders`, ...). A plain object of refs would hand the
   * template the *ref objects* instead of their values, so a check such as
   * `v-if="!addressCascade.manual"` would always be false and every level would
   * silently fall back to its free-text input instead of the dropdown. `reactive`
   * unwraps each property on read, so the template sees plain values while the
   * composable keeps its refs internally.
   */
  return reactive({
    philippines,
    loading,
    failure,
    dropdowns,
    manual,
    manualBarangay,
    regionEnabled,
    provinceEnabled,
    districtEnabled,
    cityEnabled,
    barangayEnabled,
    placeholders,
    notice,
    retryable,
    regionOptions,
    provinceOptions,
    districtOptions,
    cityOptions,
    barangayOptions,
    independentCity,
    select,
    retry,
  })
}
