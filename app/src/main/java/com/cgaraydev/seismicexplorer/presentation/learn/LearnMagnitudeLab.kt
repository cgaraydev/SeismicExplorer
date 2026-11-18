package com.cgaraydev.seismicexplorer.presentation.learn

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cgaraydev.seismicexplorer.R
import java.text.NumberFormat
import kotlin.math.roundToLong

/** Laboratorio local. Valores ficticios, sin acceso a USGS ni dependencia de Room. */
@Composable
fun LearnMagnitudeLabScreen(
    onBack: () -> Unit,
    onOpenArticle: (String) -> Unit,
    onOpenSource: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var magnitudeA by rememberSaveable { mutableFloatStateOf(5.0f) }
    var magnitudeB by rememberSaveable { mutableFloatStateOf(6.0f) }
    var showFormula by rememberSaveable { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val locale = LocalConfiguration.current.locales[0]
    val comparison = MagnitudeEnergyModel.compare(
        (magnitudeA.toDouble() * 10).roundToLong() / 10.0,
        (magnitudeB.toDouble() * 10).roundToLong() / 10.0
    )
    val number = NumberFormat.getNumberInstance(locale).apply {
        maximumFractionDigits = if (comparison.factor < 10.0) 1 else 0
        minimumFractionDigits = 0
    }
    val factorText = number.format(comparison.factor)
    val primaryText = colors.onBackground
    val surfaceText = colors.onSurface
    val accent = colors.secondary
    val blue = if (colors.background.red < 0.5f) Color(0xFF61A9D5) else Color(0xFF2376A2)

    LazyColumn(
        modifier = modifier.fillMaxSize().background(colors.background)
            .statusBarsPadding().testTag("learn_lab"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onBack, modifier = Modifier.testTag("learn_lab_back")) {
                    Text(stringResource(R.string.learn_lab_back))
                }
                Text(stringResource(R.string.learn_lab_eyebrow), color = accent,
                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.learn_lab_title), color = primaryText,
                    style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            }
        }
        item(key = "controls") {
            Surface(
                color = colors.surfaceContainerHigh,
                contentColor = surfaceText,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, colors.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MagnitudeSlider(
                        title = stringResource(R.string.learn_lab_magnitude_a),
                        value = magnitudeA,
                        onValueChange = { magnitudeA = it },
                        formattedValue = numberForMagnitude(magnitudeA, locale),
                        locale = locale,
                        accentColor = blue,
                        testTag = "learn_lab_slider_a"
                    )
                    MagnitudeSlider(
                        title = stringResource(R.string.learn_lab_magnitude_b),
                        value = magnitudeB,
                        onValueChange = { magnitudeB = it },
                        formattedValue = numberForMagnitude(magnitudeB, locale),
                        locale = locale,
                        accentColor = blue,
                        testTag = "learn_lab_slider_b"
                    )
                    HorizontalDivider(color = colors.outline)
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = stringResource(R.string.learn_lab_energy_factor, factorText),
                            modifier = Modifier.testTag("learn_lab_energy_ratio"),
                            color = accent,
                            style = MaterialTheme.typography.headlineLarge,
                            fontSize = if (factorText.length > 7) 26.sp else 36.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.learn_lab_approximate_label),
                            modifier = Modifier.weight(1f),
                            color = surfaceText,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    val comparisonText = when (comparison.greater) {
                        GreaterMagnitude.A -> stringResource(R.string.learn_lab_a_greater)
                        GreaterMagnitude.B -> stringResource(R.string.learn_lab_b_greater)
                        GreaterMagnitude.EQUAL -> stringResource(R.string.learn_lab_equal)
                    }
                    Text(comparisonText, color = surfaceText,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag("learn_lab_comparison"))
                    Text(stringResource(R.string.learn_lab_caveat),
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(
                        onClick = { showFormula = true },
                        modifier = Modifier.fillMaxWidth().testTag("learn_lab_formula_button")
                    ) { Text(stringResource(R.string.learn_lab_how_calculated)) }
                }
            }
        }
        item(key = "related") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.learn_lab_related), color = primaryText,
                    style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = { onOpenArticle(LearnCatalog.MAGNITUDE_INTENSITY) },
                    modifier = Modifier.fillMaxWidth().testTag("learn_lab_open_magnitude")) {
                    Text(stringResource(R.string.learn_lab_related_magnitude),
                        modifier = Modifier.weight(1f), color = primaryText)
                    Text("›", color = accent)
                }
                HorizontalDivider(color = colors.outlineVariant)
                TextButton(onClick = { onOpenArticle(LearnCatalog.EPICENTER_DEPTH) },
                    modifier = Modifier.fillMaxWidth().testTag("learn_lab_open_depth")) {
                    Text(stringResource(R.string.learn_lab_related_depth),
                        modifier = Modifier.weight(1f), color = primaryText)
                    Text("›", color = accent)
                }
            }
        }
    }

    if (showFormula) {
        AlertDialog(
            onDismissRequest = { showFormula = false },
            title = { Text(stringResource(R.string.learn_lab_formula_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.learn_lab_formula_description))
                    Text(stringResource(R.string.learn_lab_formula_equation))
                    Text(stringResource(R.string.learn_lab_formula_disclaimer))
                }
            },
            confirmButton = {
                TextButton(onClick = { showFormula = false },
                    modifier = Modifier.testTag("learn_lab_formula_close")) {
                    Text(stringResource(R.string.observatory_close))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showFormula = false
                    onOpenSource("https://earthquake.usgs.gov/education/how_much_bigger.php")
                }, modifier = Modifier.testTag("learn_lab_usgs")) {
                    Text(stringResource(R.string.learn_lab_official_source))
                }
            }
        )
    }
}

@Composable
private fun MagnitudeSlider(
    title: String,
    value: Float,
    formattedValue: String,
    locale: java.util.Locale,
    onValueChange: (Float) -> Unit,
    accentColor: Color,
    testTag: String
) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
            Text(formattedValue, style = MaterialTheme.typography.headlineSmall,
                color = colors.onSurface, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 3.0f..8.0f,
            steps = 49,
            modifier = Modifier.fillMaxWidth().testTag(testTag)
                .semantics { contentDescription = title },
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor
            )
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(numberForMagnitude(3.0f, locale), color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium)
            Text(numberForMagnitude(8.0f, locale), color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun numberForMagnitude(value: Float, locale: java.util.Locale): String =
    String.format(locale, "%.1f", value)
