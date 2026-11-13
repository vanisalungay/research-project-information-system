/**
 * statusTone.js
 * ---------------------------------------------------------------------------
 * The one place a workflow status is translated into a colour tone.
 *
 * The approved colour system itself lives in the centralized status catalog
 * (utils/statusCatalog.js): `STATUS_PALETTE` holds the hex colours,
 * `STATUS_COLOR_VARIANTS` maps every stored status to its semantic variant and
 * `STATUS_VARIANT_CLASSES` names the class the stylesheet paints. This module
 * only exposes that mapping under the name every role and module already uses,
 * so the same saved status can never be green on one page and amber on another.
 *
 * The actual colours live in the design tokens in assets/css/base.css; the tone
 * classes are styled by assets/status.css.
 *
 * Verifiable without a browser - see statusTone.test.js.
 */
import {
  STATUS_PALETTE,
  STATUS_VARIANT_CLASSES,
  statusColor,
  statusVariant,
  statusVariantClass,
} from './statusCatalog.js'

export { STATUS_PALETTE, statusColor, statusVariant }

/** Every tone class the shared stylesheet styles, one per semantic variant. */
export const STATUS_TONES = Object.freeze(Object.values(STATUS_VARIANT_CLASSES))

/** The saved status as the uppercase text the rules compare against ('' when there is none). */
export function normalizeStatus(status) {
  return String(status ?? '')
    .trim()
    .toUpperCase()
}

/**
 * The tone class of a saved status. Never returns '' - an empty, inactive or
 * unknown value is gray, so a badge always has a defined look and an
 * unrecognised value can never appear approved or completed.
 */
export function statusTone(status) {
  return statusVariantClass(status)
}
