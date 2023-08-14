package io.michaelrocks.libphonenumber.kotlin.metadata.source

import dev.icerock.moko.resources.AssetResource
import dev.icerock.moko.resources.ResourceContainer

// TODO: Remove after https://github.com/icerockdev/moko-resources/pull/550 is merged
actual fun ResourceContainer<AssetResource>.getAssetByFilePath(filePath: String): AssetResource? {
    //get name without extension and extension
    val ext = filePath.substringAfterLast('.', "")
    val name = filePath.substringBeforeLast('.')
        .replace('/', '+')

    return AssetResource(filePath.removePrefix("/"), name, ext, nsBundle)
}
