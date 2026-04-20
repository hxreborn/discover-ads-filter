# Google Discover Ads Filter

Remove sponsored cards from Google Discover feed.

<p align="center">
  <a href="https://developer.android.com">
    <img
      src="https://img.shields.io/badge/Android-11%2B-3DDC84?style=flat&logo=android&logoColor=white"
      alt="Android 11+"
    />
  </a>
  <a href="https://play.google.com/store/apps/details?id=com.google.android.googlequicksearchbox">
    <img
      src="https://img.shields.io/badge/AGSA-17.14%2B-4285F4?style=flat&logo=google&logoColor=white"
      alt="AGSA 17.14+"
    />
  </a>
  <img
    src="https://img.shields.io/badge/libxposed-101.0.0-ff69b4?style=flat"
    alt="libxposed 101.0.0"
  />
  <img
    src="https://img.shields.io/badge/DexKit-2.2.0-E65100?style=flat"
    alt="DexKit 2.2.0"
  />
  <a href="LICENSE">
    <img
      src="https://img.shields.io/badge/License-GPL--3.0-CC0000?style=flat&logo=gnu&logoColor=white"
      alt="GPL-3.0"
    />
  </a>
</p>

## Requirements
- Android 11+ (API 30+)
- LSPosed manager with libxposed API 101 support

## Install
1. Install the APK.
2. Enable the module in LSPosed.
3. Scope it to Google App (`com.google.android.googlequicksearchbox`).
4. Force stop Google App or reboot.

## How it works

Hook targets are resolved with DexKit against the installed Google App build using protobuf wire-format field numbers and type signatures. Re-resolved automatically after each update.

## License
GPL-3.0. See [`LICENSE`](LICENSE) for details.
