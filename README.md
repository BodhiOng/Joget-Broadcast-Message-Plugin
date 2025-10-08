# Description

This repo consists of several plugins to demo the new plugin types added in DX 8.2.

## Web Filter Plugins
- SampleHeaderFilter
- SampleWebFilterPlugin
- SampleLoginFormEncryption

## UI HTML Injector Plugins
- SampleChatUiHtmlInjector
- SampleLogUrlUiHtmlInjector
- BroadcastMessagePlugin - A plugin to broadcast messages to all Joget users

## Broadcast Message Plugin

The Broadcast Message Plugin allows administrators to broadcast important messages to all users currently logged into the Joget platform. The plugin can source messages either from static configuration or from a CRUD form in a Joget app.

### Features

- Real-time message broadcasting to all connected users
- WebSocket-based communication for instant message delivery
- Option to source messages from a CRUD form in Joget
- Support for message prioritization and active status filtering
- Admin-only message broadcasting capability

### Configuration

The plugin supports the following configuration options:

#### Basic Configuration
- **Use CRUD for Messages**: Enable to source messages from a CRUD form instead of static text
- **Broadcast Message**: Static message to display when CRUD is not enabled or no active CRUD message is found

#### CRUD Integration Configuration
- **App ID**: ID of the app containing the CRUD form
- **App Version**: Version of the app (leave blank for latest published version)
- **Form ID**: ID of the form containing broadcast messages
- **Message Field ID**: ID of the field containing the message text
- **Active Status Field ID**: ID of the field indicating if the message is active (optional)
- **Priority Field ID**: ID of the field indicating message priority (optional)

### CRUD Form Setup

To use a CRUD form as the message source:

1. Create a form in Joget with at least the following fields:
   - A text field for the message content
   - A checkbox or radio field for active status (optional)
   - A number field for priority (optional)

2. Configure the plugin with the appropriate App ID, Form ID, and field IDs

3. Set "Use CRUD for Messages" to true

The plugin will automatically fetch the highest priority active message from the form and display it to all users.

## Console Page Plugin
- SampleConsolePagePlugin

## System Configurable Plugin
- SampleWebFilterPlugin

## Activation Aware Plugin
- SampleHeaderFilter
- SampleWebFilterPlugin
- SampleLoginFormEncryption

# Getting Help

JogetOSS is a community-led team for open source software related to the [Joget](https://www.joget.org) no-code/low-code application platform.
Projects under JogetOSS are community-driven and community-supported.
To obtain support, ask questions, get answers and help others, please participate in the [Community Q&A](https://answers.joget.org/).

# Contributing

This project welcomes contributions and suggestions, please open an issue or create a pull request.

Please note that all interactions fall under our [Code of Conduct](https://github.com/jogetoss/repo-template/blob/main/CODE_OF_CONDUCT.md).

# Licensing

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

NOTE: This software may depend on other packages that may be licensed under different open source licenses.
