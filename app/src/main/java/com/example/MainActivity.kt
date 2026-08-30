package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppThemeMode
import com.example.data.model.ReaderPreferences
import com.example.ui.editorial.EditorialTheme
import com.example.ui.reader.EditorialReaderApp

/**
 * Host activity for the public reader.
 *
 * All navigation lives in [EditorialReaderApp]; this class only owns the theme
 * (system / light / dark, read from DataStore) and the edge-to-edge window.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as NinghsingCheApp

        setContent {
            val preferences by app.preferencesRepository.readerPreferences
                .collectAsStateWithLifecycle(initialValue = ReaderPreferences())
            val darkTheme = when (preferences.appThemeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            EditorialTheme(darkTheme = darkTheme) {
                EditorialReaderApp(
                    repository = app.portalRepository,
                    modifier = Modifier
                )
            }
        }
    }
}
