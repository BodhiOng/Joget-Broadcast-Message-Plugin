(function ($) {

    $.fn.broadcastMessage = function (options) {
        var container = $(this);
        var initialMessage = options.initialMessage || "";
        var messagesDataStr = options.messagesData || "{}";
        var messageReadStatus = {};
        var currentPage = 1;
        var totalPages = 1;
        var messages = [];
        
        // Parse messagesData from JSON string
        var messagesData = {};
        console.log("Raw messagesDataStr:", messagesDataStr);
        try {
            if (typeof messagesDataStr === 'string') {
                messagesData = JSON.parse(messagesDataStr);
                console.log("Parsed messagesData:", messagesData);
            } else {
                messagesData = messagesDataStr;
                console.log("Using messagesDataStr directly:", messagesData);
            }
        } catch (e) {
            console.error("Error parsing messagesData JSON:", e);
            console.error("Invalid JSON string:", messagesDataStr);
            messagesData = {};
        }
        
        // Initialize message data
        if (messagesData && messagesData.messages) {
            messages = messagesData.messages || [];
            console.log("Received " + messages.length + " messages");
            
            totalPages = messages.length;
            currentPage = 1; // Start with the first message
            
            // Display the first message
            if (messages.length > 0) {
                console.log("Showing first message: " + JSON.stringify(messages[0]));
                showMessage(messages[0]);
                updatePaginationDisplay();
                
                // Force pagination controls to be always visible if there are multiple messages
                if (messages.length > 1) {
                    console.log("Forcing pagination controls to be visible - " + messages.length + " messages");
                    // Immediate display
                    container.find('#broadcastPagination').css('display', 'flex !important').show();
                    
                    // Also set with timeout to handle any CSS that might override it
                    setTimeout(function() {
                        container.find('#broadcastPagination').attr('style', 'display: flex !important');
                        console.log("Pagination element after timeout: " + container.find('#broadcastPagination').css('display'));
                    }, 100);
                    
                    // And another timeout just to be sure
                    setTimeout(function() {
                        container.find('#broadcastPagination').attr('style', 'display: flex !important');
                        console.log("Pagination element after second timeout: " + container.find('#broadcastPagination').css('display'));
                    }, 1000);
                }
            } else if (initialMessage && initialMessage.trim() !== "") {
                // Fallback to initial message if no messages from server
                console.log("No messages found, showing initial message");
                showBroadcastBanner(initialMessage);
                // Hide pagination controls if there's only one message
                container.find('#broadcastPagination').hide();
            }
        } else if (initialMessage && initialMessage.trim() !== "") {
            // Fallback to initial message if no messages data
            showBroadcastBanner(initialMessage);
        }
        
        // Initialize WebSocket connection
        const ws = new WebSocket(((window.location.protocol === "https:") ? "wss://" : "ws://") + 
                               window.location.host + 
                               options.contextPath + 
                               "/web/socket/plugin/org.joget.marketplace.BroadcastMessagePlugin");
        
        ws.onopen = function(event) {
            console.log("WebSocket connection established");
        };
        
        ws.onmessage = function(event) {
            try {
                let data = JSON.parse(event.data);
                
                if (data.type === "messages") {
                    // Handle paginated messages
                    messages = data.messages || [];
                    console.log("WebSocket received " + messages.length + " messages");
                    
                    // Set the correct total pages based on messages length
                    totalPages = messages.length;
                    currentPage = 1;
                    
                    updatePaginationDisplay();
                    
                    // Display the first message
                    if (messages.length > 0) {
                        console.log("WebSocket showing first message: " + JSON.stringify(messages[0]));
                        showMessage(messages[0]);
                        
                        // Force pagination buttons to be visible if there are multiple messages
                        if (messages.length > 1) {
                            console.log("WebSocket forcing pagination buttons to be visible - " + messages.length + " messages");
                            container.find('#prevPage, #nextPage').css('display', 'inline-block');
                            container.find('table').css('display', 'inline-table');
                        }
                    }
                } else if (data.message) {
                    // Handle single message (legacy support)
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
                console.log("Message marked as read: " + messageText);
            }
        });
        
        // Pagination controls
        container.find('#prevPage').on('click', function() {
            if (currentPage > 1) {
                currentPage--;
                showCurrentMessage();
                updatePaginationDisplay();
            }
        });
        
        container.find('#nextPage').on('click', function() {
            if (currentPage < totalPages) {
                currentPage++;
                showCurrentMessage();
                updatePaginationDisplay();
            }
        });
        
        // Function to show the current message based on currentPage
        function showCurrentMessage() {
            if (messages.length > 0 && currentPage > 0 && currentPage <= messages.length) {
                // Show the message at the current page index (0-based array)
                showMessage(messages[currentPage - 1]);
            }
        }
        
        // Function to update pagination display
        function updatePaginationDisplay() {
            container.find('#currentPage').text(currentPage);
            container.find('#totalPages').text(totalPages);
            
            console.log("Updating pagination: page " + currentPage + " of " + totalPages);
            
            // Enable/disable pagination buttons
            if (currentPage <= 1) {
                container.find('#prevPage').prop('disabled', true).addClass('disabled');
            } else {
                container.find('#prevPage').prop('disabled', false).removeClass('disabled');
            }
            
            if (currentPage >= totalPages) {
                container.find('#nextPage').prop('disabled', true).addClass('disabled');
            } else {
                container.find('#nextPage').prop('disabled', false).removeClass('disabled');
            }
            
            // Always update the total pages display
            container.find('#totalPages').text(totalPages);
            
            // Show/hide pagination buttons
            if (totalPages <= 1) {
                console.log("Hiding pagination buttons - only one message");
                container.find('#prevPage, #nextPage').hide();
                container.find('table').hide();
            } else {
                console.log("Showing pagination buttons - " + totalPages + " messages");
                // Force display
                container.find('#prevPage, #nextPage').css('display', 'inline-block');
                container.find('table').css('display', 'inline-table');
                console.log("Pagination buttons visible: prev=" + (container.find('#prevPage').is(':visible')) + ", next=" + (container.find('#nextPage').is(':visible')));
            }
        }
        
        // Function to show a message object
        function showMessage(message) {
            if (message && message.text) {
                // Get priority info for debugging
                let priorityInfo = message.priority ? " (Priority: " + message.priority + ")" : "";
                console.log("Showing message with text: " + message.text + priorityInfo);
                
                // Show the message text in the banner
                showBroadcastBanner(message.text);
            }
        }
        
        // Function to show broadcast banner
        function showBroadcastBanner(message) {
            // For testing purposes, always show the message
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
