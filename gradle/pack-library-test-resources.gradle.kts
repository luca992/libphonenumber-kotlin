val rootProjectAbsPath = rootProject.projectDir.absolutePath
val path = """"$rootProjectAbsPath/libphonenumber/build/generated/moko-resources/jsMain/res""""
val webpackConfig = File(projectDir, "webpack.config.d/pack-library-resources-generated.js")

fun createWebpackConfig() {
    val configText = """const path = require('path');

const mokoResourcePath = path.resolve($path);

config.module.rules.push({
    test: /(ShortNumberMetadataProto_|PhoneNumberMetadataProto_|PhoneNumberMetadataProtoForTesting_|PhoneNumberAlternateFormatsProto_)[^.]+${'$'}/,
    include: [
        path.resolve(mokoResourcePath, "assets"),
    ],
    type: 'asset/inline',
    generator: {
        dataUrl: content => {
            // Convert buffer content to base64 and prepend it with mimetype
            return `${'$'}{content.toString('base64')}`
        }
    }
});"""
    webpackConfig.writeText(configText)
}

tasks.create("createPackResourcesWebpackConfig") {
    // Define the task's output
    outputs.file(webpackConfig)

    doFirst {
        createWebpackConfig()
    }
}

tasks.getByName("compileKotlinJs").dependsOn("createPackResourcesWebpackConfig")
