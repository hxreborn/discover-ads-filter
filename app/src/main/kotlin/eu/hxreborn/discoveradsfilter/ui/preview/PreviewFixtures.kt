package eu.hxreborn.discoveradsfilter.ui.preview

import eu.hxreborn.discoveradsfilter.BuildConfig
import eu.hxreborn.discoveradsfilter.discovery.MethodRef
import eu.hxreborn.discoveradsfilter.discovery.ResolvedTargets
import eu.hxreborn.discoveradsfilter.ui.state.HookStatus
import eu.hxreborn.discoveradsfilter.ui.state.ModuleStatus
import eu.hxreborn.discoveradsfilter.ui.state.SymbolRow
import eu.hxreborn.discoveradsfilter.ui.state.SymbolSection
import eu.hxreborn.discoveradsfilter.ui.state.SymbolStatus
import eu.hxreborn.discoveradsfilter.ui.state.VerifyPhase
import eu.hxreborn.discoveradsfilter.ui.state.VerifyResult
import eu.hxreborn.discoveradsfilter.ui.state.VerifyUiState
import eu.hxreborn.discoveradsfilter.ui.state.toSymbolSections

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

    val resolvedTargetsNone: ResolvedTargets.Resolved = ResolvedTargets.Resolved()

    fun allMappedSections(): List<SymbolSection> = verifySuccessFull().toSymbolSections()

    fun mixedSections(): List<SymbolSection> =
        listOf(
            SymbolSection(
                title = "Classes",
                rows =
                    listOf(
                        SymbolRow("AdMetadata", "fwm1", SymbolStatus.Mapped),
                        SymbolRow("FeedCard", "fwrv", SymbolStatus.Mapped),
                        SymbolRow("PromoUnit", null, SymbolStatus.NotFound),
                    ),
            ),
            SymbolSection(
                title = "Fields",
                rows =
                    listOf(
                        SymbolRow("isAd", "e", SymbolStatus.Mapped),
                        SymbolRow("adLabel", "d", SymbolStatus.Mapped),
                        SymbolRow("adMetadata", null, SymbolStatus.NotMapped),
                        SymbolRow("trackingToken", null, SymbolStatus.Partial),
                    ),
            ),
            SymbolSection(
                title = "Methods",
                rows =
                    listOf(
                        SymbolRow("Card processors", "17 methods", SymbolStatus.Mapped),
                        SymbolRow("Stream list", "bzat.a", SymbolStatus.Mapped),
                    ),
            ),
        )

    fun longValueSections(): List<SymbolSection> =
        listOf(
            SymbolSection(
                title = "Classes",
                rows =
                    listOf(
                        SymbolRow(
                            "AdMetadataCandidateWithVeryLongDiagnosticName",
                            "com.google.android.apps.search.feed.rendering.obfuscated.LongClassName",
                            SymbolStatus.Mapped,
                        ),
                        SymbolRow("FeedCard", "fwrv", SymbolStatus.Mapped),
                    ),
            ),
            SymbolSection(
                title = "Fields",
                rows =
                    listOf(
                        SymbolRow("trackingTokenResolverCandidate", "obfuscated_field_with_extra_suffix", SymbolStatus.Mapped),
                        SymbolRow("adLabel", "d", SymbolStatus.Mapped),
                    ),
            ),
            SymbolSection(
                title = "Methods",
                rows =
                    listOf(
                        SymbolRow("Card processors", "17 methods", SymbolStatus.Mapped),
                        SymbolRow("StreamRenderableSliceAssembler", "bzat.superLongMethodName", SymbolStatus.Mapped),
                    ),
            ),
        )

    fun zeroResolvedSections(): List<SymbolSection> =
        listOf(
            SymbolSection(
                title = "Classes",
                rows =
                    listOf(
                        SymbolRow("AdMetadata", null, SymbolStatus.NotFound),
                        SymbolRow("FeedCard", null, SymbolStatus.NotFound),
                    ),
            ),
            SymbolSection(
                title = "Fields",
                rows =
                    listOf(
                        SymbolRow("isAd", null, SymbolStatus.NotMapped),
                        SymbolRow("adLabel", null, SymbolStatus.NotMapped),
                        SymbolRow("adMetadata", null, SymbolStatus.NotFound),
                    ),
            ),
            SymbolSection(
                title = "Methods",
                rows =
                    listOf(
                        SymbolRow("Card processors", null, SymbolStatus.Partial),
                        SymbolRow("Stream list", null, SymbolStatus.NotFound),
                    ),
            ),
        )

    fun verifyNotScanned(): VerifyUiState = VerifyUiState()

    fun verifyRunning(): VerifyUiState = VerifyUiState(phase = VerifyPhase.Running)

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
            hookStatus = HookStatus(5, 5),
            hookProcess = "com.google.android.googlequicksearchbox",
            adsHidden = 1_247L,
            moduleStatus = ModuleStatus.Active,
        )

    fun verifyNoTargetsResolved(): VerifyUiState =
        VerifyUiState(
            lastResult =
                VerifyResult.Success(
                    versionCode = AGSA_VERSION_CODE_AT_SCAN,
                    targets = resolvedTargetsNone,
                ),
            installedAgsaVersion = AGSA_VERSION_CODE_AT_SCAN,
            installedAgsaVersionName = AGSA_VERSION_NAME_AT_SCAN,
            hookStatus = HookStatus(0, 5),
            adsHidden = 0L,
        )

    fun verifyFailureDexKitNoMatches(): VerifyUiState =
        VerifyUiState(
            lastResult =
                VerifyResult.Failure(
                    reason = "Signatures not resolved",
                    detail = "DexKit found 0 matches for 7 queries",
                ),
        )

    fun verifyStaleModuleUpdated(): VerifyUiState =
        verifySuccessFull().copy(
            scanModuleVersion = BuildConfig.VERSION_CODE - 1,
        )

    fun verifyModuleNotActive(): VerifyUiState = VerifyUiState(moduleStatus = ModuleStatus.Inactive)
}
