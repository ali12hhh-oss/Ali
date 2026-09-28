package com.velocity.editor.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velocity.editor.ui.components.SectionHeader
import com.velocity.editor.ui.theme.VelocityColors

/** Shared by the Library screen and the in-editor Effects sheet, so both render filters/transitions/titles identically. */
val effectTileColors = listOf(
    listOf(Color(0xFF1E6F7A), Color(0xFF12323A)),
    listOf(Color(0xFF3B4C7A), Color(0xFF1A1F3A)),
    listOf(Color(0xFF7A4B1E), Color(0xFF3A210F)),
    listOf(Color(0xFF6B2E7A), Color(0xFF2E1238)),
    listOf(Color(0xFF2E7D4F), Color(0xFF133D26)),
    listOf(Color(0xFF7A2E3B), Color(0xFF38121A)),
)

@Composable
fun EffectSection(
    title: Int,
    items: List<EffectItem>,
    premiumUnlocked: Boolean,
    isSelected: (EffectItem) -> Boolean,
    showHeader: Boolean = true,
    onClick: (EffectItem) -> Unit,
) {
    Column {
        if (showHeader) SectionHeader(title)
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items, key = { it.id }) { item ->
                val index = items.indexOf(item)
                val selected = isSelected(item)
                val locked = item.premium && !premiumUnlocked
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(92.dp).clickable { onClick(item) }) {
                    Box(
                        Modifier.fillMaxWidth().height(76.dp).clip(RoundedCornerShape(12.dp))
                            .background(Brush.verticalGradient(effectTileColors[index % effectTileColors.size]))
                            .then(if (selected) Modifier.border(2.dp, VelocityColors.Teal, RoundedCornerShape(12.dp)) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(item.icon, null, tint = Color.White.copy(alpha = if (locked) 0.4f else 0.95f), modifier = Modifier.size(28.dp))
                        if (locked) Icon(Icons.Outlined.Lock, null, tint = Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(14.dp))
                        if (selected) {
                            Box(
                                Modifier.align(Alignment.TopStart).padding(6.dp).size(18.dp).clip(CircleShape).background(VelocityColors.Teal),
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(12.dp)) }
                        }
                    }
                    Text(stringResource(item.label), fontSize = 12.sp, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}
