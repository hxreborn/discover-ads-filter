package eu.hxreborn.discoveradsfilter.ui.state

import androidx.compose.runtime.Immutable

@Immutable
enum class SymbolStatus {
    Mapped,
    Partial,
    NotFound,
    NotMapped,
}

@Immutable
data class SymbolRow(
    val name: String,
    val value: String?,
    val status: SymbolStatus,
)

@Immutable
data class SymbolSection(
    val title: String,
    val rows: List<SymbolRow>,
) {
    val resolvedCount: Int get() = rows.count { it.status == SymbolStatus.Mapped }

    val totalCount: Int get() = rows.size
}

fun VerifyUiState.toSymbolSections(): List<SymbolSection> {
    val targets = (lastResult as? VerifyResult.Success)?.targets
    return listOf(
        SymbolSection(
            "Classes",
            listOf(
                SymbolRow(
                    name = "Ad metadata",
                    value = targets?.adMetadataClass?.substringAfterLast('.'),
                    status = targets?.adMetadataClass.toSymbolStatus(),
                ),
                SymbolRow(
                    name = "Feed card",
                    value = targets?.feedCardClass?.substringAfterLast('.'),
                    status = targets?.feedCardClass.toSymbolStatus(),
                ),
            ),
        ),
        SymbolSection(
            "Fields",
            listOf(
                SymbolRow("Ad flag", targets?.adFlagFieldName, targets?.adFlagFieldName.toSymbolStatus()),
                SymbolRow("Ad label", targets?.adLabelFieldName, targets?.adLabelFieldName.toSymbolStatus()),
                SymbolRow("Ad metadata ref", targets?.adMetadataFieldName, targets?.adMetadataFieldName.toSymbolStatus()),
            ),
        ),
        SymbolSection(
            "Methods",
            listOf(
                SymbolRow(
                    name = "Card processors",
                    value = targets?.cardProcessorMethods?.takeIf { it.isNotEmpty() }?.let { "${it.size} methods" },
                    status =
                        if (!targets?.cardProcessorMethods.isNullOrEmpty()) {
                            SymbolStatus.Mapped
                        } else {
                            SymbolStatus.NotFound
                        },
                ),
                SymbolRow(
                    name = "Stream list",
                    value =
                        targets?.streamRenderableListMethod?.let {
                            "${it.className.substringAfterLast('.')}.${it.methodName}"
                        },
                    status =
                        if (targets?.streamRenderableListMethod != null) {
                            SymbolStatus.Mapped
                        } else {
                            SymbolStatus.NotFound
                        },
                ),
            ),
        ),
    )
}

private fun String?.toSymbolStatus(): SymbolStatus = if (this.isNullOrBlank()) SymbolStatus.NotFound else SymbolStatus.Mapped
