package com.cgaraydev.seismicexplorer.domain.repository

import com.cgaraydev.seismicexplorer.domain.model.ShakeMapImageResult
import com.cgaraydev.seismicexplorer.domain.model.ShakeMapResource

interface ShakeMapImageRepository {
    suspend fun load(resource: ShakeMapResource): ShakeMapImageResult
}
