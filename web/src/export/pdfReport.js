/**
 * PDF report of a search result, laid out like the one of the desktop application: gene card,
 * distribution of clinical significance and table of variants, with the author signature
 * (clickable website and e-mail), page numbers and the data provenance on every page.
 *
 * The report uses the standard PDF fonts, which every reader can display without embedding;
 * they cover Western European languages, so other characters are replaced by `?`.
 */

import { jsPDF } from 'jspdf'
import { autoTable } from 'jspdf-autotable'
import { BRAND } from '../brand.js'
import { countByPrimary, colorOf, primaryOf } from '../model/significance.js'

const MARGIN = 50
const FOOTER_HEIGHT = 46
const LINE_SPACING = 1.3
const BODY_SIZE = 9.5
const SMALL_SIZE = 7.5
const SWATCH_SIZE = 7

const TEXT = '#0f172a'
const MUTED = '#64748b'
const ACCENT = '#4f46e5'
const RULE = '#cbd5e1'
const HEADER_FILL = '#e2e8f0'
const STRIPE_FILL = '#f8fafc'
const BAR_TRACK = '#f1f5f9'

/** Typographic characters replaced by their closest Latin-1 equivalent. */
const REPLACEMENTS = {
  '\u2018': "'",
  '\u2019': "'",
  '\u201C': '"',
  '\u201D': '"',
  '\u2013': '-',
  '\u2014': '-',
  '\u2026': '...',
  '\u2022': '\u00B7',
  '\u03B1': 'alpha',
  '\u03B2': 'beta',
  '\u03B3': 'gamma',
  '\u03B4': 'delta',
  '\u03B5': 'epsilon',
  '\u03BA': 'kappa',
  '\u03BB': 'lambda',
  '\u03BC': '\u00B5',
}

/**
 * Makes text drawable with a standard font: special spaces and line breaks become plain
 * spaces, typographic punctuation is simplified and other characters outside Latin-1 become
 * `?`.
 *
 * @param {unknown} value Text to clean.
 * @returns {string} The cleaned text.
 */
export function pdfText(value) {
  if (value == null) return ''
  return Array.from(String(value), (char) => {
    if (/\s/.test(char)) return ' '
    if (Object.hasOwn(REPLACEMENTS, char)) return REPLACEMENTS[char]
    return char.codePointAt(0) <= 0xff ? char : '?'
  }).join('')
}

/**
 * Returns the usual paper size of a locale: US Letter in the United States and Canada, A4
 * elsewhere.
 *
 * @param {string} locale Locale tag such as `fr-CA`.
 * @returns {'letter'|'a4'} The paper size.
 */
export function paperFor(locale) {
  let region
  try {
    region = new Intl.Locale(locale).maximize().region
  } catch {
    region = undefined
  }
  return region === 'US' || region === 'CA' ? 'letter' : 'a4'
}

/**
 * Builds the PDF report.
 *
 * @param {object} result Search result.
 * @param {object[]} rows Variants to list, in display order (e.g. filtered and sorted).
 * @param {{translator: ReturnType<import('../i18n/translate.js').createTranslator>,
 *   appVersion: string, paper: 'letter'|'a4', generatedAt: Date}} options
 * @returns {jsPDF} The document, ready to be saved.
 */
export function buildPdfReport(result, rows, { translator, appVersion, paper, generatedAt }) {
  const doc = new jsPDF({ unit: 'pt', format: paper, compress: true })
  const report = new ReportWriter(doc, translator)

  doc.setProperties({
    title: `${result.gene.symbol} - ${translator.t('pdf.title')}`,
    author: translator.t('brand.title'),
    subject: translator.t('status.sources'),
    keywords: `${translator.t('brand.website')}, ${BRAND.email}`,
    creator: BRAND.application,
  })
  doc.setCreationDate(generatedAt)

  report.header(result)
  report.geneDetails(result)
  report.distribution(result)
  report.variants(result, rows)
  report.footers(appVersion, generatedAt)
  return doc
}

/** Draws the report sections on a jsPDF document, keeping track of the vertical position. */
class ReportWriter {
  /**
   * @param {jsPDF} doc Document receiving the pages.
   * @param {ReturnType<import('../i18n/translate.js').createTranslator>} translator Texts and
   *   formatters of the report language.
   */
  constructor(doc, translator) {
    this.doc = doc
    this.tr = translator
    this.t = translator.t
    this.pageWidth = doc.internal.pageSize.getWidth()
    this.pageHeight = doc.internal.pageSize.getHeight()
    this.width = this.pageWidth - 2 * MARGIN
    this.y = MARGIN
  }

  // -------------------------------------------------------------------------
  // Sections
  // -------------------------------------------------------------------------

  /** Renders the title block: application, gene symbol and name. */
  header(result) {
    const { gene } = result
    this.line(`${BRAND.application}  ·  ${this.t('pdf.title')}`, {
      size: SMALL_SIZE + 1,
      bold: true,
      color: ACCENT,
    })
    this.y += 4
    this.line(gene.symbol, { size: 22, bold: true, color: TEXT })
    if (gene.name) this.paragraph(gene.name, { size: 12, color: MUTED })
    this.y += 4
    this.rule()
    this.y += 12
  }

  /** Renders the gene details and its summary. */
  geneDetails(result) {
    const { gene } = result
    const labelWidth = 120
    this.detailRow(this.t('gene.type'), gene.typeOfGene, labelWidth)
    this.detailRow(this.t('gene.location'), gene.mapLocation, labelWidth)
    this.detailRow(this.t('gene.aliases'), gene.aliases.join(', '), labelWidth)
    this.detailRow(this.t('pdf.entrez'), String(gene.entrezGene ?? gene.id ?? ''), labelWidth)
    this.detailRow(this.t('pdf.retrieved'), this.tr.dateTime(result.retrievedAt), labelWidth)

    if (gene.summary?.trim()) {
      this.y += 6
      this.sectionTitle(this.t('gene.summary'))
      this.paragraph(gene.summary, { size: BODY_SIZE, color: TEXT })
    }
    this.y += 10
  }

  /** Renders the distribution of the loaded variants by clinical significance, as bars. */
  distribution(result) {
    const { variants } = result.clinvar
    this.sectionTitle(this.t('summary.title'))
    if (variants.length === 0) {
      this.paragraph(this.t('summary.empty'), { size: BODY_SIZE, color: MUTED })
      this.y += 10
      return
    }

    const size = variants.length
    const counts = countByPrimary(variants)
    const lineHeight = BODY_SIZE * LINE_SPACING + 4
    const nameWidth = 170
    const barWidth = this.width - nameWidth - 110

    for (const { id, color, count } of counts) {
      if (count === 0) continue
      this.ensureSpace(lineHeight)
      const baseline = this.y + BODY_SIZE
      this.fillRect(MARGIN, baseline - SWATCH_SIZE, SWATCH_SIZE, SWATCH_SIZE, color)
      this.text(this.t(`significance.${id}`), MARGIN + SWATCH_SIZE + 6, baseline, {
        size: BODY_SIZE,
        color: TEXT,
      })
      const barX = MARGIN + nameWidth
      this.fillRect(barX, baseline - BODY_SIZE + 1, barWidth, BODY_SIZE, BAR_TRACK)
      this.fillRect(
        barX,
        baseline - BODY_SIZE + 1,
        Math.max(1, (barWidth * count) / size),
        BODY_SIZE,
        color,
      )
      this.text(
        this.t('summary.legendCount', { count, percent: this.tr.percent(count / size) }),
        barX + barWidth + 8,
        baseline,
        { size: BODY_SIZE, color: MUTED },
      )
      this.y += lineHeight
    }

    const pathogenic = counts
      .filter(({ id }) => id === 'pathogenic' || id === 'likely-pathogenic')
      .reduce((sum, { count }) => sum + count, 0)
    this.y += 4
    this.paragraph(
      this.t('summary.pathogenicShare', {
        count: pathogenic,
        percent: this.tr.percent(pathogenic / size),
      }),
      { size: BODY_SIZE, bold: true, color: TEXT },
    )
    this.paragraph(this.t('summary.caption', { loaded: size, total: result.clinvar.total }), {
      size: SMALL_SIZE,
      color: MUTED,
    })
    this.y += 12
  }

  /** Renders the table of variants; its header is repeated on every page. */
  variants(result, rows) {
    const loaded = result.clinvar.variants.length
    this.sectionTitle(this.t('pdf.variants'))
    this.paragraph(
      this.t('filter.count', { shown: rows.length, loaded, total: result.clinvar.total }),
      { size: SMALL_SIZE, color: MUTED },
    )
    this.y += 6

    if (rows.length === 0) {
      const key = loaded === 0 ? 'table.empty' : 'filter.noMatch'
      this.paragraph(this.t(key), { size: BODY_SIZE, color: MUTED })
      return
    }

    const swatchPadding = SWATCH_SIZE + 8
    autoTable(this.doc, {
      startY: this.y,
      margin: { top: MARGIN, left: MARGIN, right: MARGIN, bottom: MARGIN + FOOTER_HEIGHT },
      head: [
        [
          this.t('table.variantId'),
          this.t('table.hgvs'),
          this.t('table.significance'),
          this.t('table.origin'),
        ].map(pdfText),
      ],
      body: rows.map((variant) =>
        [
          variant.variantId == null ? '-' : String(variant.variantId),
          variant.hgvs || '-',
          variant.significances.join(', ') || '-',
          variant.origins.join(', ') || '-',
        ].map(pdfText),
      ),
      showHead: 'everyPage',
      styles: {
        font: 'helvetica',
        fontSize: BODY_SIZE - 1,
        textColor: TEXT,
        cellPadding: 4,
        lineColor: RULE,
        lineWidth: { bottom: 0.5 },
        overflow: 'linebreak',
      },
      headStyles: { fillColor: HEADER_FILL, textColor: TEXT, fontStyle: 'bold' },
      bodyStyles: { fillColor: '#ffffff' },
      alternateRowStyles: { fillColor: STRIPE_FILL },
      columnStyles: {
        0: { cellWidth: this.width * 0.13 },
        1: { cellWidth: this.width * 0.37 },
        2: { cellWidth: this.width * 0.32 },
        3: { cellWidth: this.width * 0.18 },
      },
      didParseCell: (data) => {
        if (data.section === 'body' && data.column.index === 2) {
          data.cell.styles.cellPadding = { top: 4, bottom: 4, right: 4, left: swatchPadding }
        }
      },
      didDrawCell: (data) => {
        if (data.section !== 'body' || data.column.index !== 2) return
        const color = colorOf(primaryOf(rows[data.row.index]))
        this.fillRect(data.cell.x + 4, data.cell.y + 5, SWATCH_SIZE, SWATCH_SIZE, color)
      },
    })
    this.y = this.doc.lastAutoTable.finalY
  }

  /**
   * Adds the footer of every page: signature with clickable links, page number, disclaimer
   * and provenance.
   */
  footers(appVersion, generatedAt) {
    const doc = this.doc
    const pages = doc.getNumberOfPages()
    const provenance =
      this.t('pdf.generated', {
        date: this.tr.dateTime(generatedAt),
        app: BRAND.application,
        version: appVersion,
      }) +
      '  ·  ' +
      this.t('status.sources')

    for (let page = 1; page <= pages; page++) {
      doc.setPage(page)
      const top = this.pageHeight - MARGIN - FOOTER_HEIGHT + 10
      this.strokeLine(MARGIN, top, MARGIN + this.width, top)

      const pageNumber = pdfText(this.t('pdf.page', { page, pages }))
      this.setFont(SMALL_SIZE, true)
      const numberWidth = doc.getTextWidth(pageNumber)
      this.signature(top + 11)
      this.text(pageNumber, MARGIN + this.width - numberWidth, top + 11, {
        size: SMALL_SIZE,
        bold: true,
        color: TEXT,
      })
      this.text(this.fit(this.t('app.disclaimer'), SMALL_SIZE), MARGIN, top + 22, {
        size: SMALL_SIZE,
        color: MUTED,
      })
      this.text(this.fit(provenance, SMALL_SIZE), MARGIN, top + 33, {
        size: SMALL_SIZE,
        color: MUTED,
      })
    }
  }

  /** Draws the author signature: name and title, website and e-mail, the last two clickable. */
  signature(baseline) {
    const separator = '  ·  '
    const title = this.t('brand.title')
    const website = this.t('brand.website')
    let x = MARGIN
    x += this.text(title, x, baseline, { size: SMALL_SIZE, bold: true, color: ACCENT })
    x += this.text(separator, x, baseline, { size: SMALL_SIZE, color: MUTED })
    x += this.link(website, website, x, baseline)
    x += this.text(separator, x, baseline, { size: SMALL_SIZE, color: MUTED })
    this.link(BRAND.email, `mailto:${BRAND.email}`, x, baseline)
  }

  // -------------------------------------------------------------------------
  // Building blocks
  // -------------------------------------------------------------------------

  /** Starts a new page if fewer than `height` points remain above the footer. */
  ensureSpace(height) {
    if (this.y + height > this.pageHeight - MARGIN - FOOTER_HEIGHT) {
      this.doc.addPage()
      this.y = MARGIN
    }
  }

  /** Renders a section title followed by a thin rule. */
  sectionTitle(title) {
    this.ensureSpace(40)
    this.line(title, { size: 12.5, bold: true, color: ACCENT })
    this.y += 4
    this.rule()
    this.y += 8
  }

  /** Renders a label and a wrapped value side by side; missing values show a dash. */
  detailRow(label, value, labelWidth) {
    this.setFont(BODY_SIZE, false)
    const lines = this.doc.splitTextToSize(
      pdfText(value?.trim() ? value : '-'),
      this.width - labelWidth,
    )
    const lineHeight = BODY_SIZE * LINE_SPACING
    this.ensureSpace(lines.length * lineHeight)
    this.text(label, MARGIN, this.y + BODY_SIZE, { size: BODY_SIZE, bold: true, color: MUTED })
    for (const line of lines) {
      this.text(line, MARGIN + labelWidth, this.y + BODY_SIZE, { size: BODY_SIZE, color: TEXT })
      this.y += lineHeight
    }
  }

  /** Renders wrapped text across the full width, continuing on a new page if needed. */
  paragraph(value, { size, bold = false, color }) {
    this.setFont(size, bold)
    for (const line of this.doc.splitTextToSize(pdfText(value), this.width)) {
      this.line(line, { size, bold, color })
    }
  }

  /** Renders one line of text and moves the cursor below it. */
  line(value, { size, bold = false, color }) {
    const lineHeight = size * LINE_SPACING
    this.ensureSpace(lineHeight)
    this.text(value, MARGIN, this.y + size, { size, bold, color })
    this.y += lineHeight
  }

  /** Draws a horizontal rule across the full width at the cursor. */
  rule() {
    this.strokeLine(MARGIN, this.y, MARGIN + this.width, this.y)
  }

  /** Shortens text with an ellipsis so it fits the full width. */
  fit(value, size) {
    this.setFont(size, false)
    const text = pdfText(value)
    if (this.doc.getTextWidth(text) <= this.width) return text
    let end = text.length
    while (end > 0 && this.doc.getTextWidth(`${text.slice(0, end)}...`) > this.width) end--
    return `${text.slice(0, end)}...`
  }

  // -------------------------------------------------------------------------
  // Drawing primitives
  // -------------------------------------------------------------------------

  /** Selects Helvetica at a size, regular or bold. */
  setFont(size, bold) {
    this.doc.setFont('helvetica', bold ? 'bold' : 'normal')
    this.doc.setFontSize(size)
  }

  /**
   * Draws a line of text at an absolute position.
   *
   * @returns {number} Width of the text.
   */
  text(value, x, baseline, { size, bold = false, color }) {
    const text = pdfText(value)
    this.setFont(size, bold)
    this.doc.setTextColor(color)
    this.doc.text(text, x, baseline)
    return this.doc.getTextWidth(text)
  }

  /**
   * Draws small accent-colored text and makes it a clickable link.
   *
   * @returns {number} Width of the text.
   */
  link(value, url, x, baseline) {
    const width = this.text(value, x, baseline, { size: SMALL_SIZE, color: ACCENT })
    this.doc.link(x, baseline - SMALL_SIZE, width, SMALL_SIZE + 3, { url })
    return width
  }

  /** Fills a rectangle. */
  fillRect(x, y, width, height, color) {
    this.doc.setFillColor(color)
    this.doc.rect(x, y, width, height, 'F')
  }

  /** Draws a thin line. */
  strokeLine(x1, y1, x2, y2) {
    this.doc.setDrawColor(RULE)
    this.doc.setLineWidth(0.5)
    this.doc.line(x1, y1, x2, y2)
  }
}
