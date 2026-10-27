# Steam Showcase

A configurable Steam profile board for the AYN Thor (or any Android 9+ device).
Think of the showcases on your Steam profile page, but you choose which ones
appear, in what order, how wide, and what each one shows.

## Showcase tiles ("plugins")

| Tile | What it shows |
|---|---|
| Profile header | Avatar (with frame / animated avatar), name, status / current game, level, profile background |
| Level & badges | Level ring, XP progress bar, top badges |
| Recently played | Last two weeks, as a list or capsule row |
| Achievement showcase | Pick any game in your library, show its unlocked achievement icons |
| Recent achievements | Latest unlocks across your recently played games |
| Screenshots | Newest public Steam Community screenshots |
| Artwork gallery | Your own images (device photos or URLs) |
| Favourite game | One game, hero art, hours and achievement progress |
| Text / info box | Custom heading + text |

Tap the pencil to enter edit mode: move tiles up/down, toggle half / full width,
configure (gear) or remove them, and add new ones with **Add tile**.

## Setup

1. Get a free Steam Web API key at <https://steamcommunity.com/dev/apikey> (any domain name).
2. Set your Steam profile — including *Game details* — to **Public**.
3. Install the APK, open Settings, paste the key and your profile URL / SteamID / vanity name, Save.

The key only lives on the device (SharedPreferences) and is only sent to `api.steampowered.com`.

## Building

Builds run on GitHub Actions on every push to `main`; the APK is attached to the
"Latest build" pre-release and to the workflow run as an artifact.

Locally: Android Studio, or `gradle assembleRelease` with the Android SDK installed.
Stack: Kotlin, Jetpack Compose (Material 3), OkHttp, Coil. No other dependencies.

## Adding a new tile type

1. Add an entry to `TileType` in `data/Tiles.kt` (id, title, blurb, default size, default settings).
2. Write a composable in `ui/tiles/`.
3. Wire it in `RenderTile` (`MainActivity.kt`) and, if it needs settings, in `TileConfigDialog.kt`.
4. If it needs new data, fetch it in `ShowcaseViewModel.fetchAll`.
