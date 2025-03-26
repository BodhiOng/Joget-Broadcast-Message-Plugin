package org.joget.dx82.sample;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.joget.apps.app.model.PluginWebFilterAbstract;
import org.joget.apps.app.service.AppUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.plugin.base.SystemConfigurablePlugin;
import org.joget.workflow.model.service.WorkflowUserManager;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

/**
 * filter applied on url like http://localhost:8080/jw/web/userview/appcenter/home/_/_ja_inbox?_mode=assignment&activityId=403_401_JogetDxShowcase_process1_approver1
 */
public class SampleWebFilterPlugin extends PluginWebFilterAbstract implements SystemConfigurablePlugin {
    
    @Override
    public String getName() {
        return "SampleWebFilterPlugin";
    }

    @Override
    public String getVersion() {
        return "8.2.0";
    }
    
    @Override
    public String getLabel() {
        return "Sample Web Filter Plugin";
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/"+getName()+".json", null, false, null);
    }

    @Override
    public String[] getUrlPatterns() {
        return new String[]{"/web/login"};
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        WorkflowUserManager workflowUserManager = (WorkflowUserManager) AppUtil.getApplicationContext().getBean("workflowUserManager");
         
        if (workflowUserManager.isCurrentUserAnonymous()) {
            HttpServletRequest httprequest = (HttpServletRequest) request;
            HttpServletResponse httpresponse = (HttpServletResponse) response;
             
            SavedRequest savedRequest = new HttpSessionRequestCache().getRequest(httprequest, httpresponse);
            String savedUrl = "";
            if (savedRequest != null) {
                savedUrl = savedRequest.getRedirectUrl();
            } else if (httprequest.getHeader("referer") != null) {
                savedUrl = httprequest.getHeader("referer");
            }
             
            if (savedUrl.contains("ulogin")) {
                savedUrl = savedUrl.replaceAll("ulogin", "userview");
            }
             
            // Only apply for navigating to assignments. Feel free to change this condition for any other menus to login redirect
            if (savedUrl.contains("assignment")) {
                PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
                Map data = new HashMap();
                data.put("plugin", this);
                data.put("request", request);
                data.put("savedUrl", savedUrl);

                String html = pluginManager.getPluginFreeMarkerTemplate(data, getClassName(), "/templates/SampleWebFilterPlugin.ftl", null);
                
                httpresponse.getWriter().write(html);
                httpresponse.setContentType("text/html;charset=UTF-8");
 
                return;
            }
        }
         
        filterChain.doFilter(request, response);
    }
}