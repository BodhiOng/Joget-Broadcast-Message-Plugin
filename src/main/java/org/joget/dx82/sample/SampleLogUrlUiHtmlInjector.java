package org.joget.dx82.sample;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.joget.apps.app.model.UiHtmlInjectorPluginAbstract;
import org.joget.plugin.base.PluginProperty;

public class SampleLogUrlUiHtmlInjector extends UiHtmlInjectorPluginAbstract {

    @Override
    public String getName() {
        return "SampleLogUrlUiHtmlInjector";
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
    public PluginProperty[] getPluginProperties() {
        return null;
    }

    @Override
    public Object execute(Map properties) {
        return null;
    }

    @Override
    public String[] getInjectUrlPatterns() {
        return new String[]{"/**"};
    }

    @Override
    public String getHtml(HttpServletRequest request) {
        return "<script>console.log(location.href)</script>";
    }

    @Override
    public boolean isIncludeForAjaxThemePageSwitching() {
        return true;
    }
}