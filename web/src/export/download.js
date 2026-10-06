/**
 * Browser downloads of exported files.
 */

/**
 * Suggests a file name for exporting a result.
 *
 * @param {{gene: {symbol: string}, retrievedAt: string}} result Search result.
 * @param {string} extension File extension, without the dot.
 * @returns {string} E.g. `BRCA1_clinvar_2026-10-06.csv` (date of retrieval, local time).
 */
export function exportFileName(result, extension) {
  const date = new Date(result.retrievedAt)
  const pad = (n) => String(n).padStart(2, '0')
  const day = `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
  return `${result.gene.symbol}_clinvar_${day}.${extension}`
}

/**
 * Makes the browser download a file.
 *
 * @param {Blob} blob File content.
 * @param {string} fileName Suggested file name.
 */
export function downloadBlob(blob, fileName) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.style.display = 'none'
  document.body.append(link)
  link.click()
  link.remove()
  // Revoked later: some browsers start reading the URL after click() returns.
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
