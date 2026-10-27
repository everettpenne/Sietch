package com.everettpenne.sietch

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.everettpenne.media.VideoPip
import com.everettpenne.media.enterVideoPip
import com.everettpenne.sietch.ui.SietchRoot

class MainActivity : ComponentActivity() {
    /** A launcher shortcut's own address (sietch://recents, ...), handed to the Compose root and cleared once used. */
    private var deepLink by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT), navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT))
        if (savedInstanceState == null) deepLink = intent?.data
        setContent { SietchRoot(deepLink, onDeepLinkHandled = { deepLink = null }) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLink = intent.data
    }

    /** Going home while a video plays full screen keeps it going in a small window (Glass decides whether one is playing). */
    override fun onUserLeaveHint() { super.onUserLeaveHint(); enterVideoPip(this) }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        VideoPip.pipChanged(isInPictureInPictureMode)
    }
}
