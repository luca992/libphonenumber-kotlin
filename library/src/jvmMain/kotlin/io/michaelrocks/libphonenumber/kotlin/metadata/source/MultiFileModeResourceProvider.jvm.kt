package io.michaelrocks.libphonenumber.kotlin.metadata.source

import dev.icerock.moko.resources.AssetResource
import dev.icerock.moko.resources.ResourceContainer
import dev.icerock.moko.resources.getAssetByFilePath

actual fun ResourceContainer<AssetResource>.getAssetByFilePath(
    filePath: String
): AssetResource? = getAssetByFilePath(filePath)