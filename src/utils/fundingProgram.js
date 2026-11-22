/**
 * fundingProgram.js
 * ---------------------------------------------------------------------------
 * The one user-facing name of the RPIS funding program.
 *
 * All proposals are classified as an Internally Funded Proposal (IFP). The stored
 * value stays the stable key "IFP" for compatibility with the Call for Proposal
 * records and the existing endorsement-letter template; every screen shows the
 * complete name, never the bare abbreviation.
 */
export const FUNDING_PROGRAM_IFP = 'IFP'
export const FUNDING_PROGRAM_IFP_LABEL = 'Internally Funded Proposal'

/**
 * The display name of a funding program from its stored key and/or an optional
 * backend label. IFP - whether it arrives as the key or as a label such as
 * "Internally Funded Project Proposal (IFP)" - always reads as the complete name.
 * Another program keeps its own label (or key when no label is supplied).
 */
export function fundingProgramDisplayName(program, label = '') {
  const key = String(program ?? '').trim().toUpperCase()
  const text = String(label ?? '').trim()
  if (key === FUNDING_PROGRAM_IFP || /^internally funded/i.test(text)) {
    return FUNDING_PROGRAM_IFP_LABEL
  }
  if (!key && !text) return 'Unspecified funding program'
  return text || key
}

/** The display name from an object carrying `fundingProgram` / `fundingProgramLabel`. */
export function fundingProgramLabelOf(request) {
  return fundingProgramDisplayName(request?.fundingProgram, request?.fundingProgramLabel)
}
