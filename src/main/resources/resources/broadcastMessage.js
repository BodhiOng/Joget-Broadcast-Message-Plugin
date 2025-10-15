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
        
        // Load previously read messages from localStorage
        const storedStatus = localStorage.getItem('broadcastMessageReadStatus');
        if (storedStatus) {
            try {
                messageReadStatus = JSON.parse(storedStatus);
                console.log("Loaded read status for " + Object.keys(messageReadStatus).length + " messages");
            } catch (e) {
                console.error("Error parsing stored message read status:", e);
                messageReadStatus = {};
            }
        }
        
        // Initialize message data
        if (messagesData && messagesData.messages) {
            // Get all messages
            let allMessages = messagesData.messages || [];
            console.log("Received " + allMessages.length + " total messages");
            
            // Filter out already read messages
            messages = allMessages.filter(msg => !messageReadStatus[msg.id] && !messageReadStatus[msg.text]);
            console.log("After filtering: " + messages.length + " unread messages");
            
            totalPages = messages.length;
            currentPage = 1; // Start with the first message
            
            // Display the first message if there are any unread messages
            if (messages.length > 0) {
                console.log("Showing first unread message: " + JSON.stringify(messages[0]));
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
            } else {
                // No unread messages - don't show anything
                console.log("No unread messages found, hiding banner");
                container.find('.broadcast-message-banner').removeClass('show');
                container.find('#prevPage, #nextPage').hide();
                container.find('table').hide();
            }
        } else if (initialMessage && initialMessage.trim() !== "") {
            // Fallback to initial message if no messages data, but only if not already read
            if (!messageReadStatus[initialMessage]) {
                showBroadcastBanner(initialMessage);
            } else {
                console.log("Initial message has already been read, not showing: " + initialMessage);
                container.find('.broadcast-message-banner').removeClass('show');
            }
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
                    let allMessages = data.messages || [];
                    console.log("WebSocket received " + allMessages.length + " total messages");
                    
                    // Filter out already read messages
                    messages = allMessages.filter(msg => !messageReadStatus[msg.id] && !messageReadStatus[msg.text]);
                    console.log("After filtering: " + messages.length + " unread messages");
                    
                    // Set the correct total pages based on unread messages length
                    totalPages = messages.length;
                    currentPage = 1;
                    
                    updatePaginationDisplay();
                    
                    // Display the first unread message if there are any
                    if (messages.length > 0) {
                        console.log("WebSocket showing first unread message: " + JSON.stringify(messages[0]));
                        showMessage(messages[0]);
                        
                        // Force pagination buttons to be visible if there are multiple messages
                        if (messages.length > 1) {
                            console.log("WebSocket forcing pagination buttons to be visible - " + messages.length + " messages");
                            container.find('#prevPage, #nextPage').css('display', 'inline-block');
                            container.find('table').css('display', 'inline-table');
                        }
                    } else {
                        // No unread messages - hide everything
                        console.log("WebSocket: No unread messages found, hiding banner");
                        container.find('.broadcast-message-banner').removeClass('show');
                        container.find('#prevPage, #nextPage').hide();
                        container.find('table').hide();
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
            // Get current message ID and text
            const currentMessageIndex = currentPage - 1;
            if (messages.length > 0 && currentMessageIndex >= 0 && currentMessageIndex < messages.length) {
                const currentMessage = messages[currentMessageIndex];
                const messageId = currentMessage.id;
                const messageText = currentMessage.text;
                
                // Store both ID and text in localStorage
                if (messageId) {
                    messageReadStatus[messageId] = true;
                    // Also store by text as fallback
                    if (messageText) {
                        messageReadStatus[messageText] = true;
                    }
                    
                    localStorage.setItem('broadcastMessageReadStatus', JSON.stringify(messageReadStatus));
                    console.log("Message marked as read - ID: " + messageId + ", Text: " + messageText);
                    
                    // Hide the current message
                    container.find('.broadcast-message-banner').removeClass('show');
                    
                    // Move to the next unread message if available
                    findAndShowNextUnreadMessage();
                }
            }
        });
        
        // Function to find and show the next unread message
        function findAndShowNextUnreadMessage() {
            // Filter out read messages
            const unreadMessages = messages.filter(msg => !messageReadStatus[msg.id]);
            console.log(unreadMessages.length + " unread messages remaining");
            
            if (unreadMessages.length > 0) {
                // Update the messages array with only unread messages
                messages = unreadMessages;
                totalPages = messages.length;
                currentPage = 1;
                
                // Show the first unread message
                showMessage(messages[0]);
                updatePaginationDisplay();
            } else {
                // All messages have been read
                console.log("All messages have been read");
                container.find('.broadcast-message-banner').removeClass('show');
                // Hide pagination as there are no more messages
                container.find('#prevPage, #nextPage').hide();
                container.find('table').hide();
            }
        }
        
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
            // Check if this message has been read before
            if (messageReadStatus[message]) {
                console.log("Message has already been read, not showing: " + message);
                // User has already read this message - hide the banner
                container.find('.broadcast-message-banner').removeClass('show');
                return;
            }
            
            // Only show if we have an actual message
            if (message && message.trim() !== "") {
                container.find('#broadcastText').text(message);
                container.find('.broadcast-message-banner').addClass('show');
            } else {
                // No message to show
                container.find('.broadcast-message-banner').removeClass('show');
            }
        }
    };
    
})(jQuery);
