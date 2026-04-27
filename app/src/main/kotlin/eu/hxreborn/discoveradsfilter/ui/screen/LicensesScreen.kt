package eu.hxreborn.discoveradsfilter.ui.screen

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import eu.hxreborn.discoveradsfilter.R
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val libs by produceLibraries()
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

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.about_licenses),
                        style =
                            if (scrollBehavior.state.collapsedFraction < 0.5f) {
                                MaterialTheme.typography.headlineLarge
                            } else {
                                MaterialTheme.typography.titleLarge
                            },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        LibrariesContainer(
            libraries = filtered,
            modifier = Modifier.fillMaxSize().padding(innerPadding).drawVerticalScrollbar(listState),
            lazyListState = listState,
        )
    }
}
