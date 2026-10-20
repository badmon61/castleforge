# ADR 0002: reject v1 visually; develop bounded architectural detail studies

Date: 2026-10-20. Status: implemented experimental study, **not visual acceptance**.

## Evidence and decision

Emre rejected Falkenwacht v1 after a real Minecraft trial. Four supplied
screenshots (19:22:20, :36, :40, :46) show a broad empty stone court, largely
flat walls, narrow unframed glazing and undivided rooms. Furniture and beams
do not rescue the overall composition. Hostile mobs are visible; this does not
prove exactly where they spawned, but lantern counts never established spawn
safety. Successful geometry assertions did not establish design quality.

Repository checked before edits: PR #7 open/draft, head
`e94e13f638a2e27d588661a4d06c482570a2b369`, based on PR #6 at `d2224b0`;
PRs #2/#3/#6 remain open drafts. Both Actions runs on the inspected #7 head
passed. Local work was clean. Continue the existing #7 branch, preserving
the baseline commit, old evidence and classic/terraced generators.

The existing ownership/route/worker architecture is useful. Its **design
vocabulary is insufficient**: a Room mostly represented a whole storey,
window rules cut single-block slits, and all courtyard cells were reserved
as circulation, leaving no modeled place for purposeful amenities.

Extend that model, not a parallel engine: actual partitions and portals,
facade bays with depth/envelopes, owned dormer junctions, and court-use areas
are specified before emission. Add independent checks for those contracts.
The next version is named `falkenwacht-v2-study`, not a production standard.

## External examples: inspection only

User supplied Louvre (semi-finished), Creepy Blackstone Castle by
NevasBuildings, Palace of the Ancients, Confessor's Palace v1.1, and local
Falcon's Rock v2.0.0. All five ZIPs were read without importing them into
Minecraft. World metadata/region files and a bounded 256-chunk (256 x 256
block) sample around each saved player position were decoded locally.
That includes older numeric block IDs in Falcon's Rock. Approximate top
surfaces were inspected; this is **not a complete architectural survey or
interior walkthrough** of those large maps. Some saved positions show
approaches/terrain rather than the principal building.

They establish the user's intended category and ambition, not a source of
modules to copy. No foreign world, schematic, voxel sample, extracted asset
or template is checked in or used by the runtime. User screenshots and
downloaded worlds are not republished with this PR.

## Implemented scope

- Nine physical partitions and six additional residential rooms with their
  own protected door approaches. Furniture is constrained to its room bounds.
- Twenty-two articulated facade bays, wider glazing, projected jambs, sills
  and hoods; hall buttresses and a framed main entrance. Upper residential
  plaster infill and timber gable framing differentiate building uses.
- Two east-facing roof lights with owned subordinate roof junctions. They
  are attic details, not new habitable rooms or accessible balconies.
- Corrected hall table height (v1 was 2.5 blocks above floor), processional
  floor band, lower supported wall lamps, and hearth/chimney placement.
- Covered decorative well, bounded herb garden and a seating arbor with a
  reserved approach. Court paving distinguishes paths from open areas.
- Window checks now require clear cells on both sides. This caught a hearth
  obscuring a hall window during iteration; the hearth was relocated.
- Terrain rejection now includes the actual BlockState and distinguishes a
  block entity from an unsupported surface type. No whitelist relaxation or
  claim of reliable player-placed-block detection.

## What this does not solve

The enclosing rectangle, large court, repetitive curtains, simple keep,
ground-floor tower rooms and incomplete wall walks remain. The new study is
**still below the requested hand-built benchmark**. Do not mark visual
acceptance complete or present the project as release-ready.

There is still one fixed composition with four deterministic rotations;
no epoch selection, parametric size families, terrain-driven massing or
eight cultural building grammars. This bounded authored vocabulary is only
a step toward that engine. The three-block relief limit is unchanged.

No live fire, block-entity furniture, working beds/doors, inventories, loot,
inhabitants, worldgen or platform ports. Table tops introduce explicit top
slabs; renderer and Fabric adapter distinguish them from bottom slabs.
No neighbor-sensitive fences/bars or new multi-block objects were added.
Actual lighting, movement, stairs, slab behavior and undo need separate
runtime checks. Existing session-only undo is not a backup.

## Visual gate and resource discipline

Compare actual block output at identical seed/site before another broad
playtest. Improve one coherent architectural ensemble and show close views;
do not request repeated full-castle tests merely because tests are green.
The project owner decides visual acceptance. No numeric assertion count,
shader, concept image or claimed development effort substitutes for it.

Work is bounded by a concrete comparison deliverable. No parallel agents,
speculative platform ports or repeated full baseline tests during each
small iteration. There is no reliable access here to the user's remaining
ChatGPT allowance, and no promise of a cheap or one-prompt production engine.
