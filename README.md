<p align="center">
  <img src="docs/banner.svg" alt="Steam Showcase" width="100%">
</p>

<p align="center">
  <a href="https://github.com/alexboydray/SteamShowcase/releases/download/latest/SteamShowcase.apk"><img alt="Download the APK" src="https://img.shields.io/badge/Download-APK-66C0F4?style=for-the-badge&logo=android&logoColor=white"></a>
  <a href="https://github.com/alexboydray/SteamShowcase/releases/tag/latest"><img alt="Latest build" src="https://img.shields.io/badge/Latest-build-1B2838?style=for-the-badge&logo=steam&logoColor=white"></a>
  <img alt="Android 9 or newer" src="https://img.shields.io/badge/Android-9%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white">
</p>

---

### Love the showcases on your Steam profile, but wish you could pick exactly what goes on them?

**Now you can.** Steam Showcase turns your Steam profile into a board of tiles you arrange yourself: your avatar and level, what you've been playing, the achievements you're proudest of, your screenshots and your favourite game. Choose which tiles show, in what order, half or full width, and what each one shows.

It was made for the AYN Thor's second screen, but it runs on any Android 9+ phone or tablet.

## ✨ What you get

| | |
|---|---|
| 🧑 **Profile header** | Your avatar (frame and animated avatar included), name, status or current game, level and profile background. |
| 🏅 **Level & badges** | Your level ring, XP progress and top badges. |
| 🎮 **Recently played** | The last two weeks, as a list or a row of game capsules. |
| 🏆 **Achievements** | Show off any game's unlocked achievements, or your latest unlocks across recent games. |
| 🖼️ **Screenshots & artwork** | Your newest public Steam screenshots, or your own pictures from the device or a link. |
| ⭐ **Favourite game** | One game with its hero art, hours played and achievement progress. |
| 📝 **Text box** | A heading and any text you like. |
| ✏️ **Your layout** | Tap the pencil to move tiles, switch them between half and full width, set them up or remove them, and add new ones. |

## 📲 Install

1. On your device, tap **[Download the APK](https://github.com/alexboydray/SteamShowcase/releases/download/latest/SteamShowcase.apk)**.
2. Open the downloaded file. If Android asks, allow your browser to **install unknown apps**.
3. Get a free **Steam Web API key** at [steamcommunity.com/dev/apikey](https://steamcommunity.com/dev/apikey) (any domain name works).
4. In Steam, set your profile, including **Game details**, to **Public**.
5. Open **Steam Showcase**, go to Settings, paste the key and your SteamID64, custom URL name or profile link, and tap **Save**.

> [!NOTE]
> Builds are signed with a temporary key for now, so a new version may not install over the old one. If Android refuses the update, uninstall the old version first; you'll need to enter your key and set up your layout again.

## ❓ FAQ

<details>
<summary><b>Where does my API key go?</b></summary>

It stays on your device, and it's only ever sent to Steam's own API (`api.steampowered.com`). Nothing else sees it.
</details>

<details>
<summary><b>A tile is empty.</b></summary>

Most often your Steam profile or game details are set to private or friends only. Set both to **Public** in Steam's privacy settings, then tap refresh.
</details>

<details>
<summary><b>Do I need an AYN Thor?</b></summary>

No. It was designed for the Thor's bottom screen, but it works on any Android 9+ device.
</details>

## 🛠️ For developers

<details>
<summary>How it works, how to build it, and how to add a tile</summary>

- Kotlin and Jetpack Compose (Material 3), with OkHttp for the Steam API and Coil for images. No other dependencies.
- `data/SteamApi.kt` talks to the Steam Web API and community pages, `ShowcaseViewModel.kt` fetches everything a layout needs, and `data/Tiles.kt` lists the tile types.
- Your layout and settings are kept on the device (`data/Prefs.kt`).

**Building:** every push to `main` is built by GitHub Actions and published to the *Latest build* pre-release. Locally, use Android Studio, or `gradle assembleRelease` with the Android SDK installed.

**Adding a tile type**

1. Add an entry to `TileType` in `data/Tiles.kt` (id, title, blurb, default size, default settings).
2. Write its composable in `ui/tiles/`.
3. Wire it into `RenderTile` (`MainActivity.kt`) and, if it has settings, into `TileConfigDialog.kt`.
4. If it needs new data, fetch it in `ShowcaseViewModel.fetchAll`.
</details>

---

<sub>Steam Showcase is an independent project and is not affiliated with, endorsed by, or sponsored by Valve Corporation. Steam is a trademark of Valve Corporation.</sub>
