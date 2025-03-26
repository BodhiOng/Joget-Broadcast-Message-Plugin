(function ($) {

    $.fn.sampleChat = function (options) {
        var chatEl = $(this);
        
        const chatIcon = chatEl.find('#chatIcon');
        const chatpopup = chatEl.find('#chatPopup');
        
        chatIcon.off('click.sampleChat').on('click.sampleChat', function (e) {
            chatpopup.toggleClass("show");
        });
        
        const sendButton = chatEl.find('#chat-send-btn');
        const chatForm = chatEl.find('#chat-form');
        const userInput = chatEl.find('#user-input');
        
        chatForm.off('submit.sampleChat').on('submit.sampleChat', function (e) {
            e.preventDefault();
            return false;
        });
        
        const ws = new WebSocket(((window.location.protocol === "https:") ? "wss://" : "ws://") + window.location.host + options.contextPath + "/web/socket/plugin/org.joget.dx82.sample.SampleChatUiHtmlInjector");
        
        ws.onopen = function(event) {
            displayMessage(chatEl, options, 'Connection opened with timeStamp: ' + event.timeStamp, "", false);
            
            sendButton.off('click.sampleChat').on('click.sampleChat', function (e) {
                e.preventDefault();
                sendMessage(ws, chatEl, options);
            });

            userInput.off('keyup.sampleChat').on('keyup.sampleChat', function (e) {
                if (e.key === 'Enter' || e.keyCode === 13) {
                    sendMessage(ws, chatEl, options);
                }
            });
        };
        
        ws.onmessage = function(event) {
            let messageData = JSON.parse(event.data);
            displayMessage(chatEl, options, messageData.message, messageData.author, messageData.sentByMe);
        }; 

        ws.onclose = function(event) {
            displayMessage(chatEl, options, 'Connection closed with timeStamp: ' + event.timeStamp, "", false);
            displayMessage(chatEl, options, 'WebSocket closed<br/>', "", false);
        }; 

        ws.onError = function(event) {
            displayMessage(chatEl, options, "Error: " + event.data, "", false);
        };
    };
    
    function displayMessage(chatEl, options, message, author, isSentByMe) {
        const messagesList = $(chatEl).find('#messages');
        
        let messageItem = document.createElement("li");
        if(!isSentByMe){
            message = (author?(author + " : "):"") + message;
        }
        messageItem.textContent = message;
        messageItem.classList.add("message", isSentByMe ? "sent" : "received");
        messagesList.append(messageItem);

        // Auto-scroll to the latest message
        $(chatEl).find('.msger-messages')[0].scrollTop = $(chatEl).find('.msger-messages')[0].scrollHeight;
    }
    
    function sendMessage(ws, chatEl, options) {
        const msgText = chatEl.find('#user-input').val(); 
        if (!msgText)
            return;
            
        //send message to endpoint
        ws.send(msgText);
        
        chatEl.find('#user-input').val("");
    }
})(jQuery);