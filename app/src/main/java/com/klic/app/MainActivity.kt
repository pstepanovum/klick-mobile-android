package com.klic.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.klic.app.feature.KlicViewModel
import com.klic.app.feature.auth.AuthScreen
import com.klic.app.feature.call.CallScreen
import com.klic.app.feature.chat.ChatScreen
import com.klic.app.feature.conversations.ConversationsScreen
import com.klic.app.feature.friends.FriendsScreen
import com.klic.app.ui.theme.KlicTheme

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val tabs = listOf(
    Tab("home", "Chats", Icons.AutoMirrored.Filled.Chat),
    Tab("friends", "Friends", Icons.Default.People),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as KlicApplication).container

        setContent {
            KlicTheme {
                val vm: KlicViewModel = viewModel(factory = factory(container))
                val isAuthed by vm.isAuthenticated.collectAsState()
                if (!isAuthed) AuthScreen(vm) else Home(vm)
            }
        }
    }

    @Composable
    private fun Home(vm: KlicViewModel) {
        val navController = rememberNavController()
        val backStack by navController.currentBackStackEntryAsState()
        val route = backStack?.destination?.route
        val showBar = route == "home" || route == "friends"

        Scaffold(
            bottomBar = {
                if (showBar) {
                    NavigationBar {
                        val current = backStack?.destination
                        tabs.forEach { tab ->
                            NavigationBarItem(
                                selected = current?.hierarchy?.any { it.route == tab.route } == true,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                }
            },
        ) { padding ->
            NavHost(navController, startDestination = "home", modifier = Modifier.padding(padding)) {
                composable("home") {
                    ConversationsScreen(vm) { convo -> navController.navigate("chat/${convo.id}") }
                }
                composable("friends") {
                    FriendsScreen(vm) { conversationId -> navController.navigate("chat/$conversationId") }
                }
                composable("chat/{conversationId}") { entry ->
                    val id = entry.arguments?.getString("conversationId").orEmpty()
                    vm.conversations.value.firstOrNull { it.id == id }?.let { convo ->
                        ChatScreen(
                            vm = vm,
                            conversation = convo,
                            onBack = { navController.popBackStack() },
                            onCall = { navController.navigate("call") },
                        )
                    }
                }
                composable("call") {
                    val call by vm.activeCall.collectAsState()
                    call?.let { CallScreen(vm, it, peerName = "Call") { navController.popBackStack() } }
                }
            }
        }
    }

    private fun factory(container: AppContainer) = object : Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            KlicViewModel(container.repository, container.tokenStore, container.socket, container.callManager) as T
    }
}
