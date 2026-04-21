# Google Discover Ads Filter

Hide sponsored cards in Google Discover.

## Requirements

- Android 11+ (API 30+)
- LSPosed manager with libxposed API 101 support

## Install

1. Install the APK.
2. Enable the module in LSPosed.
3. Scope it to Google App (`com.google.android.googlequicksearchbox`).
4. Open the app and run a scan after each Google App update.
5. Force stop Google App or reboot.

## How it works

- The app scans the installed Google App with DexKit.
- It stores the resolved hook targets by AGSA version.
- The hook process reads that cache and filters Discover cards before they render.

## License

GPL-3.0. See [`LICENSE`](LICENSE) for details.
