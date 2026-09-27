package com.vibearc.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val LocalGlass = staticCompositionLocalOf { true }

internal fun groupShape(index: Int, count: Int) = RoundedCornerShape(
    topStart = if (index == 0) 28.dp else 6.dp,
    topEnd = if (index == 0) 28.dp else 6.dp,
    bottomStart = if (index == count - 1) 28.dp else 6.dp,
    bottomEnd = if (index == count - 1) 28.dp else 6.dp,
)

@Composable
internal fun ReferenceSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    highlighted: Boolean = false,
    content: @Composable () -> Unit,
) {
    val glass = LocalGlass.current
    val base = if (highlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    Surface(modifier, shape = shape, color = if (glass) base.copy(alpha = .92f) else base,
        border = if (glass) BorderStroke(0.7.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = .23f), Color.Transparent))) else null,
    ) {
        Box(Modifier.background(Brush.verticalGradient(
            if (glass) listOf(Color.White.copy(alpha = .045f), Color.Transparent) else listOf(Color.Transparent, Color.Transparent),
        ))) { content() }
    }
}

// Small local glyphs keep the reference's icon vocabulary without a large icon dependency.
@Composable
internal fun Glyph(name: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    Canvas(modifier.size(24.dp)) {
        val u = size.width / 24f
        fun line(x: Float, y: Float, xx: Float, yy: Float, width: Float = 2f) =
            drawLine(color, Offset(x*u,y*u), Offset(xx*u,yy*u), width*u, StrokeCap.Round)
        fun rect(x: Float, y: Float, w: Float, h: Float) =
            drawRect(color, Offset(x*u,y*u), Size(w*u,h*u))
        when (name) {
            "stats", "equalizer" -> {
                rect(3f,10f,3f,11f); rect(8f,3f,3f,18f); rect(13f,6f,3f,15f); rect(18f,12f,3f,9f)
            }
            "music", "playlist", "lyrics" -> {
                if (name != "music") { line(2f,5f,13f,5f); line(2f,10f,10f,10f); line(2f,15f,8f,15f) }
                line(17f,4f,17f,17f,3f); line(17f,4f,22f,4f,3f)
                drawCircle(color,4*u,Offset(14*u,18*u))
            }
            "download", "backup", "restore" -> {
                val up = name == "backup"
                line(12f, if(up) 16f else 4f, 12f,if(up) 4f else 16f,3f)
                line(7f,if(up) 9f else 11f,12f,if(up) 4f else 16f,3f)
                line(17f,if(up) 9f else 11f,12f,if(up) 4f else 16f,3f)
                line(5f,21f,19f,21f)
            }
            "discover" -> {
                drawCircle(color,10*u)
                val p=Path().apply { moveTo(16*u,7*u); lineTo(13*u,14*u); lineTo(7*u,17*u); lineTo(10*u,10*u); close() }
                drawPath(p,Color(0xFF303030))
            }
            "palette" -> {
                drawCircle(color,10*u)
                listOf(Offset(8f,7f),Offset(14f,6f),Offset(18f,11f)).forEach { drawCircle(Color(0xFF444444),1.8f*u, it*u) }
            }
            "amoled" -> {
                drawCircle(color,9*u,style=Stroke(2*u))
                drawArc(color,-90f,180f,true,Offset(3*u,3*u),Size(18*u,18*u))
            }
            "album" -> { drawCircle(color,9*u); drawCircle(Color(0xFF444444),3*u) }
            "glass" -> {
                drawCircle(color,5*u,Offset(15*u,8*u)); drawCircle(color,3*u,Offset(6*u,14*u)); drawCircle(color,2*u,Offset(13*u,19*u))
            }
            "wave" -> {
                for (j in 0..2) { val p=Path(); for(i in 0..24) { val x=i.toFloat(); val y=6f+j*6+2*kotlin.math.sin(x/3f); if(i==0)p.moveTo(x*u,y*u) else p.lineTo(x*u,y*u) }; drawPath(p,color,style=Stroke(1.8f*u)) }
            }
            "check" -> { line(5f,12f,10f,17f); line(10f,17f,20f,6f) }
            "chevron" -> { line(9f,5f,16f,12f); line(16f,12f,9f,19f) }
            "sort" -> { line(4f,6f,20f,6f); line(4f,12f,14f,12f); line(4f,18f,8f,18f) }
            "shuffle" -> {
                line(3f,5f,20f,19f); line(3f,19f,9f,14f); line(14f,9f,20f,5f)
                line(16f,5f,20f,5f); line(20f,5f,20f,9f); line(16f,19f,20f,19f); line(20f,15f,20f,19f)
            }
            "spark" -> {
                val p=Path().apply { moveTo(12*u,1*u); lineTo(15*u,9*u); lineTo(23*u,12*u); lineTo(15*u,15*u); lineTo(12*u,23*u); lineTo(9*u,15*u); lineTo(1*u,12*u); lineTo(9*u,9*u); close() }; drawPath(p,color)
            }
            "clock" -> { drawCircle(color,9*u,style=Stroke(2*u)); line(12f,6f,12f,12f); line(12f,12f,17f,15f) }
            "account" -> { drawCircle(color,9*u,style=Stroke(2*u)); drawCircle(color,3*u,Offset(12*u,8*u)); drawArc(color,180f,180f,true,Offset(6*u,13*u),Size(12*u,9*u)) }
            "more" -> for(y in listOf(5f,12f,19f)) drawCircle(color,1.7f*u,Offset(12*u,y*u))
            "font" -> { line(3f,5f,15f,5f); line(9f,5f,9f,20f); line(13f,11f,22f,11f); line(18f,11f,18f,20f) }
            "quality" -> { drawRect(color,Offset(3*u,4*u),Size(18*u,16*u),style=Stroke(2*u)); line(7f,8f,7f,16f); line(11f,8f,11f,16f); line(7f,12f,11f,12f); drawCircle(color,2.5f*u,Offset(17*u,11*u),style=Stroke(1.5f*u)); line(18f,14f,20f,17f) }
            else -> { line(7f,6f,2f,12f); line(2f,12f,7f,18f); line(17f,6f,22f,12f); line(22f,12f,17f,18f) }
        }
    }
}

@Composable
internal fun RoundAction(label: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = .8f), shape = CircleShape) {
        IconButton(onClick, Modifier.size(44.dp).semantics { contentDescription = label }) { content() }
    }
}

@Composable
internal fun ReferenceRow(
    title: String,
    subtitle: String,
    glyph: String,
    index: Int = 0,
    count: Int = 1,
    checked: Boolean? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    ReferenceSurface(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
        shape = groupShape(index, count),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { Glyph(glyph) }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f).padding(end = 10.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            if (checked == null) Glyph("chevron", Modifier.size(20.dp))
            else Switch(checked, onCheckedChange = null, enabled = enabled,
                thumbContent = if(checked) {{ Glyph("check", Modifier.size(16.dp), MaterialTheme.colorScheme.onPrimary) }} else null)
        }
    }
}

@Composable
internal fun ReferenceHeader(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Surface(shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Row(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color.White.copy(alpha=if(LocalGlass.current) .06f else 0f), Color.Transparent)))
            .statusBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp).heightIn(min = 50.dp), verticalAlignment = Alignment.CenterVertically) {
            if(onBack != null) {
                RoundAction("Back",onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack,null) }
                Spacer(Modifier.width(16.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style=if(onBack == null) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium, maxLines=1,overflow=TextOverflow.Ellipsis)
                if(subtitle!=null) Text(subtitle, color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis)
            }
            actions()
        }
    }
}
