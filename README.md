# NEON ECHO // HYTALE MOD
Cyberpunk starter mod with Street Cred, netrun challenges, and a themed config. Built to be hacked on.

---

## SIGNALS

- Neon welcome on join.
- `/netrun` mini-game with tiers, cooldowns, streaks, and rewards.
- Street Cred system (`/cred`, `/credtop`, `/credset`).
- Runner profiles + titles (`/neonprofile`), netrun stats (`/netrunstats`).
- Daily contracts (`/contracts`, `/claim`).
- `/neonhelp`, `/neonstatus`, `/neonmute`, `/neonreload` helpers.

## STREET CRED

- Earn cred for chatting and for time online (tunable in config).
- Netrun success grants bonus cred; failures can deduct cred.
- Data persists to `data.json` in the plugin data directory.

## NETRUN TIERS

- Default tiers: `easy`, `medium`, `hard` (configurable).
- Use `/netrun <tier>` to start a specific tier.
- Streaks tracked across sessions; view with `/netrunstats`.

## PROFILES

- Titles unlocked at cred milestones (edit in config).
- Join message shows your current title and cred.
- `/neonprofile` shows your stats and contract status.

## DAILY CONTRACTS

- Daily objectives award bonus cred when claimed.
- `/contracts` shows objectives; `/claim` collects rewards.
- Configure objectives and reward in `config.json`.

## CYBERPUNK ASSETS

- `NeonEcho_Datachip` ingredient (crafts from Prisma + cyan crystal).
- `NeonEcho_NeonPanel` glowing block (cyan crystal panel).
- `NeonEcho_HoloLamp` hologlow lamp with particles.
- `NeonEcho_DangerSign` neon wall sign.

## THEMES + CONFIG

- Theme packs: `neon`, `chrome`, `ghost` (set `theme` in `config.json`).
- Override message lines and prefix in `config.json`.
- Use `/neonreload` after edits.

## RUNTIME

- Hytale installed via the launcher.
- Java 25 for building.

## BUILD

```sh
./gradlew build
```

Output:

```
build/libs/NeonEcho-0.0.3.jar
```

## INSTALL (SINGLEPLAYER)

1. Drop the jar into your Mods folder.
2. Restart Hytale (mods load on startup).
3. Join your world and run `/netrun`.

macOS Mods path:

```
~/Library/Application Support/HytaleF2P/release/package/game/latest/Client/UserData/Mods
```

## DEV NOTES

- If Hytale lives elsewhere, set `hytale_home` in `gradle.properties` or pass `-Phytale_home=...` to Gradle.
- Entrypoint: `com.alexleo.neonecho.NeonEchoPlugin`
- Config + data live in the plugin data directory created by Hytale (look for `config.json` and `data.json`).

## LICENSE

MIT (swap if you want a different one).
