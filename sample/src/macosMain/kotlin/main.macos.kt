import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import io.luca992.libphonenumber.sample.App
import io.michaelrocks.libphonenumber.kotlin.PhoneNumberUtil
import io.michaelrocks.libphonenumber.kotlin.metadata.init.MokoAssetResourceMetadataLoader
import platform.AppKit.NSApp
import platform.AppKit.NSApplication

fun main() {
    NSApplication.sharedApplication()
    Window("Sample App") {
        val util = remember { PhoneNumberUtil.createInstance(MokoAssetResourceMetadataLoader()) }
        App(util, "Native MacOS")
    }
    NSApp?.run()
}
