package org.joget.dx82.sample;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.servlet.http.HttpServletRequest;
import javax.websocket.Session;

import org.joget.apps.app.model.UiHtmlInjectorPluginAbstract;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.plugin.base.PluginWebSocket;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONObject;

public class BroadcastMessagePlugin extends UiHtmlInjectorPluginAbstract implements PluginWebSocket {

    private static Set<Session> clients = new CopyOnWriteArraySet<>();
    private static Map<String, List<String>> userClients = new HashMap<>();
    private static String broadcastMessage = "IMPORTANT: System maintenance scheduled for today at 5:00 PM. Please save your work before this time.";

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

    @Override
    public String getHtml(HttpServletRequest request) {
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        Map data = new HashMap();
        data.put("plugin", this);
        data.put("request", request);

        // Use configured message or default to the static broadcastMessage
        String configuredMessage = getPropertyString("message");
        String messageToDisplay = (configuredMessage != null && !configuredMessage.trim().isEmpty())
                ? configuredMessage
                : broadcastMessage;

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

            // Send the current broadcast message to the new client
            String configuredMessage = getPropertyString("message");
            String messageToSend = (configuredMessage != null && !configuredMessage.trim().isEmpty())
                    ? configuredMessage
                    : broadcastMessage;

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
                broadcastMessage(message, username, session);
                // Update the stored message
                broadcastMessage = message;
            }
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "onMessage Error");
        }
    }

    public void broadcastMessage(String message, String author, Session currentSession) {
        try {
            // Broadcast to all connected clients
            for (Session client : clients) {
                if (client.isOpen()) {
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
        String[] propertyOptions = {
                "message",
                "textarea",
                "Broadcast Message",
                "Enter the message to broadcast to all users"
        };
        return propertyOptions;
    }
}
