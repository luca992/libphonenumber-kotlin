/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import io.luca992.libphonenumber.sample.App
import io.michaelrocks.libphonenumber.kotlin.PhoneNumberUtil
import io.michaelrocks.libphonenumber.kotlin.metadata.init.MokoAssetResourceMetadataLoader
import platform.UIKit.UIViewController

fun MainViewController() : UIViewController = ComposeUIViewController {
    val util = remember { PhoneNumberUtil.createInstance(MokoAssetResourceMetadataLoader()) }
    App(util, "iOS")
}
