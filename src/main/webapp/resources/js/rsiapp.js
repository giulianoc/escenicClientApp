
var handle = {}

function on_start() {
    document.body.style.cursor='wait';

    PF('globalAjaxProgressBarDialog').show();
}

function on_complete() {
    document.body.style.cursor='default';

    PF('globalAjaxProgressBarDialog').hide();
}

function locksChannelMessage(lockMessage) {
    // console.log("lockMessage. id: " + lockMessage.id + ", keyField: " + lockMessage.keyField);

    var currentArticleId = jQuery(".hiddenArticleId label").text();
    // console.log("currentArticleId: " + currentArticleId);

    if (currentArticleId == lockMessage.id)
        jQuery(".locksToBeUpdated").click();
}

function userChannelMessage(messageInfo) {
    // console.log("messageInfo. from: " + messageInfo.from + ", to: " + messageInfo.to + ", message: " + messageInfo.message);

    var currentUserName = jQuery(".hiddenUserName label").text();
    // console.log("currentUserName: " + currentUserName);

    if (messageInfo.from != currentUserName)
    {
        receivedMessage([
            {name:'from',value:messageInfo.from},
            {name:'to',value:messageInfo.to},
            {name:'message',value:messageInfo.message}
        ])

        // allChatEditorScrollAtTheEnd();
    }
    else
    {
        console.log("This is a message I just sent and does not have to be written in my chat");
    }
}

function initDND() {
    $('.ui-treenode').draggable({
        helper: 'clone',
        scope: 'sectionTreeDropOnSectionsTable',
        zIndex: ++PrimeFaces.zindex,
        appendTo: 'body'
    });

    $('.droppoint').droppable({
        activeClass: 'ui-state-active',
        hoverClass: 'ui-state-highlight',
        tolerance: 'pointer',
        scope: 'sectionTreeDropOnSectionsTable',
        drop: function(event, ui) {
            var sectionId = ui.draggable.find('.hiddenSectionId label').text();

            console.log("sectionId: " + sectionId);
            sectionTreeDropOnSectionsTable([
                {name: 'sectionId', value:  sectionId}
            ]);
        }
    });
}

/*
function allChatEditorScrollAtTheEnd() {

    console.log('allChatEditorScrollAtTheEnd');

    var element = PF('allChatEditor').jq.find("iframe").contents().find('body');

    console.log("height: " + element.height())
    element.scrollTop(element.height());
    element.css("color", "red");
}
*/