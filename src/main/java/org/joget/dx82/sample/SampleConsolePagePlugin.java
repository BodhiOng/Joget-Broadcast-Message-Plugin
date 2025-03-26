package org.joget.dx82.sample;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static org.bouncycastle.internal.asn1.cms.CMSObjectIdentifiers.data;
import org.joget.apps.app.model.ConsolePagePluginAbstract;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.userview.service.UserviewUtil;
import org.joget.commons.util.PagedList;
import org.joget.plugin.base.ConsolePagePlugin;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.WorkflowProcess;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.workflow.model.service.WorkflowUserManager;
import org.json.JSONObject;

public class SampleConsolePagePlugin extends ConsolePagePluginAbstract {

    @Override
    public String getName() {
        return "SampleConsolePage";
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
    public String getLabel() {
        return "Sample Console Page";
    }
    
    @Override
    public String getPluginIcon() {
        return "<i class=\"fas fa-envelope\"></i>";
    }

    @Override
    public int getOrder() {
        return 350;
    }

    @Override
    public ConsolePagePlugin.Location getLocation() {
        return ConsolePagePlugin.Location.MONITOR;
    }

    @Override
    public boolean isAuthorized() {
        WorkflowUserManager wum = (WorkflowUserManager) AppUtil.getApplicationContext().getBean("workflowUserManager");
        return wum.isCurrentUserInRole(WorkflowUserManager.ROLE_ADMIN);
    }

    @Override
    public String render(HttpServletRequest request, HttpServletResponse response) {
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        
        Map tabledata = new HashMap();
        tabledata.put("divId", "sampleList");
        tabledata.put("url", request.getContextPath()+"/web/console/plugin/SampleConsolePage/list?"+ request.getQueryString());
        tabledata.put("fields", "['packageId','packageName']");
        Map dynamicAttributes = new HashMap();
        dynamicAttributes.put("rowsPerPage", "15");
        dynamicAttributes.put("width", "100%");
        dynamicAttributes.put("sort", "packageId");
        dynamicAttributes.put("desc", "false");
        dynamicAttributes.put("column1", "{key: 'packageId', label: 'Package Id', sortable: true, width: '40%'}");
        dynamicAttributes.put("column2", "{key: 'packageName', label: 'Package Name', sortable: true, width: '50%'}");
        tabledata.put("dynamicAttributes", dynamicAttributes);
        String tableHtml = UserviewUtil.renderJspAsString("console/page/jsontable.jsp", tabledata);
        
        Map data = new HashMap();
        data.put("tableHtml", tableHtml);
        return pluginManager.getPluginFreeMarkerTemplate(data, getClassName(), "/templates/SampleConsolePage.ftl", null);
    }
    
    @ConsolePagePlugin.Path("/list")
    public void renderListJsonData(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String callback = request.getParameter("callback");
        String sort = request.getParameter("sort");
        Boolean desc = request.getParameter("desc") != null?Boolean.valueOf(request.getParameter("desc")):null;
        Integer start = request.getParameter("start") != null?Integer.valueOf(request.getParameter("start")):null;
        Integer rows = request.getParameter("rows") != null?Integer.valueOf(request.getParameter("rows")):null;
        
        WorkflowManager workflowManager = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");
        PagedList<WorkflowProcess> processList = workflowManager.getProcessList(sort, desc, start, rows, null, false, false);

        Integer total = processList.getTotal();
        JSONObject jsonObject = new JSONObject();
        for (WorkflowProcess process : processList) {
            Map data = new HashMap();
            String label = process.getName() + " ver " + process.getVersion();
            data.put("id", process.getId());
            data.put("packageId", process.getPackageId());
            data.put("packageName", process.getPackageName());
            data.put("name", process.getName());
            data.put("version", process.getVersion());
            data.put("label", label);
            jsonObject.accumulate("data", data);
        }

        jsonObject.accumulate("total", total);
        jsonObject.accumulate("start", start);
        jsonObject.accumulate("sort", sort);
        jsonObject.accumulate("desc", desc);

        AppUtil.writeJson(response.getWriter(), jsonObject, callback);
    }
}
