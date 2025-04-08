package org.joget.dx82.sample;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import org.joget.apps.app.model.PluginWebFilterAbstract;


public class SampleHeaderFilter extends PluginWebFilterAbstract {
    
    @Override
    public String getName() {
        return "SampleHeaderFilter";
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
    public String[] getUrlPatterns() {
        return new String[]{"/web/login"};
    }
    
    @Override
    public boolean isPositionAfterSecurityFilter() {
        return false;
    }
    
    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setHeader("X-Frame-Options", "DENY");
        
        filterChain.doFilter(request, response);
    }
}