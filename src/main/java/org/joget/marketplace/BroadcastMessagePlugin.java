package org.joget.marketplace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.servlet.http.HttpServletRequest;
import javax.websocket.Session;

import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.UiHtmlInjectorPluginAbstract;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.plugin.base.PluginWebSocket;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONObject;

public class BroadcastMessagePlugin extends UiHtmlInjectorPluginAbstract implements PluginWebSocket {

    private static Set<Session> clients = new CopyOnWriteArraySet<>();
    private static Map<String, List<String>> userClients = new HashMap<>();
    private static String defaultBroadcastMessage = "IMPORTANT: System maintenance scheduled for today at 5:00 PM. Please save your work before this time.";

    @Override
    public String getName() {
        return "Broadcast Message Plugin";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "A plugin to broadcast messages to all Joget users";
    }

    @Override
    public String[] getInjectUrlPatterns() {
        return new String[] { "/web/userview/**" };
    }

    /**
     * Fetches the first message from the configured CRUD form
     * 
     * @return The message text or null if no message is found
     */
    protected String getMessageFromCrud() {
        try {
            // Hardcoded CRUD configuration
            String appId = "broadcast_memo_plugin_app"; // Replace with your actual app ID
            String formId = "broadcast_messages"; // Replace with your actual form ID
            String messageField = "message_text"; // Replace with your message field ID

            // Get the AppDefinition
            AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
            AppDefinition appDef = appService.getPublishedAppDefinition(appId);

            if (appDef == null) {
                LogUtil.warn(getClassName(), "App not found: " + appId);
                return null;
            }

            // Skip loading the form definition and access form data directly
            // This is more compatible across different Joget versions

            // Get form data
            FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
            FormRowSet rowSet = formDataDao.find(formId, null, null, null, null, null, null, null);

            LogUtil.info(getClassName(),
                    "Found " + (rowSet != null ? rowSet.size() : 0) + " messages in form: " + formId);

            if (rowSet == null || rowSet.isEmpty()) {
                LogUtil.info(getClassName(), "No messages found in form: " + formId);
                return null;
            }

            // Simply get the first message
            FormRow selectedRow = rowSet.iterator().next();
            String msgText = selectedRow.getProperty(messageField);
            LogUtil.info(getClassName(), "Message found - Text: " + msgText);

            // Return the message text
            String message = selectedRow.getProperty(messageField);
            if (message != null && !message.isEmpty()) {
                LogUtil.info(getClassName(), "Selected message to display: " + message);
                return message;
            } else {
                LogUtil.info(getClassName(), "Selected row has empty message");
            }

            return null;

        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "Error fetching message from CRUD");
            return null;
        }
    }

    @Override
    public String getHtml(HttpServletRequest request) {
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        Map data = new HashMap();
        data.put("plugin", this);
        data.put("request", request);

        // Always get message from CRUD
        String messageToDisplay;
        String crudMessage = getMessageFromCrud();
        if (crudMessage != null && !crudMessage.isEmpty()) {
            messageToDisplay = crudMessage;
        } else {
            // Fall back to default message if CRUD message not found
            messageToDisplay = defaultBroadcastMessage;
        }

        data.put("message", messageToDisplay);

        String html = pluginManager.getPluginFreeMarkerTemplate(data, getClassName(),
                "/templates/BroadcastMessagePlugin.ftl", null);
        return html;
    }

    public void addSession(String user, String session) {
        userClients.computeIfAbsent(user, k -> new ArrayList<>()).add(session);
        LogUtil.info(getClassName(), "All sessions - " + userClients.toString());
    }

    public String findUserBySession(String sessionToFind) {
        for (Map.Entry<String, List<String>> entry : userClients.entrySet()) {
            if (entry.getValue().contains(sessionToFind)) {
                return entry.getKey(); // Return the username
            }
        }
        return "";
    }

    public void removeSession(String sessionToRemove) {
        for (Iterator<Map.Entry<String, List<String>>> it = userClients.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, List<String>> entry = it.next();
            List<String> sessions = entry.getValue();

            // Remove the session if it exists
            sessions.remove(sessionToRemove);

            // If the list becomes empty, remove the user from the map
            if (sessions.isEmpty()) {
                it.remove();
            }
        }
        LogUtil.info(getClassName(), "All sessions - " + userClients.toString());
    }

    @Override
    public void onOpen(Session session) {
        try {
            String username = WorkflowUtil.getCurrentUsername();
            clients.add(session);
            addSession(username, session.getId());
            LogUtil.info(getClassName(), "Connection established - " + session.getId() + " - " + username);

            // Always get message from CRUD
            String messageToSend;
            String crudMessage = getMessageFromCrud();
            if (crudMessage != null && !crudMessage.isEmpty()) {
                messageToSend = crudMessage;
            } else {
                // Fall back to default message if CRUD message not found
                messageToSend = defaultBroadcastMessage;
            }

            if (messageToSend != null && !messageToSend.isEmpty()) {
                sendMessageToClient(messageToSend, "System", session);
            }

        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "onOpen Error");
        }
    }

    @Override
    public void onMessage(String message, Session session) {
        try {
            // Only admin users can broadcast messages
            String username = WorkflowUtil.getCurrentUsername();
            if (WorkflowUtil.isCurrentUserInRole("ROLE_ADMIN")) {
                // Broadcast the message to all clients
                broadcastMessage(message, username, session);
            }
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "onMessage Error");
        }
    }

    public void broadcastMessage(String message, String author, Session currentSession) {
        try {
            // Broadcast to all connected clients
            for (Session client : clients) {
                if (client.isOpen() && !client.getId().equals(currentSession.getId())) {
                    sendMessageToClient(message, author, client);
                }
            }
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "broadcastMessage Error");
        }
    }

    private void sendMessageToClient(String message, String author, Session client) {
        try {
            JSONObject jsonMessage = new JSONObject();
            jsonMessage.put("message", message);
            jsonMessage.put("author", author);
            jsonMessage.put("timestamp", System.currentTimeMillis());

            client.getBasicRemote().sendText(jsonMessage.toString());
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "sendMessageToClient Error");
        }
    }

    @Override
    public void onClose(Session session) {
        try {
            String userLeft = findUserBySession(session.getId());
            removeSession(session.getId());
            LogUtil.info(getClassName(), "WebSocket connection closed - " + session.getId() + " - " + userLeft);
            clients.remove(session);
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "onClose Error");
        }
    }

    @Override
    public void onError(Session session, Throwable throwable) {
        LogUtil.error(getClassName(), throwable, "");
    }

    @Override
    public boolean isIncludeForAjaxThemePageSwitching() {
        return false;
    }

    public String[] getPropertyOptions() {
        // No configurable properties in this simplified version
        String[] propertyOptions = {};
        return propertyOptions;
    }
}