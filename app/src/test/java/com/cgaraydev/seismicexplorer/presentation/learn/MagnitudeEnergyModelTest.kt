package com.cgaraydev.seismicexplorer.presentation.learn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MagnitudeEnergyModelTest {
    @Test
    fun oneUnitCorrespondsToAboutThirtyTwoTimesEnergy() {
        val result = MagnitudeEnergyModel.compare(5.0, 6.0)
        assertEquals(GreaterMagnitude.B, result.greater)
        assertEquals(31.6227766, result.factor, 0.00001)
    }

    @Test
    fun twoUnitsCorrespondToOneThousandTimesEnergy() {
        assertEquals(1000.0, MagnitudeEnergyModel.compare(4.0, 6.0).factor, 0.00001)
    }

    @Test
    fun reversingValuesChangesOnlyWhichMagnitudeIsLarger() {
        val result = MagnitudeEnergyModel.compare(6.0, 5.0)
        assertEquals(GreaterMagnitude.A, result.greater)
        assertEquals(31.6227766, result.factor, 0.00001)
    }

    @Test
    fun equalValuesHaveUnitRatio() {
        val result = MagnitudeEnergyModel.compare(5.0, 5.0)
        assertEquals(GreaterMagnitude.EQUAL, result.greater)
        assertEquals(1.0, result.factor, 0.0)
    }

    @Test
    fun fractionalDifferenceUsesLogarithmicRelation() {
        assertEquals(10.0, MagnitudeEnergyModel.compare(5.0, 5.0 + 2.0 / 3.0).factor, 0.00001)
    }

    @Test
    fun rejectsNonFiniteInputs() {
        assertThrows(IllegalArgumentException::class.java) {
            MagnitudeEnergyModel.compare(Double.NaN, 5.0)
        }
    }
}
