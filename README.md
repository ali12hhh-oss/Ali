# Velocity — Professional Video Editor for Android

Native Android app (Kotlin + Jetpack Compose) that follows the Velocity reference design:
Home, Multi-track Timeline Editor, Effects/Library, and Export & Add-ons screens.
Fully bilingual: **English (LTR)** and **العربية (RTL)** — the layout mirrors automatically per language.

## Screens
| Screen | Package |
|---|---|
| Home (search, my projects, create new, templates) + Studio + Profile | `ui/home` |
| Timeline editor (preview, transport, V1/V2/A2/A1 tracks, tools bar) | `ui/editor` |
| Effects / Library (Video, Audio, Effects: transitions, filters, titles) | `ui/library` |
| Export settings + free professional add-ons | `ui/export` |

## Stack
MVVM · Hilt · Room · Navigation-Compose · Media3 (ExoPlayer preview, Transformer export) · Coil

## Build
1. Open the folder in **Android Studio** (Ladybug or newer, JDK 17).
2. Let Gradle sync (Android Studio creates the Gradle wrapper jar for you).
3. Run on a device/emulator with Android 10+ (minSdk 29).

## Languages
Strings live in `res/values/strings.xml` (English) and `res/values-ar/strings.xml` (Arabic).
Change the language from **Profile → Language**; it is applied per-app via `AppCompatDelegate`.

## Notes / roadmap
* Export applies filters, title overlay, watermark (when the add-on is off), resolution, frame rate, bitrate and background music.
* Transitions are placed on the V2 track; rendering them into the exported file is on the roadmap.
* Voiceover recording (A1), text-layer editing and clip trimming are planned next.

---

## العربية
تطبيق أندرويد أصلي (Kotlin + Jetpack Compose) لمحرر فيديو احترافي بنفس تصميم Velocity المرجعي، بلغتين (English / العربية) وكل لغة بتنسيقها (LTR / RTL).
افتح المجلد في Android Studio، انتظر مزامنة Gradle ثم شغّل التطبيق. تغيير اللغة من **الملف الشخصي ← اللغة**.
