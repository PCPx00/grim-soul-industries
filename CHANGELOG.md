# Changelog

## 2.1.0 — Farming cards, Soul Forge and quality of life

- **Beheading card:** Reaper Block cards (up to 10). Each card adds a chance to drop the head of a zombie, skeleton, creeper or piglin when the Reaper kills it.
- **Harvest card:** a Reaper Block card (1 max) that takes wool from adult sheep, milk from adult cows and eggs from adult chickens without hurting them. Cows and chickens are ready again after 2 minutes. Harvested animals are not attacked, even in Hostile mode.
- **Settings Card:** right-click a machine to copy its settings (work area, whitelist, filter, auto-eject, magnet, redstone mode and on/off). Right-click another machine of the same kind to paste them. Sneak-right-click to clear the card. Items, upgrades and power are never copied.
- **Soul Steel and Soul Forge:** Soul Steel is made from Netherite and a Nether Star. Hold an item in your main hand and Soul Steel in your off hand, then right-click a Soul Forge:
  - a Soulforged Data Core of a mob with a spawn egg makes that spawn egg;
  - a Soulforged Data Core of a boss makes a **Boss Core card**;
  - any single item makes a **soulbound** copy, kept in your inventory when you die.
- **Boss Core card:** a fifth Virtualization Chamber card. It halves the power cost of running a boss (Wither, Ender Dragon, c:bosses). Max 1.
- **JEI, EMI and Jade support (optional):** the Reaper Generator's fuel values show in JEI and EMI, and Jade shows each machine's status (power, XP, mob, energy).
- **Smite and Bane of Arthropods cards removed:** they weren't needed. Any of these cards you had are gone when you load 2.1.0.
- **Guide:** a new Endgame & Utilities section covers the Settings Card, Soul Steel, Soul Forge and Boss Core card.
- **FTB Quests chapter:** `ftbquests/grimsoul.snbt` in the repo is a starter chapter that walks through the machines. Copy it into your pack's `config/ftbquests/quests/chapters/` folder.

## 2.0.0 — Apotheosis support, boss Soul Cards and smoother visuals

- **Apotheosis support:** with a **Player Kill** card, hostile mobs killed by the Reaper Block or simulated in the Virtualization Chamber can drop Apotheosis affix weapons, tools and armor. Rarity follows the nearest player's World Tier and luck. The chance is 5% per kill by default and can be changed with `compat.apotheosisGearChance` in the config. Apotheosis is optional.
- **Boss Soul Cards:** Soul Cards can now be bound to the **Ender Dragon** (click any part of it) and the **Wither**, so you can farm them in the Virtualization Chamber. Simulated dragons give 500 XP with a Player Kill card. The Reaper's Lantern still can't capture either of them.
- **No AI card fixed:** mobs from a spawner with a No AI card can no longer hurt you in any way (melee, touch damage, arrows, explosions), never target you and make no sound. They stay frozen even after converting (like a zombie drowning).
- **Reaper Block lasers fixed:** the lasers now show up reliably, with or without shaders (tested with Iris and Complementary). They fire out to the target, pulse along the beam and fade out smoothly.
- **See-through Reaper Block screen:** the screen is now about 70% transparent, so you can see the work area outline while changing it. Labels have a shadow so they stay readable.
- **Smoother animations:** spinning, bobbing and glowing parts on every machine now run on the client's own clock, so they no longer stutter when the server lags.
- **Machines no longer give off light:** the Reaper Spawner, Reaper Generator and Virtualization Chamber used to light up the area around them, which stopped mobs from spawning nearby. They still glow visually, but no longer light up blocks.
- **Every upgrade card has its own recipe:** no more clashes (No AI and Bane of Arthropods shared a recipe before). Each recipe uses a frame item that matches the card's machine: iron for shared cards, amethyst for the Reaper Block, gold for the Reaper Spawner and prismarine for the Virtualization Chamber. The Codex shows the new recipes.

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
