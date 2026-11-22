package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.WolfGait
import com.example.ui.theme.WolfFrostBlue

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WolfMascot3DView(
    gait: WolfGait,
    wolfCoat: com.example.data.model.WolfCoat = com.example.data.model.WolfCoat.TIMBER_GREY,
    onTriggerHowl: () -> Unit,
    modifier: Modifier = Modifier
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    fun syncWolfToWebView(view: WebView?) {
        if (view == null) return
        val body = String.format("#%06X", 0xFFFFFF and wolfCoat.bodyHex.toInt())
        val dark = String.format("#%06X", 0xFFFFFF and wolfCoat.darkHex.toInt())
        val frost = String.format("#%06X", 0xFFFFFF and wolfCoat.frostHex.toInt())
        val eye = String.format("#%06X", 0xFFFFFF and wolfCoat.eyeHex.toInt())
        view.evaluateJavascript(
            "window.setWolfGait && window.setWolfGait('${gait.jsKey}');" +
                "window.setWolfCoat && window.setWolfCoat('$body','$dark','$frost','$eye');",
            null
        )
    }

    LaunchedEffect(gait, wolfCoat, webViewRef) {
        syncWolfToWebView(webViewRef)
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.5.dp, WolfFrostBlue.copy(alpha = 0.55f)),
        color = Color(0xFF131D26),
        modifier = modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(MaterialTheme.shapes.large)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        setBackgroundColor(android.graphics.Color.parseColor("#131D26"))
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        settings.allowFileAccess = true
                        loadUrl("file:///android_asset/wolf_3d.html")
                        webViewRef = this
                    }
                },
                update = { view ->
                    webViewRef = view
                    syncWolfToWebView(view)
                },
                modifier = Modifier.fillMaxSize()
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(10.dp)
            ) {
                Surface(
                    color = Color(0xCC0D1821),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, WolfFrostBlue.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "3D Pack Wolf · ${gait.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFD5F4FF),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                FilledTonalIconButton(
                    onClick = onTriggerHowl,
                    modifier = Modifier.testTag("wolf_howl_preview_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Trigger wolf victory howl"
                    )
                }
            }
        }
    }
}
