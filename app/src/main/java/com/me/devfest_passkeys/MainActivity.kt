package com.me.devfest_passkeys

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.me.devfest_passkeys.ui.screens.LoginScreen
import com.me.devfest_passkeys.ui.screens.HomeScreen
import com.me.devfest_passkeys.ui.screens.LoginScreenWithPasskeys
import com.me.devfest_passkeys.ui.theme.Devfest_passkeysTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Devfest_passkeysTheme {
                MainScreen()
            }
        }
    }

    @Composable
    fun MainScreen() {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = "login") {
            composable("login") {
                LoginScreen(navController = navController, activity = this@MainActivity)
            }
            composable("login-with-passkeys") {
                LoginScreenWithPasskeys(navController = navController, activity = this@MainActivity)
            }
            composable(
                "home/{email}",
                arguments = listOf(navArgument("email") { type = NavType.StringType })
            ) { backStackEntry ->
                val email = backStackEntry.arguments?.getString("email")!!
                HomeScreen(navController, email)
            }
        }
    }
}