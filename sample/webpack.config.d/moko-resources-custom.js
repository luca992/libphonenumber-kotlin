// noinspection JSUnnecessarySemicolon
;(function(config) {
    const path = require('path');
    const mokoResourcePath = path.resolve("/Users/lucaspinazzola/Projects/libphonenumber-kotlin/sample/build/generated/moko/jsMain/iomichaelrockslibphonenumbersample/res");

    config.module.rules.push({
        test: /(ShortNumberMetadataProto_|PhoneNumberMetadataProto_|PhoneNumberAlternateFormatsProto_)[^.]+$/,
        include: [
            path.resolve(mokoResourcePath, "files"),
        ],
        type: 'asset/inline',
        generator: {
            dataUrl: content => {
                // Convert buffer content to base64 and prepend it with mimetype
                return `${content.toString('base64')}`
            }
        }
    });
})(config);