package com.sricharan.dailyschedule.domain

import com.sricharan.dailyschedule.data.Completion
import com.sricharan.dailyschedule.data.ScheduleItem

/**
 * The garden is the app's only form of "progress", and it is deliberately
 * one-directional: growth accumulates and never regresses. Skipping a day
 * doesn't wilt anything, break a streak, or show a gap — the plant simply
 * stays as it is until you come back to it.
 */
enum class GrowthStage(val label: String) {
    SEED("a seed, waiting"),
    SPROUT("a small sprout"),
    SAPLING("a sapling"),
    YOUNG("a young tree"),
    GROWN("a full tree"),
    ELDER("an old, settled tree");

    companion object {
        fun forCount(count: Int): GrowthStage = when {
            count <= 0 -> SEED
            count < 3 -> SPROUT
            count < 7 -> SAPLING
            count < 15 -> YOUNG
            count < 30 -> GROWN
            else -> ELDER
        }
    }
}

data class Plant(
    val item: ScheduleItem,
    val timesTended: Int
) {
    val stage: GrowthStage get() = GrowthStage.forCount(timesTended)
}

data class Garden(
    val plants: List<Plant>
) {
    val totalTended: Int get() = plants.sumOf { it.timesTended }

    /**
     * Reassurance rather than assessment. None of these are conditional on
     * doing "enough" — the emptiest garden gets the kindest line.
     */
    val message: String
        get() = when {
            plants.isEmpty() ->
                "Nothing planted yet. There's no hurry — add a routine whenever you feel like it."
            totalTended == 0 ->
                "Everything's still a seed. That's exactly where forests start."
            totalTended < 10 ->
                "Small green things are coming up. Nice."
            totalTended < 40 ->
                "Your garden is filling in, quietly."
            else ->
                "This has become a proper little woodland."
        }
}

fun buildGarden(items: List<ScheduleItem>, completions: List<Completion>): Garden {
    val countsByItem = completions.groupingBy { it.scheduleItemId }.eachCount()
    val plants = items
        .filter { it.isRecurring }
        .map { Plant(item = it, timesTended = countsByItem[it.id] ?: 0) }
        .sortedByDescending { it.timesTended }
    return Garden(plants)
}
