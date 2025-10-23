# Joget Broadcast Message Plugin

A UI HTML Injector plugin for Joget that displays broadcast messages to users across the platform. This plugin enables users to communicate important announcements to all logged-in users in real-time.

## Description

The Broadcast Message Plugin adds a notification banner at the top of Joget pages that displays broadcast messages to users. Messages can be sourced from a CRUD form in a Joget app, allowing for dynamic content management. Any user can broadcast messages to all other connected users, making it a versatile communication tool. The plugin uses WebSocket technology for real-time communication and updates.

## Features

- **Real-time Message Broadcasting**: Instantly deliver messages to all connected users
- **Automatic Message Updates**: Periodically checks for new messages and broadcasts them to all connected users without requiring page refresh
- **Universal Access**: Any authenticated user can broadcast messages to all other users
- **Message Pagination**: Navigate through multiple messages with previous/next controls
- **Priority-Based Sorting**: Display higher priority messages first (lower number = higher priority)
- **"Mark as Read" Functionality**: Users can mark messages as read, which won't appear again in their browser
- **Persistent Read Status**: Uses localStorage to remember which messages have been read across page refreshes
- **WebSocket Communication**: Efficient real-time updates without page refreshes
- **Background Message Checking**: Automatically checks for new messages every 10 seconds

## Configuration

The plugin sources messages from a CRUD form with the following default configuration:

- **App ID**: broadcast_memo_plugin_app
- **Form ID**: broadcast_messages
- **Message Field ID**: message_text
- **Priority Field ID**: priority

## CRUD Form Setup

To use the plugin with a CRUD form:

1. Create a form in Joget with the following fields:
   - A text field for the message content (ID: message_text)
   - A number field for priority (ID: priority) - lower numbers appear first

2. Create records in this form to add broadcast messages

3. Messages will be automatically sorted by priority (lower number = higher priority)

## User Experience

- Users see a notification banner at the top of Joget pages
- Any authenticated user can broadcast messages to all other connected users
- If multiple messages exist, pagination arrows appear to navigate between them
- Users can click "Mark as Read" to dismiss a message
- Read messages won't appear again in the user's browser (stored in localStorage)
- When all messages are read, the banner disappears until new messages are available