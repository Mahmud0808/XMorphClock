package com.drdisagree.xmorphclock.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.drdisagree.xmorphclock.di.AppContainer
import com.drdisagree.xmorphclock.domain.model.ClockGeometry
import com.drdisagree.xmorphclock.domain.model.FaceData
import com.drdisagree.xmorphclock.ui.about.AboutScreen
import com.drdisagree.xmorphclock.ui.components.DepthLayer
import com.drdisagree.xmorphclock.ui.components.LocalClockGeometry
import com.drdisagree.xmorphclock.ui.components.LocalDepth
import com.drdisagree.xmorphclock.ui.components.LocalFaceData
import com.drdisagree.xmorphclock.ui.components.LocalSampleFaceData
import com.drdisagree.xmorphclock.ui.components.LocalWallpaper
import com.drdisagree.xmorphclock.ui.editor.editorViewModelFactory
import com.drdisagree.xmorphclock.ui.home.HomeScreen
import com.drdisagree.xmorphclock.ui.home.homeViewModelFactory
import com.drdisagree.xmorphclock.ui.licenses.LicensesScreen
import com.drdisagree.xmorphclock.ui.licenses.licensesViewModelFactory
import com.drdisagree.xmorphclock.ui.onboarding.OnboardingScreen
import com.drdisagree.xmorphclock.ui.onboarding.onboardingViewModelFactory
import com.drdisagree.xmorphclock.ui.settings.SettingsScreen
import com.drdisagree.xmorphclock.ui.settings.settingsViewModelFactory
import com.drdisagree.xmorphclock.ui.status.StatusScreen
import com.drdisagree.xmorphclock.ui.status.statusViewModelFactory

private val SLIDE_DISTANCE = 30.dp

@Composable
fun AppNavigation(container: AppContainer, wallpaper: ImageBitmap?, depth: DepthLayer?, geometry: ClockGeometry, faceData: FaceData, sampleFaceData: FaceData) {
    val backStack = rememberNavBackStack(if (container.isOnboardingDone) HomeRoute else OnboardingRoute)

    fun reset(route: NavKey) {
        backStack.clear()
        backStack.add(route)
    }

    fun back() {
        backStack.removeLastOrNull()
    }

    val slide = with(LocalDensity.current) { SLIDE_DISTANCE.roundToPx() }

    CompositionLocalProvider(
        LocalWallpaper provides wallpaper,
        LocalDepth provides depth,
        LocalClockGeometry provides geometry,
        LocalFaceData provides faceData,
        LocalSampleFaceData provides sampleFaceData
    ) {
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.fillMaxSize(),
            onBack = { back() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            transitionSpec = { sharedAxisX(forward = true, slideDistance = slide) },
            popTransitionSpec = { sharedAxisX(forward = false, slideDistance = slide) },
            predictivePopTransitionSpec = { sharedAxisX(forward = false, slideDistance = slide) },
            entryProvider = entryProvider {
                entry<OnboardingRoute> {
                    OnboardingScreen(
                        viewModel = viewModel(factory = onboardingViewModelFactory(container)),
                        onFinished = { reset(HomeRoute) }
                    )
                }
                entry<HomeRoute> {
                    HomeScreen(
                        viewModel = viewModel(factory = homeViewModelFactory(container)),
                        editor = viewModel(factory = editorViewModelFactory(container)),
                        onOpenStatus = { backStack.add(StatusRoute) },
                        onOpenSettings = { backStack.add(SettingsRoute) },
                        onOpenAbout = { backStack.add(AboutRoute) }
                    )
                }
                entry<StatusRoute> {
                    StatusScreen(viewModel(factory = statusViewModelFactory(container)), onBack = { back() })
                }
                entry<SettingsRoute> {
                    SettingsScreen(
                        viewModel = viewModel(factory = settingsViewModelFactory(container)),
                        onBack = { back() }
                    )
                }
                entry<AboutRoute> {
                    AboutScreen(onBack = { back() }, onOpenLicenses = { backStack.add(LicensesRoute) })
                }
                entry<LicensesRoute> {
                    LicensesScreen(viewModel(factory = licensesViewModelFactory(container)), onBack = { back() })
                }
            }
        )
    }
}
