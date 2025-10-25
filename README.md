# Joget Broadcast Message Plugin

A UI HTML Injector plugin for Joget that displays broadcast messages to users across the platform. This plugin enables users to communicate important announcements to all logged-in users in real-time.

## Description

The Broadcast Message Plugin adds a notification banner at the top of Joget pages that displays broadcast messages to users. Messages can be sourced from a CRUD form in a Joget app, allowing for dynamic content management. Any user can broadcast messages to all other connected users, making it a versatile communication tool. The plugin uses WebSocket technology for real-time communication and updates.

## Features

- **Real-time Message Broadcasting**: Instantly deliver messages to all connected users
- **Selective Broadcasting**: Control which messages are broadcast using a status checkbox
- **Automatic Message Updates**: Periodically checks for new messages and broadcasts them to all connected users without requiring page refresh
- **Message Pagination**: Navigate through multiple messages with previous/next controls
- **Priority-Based Sorting**: Display higher priority messages first (lower number = higher priority)
- **"Mark as Read" Functionality**: Users can mark messages as read, which won't appear again in their browser
- **Persistent Read Status**: Uses localStorage to remember which messages have been read across page refreshes
- **WebSocket Communication**: Efficient real-time updates without page refreshes
- **Background Message Checking**: Automatically checks for new messages every 10 seconds
- **Notifications**: Provides sound alerts, browser notifications, and JavaScript alerts when new messages are broadcast

## Configuration

The plugin sources messages from a CRUD form with the following default configuration:

- **App ID**: broadcast_memo_plugin_app
- **Form ID**: broadcast_messages
- **Message Field ID**: message_text
- **Priority Field ID**: priority
- **Status Field ID**: status

### Plugin Properties

- **Enable Sound Notifications**: When set to "true" (default), plays a sound notification when new messages are broadcast

### Testing Sound Notifications

To manually test if sound notifications are working in your browser:

1. Open the browser console (F12 or right-click > Inspect > Console)
2. Try the following test functions:

   - `testBroadcastSound()` - Tests all sound methods
   - `testBroadcastBeep()` - Tests Web Audio API beep (works in most browsers)
   - `testBroadcastMP3()` - Tests HTML5 Audio MP3 playback
   - `testNotification()` - Tests the visual notification fallback
   - `testBrowserNotification()` - Tests browser desktop notifications
   - `testAlert()` - Tests JavaScript alert popup
   - `simulateBroadcast()` - Simulates a real broadcast message from the server

#### Troubleshooting Sound Issues

If you don't hear any sound:

1. **Browser Autoplay Restrictions**: Most browsers block automatic sound playback without user interaction. Click somewhere on the page first, then run the test function again.

2. **Web Audio API Support**: If the MP3 playback doesn't work, the plugin will try to use the Web Audio API as a fallback. This is more widely supported across browsers.

3. **Visual Notification Fallback**: If sound can't be played due to browser restrictions, a visual notification will appear prompting the user to click to enable sound.

#### Debugging WebSocket Messages

To debug WebSocket communication and sound triggers:

1. Open the browser console (F12)
2. Run `showLastWebSocketMessage()` to see the last message received from the server
3. Check if the `newBroadcast` flag is set to `true` in the message
4. If the flag is not `true`, check the server logs for messages about "New broadcast detected"

## CRUD Form Setup

To use the plugin with a CRUD form:

1. Create a form in Joget with the following fields:
   - A text field for the message content (ID: message_text)
   - A number field for priority (ID: priority) - lower numbers appear first
   - A select box field for status (ID: status) - when set to "broadcast", the message is broadcast to all users; when empty or any other value, the message is saved but not broadcast

2. Create records in this form to add broadcast messages

3. Messages will be automatically sorted by priority, in descending order it's: high, medium, low.

## User Experience

- Users see a notification banner at the top of Joget pages
- If multiple messages exist, pagination arrows appear to navigate between them
- Users can click "Mark as Read" to dismiss a message
- Read messages won't appear again in the user's browser (stored in localStorage)
- When all messages are read, the banner disappears until new messages are available
- When new messages are broadcast, a notification sound plays to alert users