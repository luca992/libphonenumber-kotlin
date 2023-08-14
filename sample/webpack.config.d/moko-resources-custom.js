// noinspection JSUnnecessarySemicolon
;(function(config) {
    const path = require('path');
    const mokoResourcePath = path.resolve("/Users/lucaspinazzola/Projects/libphonenumber-kotlin/sample/build/generated/moko/jsMain/iomichaelrockslibphonenumbersample/res");

    config.module.rules.push({
        test: /(ShortNumberMetadataProto_|PhoneNumberMetadataProto_|PhoneNumberAlternateFormatsProto_)[^.]+$/,
        include: [
            path.resolve(mokoResourcePath, "files"),
        ],
        type: 'asset/resource'
    });
})(config);