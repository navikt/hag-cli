package no.nav.helse.cli

import no.nav.helse.cli.Percentage.Companion.percentage
import kotlin.math.roundToInt

private class Percentage(private val fraction: Double) {
    companion object {
        val Number.percentage get() = this.percentage(100.0)
        fun Number.percentage(other: Number) = Percentage(toDouble() / other.toDouble())
        // fun Collection<Percentage>.percentage() = Percentage(this.fold(1.0) { acc, other -> acc * other.fraction })
        fun Collection<Percentage>.percentage() = Percentage(this.sumOf { it.fraction } / this.size.toDouble())
    }
    init {
        require(fraction in 0.0..1.0) { "Fraction must be [0, 1]" }
    }

    private fun toDouble() = fraction * 10000.0
    internal fun roundToDoubleTwoDecimals() = toDouble().roundToInt() / 100.0
    internal fun roundToInt() = toDouble().roundToInt() / 100
}

internal class ProgressBar(private val maxValue: Number) {
    private var progress = 0.percentage

    init {
        require(maxValue.toInt() > 0) { "maxValue må være større enn 0" }
    }

    fun progress(currentValue: Number) {
        progress = if (currentValue.toInt() > maxValue.toInt()) 100.percentage else currentValue.percentage(maxValue)
    }

    fun done(cb: () -> Unit) {
        if (progress.roundToInt() < 100) return
        cb()
    }

    override fun toString() = toString(10)

    internal fun toString(maxWidth: Int): String {
        val progressLength = (maxWidth * progress.roundToInt()/100.0).toInt()
        val remainder = maxWidth - progressLength
        val sb = StringBuilder()
        sb.append("[")
        sb.append("=".repeat(progressLength))
        sb.append(">".padEnd(remainder, ' '))
        sb.append("]")
        sb.append(" ")
        sb.append(progress.roundToDoubleTwoDecimals())
        sb.append(" %")
        return sb.toString()
    }

    internal companion object {
        fun progressBar(progressbars: Collection<ProgressBar>, maxWidth: Int = 10): ProgressBar {
            return ProgressBar(1).also { it.progress = progressbars.map { it.progress }.percentage() }
        }
    }
}
