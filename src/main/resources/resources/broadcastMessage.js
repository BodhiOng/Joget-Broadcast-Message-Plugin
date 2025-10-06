(function ($) {

    $.fn.broadcastMessage = function (options) {
        var container = $(this);
        var initialMessage = options.initialMessage || "";
        var isAdmin = false;
        var messageReadStatus = {};
        
        // Check if there's a message to display
        if (initialMessage && initialMessage.trim() !== "") {
            showBroadcastBanner(initialMessage);
        }
        
        // Add admin controls if user is admin
        checkIfAdmin();
        
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
        
        // Close button functionality
        container.find('#broadcastClose').on('click', function() {
            container.find('.broadcast-message-banner').removeClass('show');
            // Store in localStorage that this message has been read
            const messageText = container.find('#broadcastText').text().trim();
            if (messageText) {
                messageReadStatus[messageText] = true;
                localStorage.setItem('broadcastMessageReadStatus', JSON.stringify(messageReadStatus));
            }
        });
        
        // Admin panel functionality
        if (isAdmin) {
            // Add admin controls to the page
            container.append(
                '<div class="admin-controls"><i class="fas fa-bullhorn"></i></div>' +
                '<div class="admin-panel">' +
                '  <h4>Broadcast Message</h4>' +
                '  <textarea id="adminMessageInput" placeholder="Enter message to broadcast"></textarea>' +
                '  <button id="sendBroadcast">Send</button>' +
                '</div>'
            );
            
            // Toggle admin panel
            container.find('.admin-controls').on('click', function() {
                container.find('.admin-panel').toggleClass('show');
            });
            
            // Send broadcast message
            container.find('#sendBroadcast').on('click', function() {
                const message = container.find('#adminMessageInput').val().trim();
                if (message) {
                    ws.send(message);
                    container.find('#adminMessageInput').val('');
                    container.find('.admin-panel').removeClass('show');
                }
            });
        }
        
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
        
        // Function to check if current user is admin
        function checkIfAdmin() {
            $.ajax({
                url: options.contextPath + '/web/json/plugin/org.joget.apps.app.lib.UserProfileMenu/service',
                dataType: 'json',
                success: function(data) {
                    if (data && data.data) {
                        const roles = data.data.roles || [];
                        isAdmin = roles.includes('ROLE_ADMIN');
                        
                        if (isAdmin) {
                            // Initialize admin controls
                            initAdminControls();
                        }
                    }
                },
                error: function() {
                    console.error("Could not determine user role");
                }
            });
        }
        
        function initAdminControls() {
            // Admin controls are added when isAdmin is confirmed
        }
    };
    
})(jQuery);
