<template>
  <td>
    <select
      class="site-control"
      :value="site.country"
      :aria-label="`Site ${siteNumber} country`"
      @change="cascade.select('country', $event.target.value)"
    >
      <option value="">Select Country</option>
      <!-- Sites of Implementation are Philippine-only. The existing country data
           still supplies the name, but no other country is offered. -->
      <option :value="PHILIPPINES.name">{{ PHILIPPINES.name }}</option>
    </select>
    <small v-if="legacyForeignCountry" class="site-country-note">
      Only the Philippines is accepted. Select it to update this site.
    </small>
  </td>

  <td>
    <select
      v-if="!cascade.manual"
      class="site-control"
      :value="site.region"
      :disabled="!countryChosen || !cascade.regionEnabled"
      :aria-label="`Site ${siteNumber} region`"
      @change="cascade.select('region', $event.target.value)"
    >
      <option value="">{{ regionPlaceholder }}</option>
      <option v-for="region in cascade.regionOptions" :key="region.code" :value="region.name">
        {{ regionLabel(region) }}
      </option>
    </select>
    <input
      v-else
      class="site-control"
      :value="site.region"
      placeholder="State / Region"
      :disabled="!countryChosen"
      :aria-label="`Site ${siteNumber} region`"
      @input="cascade.select('region', $event.target.value)"
    />
  </td>

  <td>
    <select
      v-if="!cascade.manual"
      class="site-control"
      :value="site.province"
      :disabled="!countryChosen || !cascade.provinceEnabled"
      :aria-label="`Site ${siteNumber} province`"
      @change="cascade.select('province', $event.target.value)"
    >
      <option value="">{{ provincePlaceholder }}</option>
      <option v-for="province in cascade.provinceOptions" :key="province.code" :value="province.name">
        {{ province.name }}
      </option>
    </select>
    <input
      v-else
      class="site-control"
      :value="site.province"
      placeholder="Province"
      :disabled="!countryChosen"
      :aria-label="`Site ${siteNumber} province`"
      @input="cascade.select('province', $event.target.value)"
    />
  </td>

  <td>
    <!-- District is typed manually, as requested. The suggestions come from the same
         PSGC extract: in a districted region (NCR) they are the districts the
         municipality list is filtered by; in the other regions the data has no
         district level, so any text is accepted. -->
    <input
      :id="districtInputId"
      class="site-control"
      :list="districtListId"
      :value="site.district"
      :disabled="!countryChosen || !site.region"
      :placeholder="districtPlaceholder"
      :aria-label="`Site ${siteNumber} district`"
      @input="cascade.select('district', $event.target.value)"
    />
    <datalist :id="districtListId">
      <option v-for="district in cascade.districtOptions" :key="district.code" :value="district.name" />
    </datalist>
  </td>

  <td>
    <select
      v-if="!cascade.manual"
      class="site-control"
      :value="site.municipality"
      :disabled="!countryChosen || !cascade.cityEnabled"
      :aria-label="`Site ${siteNumber} municipality or city`"
      @change="cascade.select('municipality', $event.target.value)"
    >
      <option value="">{{ municipalityPlaceholder }}</option>
      <option v-for="city in cascade.cityOptions" :key="city.code" :value="city.name">
        {{ cityLabel(city) }}
      </option>
    </select>
    <input
      v-else
      class="site-control"
      :value="site.municipality"
      placeholder="City / Municipality"
      :disabled="!countryChosen"
      :aria-label="`Site ${siteNumber} municipality or city`"
      @input="cascade.select('municipality', $event.target.value)"
    />
  </td>

  <td>
    <select
      v-if="!cascade.manual && !cascade.manualBarangay"
      class="site-control"
      :value="site.barangay"
      :disabled="!countryChosen || !cascade.barangayEnabled"
      :aria-label="`Site ${siteNumber} barangay`"
      @change="cascade.select('barangay', $event.target.value)"
    >
      <option value="">{{ barangayPlaceholder }}</option>
      <option v-for="barangay in cascade.barangayOptions" :key="barangay.code" :value="barangay.name">
        {{ barangay.name }}
      </option>
    </select>
    <input
      v-else
      class="site-control"
      :value="site.barangay"
      placeholder="Barangay"
      :disabled="!countryChosen"
      :aria-label="`Site ${siteNumber} barangay`"
      @input="cascade.select('barangay', $event.target.value)"
    />
  </td>
</template>

<script setup>
import { computed } from 'vue'

import { PHILIPPINES, cityLabel, regionLabel } from '@/utils/philippineLocations'
import { useAddressCascade } from '@/composables/useAddressCascade'

const props = defineProps({
  site: { type: Object, required: true },
  siteNumber: { type: Number, required: true },
})

const emit = defineEmits(['update:site'])

/**
 * The composable owns loading, option lists, parent clearing and the barangay
 * loading for this one row. The same PSGC files back every row (the loader
 * caches them), so adding a second site never downloads a second dataset.
 */
const model = computed({
  get: () => props.site,
  set: (value) => emit('update:site', value),
})
// District is optional in Sites of Implementation: it never gates the municipality
// and is never required. With `districtRequired: false` the city list is still
// narrowed by a district when one is chosen, but the whole region's cities are
// offered when it is left blank (including NCR, where the PSGC defines districts).
const cascade = useAddressCascade(model, { districtRequired: false })

// Sites of Implementation accept the Philippines only. A legacy site that stored a
// foreign country is shown as unselected (the select has no matching option) and is
// called out, so it must be corrected before the proposal can proceed.
const countryChosen = computed(() => props.site.country === PHILIPPINES.name)
const legacyForeignCountry = computed(
  () => Boolean(props.site.country) && props.site.country !== PHILIPPINES.name,
)

const regionPlaceholder = computed(() =>
  countryChosen.value ? cascade.placeholders.region : 'Select a country first',
)
const provincePlaceholder = computed(() => {
  if (!countryChosen.value) return 'Select a country first'
  if (!props.site.region) return 'Select a region first'
  return cascade.placeholders.province
})
const municipalityPlaceholder = computed(() => {
  if (!countryChosen.value) return 'Select a country first'
  if (!props.site.region) return 'Select a region first'
  return cascade.placeholders.municipality
})
const barangayPlaceholder = computed(() => {
  if (!props.site.municipality) return 'Select a municipality / city first'
  return cascade.placeholders.barangay
})
const districtPlaceholder = computed(() => {
  if (!countryChosen.value) return 'Select a country first'
  if (!props.site.region) return 'Select a region first'
  return cascade.districtOptions.length > 0
    ? 'Type or select a district (optional)'
    : 'Type district (optional)'
})

const districtInputId = computed(() => `site-district-${props.siteNumber}`)
const districtListId = computed(() => `site-district-options-${props.siteNumber}`)

/**
 * One row's completeness check. A level that the loaded hierarchy says does not
 * apply (province in NCR) is not treated as missing; everything that does apply is
 * required, except District, which is always optional.
 */
function validate() {
  const site = props.site
  if (site.country !== PHILIPPINES.name) return 'select the Philippines as the country.'
  if (!site.region) return cascade.manual ? 'enter the state / region.' : 'select a region.'

  if (cascade.manual) {
    if (!site.province) return 'enter the province / administrative area.'
    if (!site.municipality) return 'enter the city / municipality.'
    if (!site.barangay) return 'enter the barangay.'
    return ''
  }

  const independent = cascade.independentCity
  if (cascade.provinceEnabled && !independent && !site.province) return 'select a province.'

  // District is intentionally not checked: it is optional everywhere, including
  // regions where the location data defines districts.

  if (!site.municipality) return 'select a municipality / city.'
  if (!site.barangay) return 'select a barangay.'
  return ''
}

defineExpose({ validate })
</script>

<style scoped>
.site-control {
  width: 100%;
  min-width: 128px;
  box-sizing: border-box;
  padding: 8px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #ffffff;
  color: #1e293b;
  font-family: inherit;
  font-size: var(--ui-font-small);
}

.site-control:focus {
  outline: none;
  border-color: #4f46e5;
  box-shadow: 0 0 0 2px rgba(79, 70, 229, 0.08);
}

.site-control:disabled {
  background: #f8fafc;
  color: #94a3b8;
  cursor: not-allowed;
}

.site-country-note {
  display: block;
  margin-top: 4px;
  color: #b45309;
  font-size: var(--ui-font-caption);
  line-height: var(--ui-line-snug);
}
</style>
