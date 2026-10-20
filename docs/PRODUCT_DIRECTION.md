# Product direction and implementation boundary

Owner: Emre / badmon61. Original idea: Emre's brother. Updated 2026-10-20
from the owner's full handoff. Live code determines implementation status;
these goals must survive subsequent refactors and chat handoffs.

## Product ambition

An original procedural architecture engine for outposts, castles, fortresses,
citadels, palaces and very rare monumental fantasy compounds. Results should
look intentionally designed for their site and be worth exploring with
Vanilla textures, without shader assistance. Some future compounds should
offer roughly three minutes of interesting traversal, not three minutes of
repeated corridors or an enormous bounding box.

Historical inspiration with coherent fantasy. The metaphor "95% credible,
5% madness" is not an agreed spawn probability. Extreme structures still
need purposeful support, circulation and a legible composition.

Culture, epoch/building phase, local climate/materials, terrain, scale,
building use and condition should eventually influence design. Variation
means different architectural decisions, not palette swaps or random boxes.
Own authored modules are welcome; no downloaded schematics/templates.
Eventually contributors should author modules without understanding the
entire generator. No data-authoring interface is implemented yet.

## Stable decisions

- An independent invisible culture map, not a direct biome-to-style mapping.
  Families: central European, Nordic, Mediterranean, desert/oriental-inspired,
  east Asian, dark Gothic, high fantasy and maritime. These are planned
  design families, not eight already implemented architectural grammars.
- Default culture scale: approximately **7 times linear biome extent**,
  not seven biome areas. Current 1,024-block biome reference and 7,168-block
  nominal region-site spacing are configurable development values, not a
  measured Vanilla average. Do not rescale regions as exploration proceeds.
- Save parameters and algorithm versions must remain stable. No unasked
  changes to existing structures or destructive regeneration on update.
- Materials derive from culture and local conditions. Vanilla remains the
  base; optional mod mappings later. No compulsory content-mod dependency.
- Building history should ultimately affect constructed phases, repairs and
  abandoned wings. Names, dynasties, heraldry and history currently remain
  metadata; they are not physical banners, NPCs or reconstruction simulation.
- Occupancy eventually depends on condition **and** region; not every castle
  is hostile. Population needs independent performance/simulation limits.

## Architectural obligations

Site constraints -> composition -> rooms/functions -> doors/stairs/routes ->
facades/roof junctions -> materials/condition -> furniture/light -> final
geometry checks -> bounded placement. Preserve ownership and model explicit
shared interfaces rather than allowing silent last-write-wins geometry.

Judge distance silhouette, close facade/roof depth, and interior spatial
sequence separately. Purposeful primary/secondary masses, human-scale rooms,
planned windows, usable stair landings, protected routes and meaningful
furnishing matter. Access assertions are minimum technical requirements.

Later terrain planning must choose among site-appropriate compositions,
terraces, ridge walls, foundations and bridges. Blindly increasing foundation
height or burying a fixed rectangle in a mountain is not adaptation.

## Current milestone and status

**One visually accepted medium-sized reference comes first.** Falkenwacht v1
was rejected by the owner. V2 is a bounded detail study and remains unaccepted.
See ADR 0002 and its actual block views. Fixed composition/rotation, single
central-European study, shallow dry terrain only; max survey relief 3 blocks.

Core model, deterministic generation, ownership and geometry checks exist.
Fabric has manual preview/confirm, tick-sliced terrain/preflight, bounded pure
worker, cancellation/stale-request handling, inspect and session-only undo.
Final confirmation is still synchronous. A block whitelist cannot establish
who placed a block. No guarantee of safe restoration of neighboring updates,
all-mod compatibility, real-time performance or spawn-safe lighting.

Next gates: visual foundation, actual traversal/block behavior, then more
layout families and demanding terrain. Only after that: exploration worldgen,
larger compounds, more cultures, population and physical history/condition.
Automatic worldgen remains disabled now.

Java/Fabric 26.1.2 is the current platform. Bedrock/consoles and NeoForge are
future adapters; portable architectural data is desirable, a common mod
binary is not promised. Keep existing dependency pins until intentionally
changed and checked.

## Evidence and authorization

Track compilation, pure generator checks, actual block renders, Minecraft
runtime, real movement/light/updates, owner visual approval and modpack/FPS
testing separately. Do not imply one establishes the others. No AI concept
image may masquerade as implemented geometry.

Continue on the development PR chain; do not merge main, force-push shared
history, publish a release, accept EULAs or alter normal Prism instances,
long-term worlds, global Java/graphics settings or unrelated software without
explicit authorization. Use a clearly identified isolated environment for
runtime tests. Another chat does not supervise this work live.
