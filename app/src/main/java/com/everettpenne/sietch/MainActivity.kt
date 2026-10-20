package com.everettpenne.sietch

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.everettpenne.media.VideoPip
import com.everettpenne.media.enterVideoPip
import com.everettpenne.sietch.ui.SietchRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT), navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT))
        setContent { SietchRoot() }
    }

    /** Going home while a video plays full screen keeps it going in a small window (Glass decides whether one is playing). */
    override fun onUserLeaveHint() { super.onUserLeaveHint(); enterVideoPip(this) }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        VideoPip.pipChanged(isInPictureInPictureMode)
    }
}
