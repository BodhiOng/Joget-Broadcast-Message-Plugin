package org.joget.dx82.sample;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import org.joget.apps.app.model.PluginWebFilterAbstract;

public class SampleLoginFormEncryption extends PluginWebFilterAbstract {
    
    @Override
    public String getName() {
        return "SampleLoginFormEncryption";
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
        return new String[]{"/web/login", "/j_spring_security_check"};
    }
    
    @Override
    public boolean isPositionAfterSecurityFilter() {
        return false;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if(httpRequest.getRequestURI().contains("/web/login")){
            PrintWriter out = response.getWriter();
            CharResponseWrapper responseWrapper = new CharResponseWrapper((HttpServletResponse) response);

            filterChain.doFilter(request, responseWrapper);

            String servletResponse = new String(responseWrapper.toString());
            String jsInject = "" +
                            "<script>$(document).ready(function(){ " +
                            "   $('#loginForm').submit(function(e){ " +
                            "       $('#j_username').val(btoa(Math.floor(Math.random() * 100000000) + ';' + $('#j_username').val())); " +
                            "       $('#j_password').val(btoa(Math.floor(Math.random() * 100000000) + ';' + $('#j_password').val())); " +
                            "   }) " +
                            "})</script>";
            servletResponse = servletResponse.replace("</body>", jsInject + "</body>");

            out.write(servletResponse);

        }else if(httpRequest.getRequestURI().contains("/j_spring_security_check")){
            PasswordEncryptionRequestWrapper requestWrapper = new PasswordEncryptionRequestWrapper(httpRequest);

            String encodedUsername = requestWrapper.getParameter("j_username");
            String encodedPassword = requestWrapper.getParameter("j_password");

            if(encodedUsername != null){
                try {
                    String username = new String(Base64.getDecoder().decode(encodedUsername));
                    int indexOf = username.lastIndexOf(";");
                    if(indexOf != -1){
                        username = username.substring(indexOf + 1);
                        requestWrapper.addParameter("j_username", username);
                    }
                }catch (Exception e){
                    //
                }
            }

            if(encodedPassword != null){
                try {
                    String password = new String(Base64.getDecoder().decode(encodedPassword));
                    int indexOf = password.lastIndexOf(";");
                    if(indexOf != -1){
                        password = password.substring(indexOf + 1);
                        requestWrapper.addParameter("j_password", password);
                    }
                }catch (Exception e){
                    //
                }
            }

            filterChain.doFilter(requestWrapper, response);
        }else{
            filterChain.doFilter(request, response);
        }
    }
    
    public class CharResponseWrapper extends HttpServletResponseWrapper {

        private final CharArrayWriter output;

        public CharResponseWrapper(final HttpServletResponse response) {
            super(response);
            output = new CharArrayWriter();
        }

        public String toString() {
            return output.toString();
        }

        public PrintWriter getWriter() {
            return new PrintWriter(output);
        }
    }
    
    public class PasswordEncryptionRequestWrapper extends HttpServletRequestWrapper {
        private final Map<String, String[]> modifiedParameters;
        
        public PasswordEncryptionRequestWrapper(HttpServletRequest request) {
		super(request);
                modifiedParameters = new HashMap<>(request.getParameterMap());
	}
        
        @Override
        public String getParameter(String name) {
            String[] values = modifiedParameters.get(name);
            return (values != null && values.length > 0) ? values[0] : super.getParameter(name);
        }

	public void addParameter(String name, String value) {
            modifiedParameters.put(name, new String[]{value});
        }

        @Override
        public String[] getParameterValues(String name) {
            return modifiedParameters.getOrDefault(name, super.getParameterValues(name));
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            return modifiedParameters;
        }
    }
}