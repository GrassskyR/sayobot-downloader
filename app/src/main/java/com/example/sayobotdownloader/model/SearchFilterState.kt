package com.example.sayobotdownloader.model

data class SearchFilterState(
    val selectedModes: Set<String> = emptySet(),
    val selectedStatuses: Set<String> = emptySet()
) {
    val isFilterActive: Boolean
        get() = selectedModes.isNotEmpty() || selectedStatuses.isNotEmpty()

    val modeBitmask: Int?
        get() = selectedModes.sumOf { MODE_MAP[it]!! }.takeIf { it > 0 }

    val statusBitmask: Int?
        get() = selectedStatuses.sumOf { STATUS_MAP[it]!! }.takeIf { it > 0 }

    companion object {
        const val MODE_STD = "STD"
        const val MODE_TAIKO = "Taiko"
        const val MODE_CTB = "CTB"
        const val MODE_MANIA = "Mania"

        const val STATUS_RANKED_APPROVED = "Ranked & Approved"
        const val STATUS_QUALIFIED = "Qualified"
        const val STATUS_LOVED = "Loved"
        const val STATUS_PENDING_WIP = "Pending & WIP"
        const val STATUS_GRAVEYARD = "Graveyard"

        val MODE_OPTIONS = listOf(MODE_STD, MODE_TAIKO, MODE_CTB, MODE_MANIA)
        val STATUS_OPTIONS = listOf(
            STATUS_RANKED_APPROVED,
            STATUS_QUALIFIED,
            STATUS_LOVED,
            STATUS_PENDING_WIP,
            STATUS_GRAVEYARD
        )

        val MODE_MAP = mapOf(
            MODE_STD to 1,
            MODE_TAIKO to 2,
            MODE_CTB to 4,
            MODE_MANIA to 8
        )

        val STATUS_MAP = mapOf(
            STATUS_RANKED_APPROVED to 1,
            STATUS_QUALIFIED to 2,
            STATUS_LOVED to 4,
            STATUS_PENDING_WIP to 8,
            STATUS_GRAVEYARD to 16
        )
    }
}
