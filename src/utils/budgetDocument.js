/**
 * budgetDocument.js
 * ---------------------------------------------------------------------------
 * The Line-Item Budget of a proposal is an uploaded PDF/DOCX document (section 25 of the
 * official form), not a set of amounts the system can recompute. The proposal stores its
 * name in the legacy `beneficiaries_filename` column, exposed as `beneficiariesFileName`
 * by the backend; the Proponent form also carries it as `budgetFileName`. The stored path
 * is exposed as `beneficiariesFilePath` (form field: `budget_file_path`).
 *
 * Document generators must therefore print the attached document's name (or say that no
 * document is attached) and must never rebuild the old budget table from `budget` rows -
 * those rows are empty for every proposal created since the upload design and printing
 * them would fabricate a blank/zero budget.
 *
 * A draft saved by an older build could keep the stored path while losing the display
 * name (the payload wrote an empty name after the upload had already stored the file).
 * The document is still attached and downloadable in that case, so the generator derives
 * a readable reference from the stored path instead of reporting that nothing was
 * uploaded.
 */

/** The stored path of the attached budget document, or '' when none is stored. */
export function budgetDocumentPath(proposal) {
  const p = proposal || {}
  return String(
    p.budgetFilePath || p.beneficiariesFilePath || p.budget_file_path || ''
  ).trim()
}

/**
 * A readable document name derived from a stored path. Storage prefixes generated
 * uniqueness (`<uuid>_<name>.<ext>`), so the prefix is removed; the remaining base name
 * is the closest reference the record still carries to the uploaded document. A value
 * that is only a directory, or is blank, yields ''.
 *
 * @param {string} [path] stored path, e.g. "budget-files/<uuid>_line-item-budget.docx"
 * @returns {string}
 */
export function budgetDocumentNameFromPath(path) {
  const cleaned = String(path || '').trim().replace(/\\/g, '/')
  if (!cleaned) return ''
  const base = cleaned.slice(cleaned.lastIndexOf('/') + 1).trim()
  if (!base) return ''
  const withoutGeneratedPrefix = base.replace(
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}[_-]?/i,
    ''
  )
  return withoutGeneratedPrefix || base
}

/**
 * The name of the budget document attached to a proposal, or '' when none is attached.
 * `budgetFileName` (the Proponent form's field) wins over the backend's
 * `beneficiariesFileName`; both may be present with the same value. When the name was
 * lost but a stored path survives, the name is derived from that path, because the
 * document itself is still attached and must not be reported as missing.
 *
 * @param {object} [proposal]
 * @returns {string}
 */
export function budgetDocumentName(proposal) {
  const p = proposal || {}
  const explicit = String(
    p.budgetFileName || p.beneficiariesFileName || p.budget_file_name || ''
  ).trim()
  if (explicit) return explicit
  return budgetDocumentNameFromPath(budgetDocumentPath(p))
}
