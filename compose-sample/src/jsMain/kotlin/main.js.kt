import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import io.luca992.libphonenumber.sample.App
import org.jetbrains.skiko.wasm.onWasmReady

fun main() {
    onWasmReady {
        Window("libphonenumber-kotlin sample") {
            Column(modifier = Modifier.fillMaxSize()) {
//                App("Web")
            }
        }
    }
}
