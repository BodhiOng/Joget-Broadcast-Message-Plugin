<div id="sample_chat_container">
    <link rel="stylesheet" href="${request.contextPath}/plugin/org.joget.dx82.sample.SampleChatUiHtmlInjector/sampleChat.css">
    <script src="${request.contextPath}/plugin/org.joget.dx82.sample.SampleChatUiHtmlInjector/sampleChat.js"></script>

    <div class="chat-icon" id="chatIcon">
        <i class="fas fa-comment-alt"></i>
    </div>

    <div class="chat-popup" id="chatPopup">
        <header class="msger-header">
            <div class="msger-header-title"><i class="fas fa-comment-alt"></i> Chat</div>
        </header>
        <div class="msger-messages">
            <ul id="messages"></ul>
        </div>
        <div class="msger-footer">
            <form class="msger-inputarea" id="chat-form">
                <input type="text" id="user-input" class="msger-input" placeholder="Enter your message...">
                <button type="button" class="msger-send-btn" id="chat-send-btn">Send</button>
            </form>
        </div>    
    </div>
    
    <script>
        $(document).ready(function(){
            $("#sample_chat_container").sampleChat({
                "contextPath" : "${request.contextPath}"
            });
        });
    </script>
</div>
