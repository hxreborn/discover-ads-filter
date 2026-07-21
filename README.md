<div align="center">

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="assets/banner_dark.png">
  <img src="assets/banner_light.png" alt="Discover Ads Filter">
</picture>

<p>An Xposed module that hides sponsored cards and ads from the Google Discover feed in the Pixel Launcher -1 screen and inside the Google app itself.</p>


![AGSA 17.14+](https://img.shields.io/badge/AGSA-17.14%2B-4285F4?style=flat-square&logo=google&logoColor=white)
![libxposed API 101](https://img.shields.io/badge/libxposed-API_101-ff69b4?style=flat-square)
![DexKit 2.2.0](https://img.shields.io/badge/DexKit-2.2.0-E65100?style=flat-square)

</div>

## Requirements

- Android 11+
- LSPosed manager with libxposed API 101 support

## Installation

1. Grab the APK:

    <a href="../../releases"><img src="https://github.com/user-attachments/assets/d18f850c-e4d2-4e00-8b03-3b0e87e90954" height="60" alt="GitHub Releases" /></a>
    <a href="https://f-droid.org/es/packages/eu.hxreborn.discoveradsfilter/"><img src=".github/assets/badge_fdroid.png" height="60" alt="Get it on F-Droid" /></a>

2. Enable the module in LSPosed and scope it to `com.google.android.googlequicksearchbox`.
3. Open the Discover Ads Filter app and tap Scan to resolve hook targets.
4. Force-stop Google App and relaunch.

## How It Works

The app scans the installed Google App with DexKit, resolving hook targets via protobuf extension field numbers and type signatures, and stores the result in a versioned cache. The hooked process uses the cached targets to filter ad items from the Discover feed.

## Related

Also dislike ads in the Google Play Store? Try <a href="https://github.com/hxreborn/playstore-adblock"><img src=".github/assets/playstore-adblock.png" height="16" alt=""> playstore-adblock</a>, a sister module.

## License

<a href="LICENSE"><img src=".github/assets/gplv3.svg" height="90" alt="GPLv3" /></a>

This project is licensed under the GNU General Public License v3.0. See [LICENSE](LICENSE) for details.
