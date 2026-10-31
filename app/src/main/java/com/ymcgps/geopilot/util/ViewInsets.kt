package com.ymcgps.geopilot.util

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Kenardan kenara çizimde sistem çubukları / klavye için iç boşluk bırakır.
 * Yalnızca istenen kenarlara uygulanır; böylece harita tam ekran kalırken
 * kartlar durum çubuğunun ve gezinme çubuğunun altında kalmaz.
 */
fun View.applyInsetsAsPadding(
    top: Boolean = false,
    bottom: Boolean = false,
    horizontal: Boolean = false,
    imeAware: Boolean = false,
) {
    val initialLeft = paddingLeft
    val initialTop = paddingTop
    val initialRight = paddingRight
    val initialBottom = paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        val ime = if (imeAware) insets.getInsets(WindowInsetsCompat.Type.ime()).bottom else 0
        view.setPadding(
            if (horizontal) initialLeft + bars.left else initialLeft,
            if (top) initialTop + bars.top else initialTop,
            if (horizontal) initialRight + bars.right else initialRight,
            if (bottom) initialBottom + maxOf(bars.bottom, ime) else initialBottom,
        )
        insets
    }
    ViewCompat.requestApplyInsets(this)
}
