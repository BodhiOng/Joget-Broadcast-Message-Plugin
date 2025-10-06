<div id="broadcast_message_container">
    <link rel="stylesheet" href="${request.contextPath}/plugin/org.joget.dx82.sample.BroadcastMessagePlugin/broadcastMessage.css">
    <script src="${request.contextPath}/plugin/org.joget.dx82.sample.BroadcastMessagePlugin/broadcastMessage.js"></script>

    <div class="broadcast-message-banner show" id="broadcastBanner">
        <div class="broadcast-message-content">
            <div class="broadcast-icon">
                <i class="fas fa-bullhorn"></i>
            </div>
            <div class="broadcast-text" id="broadcastText">
                ${message!}
            </div>
            <div class="broadcast-close" id="broadcastClose">
                <i class="fas fa-times"></i>
            </div>
        </div>
    </div>
    
    <script>
        $(document).ready(function(){
            $("#broadcast_message_container").broadcastMessage({
                "contextPath" : "${request.contextPath}",
                "initialMessage" : "${message!}"
            });
        });
    </script>
</div>
