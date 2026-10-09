package com.vibearc.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal fun mainTabPosition(page: Int, offset: Float): Float =
    (page + offset).coerceIn(0f, MainTabs.lastIndex.toFloat())

@Composable
internal fun MainTabContent(tab: Tab, pager: PagerState, padding: PaddingValues,
    header: @Composable (Tab) -> Unit, content: @Composable (Tab, PaddingValues) -> Unit) {
    if (tab in MainTabs) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 0, key = { MainTabs[it].name }) { page ->
            Column(Modifier.fillMaxSize()) {
                header(MainTabs[page])
                Box(Modifier.weight(1f)) {
                    content(MainTabs[page], PaddingValues(bottom = padding.calculateBottomPadding()))
                }
            }
        }
    } else MotionScene(tab, Modifier.fillMaxSize(), liftDp = if (tab == Tab.Player) 40f else 12f,
        scaleFrom = if (tab == Tab.Player) .95f else .99f) { content(tab, padding) }
}

@Composable
internal fun MainTabBar(pager: PagerState, onSelect: (Int) -> Unit) {
    val tint = MaterialTheme.colorScheme.primaryContainer
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    ReferenceSurface(Modifier.widthIn(max = 360.dp).fillMaxWidth().padding(horizontal = 16.dp),
        shape = CircleShape, floating = true) {
        Box(Modifier.padding(5.dp)) {
            // One rounded drawing, no separate pressed rectangle or delayed pill surface.
            Canvas(Modifier.matchParentSize()) {
                val width = size.width / MainTabs.size
                val position = mainTabPosition(pager.currentPage, pager.currentPageOffsetFraction)
                val left = (if (rtl) MainTabs.lastIndex - position else position) * width
                drawRoundRect(tint, Offset(left, 0f), Size(width, size.height),
                    CornerRadius(size.height / 2f))
            }
            Row(Modifier.fillMaxWidth().selectableGroup(), verticalAlignment = Alignment.CenterVertically) {
                MainTabs.forEachIndexed { index, tab ->
                    val selected = pager.currentPage == index
                    val source = remember { MutableInteractionSource() }
                    val focused by source.collectIsFocusedAsState()
                    val focusRing = if (focused) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier
                    Column(Modifier.weight(1f).heightIn(min = 56.dp).then(focusRing).selectable(
                        selected = selected, interactionSource = source, indication = null,
                        role = Role.Tab, onClick = { onSelect(index) },
                    ).padding(horizontal = 4.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)) {
                        if (tab == Tab.Home) Icon(Icons.Default.Home, null, Modifier.size(22.dp))
                        else Glyph(if (tab == Tab.Stats) "stats" else "playlist", Modifier.size(22.dp))
                        Text(if (tab == Tab.Home) "Feed" else tab.label, fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
