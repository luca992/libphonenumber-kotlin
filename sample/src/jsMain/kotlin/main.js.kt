import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import io.luca992.libphonenumber.sample.App
import io.michaelrocks.libphonenumber.kotlin.PhoneNumberUtil
import io.michaelrocks.libphonenumber.kotlin.metadata.init.MokoAssetResourceMetadataLoader
import org.jetbrains.skiko.wasm.onWasmReady

fun main() {
    onWasmReady {
        Window("libphonenumber-kotlin sample") {
            Column(modifier = Modifier.fillMaxSize()) {
                val util = remember { PhoneNumberUtil.createInstance(MokoAssetResourceMetadataLoader()) }
                App(util, "Web")
            }
        }
    }
}
