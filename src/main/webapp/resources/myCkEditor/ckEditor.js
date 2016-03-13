
var editor = CKEDITOR.replace('editor1');

// The "change" event is fired whenever a change is made in the editor.
editor.on( 'change', function( evt ) {
    // getData() returns CKEditor's HTML content.

    var data = evt.editor.getData();
    // console.log('Total bytes: ' + data.length);

    var changeEventButton = jQuery(".changeEventButton");
    if (typeof changeEventButton.attr("disabled") === 'undefined')
    {
        changeEventButton.click();
    }

    // console.log("data: " + data);
    jQuery(".hiddenBodyText input").val(data);
});

CKEDITOR.editorConfig = function(config) {
    // rule: elements [attributes]{styles}(classes)
    // config.allowedContent = 'img [*]';

//    CKEDITOR.config.toolbar = [
//        ['Styles','Format','Font','FontSize'],
//        '/',
//        ['Bold','Italic','Underline','StrikeThrough','-','Undo','Redo','-','Cut','Copy','Paste','Find','Replace','-','Outdent','Indent','-','Print','-','Save'],
//        '/',
//        ['NumberedList','BulletedList','-','JustifyLeft','JustifyCenter','JustifyRight','JustifyBlock'],
//        ['Image','Table','-','Link','Flash','Smiley','TextColor','BGColor','Source']
//    ] ;

    // All available editor features will be activated and input data will not be filtered
    config.allowedContent = true;

    // console.log("config.allowedContent: " + config.allowedContent)
    // console.log("config.disallowedContent: " + config.disallowedContent)

    // to avoid the replace of & to &amp;
    // forceSimpleAmpersand: true
}
