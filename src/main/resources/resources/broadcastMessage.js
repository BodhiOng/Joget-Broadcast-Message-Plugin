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

        // Create multiple sound options to increase compatibility
        var soundOptions = {
            // Web Audio API beep (more compatible with most browsers)
            createBeep: function () {
                try {
                    var audioContext = new (window.AudioContext || window.webkitAudioContext)();
                    var oscillator = audioContext.createOscillator();
                    var gainNode = audioContext.createGain();

                    oscillator.connect(gainNode);
                    gainNode.connect(audioContext.destination);

                    oscillator.type = 'sine';
                    oscillator.frequency.value = 800; // Hz
                    gainNode.gain.value = 0.5;

                    oscillator.start(0);
                    gainNode.gain.exponentialRampToValueAtTime(0.0001, audioContext.currentTime + 0.5);
                    setTimeout(function () {
                        oscillator.stop();
                    }, 500);

                    return true;
                } catch (e) {
                    // Web Audio API not supported
                    return false;
                }
            }
        };

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

        // Store message priorities and statuses for tracking changes
        let messagePriorities = {};
        let messageStatuses = {};

        // Initialize message data
        if (messagesData && messagesData.messages) {
            // Get all messages
            let allMessages = messagesData.messages || [];

            // Store priorities and statuses for all messages
            allMessages.forEach(msg => {
                if (msg.id) {
                    if (msg.priority) {
                        messagePriorities[msg.id] = msg.priority;
                    }
                    if (msg.status) {
                        messageStatuses[msg.id] = msg.status;
                    }
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
                    setTimeout(function () {
                        container.find('#broadcastPagination').attr('style', 'display: flex !important');
                    }, 100);

                    // And another timeout just to be sure
                    setTimeout(function () {
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

        // Add global test functions to manually trigger different sound options
        window.testBroadcastSound = function () {
            playNotificationSound();
            return 'Sound test initiated.';
        };

        window.testNotification = function () {
            // Testing visual notification
            showSoundNotification();
            return 'Visual notification test initiated.';
        };

        // Initialize WebSocket connection
        const ws = new WebSocket(((window.location.protocol === "https:") ? "wss://" : "ws://") +
            window.location.host +
            options.contextPath +
            "/web/socket/plugin/org.joget.marketplace.BroadcastMessagePlugin");

        ws.onopen = function (event) {
            // Connection established
        };

        // Store initial message IDs for comparison
        var initialMessageIds = [];
        var isFirstMessageLoad = true;

        ws.onmessage = function (event) {
            try {
                let data = JSON.parse(event.data);

                // Store the message for debugging
                window.lastWebSocketMessage = data;

                if (data.type === "messages") {
                    // Handle paginated messages
                    let allMessages = data.messages || [];

                    // Process received messages
                    if (allMessages.length > 0) {
                        // Track status changes
                        let newBroadcastDetected = false;
                        let unbroadcastDetected = false;

                        allMessages.forEach(msg => {
                            if (msg.id) {
                                const previousStatus = messageStatuses[msg.id];
                                const currentStatus = msg.status || null;

                                console.log('Message ID ' + msg.id + ': Previous status=' + previousStatus + ', Current status=' + currentStatus);

                                // Check for status changes
                                if (previousStatus === 'broadcast' && currentStatus !== 'broadcast') {
                                    // Changed FROM broadcast - don't play sound
                                    unbroadcastDetected = true;
                                    console.log('Message ID ' + msg.id + ' changed FROM broadcast status to ' + currentStatus);
                                } else if (previousStatus !== 'broadcast' && currentStatus === 'broadcast') {
                                    // Changed TO broadcast - play sound
                                    newBroadcastDetected = true;
                                    console.log('Message ID ' + msg.id + ' changed TO broadcast status');
                                } else if (!previousStatus && currentStatus === 'broadcast') {
                                    // New message with broadcast status
                                    newBroadcastDetected = true;
                                    console.log('New message with broadcast status: ' + msg.id);
                                }

                                // Update status tracking
                                messageStatuses[msg.id] = currentStatus;
                            }
                        });

                        // Only play sound for new broadcasts or status changes TO broadcast
                        // NEVER play sound when any message is unbroadcast
                        if (!unbroadcastDetected) {
                            const hasBroadcastMessages = allMessages.some(msg => msg.status === 'broadcast');
                            if (hasBroadcastMessages && newBroadcastDetected) {
                                // Get the first broadcast message text
                                const broadcastMessage = allMessages.find(msg => msg.status === 'broadcast');
                                const messageText = broadcastMessage ? broadcastMessage.text : 'New broadcast message received';

                                // Play sound for new broadcast
                                console.log('Playing sound for new broadcast: ' + messageText);
                                soundOptions.createBeep();
                            }
                        } else {
                            console.log('Suppressing sound because a message was unbroadcast');
                        }
                    }

                    // Check if we're currently showing the default message or no message
                    const currentText = container.find('#broadcastText').text();
                    const isShowingDefaultMessage = currentText === options.initialMessage || currentText === "";

                    // Log the transition for debugging
                    if (isShowingDefaultMessage && allMessages.length > 0) {
                        console.log('Transitioning from default/empty message to real messages');

                        // Check if any of the new messages have broadcast status
                        const hasBroadcastMessages = allMessages.some(msg => msg.status === 'broadcast');
                        if (hasBroadcastMessages) {
                            console.log('Found messages with broadcast status - will update display');
                        }
                    }

                    // Check for priority changes in currently displayed message
                    let currentlyDisplayedMessage = null;
                    if (messages.length > 0 && currentPage > 0 && currentPage <= messages.length) {
                        currentlyDisplayedMessage = messages[currentPage - 1];
                    }

                    // Store new priorities and statuses, detect changes
                    let newMessagePriorities = {};
                    let newMessageStatuses = {};
                    let priorityChanges = false;

                    allMessages.forEach(msg => {
                        if (msg.id) {
                            // Track priorities
                            if (msg.priority) {
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
                                        updateMessagePriority(msg.id, msg.priority);
                                    }
                                }
                            }

                            // Track statuses
                            if (msg.status) {
                                newMessageStatuses[msg.id] = msg.status;
                            }
                        }
                    });

                    // Update the stored priorities and statuses
                    messagePriorities = newMessagePriorities;
                    messageStatuses = newMessageStatuses;

                    // Check if any messages have been deleted
                    const previousMessageIds = messages.map(msg => msg.id).filter(id => id); // Get non-null IDs
                    const newMessageIds = allMessages.map(msg => msg.id).filter(id => id); // Get non-null IDs

                    // Find deleted message IDs (in previous but not in new)
                    const deletedMessageIds = previousMessageIds.filter(id => !newMessageIds.includes(id));

                    if (deletedMessageIds.length > 0) {
                        console.log('Detected deleted messages:', deletedMessageIds);
                    }

                    // Special handling for first load vs. subsequent updates
                    if (isFirstMessageLoad) {
                        // On first load, just store the IDs without playing sound
                        initialMessageIds = newMessageIds.slice();
                        isFirstMessageLoad = false;
                        console.log('Initial message IDs stored:', initialMessageIds);
                    } else {
                        // On subsequent loads, check for new messages
                        const newMessages = newMessageIds.filter(id => !initialMessageIds.includes(id));

                        // Also check for new broadcast messages (status changed to broadcast)
                        const newBroadcastMessages = allMessages.filter(msg => {
                            // Check if this message is broadcast and wasn't in the initial set
                            return msg.status === 'broadcast' && !initialMessageIds.includes(msg.id);
                        });

                        // Only play sound for new broadcast messages, not for all new messages
                        if (newBroadcastMessages.length > 0) {
                            console.log('New broadcast messages detected:', newBroadcastMessages);
                            console.log('Playing notification sound');
                            // Play notification sound for new broadcast messages
                            playNotificationSound();
                        }

                        // Always update our stored IDs to include the new ones
                        initialMessageIds = newMessageIds.slice();
                    }

                    // Filter out already read messages
                    messages = allMessages.filter(msg => !messageReadStatus[msg.id] && !messageReadStatus[msg.text]);

                    // Set the correct total pages based on unread messages length
                    totalPages = messages.length;
                    currentPage = 1;

                    updatePaginationDisplay();

                    // Check if all messages were deleted
                    const allMessagesDeleted = previousMessageIds.length > 0 && allMessages.length === 0;

                    // Display the first unread message if there are any
                    if (messages.length > 0) {
                        // If we were showing the default message and now we have real messages,
                        // make sure to replace the default message with the new real message
                        if (isShowingDefaultMessage) {
                            console.log('Replacing default message with first real message');
                            container.find('.broadcast-message-banner').removeClass('show');
                            setTimeout(() => {
                                showMessage(messages[0]);
                                // Force the banner to be visible
                                container.find('.broadcast-message-banner').addClass('show');
                            }, 100); // Small delay to ensure visual transition
                        } else {
                            showMessage(messages[0]);
                            // Double-check that the banner is visible
                            if (!container.find('.broadcast-message-banner').hasClass('show')) {
                                console.log('Forcing banner visibility');
                                container.find('.broadcast-message-banner').addClass('show');
                            }
                        }

                        // Force pagination buttons to be visible if there are multiple messages
                        if (messages.length > 1) {
                            container.find('#prevPage, #nextPage').css('display', 'inline-block');
                            container.find('table').css('display', 'inline-table');
                        }
                    } else {
                        // No unread messages or all messages were deleted - hide everything
                        if (allMessagesDeleted) {
                            console.log('All messages were deleted - hiding banner');
                        }
                        container.find('.broadcast-message-banner').removeClass('show');
                        container.find('#prevPage, #nextPage').hide();
                        container.find('table').hide();

                        // If all messages were deleted, show the default message
                        if (allMessagesDeleted && options.initialMessage) {
                            setTimeout(() => {
                                // Show the default message
                                container.find('#broadcastText').text(options.initialMessage);
                                container.find('.broadcast-message-banner')
                                    .removeClass('priority-high priority-medium priority-low')
                                    .addClass('priority-low');
                                container.find('.broadcast-message-banner').addClass('show');
                            }, 300); // Small delay to ensure visual transition
                        }
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

        ws.onclose = function (event) {
            // Connection closed
        };

        ws.onerror = function (event) {
            // WebSocket error
        };

        // Mark as Read button functionality
        container.find('#broadcastClose').on('click', function () {
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
        container.find('#prevPage').on('click', function () {
            if (currentPage > 1) {
                currentPage--;
                showCurrentMessage();
                updatePaginationDisplay();
            }
        });

        container.find('#nextPage').on('click', function () {
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

        // Function to update the priority/color of a message without changing its content
        function updateMessagePriority(messageId, newPriority) {
            // Find the message in the current messages array
            let messageToUpdate = null;
            for (let i = 0; i < messages.length; i++) {
                if (messages[i].id === messageId) {
                    messageToUpdate = messages[i];
                    // Update the priority in our local array
                    messages[i].priority = newPriority;
                    break;
                }
            }

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

        // Function to show a message object
        function showMessage(message) {
            if (message && message.text) {
                // Show the message text in the banner and apply priority-based styling
                showBroadcastBanner(message.text, message.priority);
            }
        }

        // Function to play notification sound
        function playNotificationSound() {
            try {
                // Try Web Audio API
                var success = soundOptions.createBeep();
                if (!success) {
                    // If Web Audio API fails, show visual notification
                    showSoundNotification();
                }
            } catch (e) {
                // If there's an error, show visual notification
                showSoundNotification();
            }
        }

        // Function to show a visual notification when sound can't be played
        function showSoundNotification() {
            var notification = $('<div class="broadcast-sound-notification">🔔 New message received! Click anywhere to enable sound notifications.</div>');
            notification.css({
                'position': 'fixed',
                'bottom': '20px',
                'right': '20px',
                'background-color': '#4CAF50',
                'color': 'white',
                'padding': '10px 15px',
                'border-radius': '4px',
                'box-shadow': '0 2px 5px rgba(0,0,0,0.3)',
                'z-index': '10000',
                'cursor': 'pointer',
                'font-size': '14px'
            });

            $('body').append(notification);

            notification.on('click', function () {
                $(this).fadeOut(300, function () { $(this).remove(); });
            });

            setTimeout(function () {
                notification.fadeOut(500, function () { notification.remove(); });
            }, 5000);
        }

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
    };
}(jQuery));