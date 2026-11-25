package com.pedro.baruck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pedro.baruck.ui.BaruckViewModel
import com.pedro.baruck.ui.ProfileState
import com.pedro.baruck.ui.Sfx
import com.pedro.baruck.ui.components.LevelUpDialog
import com.pedro.baruck.ui.components.XpToast
import com.pedro.baruck.ui.screens.*
import com.pedro.baruck.ui.theme.BaruckColors
import com.pedro.baruck.ui.theme.BaruckTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.WHITE, android.graphics.Color.WHITE),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.WHITE, android.graphics.Color.WHITE)
        )
        Sfx.init(this)
        val crashFile = File(filesDir, BaruckApp.CRASH_FILE)
        setContent {
            BaruckTheme {
                var crash by remember { mutableStateOf(crashFile.takeIf { it.exists() }?.readText()) }
                val shown = crash
                if (shown != null) CrashScreen(shown) { crashFile.delete(); crash = null }
                else AppRoot()
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val vm: BaruckViewModel = viewModel()
    val state by vm.profileState.collectAsState()
    val screen = when {
        state is ProfileState.Ready -> "main"
        state is ProfileState.Loading -> "loading"
        state is ProfileState.Missing && vm.busy -> "saving"
        state is ProfileState.Missing -> "onboardIn"
        else -> "onboardOut"
    }
    val snackHost = remember { SnackbarHostState() }
    LaunchedEffect(vm.snack) {
        val m = vm.snack ?: return@LaunchedEffect
        snackHost.showSnackbar(m)
        vm.snack = null
    }

    Box(Modifier.fillMaxSize().background(Color.White)) {
        AnimatedContent(
            targetState = screen,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
            label = "root"
        ) { s ->
            when (s) {
                "main" -> MainNav(vm)
                "loading" -> LoadingScreen()
                "saving" -> LoadingScreen("Preparando sua lavoura...")
                "onboardIn" -> OnboardingScreen(vm, signedIn = true)
                else -> OnboardingScreen(vm, signedIn = false)
            }
        }
        XpToast(vm.xpToast, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 12.dp)) {
            vm.xpToast = null
        }
        SnackbarHost(snackHost, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 88.dp)) {
            Snackbar(it, shape = RoundedCornerShape(16.dp), containerColor = BaruckColors.Ink, contentColor = Color.White)
        }
        vm.levelUp?.let { lvl -> LevelUpDialog(lvl) { vm.levelUp = null } }
    }
}

@Composable
private fun MainNav(vm: BaruckViewModel) {
    val nav = rememberNavController()
    LaunchedEffect(Unit) { vm.onReady() }
    NavHost(
        nav, startDestination = "main",
        enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
        exitTransition = { slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(250)) },
        popEnterTransition = { slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300)) },
        popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(250)) }
    ) {
        composable("main") { MainTabs(vm, nav) }
        composable("farm/{id}") { FarmScreen(vm, nav, it.arguments?.getString("id").orEmpty()) }
        composable("gh/{farm}/{gh}") {
            GreenhouseScreen(vm, nav, it.arguments?.getString("farm").orEmpty(), it.arguments?.getString("gh").orEmpty())
        }
        composable("members/{id}") { MembersScreen(vm, nav, it.arguments?.getString("id").orEmpty()) }
        composable("notices/{id}") { NoticesScreen(vm, nav, it.arguments?.getString("id").orEmpty()) }
        composable("editProfile") { EditProfileScreen(vm, nav) }
    }
}
