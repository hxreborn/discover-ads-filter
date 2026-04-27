<div align="center">

<img src="https://socialify.git.ci/hxreborn/discover-ads-filter/image?custom_description=Filter+ads+and+sponsored+content+from+Google+Discover+via+Xposed+framework.&description=1&font=Inter&issues=1&logo=https%3A%2F%2Fraw.githubusercontent.com%2Fhxreborn%2Fdiscover-ads-filter%2Frefs%2Fheads%2Fmain%2Fartwork%2Ficon.png&name=1&owner=1&pattern=Brick+Wall&stargazers=1&theme=Auto" alt="discover-ads-filter" width="640" height="320" />

<p>An Xposed module that hides sponsored cards and ads from the Google Discover feed in the Pixel Launcher -1 screen and inside the Google app itself. Uses DexKit to dynamically resolve obfuscated targets.</p>


![AGSA 17.14+](https://img.shields.io/badge/AGSA-17.14%2B-4285F4?style=flat&logo=google&logoColor=white)
![libxposed API 101](https://img.shields.io/badge/libxposed-API_101-ff69b4?style=flat)
![DexKit 2.2.0](https://img.shields.io/badge/DexKit-2.2.0-E65100?style=flat)

</div>

## Requirements

- Android 11+
- LSPosed manager with libxposed API 101 support

## Installation

1. Grab the APK:

    <a href="../../releases"><img src="https://github.com/user-attachments/assets/d18f850c-e4d2-4e00-8b03-3b0e87e90954" height="60" alt="GitHub Releases" /></a>
    <a href="http://apps.obtainium.imranr.dev/redirect.html?r=obtainium://app/%7B%22id%22%3A%22eu.hxreborn.discoveradsfilter%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fhxreborn%2Fdiscover-ads-filter%22%2C%22author%22%3A%22rafareborn%22%2C%22name%22%3A%22Discover%20Ads%20Filter%22%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Afalse%7D%22%7D"><img src="https://github.com/user-attachments/assets/dffe8fb9-c0d1-470b-8d69-6d5b38a8aa2d" height="60" alt="Obtainium" /></a>

2. Enable the module in LSPosed and scope it to `com.google.android.googlequicksearchbox`.
3. Open the Discover Ads Filter app and tap Scan to resolve hook targets.
4. Force-stop Google App and relaunch.

## How It Works

The app scans the installed Google App with DexKit, resolving hook targets via protobuf extension field numbers and type signatures, and stores the result in a versioned cache. The hooked process uses the cached targets to filter ad items from the Discover feed.

## License

<a href="LICENSE"><img src=".github/assets/gplv3.svg" height="90" alt="GPLv3" /></a>

This project is licensed under the GNU General Public License v3.0. See [LICENSE](LICENSE) for details.
