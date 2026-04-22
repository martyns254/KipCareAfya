package com.example.kipcareafya.domain

import com.example.kipcareafya.data.local.dao.UserMetricWithType

object HealthScoreCalculator {

    data class ScoreBreakdown(
        val total: Int,
        val metricScore: Int,
        val waterScore: Int,
        val stepsScore: Int,
        val consistencyScore: Int,
        val grade: String,
        val gradeColor: Long,
        val message: String
    )

    fun calculate(
        recentMetrics: List<UserMetricWithType>,
        waterIntakeMl: Int,
        waterGoalMl: Int,
        stepCount: Int,
        stepGoal: Int,
        logsLast7Days: Int
    ): ScoreBreakdown {

        // ── Metric score (40 points) ──────────────────────────
        // Check how many metrics are in normal range
        val metricScore = calculateMetricScore(recentMetrics)

        // ── Water score (20 points) ───────────────────────────
        val waterScore = if (waterGoalMl > 0) {
            ((waterIntakeMl.toFloat() / waterGoalMl) * 20)
                .toInt().coerceIn(0, 20)
        } else 0

        // ── Steps score (20 points) ───────────────────────────
        val stepsScore = if (stepGoal > 0) {
            ((stepCount.toFloat() / stepGoal) * 20)
                .toInt().coerceIn(0, 20)
        } else 0

        // ── Consistency score (20 points) ─────────────────────
        // Based on how many days in last 7 user logged something
        val consistencyScore = ((logsLast7Days.toFloat() / 7) * 20)
            .toInt().coerceIn(0, 20)

        val total = (metricScore + waterScore + stepsScore + consistencyScore)
            .coerceIn(0, 100)

        val (grade, color, message) = when {
            total >= 90 -> Triple("A+", 0xFF43A047L, "Excellent! Keep it up!")
            total >= 80 -> Triple("A",  0xFF66BB6AL, "Great work today!")
            total >= 70 -> Triple("B",  0xFF1565C0L, "Good — a little more effort!")
            total >= 60 -> Triple("C",  0xFFF57C00L, "Fair — room to improve")
            total >= 50 -> Triple("D",  0xFFEF6C00L, "Needs attention")
            else        -> Triple("F",  0xFFE53935L, "Let's get back on track!")
        }

        return ScoreBreakdown(
            total = total,
            metricScore = metricScore,
            waterScore = waterScore,
            stepsScore = stepsScore,
            consistencyScore = consistencyScore,
            grade = grade,
            gradeColor = color,
            message = message
        )
    }

    private fun calculateMetricScore(metrics: List<UserMetricWithType>): Int {
        if (metrics.isEmpty()) return 20 // neutral if no data yet
        // We only score the ones we have normal ranges for
        val scored = metrics.filter { entry ->
            // We can't access MetricTypeEntity here so use known ranges
            knownRanges[entry.name] != null
        }
        if (scored.isEmpty()) return 20

        val inRange = scored.count { entry ->
            val (min, max) = knownRanges[entry.name] ?: return@count true
            (min == null || entry.value >= min) && (max == null || entry.value <= max)
        }

        return ((inRange.toFloat() / scored.size) * 40).toInt().coerceIn(0, 40)
    }

    // Mirror the seed data normal ranges
    private val knownRanges = mapOf(
        "blood_glucose"             to (70f to 100f),
        "total_cholesterol"         to (null to 200f),
        "bmi"                       to (18.5f to 24.9f),
        "blood_pressure_systolic"   to (90f to 120f),
        "blood_pressure_diastolic"  to (60f to 80f),
        "heart_rate"                to (60f to 100f),
        "oxygen_saturation"         to (95f to 100f)
    )
}