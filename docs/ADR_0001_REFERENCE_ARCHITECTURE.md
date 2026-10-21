# ADR 0001: one authored reference before procedural diversity

Status: accepted for the experimental 0.3 branch, not a visual acceptance decision.

## Baseline and branch decision

Inspected on 2026-10-20: open draft PR #3 contains identity/diagnostics at
`740bcd699957f4cf36faf2820cccdc92e757be51`, stacked on terraced PR #2.
Open draft PR #6 adds only the Work order at
`d2224b09c2b6ca34f13fb6ce7fb64ca4611a0378`.
`feat/castleforge-0.3-architecture-core` starts from that exact #6 head, thereby
preserving both the latest implementation and its design brief. It targets the
preparation branch `feat/reference-castle-0.3`; nothing is merged into `main`.

The baseline's three dependency-free suites were rerun locally with Temurin
21.0.5 before implementation: classic 50 seeds, terraces 160 terrain/seed cases,
identity 100 seeds x eight cultures. All passed; the existing terraced golden
digest remains `6fd20d8855fdc190a7e81e9772cef9d4aad3974a3a405ca25f31161b643f7828`.

## Decision

Create a separate `dev.castleforge.core.architecture` package. Compose a typed
architecture model before rendering blocks. Author one connected, asymmetric
castle with a dominant keep, a great hall, residential/service rooms, gatehouse,
courtyard and curtain walls. Module ownership, intentional connections,
circulation reservations and final block checks are explicit contracts.

Use one central-European material palette and maintained condition for the
reference. The site still has its original culture-region identity. This is
deliberately not a claim of eight new cultural grammars. Seed and position choose
stable orientation under a separate architecture signature; they do not
modify `CultureMap` or `generation-v1.properties`.

Use a bounded authored footprint, smaller than the Work order's aspirational
120-160-block long axis. This gives the design room for distinct masses without
blindly raising the old generator budgets or making whole-bounding-box AIR plans.
Keep terrain handling conservative: shallow supports and explicit rejection,
not deep excavation, global plate filling or arbitrary mountain adaptation.

Reserve intended main routes and validate them after the furniture pass. A
global two-block voxel flood-fill that can climb roofs is insufficient evidence
of an internal staircase. Checks are a conservative model of supported building
primitives, not a substitute for Minecraft collision and player movement tests.

## Placement and undo compatibility

Keep classic/terraced implementation and palettes intact. Use a separate
`/castleforge preview reference` path with the existing confirmation, conflict
checks, placement budget, status and session-only undo semantics.

For this milestone furniture is built from blocks without BlockEntities:
timber, planks, stairs, slabs, ordinary bookshelves, wool and supported lanterns.
Hearths are decorative; no live fire, lava, campfires or inventory containers.
Open framed doorways avoid half-placed multi-block doors. No fences, iron bars
or wall connection states are added. This deliberate subset fits the existing
BlockState-only journal; safe data-preserving container undo is deferred.
Hanging lanterns are placed after their supports and undone before them.

## Evidence and remaining boundaries

The reproducible exporter/renderer uses actual generated block data and no
Minecraft assets. Its views show composition, floor plans and sections. The
primitive renderer does not prove vanilla-texture appearance, lighting,
neighbor-update behavior, real navigation, performance or visual approval.

Local JDK 25 is present, but pinned Gradle 9.4.0 and Fabric/Mojang dependencies
were unavailable in the offline cache. The complete pinned build is verified
separately by GitHub Actions. No installed Prism instance or existing world is
modified by this development session. No EULA is accepted on the user's behalf.

Automatic worldgen/exploration generation, arbitrary slopes, monuments, NPCs,
loot, Bedrock/consoles and NeoForge remain outside this milestone. See the
reference guide and validation record for actual results and open checks.
