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
import org.json.JSONArray;
import org.json.JSONObject;

public class BroadcastMessagePlugin extends UiHtmlInjectorPluginAbstract implements PluginWebSocket {

    private static Set<Session> clients = new CopyOnWriteArraySet<>();
    private static Map<String, List<String>> userClients = new HashMap<>();
    private static String defaultBroadcastMessage = "No memos available as of now";

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
     * Fetches all messages from the configured CRUD form
     * 
     * @return A map containing all the messages
     */
    protected Map<String, Object> getMessagesFromCrud() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, String>> messages = new ArrayList<>();
        result.put("messages", messages);
        
        try {
            // Hardcoded CRUD configuration
            String appId = "broadcast_memo_plugin_app"; // Replace with your actual app ID
            String formId = "broadcast_messages"; // Replace with your actual form ID
            String messageField = "message_text"; // Replace with your message field ID
            String priorityField = "priority"; // Field for message priority

            // Get the AppDefinition
            AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
            AppDefinition appDef = appService.getPublishedAppDefinition(appId);

            if (appDef == null) {
                LogUtil.warn(getClassName(), "App not found: " + appId);
                return result;
            }

            // Get form data
            FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
            
            // Get all messages
            FormRowSet allRows = formDataDao.find(formId, null, null, null, null, null, null, null);
            int totalMessages = allRows != null ? allRows.size() : 0;
            
            if (totalMessages == 0) {
                return result;
            }
            
            // Add all messages to the result
            for (FormRow row : allRows) {
                Map<String, String> message = new HashMap<>();
                String id = row.getId();
                String text = row.getProperty(messageField);
                String priority = row.getProperty(priorityField);
                
                message.put("id", id);
                message.put("text", text != null ? text : "");
                message.put("priority", priority != null ? priority : "low"); // Default to low priority if not set
                
                messages.add(message);
            }
            
            // Sort messages by priority (high > medium > low)
            messages.sort((m1, m2) -> {
                String p1 = m1.get("priority");
                String p2 = m2.get("priority");
                
                // Define priority order: high (1) > medium (2) > low (3)
                int p1Value = getPriorityValue(p1);
                int p2Value = getPriorityValue(p2);
                
                return Integer.compare(p1Value, p2Value); // Lower number = higher priority
            });
            
            // Log the sorted messages for debugging
            LogUtil.info(getClassName(), "Sorted messages by priority: " + messages.size() + " messages");
            for (Map<String, String> msg : messages) {
                LogUtil.info(getClassName(), "Message: " + msg.get("text") + ", Priority: " + msg.get("priority"));
            }
            
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "Error fetching messages from CRUD");
        }
        
        return result;
    }
    

    @Override
    public String getHtml(HttpServletRequest request) {
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        Map data = new HashMap();
        data.put("plugin", this);
        data.put("request", request);
        
        // Get all messages
        Map<String, Object> messagesData = getMessagesFromCrud();
        
        // For backward compatibility
        List<Map<String, String>> messages = (List<Map<String, String>>) messagesData.get("messages");
        int messageCount = messages != null ? messages.size() : 0;
        
        // Convert messagesData to JSON string
        JSONObject jsonMessagesData = new JSONObject(messagesData);
        String messagesDataJson = jsonMessagesData.toString();
        
        // Pass data to template
        data.put("messagesData", messagesDataJson);
        data.put("messageCount", messageCount);
        if (messages != null && !messages.isEmpty()) {
            Map<String, String> firstMessage = messages.get(0);
            data.put("message", firstMessage.get("text"));
            data.put("priority", firstMessage.get("priority"));
        } else {
            // Fall back to default message if no messages found
            data.put("message", defaultBroadcastMessage);
            data.put("priority", "low"); // Default priority
        }

        String html = pluginManager.getPluginFreeMarkerTemplate(data, getClassName(),
                "/templates/BroadcastMessagePlugin.ftl", null);
        return html;
    }

    public void addSession(String user, String session) {
        userClients.computeIfAbsent(user, k -> new ArrayList<>()).add(session);
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
    }

    @Override
    public void onOpen(Session session) {
        try {
            String username = WorkflowUtil.getCurrentUsername();
            clients.add(session);
            addSession(username, session.getId());

            // Get all messages
            Map<String, Object> messagesData = getMessagesFromCrud();
            List<Map<String, String>> messages = (List<Map<String, String>>) messagesData.get("messages");
            
            if (messages != null && !messages.isEmpty()) {
                // Send all messages to the client
                sendMessagesToClient(messages, messagesData, session);
            } else {
                // Fall back to default message if no messages found
                sendMessageToClient(defaultBroadcastMessage, "System", session);
            }

        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "onOpen Error");
        }
    }
    
    /**
     * Helper method to convert text priority to numeric value for sorting
     * 
     * @param priority The text priority (high, medium, low)
     * @return Numeric value (1 for high, 2 for medium, 3 for low)
     */
    private int getPriorityValue(String priority) {
        if (priority == null) {
            return 3; // Default to lowest priority
        }
        
        switch (priority.toLowerCase()) {
            case "high":
                return 1;
            case "medium":
                return 2;
            case "low":
            default:
                return 3;
        }
    }
    
    /**
     * Sends multiple messages to a client with pagination metadata
     * 
     * @param messages List of message maps
     * @param paginationData Pagination metadata
     * @param client The WebSocket session
     */
    private void sendMessagesToClient(List<Map<String, String>> messages, Map<String, Object> paginationData, Session client) {
        try {
            JSONObject jsonResponse = new JSONObject();
            
            // Handle messages array safely
            if (messages != null) {
                // Convert each message to a JSONObject to ensure proper serialization
                JSONArray jsonMessages = new JSONArray();
                for (Map<String, String> message : messages) {
                    JSONObject jsonMessage = new JSONObject();
                    for (Map.Entry<String, String> entry : message.entrySet()) {
                        jsonMessage.put(entry.getKey(), entry.getValue() != null ? entry.getValue() : "");
                    }
                    jsonMessages.put(jsonMessage);
                }
                jsonResponse.put("messages", jsonMessages);
            } else {
                jsonResponse.put("messages", new JSONArray());
            }
            
            // Set pagination data based on the actual number of messages
            int messageCount = messages != null ? messages.size() : 0;
            jsonResponse.put("currentPage", 1); // Always start at page 1
            jsonResponse.put("totalMessages", messageCount);
            jsonResponse.put("totalPages", messageCount); // Each message is its own page
            
            jsonResponse.put("timestamp", System.currentTimeMillis());
            jsonResponse.put("type", "messages");
            
            client.getBasicRemote().sendText(jsonResponse.toString());
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "sendMessagesToClient Error: " + e.getMessage());
        }
    }

    @Override
    public void onMessage(String message, Session session) {
        try {
            // Try to parse as JSON first
            try {
                JSONObject jsonMessage = new JSONObject(message);
                String messageType = jsonMessage.optString("type", "");
                
                if ("broadcast".equals(messageType)) {
                    // Allow any user to broadcast messages
                    String username = WorkflowUtil.getCurrentUsername();
                    // Broadcast the message to all clients
                    String broadcastText = jsonMessage.optString("text", "");
                    broadcastMessage(broadcastText, username, session);
                }
            } catch (Exception jsonEx) {
                // Legacy support for simple string messages
                String username = WorkflowUtil.getCurrentUsername();
                // Allow any user to broadcast messages
                broadcastMessage(message, username, session);
            }
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "onMessage Error: " + e.getMessage());
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