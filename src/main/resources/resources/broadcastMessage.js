(function ($) {

    $.fn.broadcastMessage = function (options) {
        var container = $(this);
        var initialMessage = options.initialMessage || "";
        var initialPriority = options.initialPriority || "low";
        var messagesDataStr = options.messagesData || "{}";
        var messageReadStatus = {};
        var currentPage = 1;
        var totalPages = 1;
        var messages = [];
        
        // Parse messagesData from JSON string
        var messagesData = {};
        try {
            if (typeof messagesDataStr === 'string') {
                messagesData = JSON.parse(messagesDataStr);
            } else {
                messagesData = messagesDataStr;
            }
        } catch (e) {
            messagesData = {};
        }
        
        // Load previously read messages from localStorage
        const storedStatus = localStorage.getItem('broadcastMessageReadStatus');
        if (storedStatus) {
            try {
                messageReadStatus = JSON.parse(storedStatus);
            } catch (e) {
                messageReadStatus = {};
            }
        }
        
        // Store message priorities for tracking changes
        let messagePriorities = {};
        
        // Initialize message data
        if (messagesData && messagesData.messages) {
            // Get all messages
            let allMessages = messagesData.messages || [];
            
            // Store priorities for all messages
            allMessages.forEach(msg => {
                if (msg.id && msg.priority) {
                    messagePriorities[msg.id] = msg.priority;
                }
            });
            
            // Filter out already read messages
            messages = allMessages.filter(msg => !messageReadStatus[msg.id] && !messageReadStatus[msg.text]);
            
            totalPages = messages.length;
            currentPage = 1; // Start with the first message
            
            // Display the first message if there are any unread messages
            if (messages.length > 0) {
                showMessage(messages[0]);
                updatePaginationDisplay();
                
                // Force pagination controls to be always visible if there are multiple messages
                if (messages.length > 1) {
                    // Immediate display
                    container.find('#broadcastPagination').css('display', 'flex !important').show();
                    
                    // Also set with timeout to handle any CSS that might override it
                    setTimeout(function() {
                        container.find('#broadcastPagination').attr('style', 'display: flex !important');
                    }, 100);
                    
                    // And another timeout just to be sure
                    setTimeout(function() {
                        container.find('#broadcastPagination').attr('style', 'display: flex !important');
                    }, 1000);
                }
            } else {
                // No unread messages - don't show anything
                container.find('.broadcast-message-banner').removeClass('show');
                container.find('#prevPage, #nextPage').hide();
                container.find('table').hide();
            }
        } else if (initialMessage && initialMessage.trim() !== "") {
            // Fallback to initial message if no messages data, but only if not already read
            if (!messageReadStatus[initialMessage]) {
                // Use the provided initialPriority
                showBroadcastBanner(initialMessage, initialPriority);
            } else {
                container.find('.broadcast-message-banner').removeClass('show');
            }
        }
        
        // Initialize WebSocket connection
        const ws = new WebSocket(((window.location.protocol === "https:") ? "wss://" : "ws://") + 
                               window.location.host + 
                               options.contextPath + 
                               "/web/socket/plugin/org.joget.marketplace.BroadcastMessagePlugin");
        
        ws.onopen = function(event) {
            // Connection established
        };
        
        ws.onmessage = function(event) {
            try {
                let data = JSON.parse(event.data);
                
                if (data.type === "messages") {
                    // Handle paginated messages
                    let allMessages = data.messages || [];
                    
                    // Check for priority changes in currently displayed message
                    let currentlyDisplayedMessage = null;
                    if (messages.length > 0 && currentPage > 0 && currentPage <= messages.length) {
                        currentlyDisplayedMessage = messages[currentPage - 1];
                    }
                    
                    // Store new priorities and detect changes
                    let newMessagePriorities = {};
                    let priorityChanges = false;
                    
                    allMessages.forEach(msg => {
                        if (msg.id && msg.priority) {
                            newMessagePriorities[msg.id] = msg.priority;
                            
                            // Check if priority has changed
                            if (messagePriorities[msg.id] && messagePriorities[msg.id] !== msg.priority) {
                                console.log('Priority changed for message ID: ' + msg.id + 
                                           ', Previous: ' + messagePriorities[msg.id] + 
                                           ', New: ' + msg.priority);
                                priorityChanges = true;
                                
                                // If this is the currently displayed message, update its color immediately
                                if (currentlyDisplayedMessage && currentlyDisplayedMessage.id === msg.id) {
                                    console.log('Updating color for currently displayed message');
                                    updateMessagePriority(currentlyDisplayedMessage.id, msg.priority);
                                }
                            }
                        }
                    });
                    
                    // Update the stored priorities
                    messagePriorities = newMessagePriorities;
                    
                    // Filter out already read messages
                    messages = allMessages.filter(msg => !messageReadStatus[msg.id] && !messageReadStatus[msg.text]);
                    
                    // Set the correct total pages based on unread messages length
                    totalPages = messages.length;
                    currentPage = 1;
                    
                    updatePaginationDisplay();
                    
                    // Display the first unread message if there are any
                    if (messages.length > 0) {
                        showMessage(messages[0]);
                        
                        // Force pagination buttons to be visible if there are multiple messages
                        if (messages.length > 1) {
                            container.find('#prevPage, #nextPage').css('display', 'inline-block');
                            container.find('table').css('display', 'inline-table');
                        }
                    } else {
                        // No unread messages - hide everything
                        container.find('.broadcast-message-banner').removeClass('show');
                        container.find('#prevPage, #nextPage').hide();
                        container.find('table').hide();
                    }
                } else if (data.message) {
                    // Handle single message (legacy support)
                    // Default to low priority if not specified
                    showBroadcastBanner(data.message, data.priority || "low");
                }
            } catch (e) {
                // Error parsing message
            }
        };
        
        ws.onclose = function(event) {
            // Connection closed
        };
        
        ws.onerror = function(event) {
            // WebSocket error
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
                container.find('#prevPage, #nextPage').hide();
                container.find('table').hide();
            } else {
                // Force display
                container.find('#prevPage, #nextPage').css('display', 'inline-block');
                container.find('table').css('display', 'inline-table');
            }
        }
        
        // Function to show a message object
        function showMessage(message) {
            if (message && message.text) {
    
    // If this is the currently displayed message, update its color
    if (messageToUpdate && currentPage > 0 && currentPage <= messages.length) {
        const currentMessageIndex = currentPage - 1;
        if (messages[currentMessageIndex].id === messageId) {
            // Remove all priority classes first
            container.find('.broadcast-message-banner')
                .removeClass('priority-high priority-medium priority-low');
            
            // Apply the new priority class
            container.find('.broadcast-message-banner').addClass('priority-' + newPriority.toLowerCase());
            
            console.log('Updated banner color to priority:', newPriority);
        }
    }
}

// Function to show broadcast banner with priority-based styling
function showBroadcastBanner(message, priority) {
    // Check if this message has been read before
    if (messageReadStatus[message]) {
        // User has already read this message - hide the banner
        container.find('.broadcast-message-banner').removeClass('show');
        return;
    }
    
    // Only show if we have an actual message
    if (message && message.trim() !== "") {
        // Remove all priority classes first
        container.find('.broadcast-message-banner')
            .removeClass('priority-high priority-medium priority-low');
        
        // Apply the appropriate priority class
        if (priority) {
            container.find('.broadcast-message-banner').addClass('priority-' + priority.toLowerCase());
        } else {
            // Default to low priority if not specified
            container.find('.broadcast-message-banner').addClass('priority-low');
        }
        
        container.find('#broadcastText').text(message);
        container.find('.broadcast-message-banner').addClass('show');
        
        // Log the priority for debugging
        console.log('Showing message with priority:', priority);
    } else {
        // No message to show
        container.find('.broadcast-message-banner').removeClass('show');
    }
}

// ... (rest of the code remains the same)