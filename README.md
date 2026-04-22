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

The app scans the installed Google App, caches the hook data for that AGSA build by version, and uses that cache in the hooked AGSA process to filter ad items out of the Discover feed list before the UI renders them.

## Notes

- The module resolves the AGSA classes and methods it hooks with DexKit against the installed Google App build, using protobuf extension field numbers and type signatures.
- Google App updates can invalidate resolved targets. Run Verify again after each update.
- DexKit resolves targets against the installed Google App build from protobuf field numbers and type signatures instead of relying on stable symbol names.

## License

<a href="LICENSE"><img src=".github/assets/gplv3.svg" height="90" alt="GPLv3" /></a>

This project is licensed under the GNU General Public License v3.0. See [LICENSE](LICENSE) for details.
