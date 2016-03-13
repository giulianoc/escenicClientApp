
CKEDITOR.editorConfig = function(config) {
    // rule: elements [attributes]{styles}(classes)
    // config.allowedContent = 'img [*]';

    CKEDITOR.config.toolbar = [
        ['Styles','Format','Font','FontSize'],
        '/',
        ['Bold','Italic','Underline','StrikeThrough','-','Undo','Redo','-','Cut','Copy','Paste','Find','Replace','-','Outdent','Indent','-','Print','-','Save'],
        '/',
        ['NumberedList','BulletedList','-','JustifyLeft','JustifyCenter','JustifyRight','JustifyBlock'],
        ['Image','Table','-','Link','Flash','Smiley','TextColor','BGColor','Source']
    ] ;

    // All available editor features will be activated and input data will not be filtered
    config.allowedContent = true;

    // console.log("config.allowedContent: " + config.allowedContent)
    // console.log("config.disallowedContent: " + config.disallowedContent)

    // to avoid the replace of & to &amp;
    // forceSimpleAmpersand: true
}
