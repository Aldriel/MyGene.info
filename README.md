# MyGene Explorer

Explore human genes and their ClinVar variants using the public
[BioThings](https://biothings.io/) APIs:

- [MyGene.info](https://mygene.info): gene information (symbol, name, summary).
- [MyVariant.info](https://myvariant.info): ClinVar variants (clinical significance, origin).
  MyGene.info does not expose ClinVar, hence the use of its sibling service.

The project provides two independent clients with the same features: a web interface and a
desktop application. Their result files are interchangeable.

## Features

- **Gene lookup** by official symbol (e.g. `BRCA1`, `TP53`, `HLA-A`, `C9orf72`). The gene card
  shows the name, gene type, cytogenetic location, aliases and an expandable summary, with links
  to NCBI Gene and GeneCards.
- **Table of ClinVar variants** with color-coded clinical significance and links to ClinVar,
  a free-text filter, a clinical significance category filter, and sorting by any column
  (by severity for clinical significance).
- **Summary tab**: distribution of the loaded variants by clinical significance (pie chart,
  legend with counts and percentages, share of pathogenic variants).
- **Configurable number of variants** (100 to 1,000 per search) and **recent searches**.
- **English and French interface**, switchable at any time without reloading; dates and numbers
  follow the language.
- **Adjustable text size** (four presets); the whole interface scales.
- **Exports**:
  - **CSV**: the table as displayed (filtered and sorted), UTF-8 with the separator expected by
    spreadsheet software (`;` in French, `,` in English); values that could be interpreted as
    formulas are neutralized.
  - **JSON**: a self-describing document (gene, variants, retrieval date, data sources,
    application version) that either client can reopen.
  - **PDF report**: gene card, clinical significance distribution and the variants as
    displayed, with the author's signature (clickable website and e-mail), page numbers, data
    provenance and disclaimer on every page; in the interface language, on Letter or A4 paper
    depending on the region.
- **Remembered settings**: language, text size, number of variants and recent searches.
- Input validation before any request: wildcards, query syntax and other malformed input are
  rejected with an explanatory message.
- Clear error messages for unknown genes, network failures, timeouts, rate limiting (HTTP 429),
  server outages (HTTP 5xx) and invalid responses.
- Cancellation of a running search when a new one is started.

## Structure

```
.
├── web/        Web interface (React + Vite + Tailwind CSS)
├── desktop/    Desktop application (Java 21 + JavaFX + Jackson, Maven)
└── .github/    Continuous integration (GitHub Actions)
```

## Web interface

Requirements: Node.js 20 or later.

```bash
cd web
npm install
npm run dev              # development server on http://localhost:5173
npm test                 # unit and component tests (Vitest + Testing Library)
npm run test:coverage    # tests with coverage report in web/coverage
npm run build            # production build in web/dist
npm run format           # format the code with Prettier
```

Web-specific features:

- **Shareable links**: the searched gene is kept in the address (`?gene=BRCA1`), so a result can
  be bookmarked or sent; the browser's back and forward buttons move between genes. A
  *Copy link* button puts the address on the clipboard.
- Exports are downloaded by the browser; *Open results…* reads a JSON file exported by either
  client. The PDF library (jsPDF) is only loaded the first time a report is requested.
- Settings are kept in the browser's `localStorage`; the first visit follows the browser
  language.
- Accessible markup: labelled controls, sortable headers with `aria-sort`, tabs, keyboard
  navigable export menu and announced notifications.

| Folder            | Role                                                                 |
|-------------------|----------------------------------------------------------------------|
| `src/api`         | BioThings client and gene symbol validation                          |
| `src/model`       | Clinical significance categories, filtering and sorting              |
| `src/i18n`        | Messages, translator (numbers, dates) and translated error messages  |
| `src/export`      | CSV, JSON result file, PDF report and downloads                      |
| `src/state`       | Preferences, search state and shareable links (React hooks)          |
| `src/components`  | User interface                                                       |

## Desktop application

Requirements: JDK 21 and Maven 3.9 or later.

```bash
cd desktop
mvn javafx:run           # launch the application
mvn test                 # unit tests (JUnit 5), coverage report in target/site/jacoco
```

To build the portable Windows distribution (PowerShell, JDK 21 in `JAVA_HOME`):

```powershell
cd desktop
.\package-windows.ps1    # target\dist\MyGeneExplorer-<version>-windows-x64.zip
```

The archive contains `MyGene Explorer.exe` with its own trimmed Java runtime (`jlink` and
`jpackage`), so users only unzip it and run the executable; no Java installation is required.

Desktop-specific features:

- Menu bar: *File* (open, export as CSV `Ctrl+E`, JSON `Ctrl+S` or PDF `Ctrl+P`, recent
  searches), *Edit*, *Options* (language, text size) and *Help* (keyboard shortcuts, data
  sources, *About* dialog).
- Texts come from `ResourceBundle` files (`messages_en.properties`, `messages_fr.properties`).
- Text size also adjustable with `Ctrl+=`, `Ctrl+-` and `Ctrl+0`; applied as `-fx-font-size`
  on the scene root, with a stylesheet in `em` units.
- Copy to clipboard (`Ctrl+C`, pastes into spreadsheets), context menu and double-click to open
  a variant in ClinVar.
- PDF reports generated with Apache PDFBox; window size and last folder used are remembered.

The desktop code follows the Model-View-Controller pattern:

| Layer      | Package                         | Role                                                     |
|------------|---------------------------------|----------------------------------------------------------|
| Model      | `model`                         | Domain records and `AppState`, the observable UI state   |
| View       | `ui`                            | JavaFX layout, bound to the state; no business logic     |
| Controller | `controller`                    | `MainController`: user actions, unit tested without a UI |
| Services   | `service`, `export`, `i18n`, `prefs` | API client, file formats, translations, settings    |

## Tests

Both clients share the same test strategy:

- **Input validation**: valid symbols (including hyphens, dots and lowercase letters) and
  malformed input (wildcards, field prefixes, spaces, quotes, accented letters, HTML).
- **Response parsing**: single objects vs. arrays, merged ClinVar submissions, missing fields.
- **Error handling**: each HTTP error class, network failure, timeout, invalid JSON and
  cancellation. The desktop tests run against a local HTTP server that simulates the APIs.
- **Features**: CSV escaping and separators, saved-file round trips (including files written
  by the other client) and rejection of corrupted or newer files, filtering, sorting and
  severity classification, PDF content and links, persisted settings, and translation
  completeness (every message exists in both languages with the same placeholders).
- **User interface** (web): component and end-to-end tests with a simulated API covering the
  search flow, loading, empty and error states, superseded searches, language and text size
  switching, recent searches, shareable links and browser history, filtering and sorting,
  exports and opening a results file.
- **Controller** (desktop): user actions unit tested without a user interface.

Continuous integration runs formatting checks, both test suites and the web build on every
push to `main` and on every pull request.

## Conventions

- Lines of at most 100 characters.
- Indentation: 2 spaces (web), 4 spaces (Java, XML).
- These rules are described in `.editorconfig`; the web code is formatted with Prettier.

## Data sources

Data is provided by the BioThings APIs and ClinVar (NCBI). This tool is intended for research
and exploration only and must not be used for clinical decision-making.

## License

Copyright 2026 Maxime Ethier - Consultant en Bioinformatique/Biocomputing Consultant.

This project is licensed under the Apache License 2.0: see the [`LICENSE`](LICENSE) file.
The web application publishes it as `LICENSE.txt` and links to it from its footer; the footer
of the desktop application links to the official text on apache.org.
