package com.everettpenne.sietch.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.everettpenne.glass.LiquidSwitch
import com.everettpenne.sietch.ui.theme.Sietch

/** A settings group: a caption, a rounded card of rows, and an optional footer, as in Seekur, Spice and Thumper. */
@Composable
fun Group(title: String, footer: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 10.dp))
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(26.dp)), content = content)
        if (footer != null) Text(footer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 10.dp))
    }
}

@Composable
private fun RowShell(icon: ImageVector?, title: String, subtitle: String?, onClick: (() -> Unit)?, trailing: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        if (icon != null) Icon(icon, null, tint = Sietch.accent, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}

@Composable
fun Info(icon: ImageVector, title: String, subtitle: String) = RowShell(icon, title, subtitle, null) {}

@Composable
fun NavRow(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit) =
    RowShell(icon, title, subtitle, onClick) { Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }

@Composable
fun ChoiceRow(title: String, selected: Boolean, onClick: () -> Unit) =
    RowShell(null, title, null, onClick) { if (selected) Icon(Icons.Rounded.Check, "Selected", tint = Sietch.accent) }

@Composable
fun SwitchRow(icon: ImageVector, title: String, checked: Boolean, onChange: (Boolean) -> Unit) =
    RowShell(icon, title, null, { onChange(!checked) }) { LiquidSwitch(checked, onChange) }

/** A labelled row of chips, for choosing one of a few values (sort order, direction). */
@Composable
fun Labeled(title: String, content: @Composable RowScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}
