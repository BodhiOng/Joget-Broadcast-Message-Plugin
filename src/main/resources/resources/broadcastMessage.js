(function ($) {

    $.fn.broadcastMessage = function (options) {
        var container = $(this);
        var initialMessage = options.initialMessage || "";
        var messageReadStatus = {};
        
        // Check if there's a message to display
        if (initialMessage && initialMessage.trim() !== "") {
            showBroadcastBanner(initialMessage);
        }
        
        // Initialize WebSocket connection
        const ws = new WebSocket(((window.location.protocol === "https:") ? "wss://" : "ws://") + 
                               window.location.host + 
                               options.contextPath + 
                               "/web/socket/plugin/org.joget.dx82.sample.BroadcastMessagePlugin");
        
        ws.onopen = function(event) {
            console.log("WebSocket connection established");
        };
        
        ws.onmessage = function(event) {
            try {
                let data = JSON.parse(event.data);
                if (data.message) {
                    showBroadcastBanner(data.message);
                }
            } catch (e) {
                console.error("Error parsing message", e);
            }
        };
        
        ws.onclose = function(event) {
            console.log("WebSocket connection closed");
        };
        
        ws.onerror = function(event) {
            console.error("WebSocket error", event);
        };
        
        // Mark as Read button functionality
        container.find('#broadcastClose').on('click', function() {
            container.find('.broadcast-message-banner').removeClass('show');
            // Store in localStorage that this message has been read
            const messageText = container.find('#broadcastText').text().trim();
            if (messageText) {
                messageReadStatus[messageText] = true;
                localStorage.setItem('broadcastMessageReadStatus', JSON.stringify(messageReadStatus));
                
                // Optional: You could add code here to update the status in the database
                // For example, make an AJAX call to mark the message as read in the CRUD form
                console.log("Message marked as read: " + messageText);
            }
        });
        
        
        // Function to show broadcast banner
        function showBroadcastBanner(message) {
            // For testing purposes, always show the initial message
            // Later you can uncomment the localStorage check
            /*
            // Check if this message has been read before
            const storedStatus = localStorage.getItem('broadcastMessageReadStatus');
            if (storedStatus) {
                messageReadStatus = JSON.parse(storedStatus);
                if (messageReadStatus[message]) {
                    return; // User has already read this message
                }
            }
            */
            
            container.find('#broadcastText').text(message);
            container.find('.broadcast-message-banner').addClass('show');
        }
        
    };
    
})(jQuery);
