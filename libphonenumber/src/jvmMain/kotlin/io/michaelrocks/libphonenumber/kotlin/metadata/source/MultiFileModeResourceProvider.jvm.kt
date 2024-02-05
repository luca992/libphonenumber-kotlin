package io.michaelrocks.libphonenumber.kotlin.metadata.source

import dev.icerock.moko.resources.AssetResource
import dev.icerock.moko.resources.ResourceContainer
import dev.icerock.moko.resources.getAssetByFilePath

internal fun String.removeFirstSlash(): String {
    return removePrefix("/")
}

actual fun ResourceContainer<AssetResource>.getAssetByFilePath(
    filePath: String
): AssetResource? { //= getAssetByFilePath(filePath)
    val originalPath: String = filePath.removeFirstSlash()

    return AssetResource(
        resourcesClassLoader,
        originalPath,
        "assets/$originalPath"
    )
}