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

public class SampleChatUiHtmlInjector extends UiHtmlInjectorPluginAbstract implements PluginWebSocket {

    private static Set<Session> clients = new CopyOnWriteArraySet<>();
    private static Map<String, List<String>> userClients = new HashMap<>();
    
    @Override
    public String getName() {
        return "SampleChatUiHtmlInjector";
    }

    @Override
    public String getVersion() {
        return "8.2.0";
    }

    @Override
    public String getDescription() {
        return "";
    }
    

    @Override
    public String[] getInjectUrlPatterns() {
        return new String[]{"/web/userview/**"};
    }

    @Override
    public String getHtml(HttpServletRequest request) {
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        Map data = new HashMap();
        data.put("plugin", this);
        data.put("request", request);
        
        String html = pluginManager.getPluginFreeMarkerTemplate(data, getClassName(), "/templates/SampleChatUiHtmlInjector.ftl", null);
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
        for (Iterator<Map.Entry<String, List<String>>> it = userClients.entrySet().iterator(); it.hasNext(); ) {
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
            
            if(username.equalsIgnoreCase("roleAnonymous")){
                username += "-" + session.getId();
            }
            
            broadCastMessage("User <" + username + "> joined", "System", session);
            
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "onOpen Error");
        }
    }
    
    @Override
    public void onMessage(String message, Session session) {
        try {
            broadCastMessage(message, "",session);
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "onMessage Error");
        }
    }

    public void broadCastMessage(String message, String author, Session currentSession){
        try {
            String username = "";
            if(!author.equalsIgnoreCase("System")){
                username = WorkflowUtil.getCurrentUsername();
                if(username.equals("roleAnonymous")){
                    author = username + "-" + currentSession.getId();
                }else{
                    author = username;
                }
            }
            
            JSONObject jsonMessage = new JSONObject();
            jsonMessage.put("message", message);
            jsonMessage.put("author", author);
            
            // Universal the message to all connected clients
            for (Session client : clients) {
                if (client.isOpen()) {
                    if(username.equals("roleAnonymous") && client.getId().equals(currentSession.getId())){
                        //anonymous user, each session is unique, DIFFERENT user context for each
                        jsonMessage.put("sentByMe", true);
                    }else if(!username.equals("roleAnonymous") && findUserBySession(client.getId()).equals(username)){
                        //logged in user with multiple sessions, same user context
                        jsonMessage.put("sentByMe", true);
                    }else{
                        jsonMessage.put("sentByMe", false);
                    }
                    
                    try {
                        //if(client.isOpen()){
                            client.getBasicRemote().sendText(jsonMessage.toString());
                        //}
                    } catch (Exception e) {
                        LogUtil.error(getClassName(), e, "sendText Error");
                    }
                }
            }
        
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "broadCastMessage Error");
        }
    }
    
    @Override
    public void onClose(Session session) {
        try {
            String userLeft = findUserBySession(session.getId());
            removeSession(session.getId());
            LogUtil.info(getClassName(), "Webscoket connection closed - " + session.getId() + " - " + userLeft);
            clients.remove(session);
            broadCastMessage("User <" + userLeft + "-" + session.getId() + "> left", "System", session);
            
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
}
