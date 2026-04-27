package eu.hxreborn.discoveradsfilter.ui.screen.preview

import eu.hxreborn.discoveradsfilter.BuildConfig
import eu.hxreborn.discoveradsfilter.discovery.MethodRef
import eu.hxreborn.discoveradsfilter.discovery.ResolvedTargets
import eu.hxreborn.discoveradsfilter.ui.state.ModuleStatus
import eu.hxreborn.discoveradsfilter.ui.state.VerifyResult
import eu.hxreborn.discoveradsfilter.ui.state.VerifyUiState

internal object PreviewFixtures {
    private const val AGSA_VERSION_CODE_AT_SCAN = 405_678_123L
    private const val AGSA_VERSION_NAME_AT_SCAN = "15.47.32.29"

    private const val AGSA_LAST_UPDATE_TIME = 1_710_000_000_000L

    val resolvedTargetsFull: ResolvedTargets.Resolved =
        ResolvedTargets.Resolved(
            adMetadataClass = "com.google.android.apps.search.model.AdMetadata",
            feedCardClass = "com.google.android.apps.search.feed.FeedCard",
            adFlagFieldName = "isAd",
            adLabelFieldName = "adLabel",
            adMetadataFieldName = "adMetadata",
            cardProcessorMethods =
                listOf(
                    MethodRef(
                        className = "com.google.android.apps.search.feed.CardProcessor",
                        methodName = "processCard",
                        paramTypeNames = listOf("com.google.android.apps.search.feed.FeedCard"),
                    ),
                    MethodRef(
                        className = "com.google.android.apps.search.feed.AdCardProcessor",
                        methodName = "process",
                        paramTypeNames =
                            listOf(
                                "com.google.android.apps.search.model.AdMetadata",
                                "int",
                            ),
                    ),
                ),
            streamRenderableListMethod =
                MethodRef(
                    className = "com.google.android.apps.search.feed.StreamAdapter",
                    methodName = "getRenderableList",
                    paramTypeNames = emptyList(),
                ),
        )

    fun verifySuccessFull(): VerifyUiState =
        VerifyUiState(
            lastResult =
                VerifyResult.Success(
                    versionCode = AGSA_VERSION_CODE_AT_SCAN,
                    targets = resolvedTargetsFull,
                ),
            installedAgsaVersion = AGSA_VERSION_CODE_AT_SCAN,
            installedAgsaVersionName = AGSA_VERSION_NAME_AT_SCAN,
            installedAgsaLastUpdateTime = AGSA_LAST_UPDATE_TIME,
            scanModuleVersion = BuildConfig.VERSION_CODE,
            adsHidden = 1_247L,
            moduleStatus = ModuleStatus.Active,
        )

    fun verifyFailureDexKitNoMatches(): VerifyUiState =
        VerifyUiState(
            lastResult =
                VerifyResult.Failure(
                    reason = "Signatures not resolved",
                    detail = "DexKit found 0 matches for 7 queries",
                ),
            moduleStatus = ModuleStatus.Active,
            installedAgsaVersion = AGSA_VERSION_CODE_AT_SCAN,
            installedAgsaVersionName = AGSA_VERSION_NAME_AT_SCAN,
        )

    fun verifyNeedsScan(): VerifyUiState =
        VerifyUiState(
            moduleStatus = ModuleStatus.Active,
            installedAgsaVersion = AGSA_VERSION_CODE_AT_SCAN,
            installedAgsaVersionName = AGSA_VERSION_NAME_AT_SCAN,
        )

    fun verifyModuleNotActive(): VerifyUiState = VerifyUiState(moduleStatus = ModuleStatus.Inactive)
}
