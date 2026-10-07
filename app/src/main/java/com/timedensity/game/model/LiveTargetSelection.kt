package com.timedensity.game.model

object LiveTargetSelection {
    fun choose(preferredId: Int?, liveIds: List<Int>): Int? =
        preferredId?.takeIf { it in liveIds } ?: liveIds.firstOrNull()
}
