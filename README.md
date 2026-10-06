# Glyphsmith

Turn any image into **text art** — an Android app with three modes: **Chinese**, **ASCII** and **Color**.

English | [简体中文](README_ch.md)

![Glyphsmith_1](https://github.com/LANZHOU-1/glyphsmith-android/blob/main/Glyphsmith_1.png)

![Glyphsmith_2](https://github.com/LANZHOU-1/glyphsmith-android/blob/main/Glyphsmith_2.png)

![Glyphsmith_3](https://github.com/LANZHOU-1/glyphsmith-android/blob/main/Glyphsmith_3.png)

> Bright areas stay blank or use light glyphs, dark areas use dense ones — the picture is literally built out of characters.

## Features

- **Three modes**: Chinese (`丶一十口日目田回無疆龘` ramp), ASCII (` .:-=+*#%@`) and Color (each glyph keeps the pixel color of the source image)
- **Two color styles**: glyph (colored ASCII characters) / block (pixel-art mosaic)
- **Adjustable**: width 30–240 characters, contrast 1.0–3.0, preview font size, black/white background, auto levels, invert for dark backgrounds
- **Export & share**: copy to clipboard, save TXT, save a colored HTML page, save PNG, system share (text / PNG)
- **PNG export**: drawn cell by cell with `Canvas` (not a screenshot); the long edge is capped at 4096 px to avoid OOM, block style tiles seamlessly
- **Built-in sample image** — the effect shows up right after launch; fully offline, **no permissions requested**
- Material Design 3, dynamic color on Android 12+, page/mode transitions and a cross-fading preview, parameters survive rotation

About 1.9 MB, supported on Android 9 (API 28) and above.

## How it works

1. Resize the image to “columns × rows” — glyph aspect ratio 1:1 for Chinese mode, 1:2 for ASCII / Color, so the result is not stretched
2. Per-pixel luminance `0.2126R + 0.7152G + 0.0722B`; optional **auto levels** (histogram stretch with a 1% cutoff) and **contrast boost**
3. Map luminance to a glyph ramp: bright → blank or light glyphs, dark → dense ones (the Chinese ramp runs from `丶` to `龘`)
4. Color mode skips the ramp lookup and paints each glyph with its pixel color
5. PNG export draws every cell with `Canvas` (square cells for Chinese / block style, 0.6× width for ASCII) and scales the whole image down when the long edge exceeds 4096 px

## Third-party components

| Component | Purpose | License |
| --- | --- | --- |
| Jetpack Compose / AndroidX (core-ktx, activity, lifecycle) | UI and platform basics | Apache-2.0 |
| Material 3 + Material Icons Extended | Components and icons | Apache-2.0 |
| Kotlin stdlib / Gradle / Android Gradle Plugin | Build | Apache-2.0 |

No other runtime dependencies.

## Install

1. Download `Glyphsmith-1.1.apk` from Releases and open it on your phone
2. If the system warns about “unknown sources”, allow this installation
3. A previously installed build signed with a **different key** has to be uninstalled first

## Build

Requirements: JDK 21, Android SDK 36, Gradle 9.7.1 (this repo ships no Gradle Wrapper — use your own Gradle).

Release signing is read from `keystore.properties` in the project root (that file and `*.jks` are git-ignored and are not distributed with this repository):

```
storeFile=keystore.jks
storePassword=your-store-password
keyAlias=your-key-alias
keyPassword=your-key-password
```

Without this file the build still **succeeds**; it simply produces an unsigned release APK. To generate your own key:

```powershell
keytool -genkeypair -v -keystore keystore.jks -alias mykey -keyalg RSA -keysize 2048 -validity 10000
```

Build command and output:

```powershell
gradle assembleRelease
# output: app/build/outputs/apk/release/app-release.apk
```

You can also open the project in Android Studio and press Run (the SDK path is configured automatically).

## Project structure

```
app/src/main/java/com/lanzhou/zj/
├── MainActivity.kt   UI and interaction (Jetpack Compose)
├── Converter.kt      Conversion: grayscale → glyph ramp / color
├── PngExport.kt      PNG export: per-cell Canvas rendering
├── ArtViewModel.kt   Parameters and state
└── Theme.kt          Material 3 theme and dynamic color
app/src/main/res/     icons, themes, values-night (dark), file_paths (sharing)
```

## Author

- 蓝昼 (lanzhou)
- Website: <https://lanzhou-1.github.io>