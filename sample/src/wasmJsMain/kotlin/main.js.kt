import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.CanvasBasedWindow
import io.luca992.libphonenumber.sample.App

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    CanvasBasedWindow("libphonenumber-kotlin wasmJs sample") {
        Column(modifier = Modifier.fillMaxSize()) {
            App("WasmJs Web")
        }
    }
}
