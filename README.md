# ResumeStudio for Android

An Android port of [ResumeStudio](https://github.com/mhalesto/ResumeStudio), the
résumé and cover-letter builder. This repository holds the Android app; iOS
remains the source of truth for the document model and the template catalogue.

Current milestone: **the render core.** The document model, the template
vocabulary, and the PDF renderer come first, because the renderer is the product
and everything else is a screen on top of it.

## Why a port and not a rewrite, and not shared code

The iOS renderer (`ResumePDFRenderer.swift`, ~8,300 lines) draws imperatively
into a `UIGraphicsPDFRenderer` context — text placed at measured coordinates,
not laid out by SwiftUI. Android's `PdfDocument` + `Canvas` + `StaticLayout` is
the same shape of API working in the same unit (points at 72dpi), so the
geometry carries across largely intact.

Kotlin Multiplatform was considered and rejected: sharing the engine would mean
rewriting a working, shipping Swift renderer in Kotlin, which destabilises the
live app for no user-visible gain. Instead the two platforms share a **spec**
rather than code, and a test suite proves they agree.

## Layout

| Module | What it is |
|---|---|
| `:core:model` | Plain JVM. The document model and template vocabulary, mirroring the Swift types. No Android dependencies, so the parity suite runs in milliseconds. |
| `:core:render` | Android library. The PDF renderer. |
| `:app` | Compose application. |
| `spec/` | `template-catalogue.json` — the mapping from all 140 templates to their layout plans, generated from iOS. |
| `tools/` | The generators that produce the spec and the mirrored enums. |

## How the two platforms are kept in step

iOS owns the catalogue. The mapping from a template to its `TemplatePlan` lives
in the `plan` switch in `ResumeDocument.swift`, and is lifted from there:

```bash
python3 tools/generate-template-spec.py \
  ../../ios/ResumeStudio/ResumeStudio/Models/ResumeDocument.swift \
  spec/template-catalogue.json
cp spec/template-catalogue.json core/model/src/main/resources/
```

The generated file ships as a resource and `TemplateCatalogue` reads it at first
use, so **Android cannot drift from the catalogue by construction** — there is no
second copy of the mapping to fall out of step, only a regeneration that is
either run or not. `TemplateCatalogueTest` fails loudly if it has not been.

The same applies to the accent palette (`tools/generate-accents.py`). Its colour
components are kept as the floats iOS writes rather than packed into hex here;
rounding happens once, at the point of use, on both platforms.

What this catches: a template added, removed, or re-planned on iOS. What it does
**not** catch: the two renderers drawing the same plan differently. That is what
the golden-image suite is for, and it does not exist yet.

## Verifying

```bash
./gradlew :core:model:test              # parity suite — 16 tests, no device needed
./gradlew :core:render:connectedAndroidTest   # renderer — needs a device
./gradlew assembleDebug
```

The renderer tests reopen every PDF they produce with `PdfRenderer`, so a file
that writes without error but cannot be parsed still fails. `everyTemplateInThe
CatalogueRenders` walks all 140.

## State of the renderer

Done: the page frame, pagination, the letterhead, the side column (band, bleed,
divider, light-ink swap), all six competency styles, three experience styles,
education, references, additional sections, `darkPaper`, `skillsFirst`,
`numberedSections`, `bodyInset`, and `density`.

Not done, and marked at each site:

- `sectionChrome: card` falls back to `plain`.
- `hangingHeadings` draws inline instead of in a gutter.
- The portrait and the signature are not drawn.
- No page-target fitting pass — `pageTarget` is read but not honoured.
- Fonts map onto platform families rather than the bundled iOS faces, so metrics
  differ. This is the largest single source of layout divergence today.

Treat the layout as close, not exact, until the golden-image suite lands.

## Stack

Kotlin 2.4 · AGP 9.3 (Kotlin support is built in — no `kotlin-android` plugin) ·
Gradle 9.6 · Compose BOM 2026.06 with Material 3 · compileSdk/targetSdk 37 ·
minSdk 26 · kotlinx.serialization · version catalogs.

Planned as the app grows past the render core: Hilt (KSP), Room + DataStore,
Navigation Compose, Firebase (Auth, Firestore, App Check via Play Integrity),
Play Billing, ML Kit for OCR, Glance for widgets.

## Where the iOS features land

| iOS | Android |
|---|---|
| PDFKit | `androidx.pdf` |
| Vision OCR | ML Kit Text Recognition |
| PencilKit signature | Compose `Canvas` + pointer input |
| WidgetKit | Glance |
| StoreKit | Play Billing (RTDN + server work) |
| App Check (DeviceCheck) | App Check (Play Integrity) |
| **FoundationModels** | Gemini Nano where available, else the existing `/v1/ai` endpoint |
| **ActivityKit** | No equivalent — ongoing notification / Live Updates |
| **iCloud sync** | No equivalent — Firestore |
| **Safari extension** | No equivalent — share-sheet intent filters |

The backend is already platform-neutral REST (`/v1/ai`, `/v1/account`,
`/v1/profile`, `/v1/links`, `/v1/referrals`, `/v1/benchmarks`,
`/v1/attestations`) and is reused as-is. Receipt validation and attestation are
the two endpoints that need Android-specific work.
