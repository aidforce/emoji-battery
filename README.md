# Emoji Battery

Emoji Battery is a personal Android app that displays a Pokémon sticker and live battery percentage in an overlay near the status bar. It uses a debug APK for simple personal sideloading.

## Install from Releases

1. Open the repository's **Releases** page and download `EmojiBattery.apk` from a release.
2. On your phone, allow installation from unknown sources for the app you used to open the APK.
3. Install and open Emoji Battery.

## Overlay permission

The app needs Android's **Display over other apps** permission to show the battery overlay. Open the app, choose **Open permission settings**, enable the permission for Emoji Battery, then return to the app and enable the widget.

## Credits

The included Pokémon artwork comes from [PokeAPI/sprites](https://github.com/PokeAPI/sprites).

## Releases

Pushing a version tag builds the debug APK and publishes a GitHub Release with the asset named `EmojiBattery.apk`:

```bash
git tag v1.0.0
git push --tags
```

You can also start the same release workflow manually from the **Actions** tab and optionally provide a version.
