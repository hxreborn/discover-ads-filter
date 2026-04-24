
## What's Changed



### Added

- *(metrics)* Replace ContentProvider with libsu file reads and add reset counter - [`7ffdaaa`](https://github.com/hxreborn/discover-ads-filter/commit/7ffdaaaee56c4fb19384f1b449b61637a3b75c8f) by @hxreborn

- *(metrics)* Report ad counts via ContentProvider instead of file reads - [`be76b93`](https://github.com/hxreborn/discover-ads-filter/commit/be76b93f92a3b8eb0802929c02b154d57b8e196a) by @hxreborn

- *(provider)* Add MetricsProvider ContentProvider - [`cb848ea`](https://github.com/hxreborn/discover-ads-filter/commit/cb848ea1acf96a34e2937273260430243a4bfb95) by @hxreborn

- *(ui)* Refine diagnostics copy and confirmations - [`412ce5a`](https://github.com/hxreborn/discover-ads-filter/commit/412ce5a01273ea3e77420a126a9c4c599dc503c4) by @hxreborn

- *(ui)* Redesign diagnostics screen with M3 hierarchy and fix libxposed service version - [`7d0a821`](https://github.com/hxreborn/discover-ads-filter/commit/7d0a821bb44e24e275e28b9d167c94a765758f89) by @hxreborn

- *(ui)* Blur dashboard content behind startup scan overlay - [`1c736e6`](https://github.com/hxreborn/discover-ads-filter/commit/1c736e6a2b381327adbb33269d2d7a5138c2eeb0) by @hxreborn

- *(ui)* Redesign status card, fix diag layout and version display - [`85a83c7`](https://github.com/hxreborn/discover-ads-filter/commit/85a83c70b9a1ad716436b82dacafe5e82a6e7c54) by @hxreborn

  Module Active replaces HookCoverage enum as primary status signal.
  StatusCard now shows installed AGSA version + hook ratio when active,
  and setup instructions when module is not active.

  > DiagnosticsScreen:show installed AGSA version instead of fingerprinted,
always show current module version, fix NotFound badge overflow (removed
20dp fixed width from StatusBadge Box).

- *(ui)* Restore clear cache with confirm, simplify diag FAB, fix lib icon - [`70aa097`](https://github.com/hxreborn/discover-ads-filter/commit/70aa0977a5260be2f4724ceb73abbe4bf4dec21b) by @hxreborn

- *(ui)* Expand diagnostics FAB with sub-actions and swap wavy indicators - [`e8cda42`](https://github.com/hxreborn/discover-ads-filter/commit/e8cda42c60d2a5860f232b73b357eade39b280d7) by @hxreborn

- *(ui)* Show dismissible dialog after startup scan completes - [`9cf6e35`](https://github.com/hxreborn/discover-ads-filter/commit/9cf6e35930b6cd472d7feb0d9b6f706ab2fc333f) by @hxreborn

- *(ui)* Add module activation detection, clear cache, about credits - [`7b58f05`](https://github.com/hxreborn/discover-ads-filter/commit/7b58f05a8a6bc078dd7fd0835fae53dce1be68ee) by @hxreborn

- *(ui)* Redesign verification flow - [`3e81f6f`](https://github.com/hxreborn/discover-ads-filter/commit/3e81f6fbebb0312fdcaab6c7325b0ece24e93c50) by @hxreborn

- *(ui)* Wire splash screen to loading state - [`c3a0814`](https://github.com/hxreborn/discover-ads-filter/commit/c3a0814f42b33123f1afabe3eb4d4a9ebfcdeae8) by @hxreborn

- *(ui)* Grouped preference items and DexKit symbol map - [`df94af1`](https://github.com/hxreborn/discover-ads-filter/commit/df94af160b5328fef9e3b999c29b5c8e95e637d2) by @hxreborn


### Fixed

- *(hook)* Remove homestack from ad tokens, deduplicate ad counts, derive target total - [`0af0012`](https://github.com/hxreborn/discover-ads-filter/commit/0af00124a07529400c498248d3e01da58042d366) by @hxreborn

- *(ui)* Show startup scan card over dashboard with scrim instead of covering it - [`bdc1b00`](https://github.com/hxreborn/discover-ads-filter/commit/bdc1b00e52683e1a3d897c1569bd321df3e09418) by @hxreborn

- *(ui)* Show all steps and pass durationMs to startup scan card - [`5f1e980`](https://github.com/hxreborn/discover-ads-filter/commit/5f1e98019c0f56865fc2b6cfa10ca962b3ffcd86) by @hxreborn

- *(ui)* Replace startup scan AlertDialog with dismiss button in progress card - [`5264dc4`](https://github.com/hxreborn/discover-ads-filter/commit/5264dc4fa837a7a246d3ccb0834df41e0e6da4db) by @hxreborn

- *(ui)* Restore not-configured state when lastResult is null - [`1f6d5ef`](https://github.com/hxreborn/discover-ads-filter/commit/1f6d5efa2b00cc69c18713b7712844374346cf55) by @hxreborn

- *(ui)* Guard module-not-active on checked flag, hide hook line when unknown - [`96d4b42`](https://github.com/hxreborn/discover-ads-filter/commit/96d4b42b6886eb2564f75f833999e6189fb3ee5f) by @hxreborn

- *(ui)* Restore wavy indicators for card bar and active step row - [`d090cb5`](https://github.com/hxreborn/discover-ads-filter/commit/d090cb5d4619022e8931e00a938bdaedca028ad5) by @hxreborn

- *(ui)* Improve verify card contrast - [`2a0c371`](https://github.com/hxreborn/discover-ads-filter/commit/2a0c3718c934579f3c4ab49e38bdcdba8cda46ec) by @hxreborn

- Start moduleStatus as Unknown, not Inactive - [`ba69cb0`](https://github.com/hxreborn/discover-ads-filter/commit/ba69cb0efcb989ef96fb5e09b5d90f09be066627) by @hxreborn

- Remove duplicate ad log and drop unused total variable - [`0d37e39`](https://github.com/hxreborn/discover-ads-filter/commit/0d37e39b0e39eb3437533682947f7de36a055d03) by @hxreborn

- Defer hook status reporting, add reset counter, fix splash race - [`98c9872`](https://github.com/hxreborn/discover-ads-filter/commit/98c9872f00a0f5b28838d5135e62e2b08d49ada5) by @hxreborn

- Tighten metrics provider and clean dead strings - [`00752d0`](https://github.com/hxreborn/discover-ads-filter/commit/00752d01009a003a95f7fd677a2fd2b0be4b7900) by @hxreborn


### Performance Improvements

- *(hook)* Eliminate reflection churn and single-pass filter - [`0727586`](https://github.com/hxreborn/discover-ads-filter/commit/0727586b3f06f616aba86b2d4047a4e8f972b65f) by @hxreborn

- *(ui)* Extract DiagnosticsFab, memoize symbol sections, fix scaffold padding - [`ea9f752`](https://github.com/hxreborn/discover-ads-filter/commit/ea9f752c7c2c1931e842e25a9f27d48f0b1e7840) by @hxreborn


### Changed

- *(discovery)* Score-based candidate selection in DexKitResolver - [`da01be4`](https://github.com/hxreborn/discover-ads-filter/commit/da01be4d9847462e43ab24b51197b782d8078c76) by @hxreborn

- *(discovery)* Rename FingerprintCache to DexKitCache - [`4c74028`](https://github.com/hxreborn/discover-ads-filter/commit/4c74028a1b8e3b64bdc26e780cfd827c88185173) by @hxreborn

- *(hook)* Remove DexKit fallback, snapshot cache, and stableItemKey fallback - [`a87e7bd`](https://github.com/hxreborn/discover-ads-filter/commit/a87e7bd5d706324cd1d08b2449c6e6dde611a17d) by @hxreborn

- *(hook)* Remove dead mutable state - [`42eb745`](https://github.com/hxreborn/discover-ads-filter/commit/42eb745bc20014f14922f6d4b67ae768c31688e5) by @hxreborn

- *(metrics)* Single-source ad counter through app prefs - [`e532ac1`](https://github.com/hxreborn/discover-ads-filter/commit/e532ac1ad87daf9a4532480a4c1deba8a233827d) by @hxreborn

- *(prefs)* Replace sealed class hierarchy with factory functions - [`26b8cbc`](https://github.com/hxreborn/discover-ads-filter/commit/26b8cbc38f1f2eb903bc8334bc9c578c740fc4ad) by @hxreborn

- *(provider)* Replace context!! with requireNotNull - [`291be51`](https://github.com/hxreborn/discover-ads-filter/commit/291be518db1194e4e4888fbb52374e8810986628) by @hxreborn

- *(ui)* Split HomeUiState, fix naming and insets - [`d02c315`](https://github.com/hxreborn/discover-ads-filter/commit/d02c31585300c5f4a6bb61823a6113e8e5e36aac) by @hxreborn

- *(ui)* Split DashboardScreenContent into composable helpers - [`0effaf6`](https://github.com/hxreborn/discover-ads-filter/commit/0effaf694dad7b01a943ff51502b2f5f375ef366) by @hxreborn

- *(ui)* Consolidate previews with PreviewParameterProvider - [`327d5c6`](https://github.com/hxreborn/discover-ads-filter/commit/327d5c63ca7ba1d803478ac2164b8e7274d3996f) by @hxreborn

- *(ui)* Move scan step labels to string resources - [`4f94b0c`](https://github.com/hxreborn/discover-ads-filter/commit/4f94b0cb318aa6b1bd4915b3ed8c620db98f3ebe) by @hxreborn

- *(ui)* Simplify compose previews - [`3d10b85`](https://github.com/hxreborn/discover-ads-filter/commit/3d10b8578bda665cebc4c5c9739e6cc172a3cb94) by @hxreborn

- *(ui)* Trim diagnostics screen redundancy and improve copy - [`f9e012f`](https://github.com/hxreborn/discover-ads-filter/commit/f9e012fdb0e329cd9493abde884abe2f1d9e6294) by @hxreborn

- *(ui)* Explicit ViewModel ownership with Factory - [`ad0bf4c`](https://github.com/hxreborn/discover-ads-filter/commit/ad0bf4cf9d3f2dcf1916c9757227c1b2ae4f4c9d) by @hxreborn

- *(ui)* Remove InfoCard, PreferencesCard, AboutFooter - [`5061660`](https://github.com/hxreborn/discover-ads-filter/commit/5061660a8278861bf60f5b650250a2d414af8696) by @hxreborn

- *(ui)* Remove KnownGood state from ViewModel - [`32494fd`](https://github.com/hxreborn/discover-ads-filter/commit/32494fd207eb0af31a4e74659ae7394ccb82bd59) by @hxreborn

- *(ui)* Remove VerifyCard - [`56cab12`](https://github.com/hxreborn/discover-ads-filter/commit/56cab12640f3e46cfc3726b3c746405980fb5d94) by @hxreborn

- *(viewmodel)* Remove nullable scratch variable from scan - [`0c4400f`](https://github.com/hxreborn/discover-ads-filter/commit/0c4400f08ad7def46733feb1be33f65c24d5d234) by @hxreborn

- *(vm)* Replace timeout with deterministic service check - [`45257db`](https://github.com/hxreborn/discover-ads-filter/commit/45257db201268e2a7bad2210ecbe48d92479914b) by @hxreborn

- *(vm)* Restore scan logs as unconditional Log.d - [`bde451a`](https://github.com/hxreborn/discover-ads-filter/commit/bde451acdc65db18a0c12ba501eac3b9485d01cd) by @hxreborn

- *(vm)* Remove verbose-gated Log.d calls from module-app scan path - [`6d6e4b6`](https://github.com/hxreborn/discover-ads-filter/commit/6d6e4b6f10b31c8580411e4708fbb7bffbb5f762) by @hxreborn

- Extract ViewModel helpers, combine hook metrics into single read - [`b0de1b7`](https://github.com/hxreborn/discover-ads-filter/commit/b0de1b7431e4468bdbeca35b6e2a8936b80f2d95) by @hxreborn

- Flatten control flow and deduplicate state transitions - [`05505b3`](https://github.com/hxreborn/discover-ads-filter/commit/05505b308be3e15c92129cdce123a4bc07453a48) by @hxreborn

- Flatten control flow and deduplicate state transitions - [`0325496`](https://github.com/hxreborn/discover-ads-filter/commit/0325496be0e38bfe75b2e1dc8b10340504ad90be) by @hxreborn

- Flatten control flow and deduplicate state transitions - [`aea4e29`](https://github.com/hxreborn/discover-ads-filter/commit/aea4e29730fbdaf44c1700be96e67ac59163c307) by @hxreborn

- Remove fingerprintCurrent fallback triple and flatten agsaPainter if/else - [`b54a856`](https://github.com/hxreborn/discover-ads-filter/commit/b54a85630e1872b798a9c347045ca379104a0521) by @hxreborn

- Use idiomatic Kotlin patterns - [`d98b3a8`](https://github.com/hxreborn/discover-ads-filter/commit/d98b3a8c6830814cbc35f017140f399c06fa1af1) by @hxreborn

- Remove KnownGoodRegistry - [`d1927dd`](https://github.com/hxreborn/discover-ads-filter/commit/d1927ddef57abbe4b9a5b092cf05316e4833e18b) by @hxreborn


### Other

- *(res)* Update strings - [`84ac1e2`](https://github.com/hxreborn/discover-ads-filter/commit/84ac1e23ba7b7bdae03896d14993e38da527ad71) by @hxreborn

- *(tools)* Add score-based sorting to DexKitProbe - [`c7deb56`](https://github.com/hxreborn/discover-ads-filter/commit/c7deb568fa1228698d5860bcaf7f772a1fa6c1fc) by @hxreborn

- *(ui)* Remove dead theme code - [`51d40a9`](https://github.com/hxreborn/discover-ads-filter/commit/51d40a920ac5eb90cc11e72912b993f752113bf7) by @hxreborn

- Remove dead code and suppress false-positive IDE warnings - [`d646aa7`](https://github.com/hxreborn/discover-ads-filter/commit/d646aa71122169ad31fa101773cc6448344e77f3) by @hxreborn

- Add Discover ad filter module - [`9a5f866`](https://github.com/hxreborn/discover-ads-filter/commit/9a5f866517580d5d7e6e434f1af1255857e8913a) by @hxreborn


### New Contributors

* @dependabot[bot] made their first contribution in [#1](https://github.com/hxreborn/discover-ads-filter/pull/1)


