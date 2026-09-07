package com.pureqr.app.model

import android.graphics.Color

enum class QrColor(val label: String, val color: Int) {
    BLACK("Black", Color.BLACK),
    WHITE("White", Color.WHITE),
    DARK_BLUE("Navy", Color.parseColor("#1A237E")),
    DARK_GREEN("Forest", Color.parseColor("#1B5E20")),
    DARK_RED("Maroon", Color.parseColor("#B71C1C")),
    CHARCOAL("Charcoal", Color.parseColor("#212121")),
    PURE_ORANGE("Orange", Color.parseColor("#E65100")),
    SOFT_BLUE("Sky Blue", Color.parseColor("#E3F2FD")),
    SOFT_GREEN("Mint", Color.parseColor("#E8F5E9")),
    SOFT_GREY("Cloud", Color.parseColor("#F5F5F5")),
    SOFT_BEIGE("Beige", Color.parseColor("#FFF8E1"))
}
