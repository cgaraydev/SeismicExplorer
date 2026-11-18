package com.cgaraydev.seismicexplorer.presentation.learn

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cgaraydev.seismicexplorer.R
import com.cgaraydev.seismicexplorer.presentation.common.AtlasWaveformIcon
import kotlinx.coroutines.launch

/** Atlas esencial. Los temas no publicados se identifican y no generan rutas ficticias. */
@Composable
fun LearnHomeScreen(
    onOpenArticle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    val resources = LocalResources.current
    val visibleTopics = LearnCatalog.topics.filter { topic ->
        query.isBlank() ||
            resources.getString(topic.titleRes).contains(query.trim(), ignoreCase = true) ||
            resources.getString(topic.hintRes).contains(query.trim(), ignoreCase = true)
    }

    val colors = MaterialTheme.colorScheme
    val darkMode = colors.background.luminance() < 0.4f
    val cardColor = if (darkMode) colors.surfaceContainerHigh else colors.surfaceContainerLowest
    val cardBorder = if (darkMode) null else BorderStroke(1.dp, colors.outlineVariant)

    LazyColumn(
        modifier = modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("learn_home"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LearnBrand()
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.learn_title),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.learn_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item(key = "search") {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().testTag("learn_search"),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                placeholder = { Text(stringResource(R.string.learn_search_hint)) },
                leadingIcon = {
                    Icon(painterResource(R.drawable.ic_learn_search), contentDescription = null)
                }
            )
        }
        if (visibleTopics.isEmpty()) {
            item(key = "empty") {
                Text(stringResource(R.string.learn_no_results),
                    modifier = Modifier.testTag("learn_no_results"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            val topicRows = visibleTopics.chunked(2)
            items(topicRows.size, key = { index -> topicRows[index].first().id }) { index ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    topicRows[index].forEach { topic ->
                        LearnTopicCard(topic, onOpenArticle, Modifier.weight(1f))
                    }
                    if (topicRows[index].size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        item(key = "featured") {
            Card(
                onClick = { onOpenArticle(LearnCatalog.MAGNITUDE_INTENSITY) },
                modifier = Modifier.fillMaxWidth().testTag("learn_featured"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                border = cardBorder
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.learn_featured_title),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge)
                        Icon(painterResource(R.drawable.ic_observatory_arrow_right),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary)
                    }
                    Text(stringResource(R.string.learn_featured_description),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LearnMagnitudeMotif()
                }
            }
        }
        item(key = "faq") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.learn_faq_heading),
                    style = MaterialTheme.typography.titleLarge)
                Card(onClick = { onOpenArticle(LearnCatalog.MAGNITUDE_INTENSITY) },
                    modifier = Modifier.fillMaxWidth().testTag("learn_faq_richter"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (darkMode) colors.surfaceContainerLow else colors.surfaceContainerLowest
                    ),
                    border = cardBorder) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.learn_faq_richter), Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge)
                        Icon(painterResource(R.drawable.ic_observatory_arrow_right),
                            contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun LearnBrand() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AtlasWaveformIcon(modifier = Modifier.size(30.dp, 24.dp))
        Text(stringResource(R.string.app_name),
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LearnTopicCard(
    topic: LearnTopic,
    onOpenArticle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val darkMode = colors.background.luminance() < 0.4f
    val cardBorder = if (darkMode) null else BorderStroke(1.dp, colors.outlineVariant)
    Card(
        onClick = { topic.articleId?.let(onOpenArticle) },
        enabled = topic.articleId != null,
        modifier = modifier.height(150.dp).testTag("learn_card_${topic.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (darkMode) colors.surfaceContainerHigh else colors.surfaceContainerLowest,
            disabledContainerColor = colors.surfaceContainerLow,
            disabledContentColor = colors.onSurfaceVariant
        ),
        border = cardBorder
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(painterResource(topic.iconRes), contentDescription = null,
                modifier = Modifier.size(27.dp))
            Spacer(Modifier.weight(1f))
            Text(stringResource(topic.titleRes), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
            Text(stringResource(if (topic.articleId == null) R.string.learn_coming_soon else topic.hintRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LearnMagnitudeMotif() {
    val foreground = MaterialTheme.colorScheme.secondary
    val track = MaterialTheme.colorScheme.outline
    Canvas(Modifier.fillMaxWidth().height(30.dp).padding(horizontal = 18.dp, vertical = 8.dp)) {
        val y = size.height / 2f
        drawLine(track, Offset(0f, y), Offset(size.width, y), strokeWidth = 4.dp.toPx())
        drawCircle(Color(0xFF73B3DF), radius = 7.dp.toPx(), center = Offset(size.width * .28f, y))
        drawCircle(foreground, radius = 7.dp.toPx(), center = Offset(size.width * .72f, y))
    }
}

/** Cuaderno científico. Se reutiliza desde la pestaña y desde el detalle del sismo. */
@Composable
fun LearnArticleScreen(
    articleId: String,
    onBack: () -> Unit,
    onOpenSource: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val article = LearnCatalog.articleOrNull(articleId)
    if (article == null) {
        Column(modifier.fillMaxSize().statusBarsPadding().padding(20.dp)) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.detail_back)) }
            Text(stringResource(R.string.learn_unavailable))
        }
        return
    }

    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val sections = article.sections
    val colors = MaterialTheme.colorScheme
    val darkMode = colors.background.luminance() < 0.4f
    val highlightedSurface = if (darkMode) colors.surfaceContainerHigh else colors.surfaceContainerLowest
    LazyColumn(
        state = state,
        modifier = modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("learn_article"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onBack, modifier = Modifier.testTag("learn_back")) {
                        Text(stringResource(R.string.detail_back))
                    }
                    Spacer(Modifier.weight(1f))
                    Text(stringResource(R.string.learn_title),
                        style = MaterialTheme.typography.titleMedium)
                }
                Text(stringResource(article.titleRes),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold)
                Text(stringResource(article.subtitleRes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item(key = "concept") {
            LearnChapter(sections.first(), Modifier.testTag("learn_article_section_01"))
        }
        item(key = "diagram") {
            when (article.diagram) {
                LearnDiagram.INTENSITY_LOCATIONS -> LearnConceptDiagram()
                LearnDiagram.FOCUS_CROSS_SECTION -> LearnDepthDiagram()
            }
        }
        item(key = "takeaway") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = highlightedSurface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.learn_takeaway_heading),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary)
                    Text(stringResource(article.takeawayBodyRes),
                        style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { scope.launch { state.animateScrollToItem(4) } },
                        modifier = Modifier.testTag("learn_explore_difference")) {
                        Text(stringResource(R.string.learn_explore_more))
                    }
                }
            }
        }
        item(key = "related") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.learn_related_heading),
                    style = MaterialTheme.typography.titleLarge)
                sections.drop(1).forEachIndexed { index, section ->
                    TextButton(
                        onClick = { scope.launch { state.animateScrollToItem(5 + index) } },
                        modifier = Modifier.fillMaxWidth()
                            .testTag("learn_related_${section.number}")
                    ) {
                        Text(section.number, color = MaterialTheme.colorScheme.secondary)
                        Text(stringResource(section.titleRes),
                            modifier = Modifier.weight(1f).padding(start = 12.dp))
                        Icon(painterResource(R.drawable.ic_observatory_arrow_right),
                            contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
        sections.drop(1).forEach { section ->
            item(key = section.number) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    LearnChapter(section, Modifier.testTag("learn_article_section_${section.number}"))
                }
            }
        }
        item(key = "sources") {
            Column(Modifier.testTag("learn_sources"),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(stringResource(R.string.learn_sources_heading),
                    style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.learn_sources_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                article.sources.forEachIndexed { index, source ->
                    TextButton(onClick = { onOpenSource(source.url) },
                        modifier = Modifier.fillMaxWidth().testTag("learn_source_$index")) {
                        Text(stringResource(source.labelRes), Modifier.weight(1f))
                        Icon(painterResource(R.drawable.ic_observatory_arrow_right),
                            contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
                Text(stringResource(R.string.home_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LearnChapter(section: LearnSection, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(section.number, color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold)
            Box(Modifier.width(1.dp).height(48.dp)
                .background(MaterialTheme.colorScheme.outlineVariant))
            Column {
                Text(stringResource(R.string.learn_essential_concept),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(section.titleRes),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
            }
        }
        Text(stringResource(section.bodyRes), style = MaterialTheme.typography.bodyLarge)
    }
}

/** Corte conceptual editable: no es un mapa, no utiliza distancias reales. */
@Composable
private fun LearnDepthDiagram() {
    val colors = MaterialTheme.colorScheme
    val surfaceColor = colors.surfaceContainerLow
    val line = colors.onSurfaceVariant
    val accent = colors.secondary
    val description = stringResource(R.string.learn_depth_diagram_accessibility)
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("learn_depth_diagram"),
        color = surfaceColor,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.learn_depth_diagram_heading),
                style = MaterialTheme.typography.titleMedium)
            Canvas(Modifier.fillMaxWidth().height(154.dp)
                .clearAndSetSemantics { contentDescription = description }) {
                val surfaceY = size.height * .20f
                val centerX = size.width * .50f
                val focusY = size.height * .76f
                val epicenter = Offset(centerX, surfaceY)
                val focus = Offset(centerX, focusY)
                // Superficie, traza simplificada de falla y profundidad vertical.
                drawLine(line, Offset(0f, surfaceY), Offset(size.width, surfaceY),
                    strokeWidth = 2.dp.toPx())
                drawLine(line.copy(alpha = .65f),
                    Offset(size.width * .18f, size.height * .97f),
                    Offset(size.width * .81f, size.height * .53f),
                    strokeWidth = 2.dp.toPx())
                drawLine(accent, epicenter, focus, strokeWidth = 2.dp.toPx())
                drawCircle(accent, radius = 6.dp.toPx(), center = epicenter)
                drawCircle(accent, radius = 9.dp.toPx(), center = focus)
                drawCircle(surfaceColor, radius = 3.dp.toPx(), center = focus)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.learn_depth_diagram_epicenter),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant)
                Text(stringResource(R.string.learn_depth_diagram_hypocenter),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant)
            }
            Text(stringResource(R.string.learn_depth_diagram_caveat),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun LearnConceptDiagram() {
    val line = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.secondary
    val surface = MaterialTheme.colorScheme.surfaceContainerLow
    val description = stringResource(R.string.learn_diagram_accessibility)
    Surface(modifier = Modifier.fillMaxWidth().testTag("learn_diagram"),
        color = surface, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Canvas(Modifier.fillMaxWidth().height(132.dp)
                .clearAndSetSemantics { contentDescription = description }) {
                val source = Offset(size.width * .16f, size.height * .50f)
                val destinations = listOf(.17f, .50f, .83f).map { y ->
                    Offset(size.width * .82f, size.height * y)
                }
                destinations.forEach { target ->
                    drawLine(line, source, target, strokeWidth = 1.5.dp.toPx())
                    drawCircle(line, radius = 6.dp.toPx(), center = target)
                    drawCircle(surface, radius = 4.dp.toPx(), center = target)
                }
                drawCircle(accent, radius = 9.dp.toPx(), center = source)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.learn_diagram_source),
                    style = MaterialTheme.typography.labelMedium)
                Text(stringResource(R.string.learn_diagram_places),
                    style = MaterialTheme.typography.labelMedium)
            }
            Text(stringResource(R.string.learn_diagram_caveat),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
