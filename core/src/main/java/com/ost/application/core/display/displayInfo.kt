package com.ost.application.core.display
data class DisplayInfo(
    val resolution: String = "N/A",
    val refreshRate: String = "N/A",
    val dpi: String = "N/A",
    val diagonal: String = "N/A",
    val orientation: String = "N/A",
    val stylusSupport: String = "N/A",
    val cornerRadius: String = "N/A"
)
data class DisplayInfoStrings(
    val hz: String,
    val dpi: String,
    val inches: String,
    val portrait: String,
    val landscape: String,
    val supported: String,
    val unsupported: String,
    val px: String = "px"
)
