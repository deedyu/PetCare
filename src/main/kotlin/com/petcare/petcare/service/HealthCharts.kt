package com.petcare.petcare.service

import com.petcare.petcare.entity.HealthRecord
import java.time.LocalDate

data class WeightPoint(
    val date: LocalDate,
    val weight: Double,
    val x: Double,
    val y: Double
)

data class WeightChart(
    val points: List<WeightPoint>,
    val polyline: String,
    val min: Double,
    val max: Double
)

/**
 * 체중 기록을 날짜순 꺾은선으로 바꾼다. 값이 없는 기록은 빠진다.
 */
object HealthCharts {
    fun of(records: List<HealthRecord>): WeightChart? {
        val samples = records.mapNotNull { record ->
            record.weight?.let { record.recordDate to it }
        }.sortedBy { it.first }
        if (samples.isEmpty()) return null

        val width = 320.0
        val height = 140.0
        val padX = 28.0
        val padY = 20.0
        val innerW = width - padX * 2
        val innerH = height - padY * 2
        val minW = samples.minOf { it.second }
        val maxW = samples.maxOf { it.second }
        val span = maxW - minW
        val last = samples.lastIndex

        val points = samples.mapIndexed { index, (date, weight) ->
            val x = if (last == 0) width / 2 else padX + innerW * index / last
            val y = if (span == 0.0) {
                height / 2
            } else {
                padY + innerH * (1 - (weight - minW) / span)
            }
            WeightPoint(date, weight, x, y)
        }
        return WeightChart(
            points = points,
            polyline = points.joinToString(" ") { "${it.x},${it.y}" },
            min = minW,
            max = maxW
        )
    }
}
