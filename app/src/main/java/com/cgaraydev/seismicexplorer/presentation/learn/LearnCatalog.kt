package com.cgaraydev.seismicexplorer.presentation.learn

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.cgaraydev.seismicexplorer.R

/** Contenido educativo local. IDs estables para navegación y futuros accesos contextuales. */
object LearnCatalog {
    const val MAGNITUDE_INTENSITY = "magnitud-intensidad"

    fun contains(articleId: String): Boolean = articleId == MAGNITUDE_INTENSITY

    val topics: List<LearnTopic> = listOf(
        LearnTopic("magnitude", R.string.learn_topic_magnitude, R.string.learn_topic_magnitude_hint,
            R.drawable.ic_learn_magnitude, MAGNITUDE_INTENSITY),
        LearnTopic("intensity", R.string.learn_topic_intensity, R.string.learn_topic_intensity_hint,
            R.drawable.ic_learn_intensity, MAGNITUDE_INTENSITY),
        LearnTopic("depth", R.string.learn_topic_depth, R.string.learn_topic_depth_hint,
            R.drawable.ic_learn_depth, null),
        LearnTopic("waves", R.string.learn_topic_waves, R.string.learn_topic_waves_hint,
            R.drawable.ic_learn_waves, null)
    )

    val magnitudeSections: List<LearnSection> = listOf(
        LearnSection("01", R.string.learn_section_one, R.string.learn_section_one_body),
        LearnSection("02", R.string.learn_section_two, R.string.learn_section_two_body),
        LearnSection("03", R.string.learn_section_three, R.string.learn_section_three_body),
        LearnSection("04", R.string.learn_section_four, R.string.learn_section_four_body)
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
}

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
