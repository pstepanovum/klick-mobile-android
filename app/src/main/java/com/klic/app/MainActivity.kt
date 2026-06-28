package com.klic.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.klic.app.feature.KlicViewModel
import com.klic.app.feature.auth.AuthScreen
import com.klic.app.feature.call.CallScreen
import com.klic.app.feature.chat.ChatScreen
import com.klic.app.feature.conversations.ConversationsScreen
import com.klic.app.ui.theme.KlicTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as KlicApplication).container

        setContent {
            KlicTheme {
                val vm: KlicViewModel = viewModel(factory = factory(container))
                val isAuthed by vm.isAuthenticated.collectAsState()

                if (!isAuthed) {
                    AuthScreen(vm)
                } else {
                    val navController = rememberNavController()
                    NavHost(navController, startDestination = "home") {
                        composable("home") {
                            ConversationsScreen(vm) { convo -> navController.navigate("chat/${convo.id}") }
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
                            call?.let {
                                CallScreen(vm, it, peerName = "Call") { navController.popBackStack() }
                            }
                        }
                    }
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
