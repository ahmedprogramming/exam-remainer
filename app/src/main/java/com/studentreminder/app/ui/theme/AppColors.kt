package com.studentreminder.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

object AppColors {
    val subjectPalette = listOf(
        Color(0xFF6366F1), // indigo
        Color(0xFF0EA5E9), // sky
        Color(0xFF10B981), // emerald
        Color(0xFFF59E0B), // amber
        Color(0xFFEF4444), // red
        Color(0xFF8B5CF6), // violet
        Color(0xFFEC4899), // pink
        Color(0xFF14B8A6), // teal
        Color(0xFFF97316), // orange
        Color(0xFF84CC16), // lime
    )

    val exam = Color(0xFF6366F1)
    val assignment = Color(0xFFF59E0B)
    val overdue = Color(0xFFEF4444)
    val completed = Color(0xFF10B981)
    val today = Color(0xFFEC4899)

    val priorityLow = Color(0xFF10B981)
    val priorityMedium = Color(0xFFF59E0B)
    val priorityHigh = Color(0xFFEF4444)

    fun subject(index: Int): Color = subjectPalette[((index % subjectPalette.size) + subjectPalette.size) % subjectPalette.size]

    fun priority(p: Int): Color = when (p) {
        0 -> priorityLow
        2 -> priorityHigh
        else -> priorityMedium
    }
}

/** Soft tinted background for a colored chip/badge that works in light and dark mode. */
@Composable
fun Color.soft(): Color =
    if (LocalIsDark.current) lerp(this, Color.Black, 0.72f) else lerp(this, Color.White, 0.86f)

/** Readable foreground for text drawn on [soft]. */
@Composable
fun Color.onSoft(): Color =
    if (LocalIsDark.current) lerp(this, Color.White, 0.45f) else lerp(this, Color.Black, 0.35f)
