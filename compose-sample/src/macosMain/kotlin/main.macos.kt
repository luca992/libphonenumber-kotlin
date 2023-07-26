import androidx.compose.ui.window.Window
import io.luca992.libphonenumber.sample.App
import platform.AppKit.NSApp
import platform.AppKit.NSApplication

fun main() {
    NSApplication.sharedApplication()
    Window("Sample App") {
        App("Native MacOS")
    }
    NSApp?.run()
}
