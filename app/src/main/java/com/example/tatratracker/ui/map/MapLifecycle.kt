package com.example.tatratracker.ui.map

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import org.maplibre.android.maps.MapView

@Composable
fun MapViewLifecycle(
    context: Context,
    mapView: MapView
) {

    val lifecycleOwner =
        remember(context) {
            context.findMapLifecycleOwner()
        }

    DisposableEffect(
        lifecycleOwner,
        mapView
    ) {

        var started =
            false

        var resumed =
            false

        var destroyed =
            false

        fun startMap() {

            if (!started) {

                mapView.onStart()

                started =
                    true
            }
        }

        fun resumeMap() {

            if (!resumed) {

                mapView.onResume()

                resumed =
                    true
            }
        }

        fun pauseMap() {

            if (resumed) {

                mapView.onPause()

                resumed =
                    false
            }
        }

        fun stopMap() {

            if (started) {

                mapView.onStop()

                started =
                    false
            }
        }

        fun destroyMap() {

            if (!destroyed) {

                mapView.onDestroy()

                destroyed =
                    true
            }
        }

        val observer =
            LifecycleEventObserver {
                    _,
                    event ->

                when (event) {

                    Lifecycle.Event.ON_START -> {

                        startMap()
                    }

                    Lifecycle.Event.ON_RESUME -> {

                        resumeMap()
                    }

                    Lifecycle.Event.ON_PAUSE -> {

                        pauseMap()
                    }

                    Lifecycle.Event.ON_STOP -> {

                        stopMap()
                    }

                    Lifecycle.Event.ON_DESTROY -> {

                        pauseMap()
                        stopMap()
                        destroyMap()
                    }

                    else -> Unit
                }
            }

        lifecycleOwner
            ?.lifecycle
            ?.addObserver(
                observer
            )

        /*
         * Jeżeli ekran został utworzony,
         * kiedy Activity jest już STARTED
         * albo RESUMED, synchronizujemy
         * stan ręcznie.
         */
        lifecycleOwner
            ?.lifecycle
            ?.currentState
            ?.let { currentState ->

                if (
                    currentState.isAtLeast(
                        Lifecycle.State.STARTED
                    )
                ) {

                    startMap()
                }

                if (
                    currentState.isAtLeast(
                        Lifecycle.State.RESUMED
                    )
                ) {

                    resumeMap()
                }
            }

        /*
         * Awaryjnie, gdy Context nie zwróci
         * LifecycleOwner.
         */
        if (lifecycleOwner == null) {

            startMap()
            resumeMap()
        }

        onDispose {

            lifecycleOwner
                ?.lifecycle
                ?.removeObserver(
                    observer
                )

            pauseMap()
            stopMap()
            destroyMap()
        }
    }
}


private fun Context.findMapLifecycleOwner():
        LifecycleOwner? {

    var currentContext:
            Context? = this

    while (
        currentContext != null
    ) {

        if (
            currentContext is LifecycleOwner
        ) {

            return currentContext
        }

        currentContext =
            if (
                currentContext
                        is ContextWrapper
            ) {

                currentContext
                    .baseContext

            } else {

                null
            }
    }

    return null
}