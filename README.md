# Google Discover Ads Filter

Hide sponsored cards in Google Discover.

![Android 11+](https://img.shields.io/badge/Android-11%2B-3DDC84?style=flat&logo=android&logoColor=white)
![AGSA 17.14+](https://img.shields.io/badge/AGSA-17.14%2B-4285F4?style=flat&logo=google&logoColor=white)
![libxposed API 101](https://img.shields.io/badge/libxposed-API_101-ff69b4?style=flat)
![DexKit 2.2.0](https://img.shields.io/badge/DexKit-2.2.0-E65100?style=flat)

## Requirements

- Android 11+
- LSPosed manager with libxposed API 101 support

## Installation

1. Download the APK from [Releases](../../releases)

2. Enable the module in LSPosed.
3. Scope it to Google App (`com.google.android.googlequicksearchbox`).

## Usage

1. Open the app and run a scan.
2. Force stop Google App or reboot.
3. Run the scan again after each Google App update.

## How It Works

The app scans the installed Google App with DexKit, resolving hook targets via protobuf extension field numbers and type signatures, and stores the result in a versioned cache. The hooked process uses the cached targets to filter ad items from the Discover feed.

## License

<a href="LICENSE"><img src=".github/assets/gplv3.svg" height="90" alt="GPLv3" /></a>

This project is licensed under the GNU General Public License v3.0. See [LICENSE](LICENSE) for details.
