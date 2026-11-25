package com.pedro.baruck

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.pedro.baruck.ui.BaruckViewModel
import com.pedro.baruck.ui.screens.*
import com.pedro.baruck.ui.theme.BaruckTheme

class MainActivity : ComponentActivity() {

    private val askNotif =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        maybeAskNotifications()
        setContent {
            BaruckTheme {
                val vm: BaruckViewModel = viewModel()
                var loggedIn by remember {
                    mutableStateOf(FirebaseAuth.getInstance().currentUser != null)
                }

                if (!loggedIn) {
                    LoginScreen(
                        webClientId = getString(R.string.web_client_id),
                        onLoggedIn = { loggedIn = true; vm.onReady() }
                    )
                } else {
                    LaunchedEffect(Unit) { vm.onReady() }
                    val nav = rememberNavController()
                    NavHost(nav, startDestination = "home") {
                        composable("home") { HomeScreen(vm, nav) { loggedIn = false } }
                        composable("farm/{id}") {
                            FarmScreen(vm, nav, it.arguments?.getString("id") ?: "")
                        }
                        composable("crop/{farm}/{crop}") {
                            CropScreen(
                                vm, nav,
                                it.arguments?.getString("farm") ?: "",
                                it.arguments?.getString("crop") ?: ""
                            )
                        }
                        composable("members/{id}") {
                            MembersScreen(vm, nav, it.arguments?.getString("id") ?: "")
                        }
                        composable("reminders/{id}") {
                            RemindersScreen(vm, nav, it.arguments?.getString("id") ?: "")
                        }
                        composable("friends") { FriendsScreen(vm, nav) }
                        composable("inbox") { InboxScreen(vm, nav) }
                    }
                }
            }
        }
    }

    private fun maybeAskNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            askNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
