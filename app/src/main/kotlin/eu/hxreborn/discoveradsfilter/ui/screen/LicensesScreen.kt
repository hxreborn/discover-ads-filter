package eu.hxreborn.discoveradsfilter.ui.screen

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import eu.hxreborn.discoveradsfilter.R
import eu.hxreborn.discoveradsfilter.ui.components.DetailScaffold
import eu.hxreborn.discoveradsfilter.ui.components.centeredContentWidth
import eu.hxreborn.discoveradsfilter.ui.util.drawVerticalScrollbar

private val DIRECT_DEPENDENCY_GROUPS =
    setOf(
        "io.github.libxposed",
        "org.luckypray",
        "me.zhanghai.compose.preference",
        "org.jetbrains.kotlinx",
        "com.mikepenz",
        "androidx.compose",
        "androidx.compose.material3",
        "androidx.compose.material",
        "androidx.compose.ui",
        "androidx.core",
        "androidx.activity",
        "androidx.lifecycle",
        "androidx.navigation3",
    )

@Composable
fun LicensesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val libs by produceLibraries(R.raw.aboutlibraries)
    val listState = rememberLazyListState()

    val filtered =
        remember(libs) {
            libs?.let { l ->
                val directLibs =
                    l.libraries.filter { lib ->
                        DIRECT_DEPENDENCY_GROUPS.any { lib.uniqueId.startsWith(it) }
                    }
                Libs(
                    libraries = directLibs,
                    licenses = l.licenses,
                )
            }
        }

    DetailScaffold(
        title = stringResource(R.string.about_licenses),
        onBack = onBack,
    ) { contentPadding ->
        LibrariesContainer(
            libraries = filtered,
            modifier = modifier.centeredContentWidth().drawVerticalScrollbar(listState),
            lazyListState = listState,
            contentPadding = contentPadding,
        )
    }
}
