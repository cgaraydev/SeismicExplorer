package com.cgaraydev.seismicexplorer.presentation.learn

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.cgaraydev.seismicexplorer.R

/** Contenido educativo local. IDs estables para navegación y futuros accesos contextuales. */
object LearnCatalog {
    const val MAGNITUDE_INTENSITY = "magnitud-intensidad"
    const val EPICENTER_DEPTH = "epicentro-profundidad"
    const val REVISIONS = "revisiones-sismicas"

    fun contains(articleId: String): Boolean = articleOrNull(articleId) != null

    fun articleOrNull(articleId: String): LearnArticle? = articles[articleId]

    val topics: List<LearnTopic> = listOf(
        LearnTopic("magnitude", R.string.learn_topic_magnitude, R.string.learn_topic_magnitude_hint,
            R.drawable.ic_learn_magnitude, MAGNITUDE_INTENSITY),
        LearnTopic("intensity", R.string.learn_topic_intensity, R.string.learn_topic_intensity_hint,
            R.drawable.ic_learn_intensity, MAGNITUDE_INTENSITY),
        LearnTopic("depth", R.string.learn_topic_depth, R.string.learn_topic_depth_hint,
            R.drawable.ic_learn_depth, EPICENTER_DEPTH),
        LearnTopic("waves", R.string.learn_topic_waves, R.string.learn_topic_waves_hint,
            R.drawable.ic_learn_waves, null)
    )

    val magnitudeSections: List<LearnSection> = listOf(
        LearnSection("01", R.string.learn_section_one, R.string.learn_section_one_body),
        LearnSection("02", R.string.learn_section_two, R.string.learn_section_two_body),
        LearnSection("03", R.string.learn_section_three, R.string.learn_section_three_body),
        LearnSection("04", R.string.learn_section_four, R.string.learn_section_four_body)
    )

    val depthSections: List<LearnSection> = listOf(
        LearnSection("01", R.string.learn_depth_section_one, R.string.learn_depth_section_one_body),
        LearnSection("02", R.string.learn_depth_section_two, R.string.learn_depth_section_two_body),
        LearnSection("03", R.string.learn_depth_section_three, R.string.learn_depth_section_three_body),
        LearnSection("04", R.string.learn_depth_section_four, R.string.learn_depth_section_four_body)
    )

    val revisionSections: List<LearnSection> = listOf(
        LearnSection("01", R.string.learn_revision_section_one, R.string.learn_revision_section_one_body),
        LearnSection("02", R.string.learn_revision_section_two, R.string.learn_revision_section_two_body),
        LearnSection("03", R.string.learn_revision_section_three, R.string.learn_revision_section_three_body),
        LearnSection("04", R.string.learn_revision_section_four, R.string.learn_revision_section_four_body)
    )

    val sources: List<LearnSource> = listOf(
        LearnSource(R.string.learn_source_usgs_magnitude,
            "https://www.usgs.gov/faqs/what-difference-between-earthquake-magnitude-and-earthquake-intensity-what-modified-mercalli"),
        LearnSource(R.string.learn_source_usgs_types,
            "https://www.usgs.gov/programs/earthquake-hazards/magnitude-types"),
        LearnSource(R.string.learn_source_usgs_energy,
            "https://earthquake.usgs.gov/education/how_much_bigger.php"),
        LearnSource(R.string.learn_source_csn,
            "https://www.sismologia.cl/informacion/glosario.html")
    )

    val depthSources: List<LearnSource> = listOf(
        LearnSource(R.string.learn_depth_source_csn,
            "https://www.sismologia.cl/informacion/preguntas-frecuentes.html"),
        LearnSource(R.string.learn_depth_source_usgs,
            "https://www.usgs.gov/glossary/earthquake-hazards-program"),
        LearnSource(R.string.learn_depth_source_reference,
            "https://www.usgs.gov/faqs/what-does-it-mean-earthquake-occurred-a-depth-0-km-how-can-earthquake-have-a-negative-depth")
    )

    val revisionSources: List<LearnSource> = listOf(
        LearnSource(R.string.learn_revision_source_updates,
            "https://www.usgs.gov/faqs/whywhen-does-usgs-update-magnitude-earthquake"),
        LearnSource(R.string.learn_revision_source_agencies,
            "https://www.usgs.gov/faqs/why-do-usgs-earthquake-magnitudes-differ-those-published-other-agencies"),
        LearnSource(R.string.learn_revision_source_removed,
            "https://www.usgs.gov/faqs/why-do-some-earthquakes-disappear-maplist")
    )

    private val articles: Map<String, LearnArticle> = listOf(
        LearnArticle(
            id = MAGNITUDE_INTENSITY,
            titleRes = R.string.learn_article_title,
            subtitleRes = R.string.learn_article_subtitle,
            sections = magnitudeSections,
            sources = sources,
            diagram = LearnDiagram.INTENSITY_LOCATIONS,
            takeawayBodyRes = R.string.learn_takeaway_body
        ),
        LearnArticle(
            id = EPICENTER_DEPTH,
            titleRes = R.string.learn_depth_article_title,
            subtitleRes = R.string.learn_depth_article_subtitle,
            sections = depthSections,
            sources = depthSources,
            diagram = LearnDiagram.FOCUS_CROSS_SECTION,
            takeawayBodyRes = R.string.learn_depth_takeaway_body
        ),
        LearnArticle(
            id = REVISIONS,
            titleRes = R.string.learn_revision_article_title,
            subtitleRes = R.string.learn_revision_article_subtitle,
            sections = revisionSections,
            sources = revisionSources,
            diagram = LearnDiagram.REVISION_TIMELINE,
            takeawayBodyRes = R.string.learn_revision_takeaway_body
        )
    ).associateBy { it.id }
}

enum class LearnDiagram { INTENSITY_LOCATIONS, FOCUS_CROSS_SECTION, REVISION_TIMELINE }

data class LearnArticle(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val sections: List<LearnSection>,
    val sources: List<LearnSource>,
    val diagram: LearnDiagram,
    @StringRes val takeawayBodyRes: Int
)

data class LearnTopic(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val hintRes: Int,
    @DrawableRes val iconRes: Int,
    val articleId: String?
)

data class LearnSection(
    val number: String,
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int
)

data class LearnSource(
    @StringRes val labelRes: Int,
    val url: String
)
