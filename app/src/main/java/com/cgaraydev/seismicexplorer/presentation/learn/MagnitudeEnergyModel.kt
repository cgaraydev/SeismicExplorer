package com.cgaraydev.seismicexplorer.presentation.learn

import kotlin.math.abs
import kotlin.math.pow

/** Comparación educativa de energía relativa; no representa energía medida ni intensidad local. */
object MagnitudeEnergyModel {
    fun compare(magnitudeA: Double, magnitudeB: Double): EnergyComparison {
        require(magnitudeA.isFinite() && magnitudeB.isFinite()) {
            "Las magnitudes deben ser finitas."
        }
        val greater = when {
            magnitudeA > magnitudeB -> GreaterMagnitude.A
            magnitudeB > magnitudeA -> GreaterMagnitude.B
            else -> GreaterMagnitude.EQUAL
        }
        val factor = 10.0.pow(1.5 * abs(magnitudeB - magnitudeA))
        return EnergyComparison(greater = greater, factor = factor)
    }
}

enum class GreaterMagnitude { A, B, EQUAL }

data class EnergyComparison(val greater: GreaterMagnitude, val factor: Double)
