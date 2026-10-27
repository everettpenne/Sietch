package com.everettpenne.sietch.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.everettpenne.sietch.data.Category
import com.everettpenne.sietch.ui.theme.Sietch

fun Category.icon(): ImageVector = when (this) {
    Category.IMAGES -> Icons.Rounded.Image
    Category.VIDEOS -> Icons.Rounded.Movie
    Category.AUDIO -> Icons.Rounded.MusicNote
    Category.DOCUMENTS -> Icons.Rounded.Description
    Category.DOWNLOADS -> Icons.Rounded.Download
    Category.APPS -> Icons.Rounded.Android
    Category.ARCHIVES -> Icons.Rounded.FolderZip
}

fun Category.color(): Color = when (this) {
    Category.IMAGES -> Sietch.green
    Category.VIDEOS -> Sietch.violet
    Category.AUDIO -> Sietch.pink
    Category.DOCUMENTS -> Sietch.blue
    Category.DOWNLOADS -> Sietch.teal
    Category.APPS -> Sietch.orange
    Category.ARCHIVES -> Sietch.sand
}
