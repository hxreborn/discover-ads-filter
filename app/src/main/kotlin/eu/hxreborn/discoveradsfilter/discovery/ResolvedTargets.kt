package eu.hxreborn.discoveradsfilter.discovery

sealed interface ResolvedTargets {
    data class Resolved(
        val streamRenderableListMethod: MethodRef,
    ) : ResolvedTargets

    data class Missing(
        val reason: String,
        val retryable: Boolean = false,
    ) : ResolvedTargets
}
