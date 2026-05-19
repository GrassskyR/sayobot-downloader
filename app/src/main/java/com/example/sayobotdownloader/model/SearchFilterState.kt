package com.example.sayobotdownloader.model

data class SearchFilterState(
    val selectedMode: String = MODE_ALL,
    val selectedStatus: String = STATUS_ALL
) {
    val isFilterActive: Boolean
        get() = selectedMode != MODE_ALL || selectedStatus != STATUS_ALL

    companion object {
        const val MODE_ALL = "全部"
        const val MODE_STD = "STD"
        const val MODE_TAIKO = "Taiko"
        const val MODE_CTB = "CTB"
        const val MODE_MANIA = "Mania"

        const val STATUS_ALL = "全部"
        const val STATUS_RANKED_APPROVED = "Ranked & Approved"
        const val STATUS_QUALIFIED = "Qualified"
        const val STATUS_LOVED = "Loved"
        const val STATUS_PENDING_WIP = "Pending & WIP"
        const val STATUS_GRAVEYARD = "Graveyard"

        val MODE_OPTIONS = listOf(MODE_ALL, MODE_STD, MODE_TAIKO, MODE_CTB, MODE_MANIA)
        val STATUS_OPTIONS = listOf(
            STATUS_ALL,
            STATUS_RANKED_APPROVED,
            STATUS_QUALIFIED,
            STATUS_LOVED,
            STATUS_PENDING_WIP,
            STATUS_GRAVEYARD
        )
    }
}
