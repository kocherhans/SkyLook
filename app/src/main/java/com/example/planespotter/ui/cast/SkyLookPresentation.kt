package com.example.planespotter.ui.cast

import android.app.Presentation
import android.os.Bundle
import android.view.Display
import android.os.Build
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.planespotter.ui.theme.PlaneSpotterTheme

/**
 * A secondary-display Presentation that renders CastAmbientView.
 *
 * Works with any display surfaced via MediaRouter:
 *   - Miracast / Wi-Fi Display (Fire TV, Android TV dongles, smart TVs)
 *   - HDMI adapters
 *   - Virtual display adapters
 *
 * The phone app calls [updateState] to push live flight data here.
 * Chromecast / Google Home targets are not reachable via MediaRouter without
 * the Cast SDK, but users can use the Android "Cast screen" tile for those.
 */
class SkyLookPresentation(
    private val activity: ComponentActivity,
    display: Display
) : Presentation(activity, display) {

    private var _state by mutableStateOf(CastState())

    fun updateState(state: CastState) {
        _state = state
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Full-screen, no system chrome
        window?.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insetsController?.apply {
                    hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }
        }

        // Wire up the Activity's lifecycle so the ComposeView inside this
        // Presentation window can use rememberCoroutineScope, LaunchedEffect, etc.
        window?.decorView?.let { root ->
            root.setViewTreeLifecycleOwner(activity)
            root.setViewTreeSavedStateRegistryOwner(activity)
        }

        setContentView(
            ComposeView(activity).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    PlaneSpotterTheme {
                        CastAmbientView(state = _state)
                    }
                }
            }
        )
    }
}
