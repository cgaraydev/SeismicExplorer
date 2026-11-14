package com.cgaraydev.seismicexplorer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class MainActivityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launch_displaysHomeScreen() {
        composeRule
            .onNodeWithText("Actividad sísmica mundial")
            .assertIsDisplayed()
    }

    @Test
    fun selectingPeriod_updatesStateThroughViewModel() {
        composeRule
            .onNodeWithText("7 días")
            .performClick()

        composeRule
            .onNodeWithText("7 días")
            .assertIsSelected()
    }
}