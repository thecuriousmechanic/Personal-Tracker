package com.krishna.personalos.ui.theme

import androidx.compose.ui.graphics.Color

internal var Paper = Color(0xFFFFFEFB)
internal var SageWash = Color(0xFFE7EDE5)
internal var PeachWash = Color(0xFFF4E8DA)
internal var LilacWash = Color(0xFFEEE8F3)
internal var Ink = Color(0xFFF8F6F0)
internal var DeepNavy = Paper
internal var SurfaceNavy = Color(0xFFF0EEE7)
internal var RaisedNavy = Color(0xFFE8E6DE)
internal var Glass = Paper
internal var ElectricBlue = Color(0xFF376B56)
internal var Cyan = Color(0xFF376B56)
internal var Violet = Color(0xFF75558A)
internal var Mint = Color(0xFF33745D)
internal var Amber = Color(0xFF936038)
internal var Ice = Color(0xFF2C302B)
internal var Muted = Color(0xFF6F7269)
internal var Divider = Color(0xFFDCDDD3)
internal var SignalBlue = ElectricBlue
internal var SignalCyan = Cyan
internal var SignalWarm = Amber

internal fun applyLegacyPalette(darkMode: Boolean) {
    if (darkMode) {
        Paper = Color(0xFF191919)
        SageWash = Color(0xFF294837)
        PeachWash = Color(0xFF49382D)
        LilacWash = Color(0xFF3D3244)
        Ink = Color(0xFF191919)
        DeepNavy = Color(0xFF191919)
        SurfaceNavy = Color(0xFF232323)
        RaisedNavy = Color(0xFF2B2B2B)
        Glass = Color(0xFF232323)
        ElectricBlue = Color(0xFF9BCBAD)
        Cyan = Color(0xFF9BCBAD)
        Violet = Color(0xFFD1B4E0)
        Mint = Color(0xFF9BCBAD)
        Amber = Color(0xFFE1B68E)
        Ice = Color(0xFFE6E6E3)
        Muted = Color(0xFFA6A6A1)
        Divider = Color(0xFF343432)
    } else {
        Paper = Color(0xFFFFFEFB)
        SageWash = Color(0xFFE7EDE5)
        PeachWash = Color(0xFFF4E8DA)
        LilacWash = Color(0xFFEEE8F3)
        Ink = Color(0xFFF8F6F0)
        DeepNavy = Paper
        SurfaceNavy = Color(0xFFF0EEE7)
        RaisedNavy = Color(0xFFE8E6DE)
        Glass = Paper
        ElectricBlue = Color(0xFF376B56)
        Cyan = Color(0xFF376B56)
        Violet = Color(0xFF75558A)
        Mint = Color(0xFF33745D)
        Amber = Color(0xFF936038)
        Ice = Color(0xFF2C302B)
        Muted = Color(0xFF6F7269)
        Divider = Color(0xFFDCDDD3)
    }
    SignalBlue = ElectricBlue
    SignalCyan = Cyan
    SignalWarm = Amber
}
