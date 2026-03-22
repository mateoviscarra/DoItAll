package com.mateoviscarra.doitall.data.persist

/**
 * Full persisted blob for one workout day (e.g. "Push A").
 * Option B: each carousel page has its own [PageLogState].
 */
data class DayLogState(
    val slots: Map<String, SlotLogState>
) {
    fun updateSlot(slotIndex: Int, transform: (SlotLogState) -> SlotLogState): DayLogState {
        val key = slotIndex.toString()
        val slot = slots[key] ?: return this
        return copy(slots = slots + (key to transform(slot)))
    }
}

data class SlotLogState(
    /** Last opened page in the horizontal carousel. */
    val selectedPage: Int,
    /** One entry per carousel page (main + alternatives in order). */
    val pages: List<PageLogState>
)

data class PageLogState(
    /** Which exercise from this slot's hardcoded list is active for this page. */
    val selectedExerciseName: String,
    val sets: Int,
    /** Single load for the exercise (user asked for one weight). */
    val weight: String,
    /** If true, [repsPerSet] has one entry per set; else [repsSingle]. */
    val usePerSetReps: Boolean,
    val repsSingle: Int?,
    val repsPerSet: List<Int>?,
    val comment: String,
    val isCardio: Boolean,
    val duration: String?,
    val intensity: String?
) {
    companion object {
        const val MAX_COMMENT_LENGTH = 128
    }
}
