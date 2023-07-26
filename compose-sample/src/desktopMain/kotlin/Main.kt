import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.luca992.libphonenumber.sample.App


fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App("Desktop")
    }
}
