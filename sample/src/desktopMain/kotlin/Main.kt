import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.luca992.libphonenumber.sample.App
import io.michaelrocks.libphonenumber.kotlin.PhoneNumberUtil
import io.michaelrocks.libphonenumber.kotlin.metadata.init.ClassPathResourceMetadataLoader


fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        val util = remember { PhoneNumberUtil.createInstance(ClassPathResourceMetadataLoader()) }
        App(util, "Desktop")
    }
}
