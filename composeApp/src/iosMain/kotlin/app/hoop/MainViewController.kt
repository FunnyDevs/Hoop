package app.hoop

import androidx.compose.ui.window.ComposeUIViewController
import app.hoop.di.initKoin
import app.hoop.location.iosPlatformModule

fun MainViewController() = ComposeUIViewController(configure = { initKoin(listOf(iosPlatformModule)) }) { App() }
