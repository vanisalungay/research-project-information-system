/**
 * statusLabel.js
 * ---------------------------------------------------------------------------
 * The generic, category-agnostic fallback formatter.
 *
 * Category labels live in statusCatalog.js (`PROPOSAL_STATUS_LABELS`,
 * `ACCOUNT_STATUS_LABELS`, ...). Every screen that knows which category it is
 * displaying must go through that category's helper instead of this module, so
 * a token such as `APPROVED` or `ACTIVE` cannot pick up another category's
 * wording.
 *
 * This formatter is only for a code that has no category entry (and for the
 * title-case translation used inside category helpers). It never changes the
 * stored value: comparisons, filters and API payloads keep using the raw code.
 *
 * Run the rules with: npm test
 */
import {
  STATUS_ACRONYMS,
  genericStatusLabel,
  normalizeStatusKey,
} from './statusCatalog.js'

export { STATUS_ACRONYMS, normalizeStatusKey }

/**
 * The display label of a status with no category-specific mapping. Known domain
 * statuses must use their category helper instead (see statusCatalog.js).
 * Empty, null and unusable values return ''.
 */
export function statusLabel(value) {
  return genericStatusLabel(value)
}
