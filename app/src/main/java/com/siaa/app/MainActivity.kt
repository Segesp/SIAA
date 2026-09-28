package com.siaa.app

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.siaa.app.media.EarbudCommandRouter
import com.siaa.app.ui.AppTab
import com.siaa.app.ui.MainViewModel
import com.siaa.app.ui.screens.HeadphonesScreen
import com.siaa.app.ui.screens.HomeScreen
import com.siaa.app.ui.screens.MoreScreen
import com.siaa.app.ui.screens.SessionScreen
import com.siaa.app.ui.theme.SIAATheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val earbudRouter = EarbudCommandRouter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val highContrast by viewModel.highContrast.collectAsState()
            val currentTab by viewModel.currentTab.collectAsState()

            SIAATheme(highContrast = highContrast) {
                BackHandler(enabled = currentTab != AppTab.HOME) {
                    if (currentTab == AppTab.ACTIVE_SESSION) {
                        viewModel.exitSessionToHome()
                    } else {
                        viewModel.selectTab(AppTab.HOME)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        if (currentTab != AppTab.ACTIVE_SESSION) {
                            SiaaTopBar(currentTab = currentTab)
                        }
                    },
                    bottomBar = {
                        if (currentTab != AppTab.ACTIVE_SESSION) {
                            SiaaBottomNavigation(
                                currentTab = currentTab,
                                onTabSelected = { viewModel.selectTab(it) }
                            )
                        }
                    }
                ) { innerPadding ->
                    val contentModifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)

                    when (currentTab) {
                        AppTab.HOME -> HomeScreen(viewModel = viewModel, modifier = contentModifier)
                        AppTab.HEADPHONES -> HeadphonesScreen(viewModel = viewModel, modifier = contentModifier)
                        AppTab.MORE -> MoreScreen(viewModel = viewModel, modifier = contentModifier)
                        AppTab.ACTIVE_SESSION -> SessionScreen(viewModel = viewModel, modifier = contentModifier)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.audioPlayer.refreshAudioRoute()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null && event.repeatCount == 0) {
            val intent = Intent(Intent.ACTION_MEDIA_BUTTON).putExtra(Intent.EXTRA_KEY_EVENT, event)
            val parsed = earbudRouter.fromMediaButtonIntent(intent, viewModel.deviceProfile.value)
            if (parsed != null) {
                viewModel.handleMediaControlEvent(parsed.first)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiaaTopBar(currentTab: AppTab) {
    val title = when (currentTab) {
        AppTab.HOME -> "SIAA · Aprendizaje Auditivo"
        AppTab.HEADPHONES -> "Audífonos y Controles"
        AppTab.MORE -> "Más y Accesibilidad"
        AppTab.ACTIVE_SESSION -> "Sesión SIAA"
    }

    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun SiaaBottomNavigation(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        NavigationBarItem(
            selected = currentTab == AppTab.HOME,
            onClick = { onTabSelected(AppTab.HOME) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Pestaña Inicio") },
            label = { Text("Inicio") },
            modifier = Modifier.testTag("nav_home")
        )
        NavigationBarItem(
            selected = currentTab == AppTab.HEADPHONES,
            onClick = { onTabSelected(AppTab.HEADPHONES) },
            icon = { Icon(Icons.Default.Headphones, contentDescription = "Pestaña Audífonos") },
            label = { Text("Audífonos") },
            modifier = Modifier.testTag("nav_headphones")
        )
        NavigationBarItem(
            selected = currentTab == AppTab.MORE,
            onClick = { onTabSelected(AppTab.MORE) },
            icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "Pestaña Más") },
            label = { Text("Más") },
            modifier = Modifier.testTag("nav_more")
        )
    }
}
