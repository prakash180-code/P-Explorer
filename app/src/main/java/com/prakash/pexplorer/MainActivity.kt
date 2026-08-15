package com.prakash.pexplorer

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.net.toUri
import com.prakash.pexplorer.presentation.ExplorerViewModel
import com.prakash.pexplorer.presentation.navigation.PExplorerApp
import com.prakash.pexplorer.presentation.theme.PExplorerTheme

class MainActivity : ComponentActivity() {
    private val explorerViewModel: ExplorerViewModel by viewModels()

    private val readStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        explorerViewModel.refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        updateSystemBarAppearance()
        setContent {
            PExplorerTheme {
                PExplorerApp(
                    viewModel = explorerViewModel,
                    onRequestStorageAccess = ::requestStorageAccess
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        explorerViewModel.refresh()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateSystemBarAppearance()
    }

    private fun requestStorageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val appAccessIntent = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                "package:$packageName".toUri()
            )
            runCatching {
                startActivity(appAccessIntent)
            }.getOrElse {
                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        } else {
            readStoragePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun updateSystemBarAppearance() {
        val isDark = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
    }
}
