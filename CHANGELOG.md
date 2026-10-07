# Changelog

## 1.1.0 — Spawn areas, No AI and the Grim Soul Codex

- **Grim Soul Codex:** a new in-game guide book covering every machine, card and recipe, in order from the basics to Virtualization. Recipes are drawn live from the game, so they always match your pack. Every player gets one the first time they join (can be turned off in the config), and you can craft another from a Book and a Bone.
- **Reaper Spawner spawn area:** move (offset) and resize the spawn area on each axis, like the Reaper Block's work area. Shrink it to a single block to spawn mobs on one exact spot. Range cards raise the limits, and Show Area draws it in the world.
- **No AI card:** a new Reaper Spawner card. Spawned mobs have no AI, so they stand still and never attack.
- The Reaper Spawner screen is taller to fit the new controls and the seventh card slot. Existing spawners keep their cards.

## 1.0.1 — Charge slots

- **Reaper Energy Cell** and **Reaper Generator** now have a **charge slot**. Put in any item that holds FE (tools, batteries, jetpacks, armor from other mods) and it charges from the machine's power, with a charge bar and percentage in the GUI.
- A picked-up **Energy Cell** now works as a **portable battery**: charge slots from Grim Soul Industries or other mods can fill it, and it keeps that charge when placed.
- The Energy Cell screen now shows your inventory, so you can shift-click items into the charge slot.

## 1.0.0 — First release

**Tier 1 – Reaping**
- Reaper Block: purple lasers kill mobs in a configurable work area. Target modes (Hostile, Friendly, Players, All), magnet on/off, item filter and void, 9–54 slots of storage, auto-eject, Soul Card slot. Starts switched off and set to Hostile mobs.
- Reaper's Lantern: capture a mob and carry it with you.
- Reaper Fan: pushes mobs with an adjustable push area.
- Reaper XP Tank: stores XP as Liquid Experience, with a magnet and pipe support.
- 15 upgrade cards, each with its own art and per-machine limits.

**Tier 2 – Spawning & Power**
- Reaper Spawner: spawns any captured mob, charging FE up to each spawn. Speed, Count, Range, No Player Needed, Ignore Light and Ignore Spawn Conditions cards.
- Reaper Generator: burns mob drops for FE (data-driven fuel values), per-side item and energy config, pass-through.
- Reaper Energy Cell: 10M FE storage with per-side In / Out / Off and a charge gauge; keeps its charge when broken.

**Tier 3 – Virtualization**
- Reaper Soul Card: bind it to a mob and level it through five tiers (Hollow → Soulforged).
- Reaper Virtualization Chamber: runs the mob's real death drops with no entity spawned. Speed, Looting, Efficiency and Player Kill cards.
- Reaper Output Crate: 54-slot storage with void overflow.

**Look & feel**
- Layered 3D models with per-machine animations, three GUI styles (Reaper, Vanilla, Dark).
