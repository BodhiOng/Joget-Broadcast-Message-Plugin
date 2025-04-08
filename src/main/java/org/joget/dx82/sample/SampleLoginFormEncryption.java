package org.joget.dx82.sample;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
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
            CharResponseWrapper responseWrapper = new CharResponseWrapper((HttpServletResponse) new DummyHttpRequest((HttpServletResponse) response));

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

            httpResponse.getWriter().write(servletResponse);
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
        
        public void flushBuffer() throws IOException {
            output.flush();
        }
        
        public void resetBuffer() {
            output.reset();
        }
        
        public void reset() {
            output.reset();
        }
    }
    
    /**
     * A dummy request to copy the current HTTP request data to use by request hash variable in plugin thread.
     */
    public final class DummyHttpRequest implements HttpServletResponse {
        
        private HttpServletResponse res;
        
        public DummyHttpRequest(HttpServletResponse res) {
            this.res = res;
        }

        @Override
        public void addCookie(Cookie cookie) {
            res.addCookie(cookie);
        }

        @Override
        public boolean containsHeader(String name) {
            return res.containsHeader(name);
        }

        @Override
        public String encodeURL(String url) {
            return res.encodeURL(url);
        }

        @Override
        public String encodeRedirectURL(String url) {
            return res.encodeRedirectUrl(url);
        }

        @Override
        public String encodeUrl(String url) {
            return res.encodeUrl(url);
        }

        @Override
        public String encodeRedirectUrl(String url) {
            return res.encodeRedirectURL(url);
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            res.sendError(sc, msg);
        }

        @Override
        public void sendError(int sc) throws IOException {
            res.sendError(sc);
        }

        @Override
        public void sendRedirect(String location) throws IOException {
            res.sendRedirect(location);
        }

        @Override
        public void setDateHeader(String name, long date) {
            res.setDateHeader(name, date);
        }

        @Override
        public void addDateHeader(String name, long date) {
            res.addDateHeader(name, date);
        }

        @Override
        public void setHeader(String name, String value) {
            res.setHeader(name, value);
        }

        @Override
        public void addHeader(String name, String value) {
            res.addHeader(name, value);
        }

        @Override
        public void setIntHeader(String name, int value) {
            res.setIntHeader(name, value);
        }

        @Override
        public void addIntHeader(String name, int value) {
            res.addIntHeader(name, value);
        }

        @Override
        public void setStatus(int sc) {
            res.setStatus(sc);
        }

        @Override
        public void setStatus(int sc, String sm) {
            res.setStatus(sc, sm);
        }

        @Override
        public String getCharacterEncoding() {
            return res.getCharacterEncoding();
        }

        @Override
        public String getContentType() {
            return res.getContentType();
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            return res.getOutputStream();
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            return res.getWriter();
        }

        @Override
        public void setCharacterEncoding(String string) {
            res.setCharacterEncoding(string);
        }

        @Override
        public void setContentLength(int i) {
            res.setContentLength(i);
        }

        @Override
        public void setContentType(String string) {
            res.setContentType(string);
        }

        @Override
        public void setBufferSize(int i) {
            res.setBufferSize(i);
        }

        @Override
        public int getBufferSize() {
            return res.getBufferSize();
        }

        @Override
        public void flushBuffer() throws IOException {
            
        }

        @Override
        public void resetBuffer() {
            
        }

        @Override
        public boolean isCommitted() {
            return res.isCommitted();
        }

        @Override
        public void reset() {
            
        }

        @Override
        public void setLocale(Locale locale) {
            res.setLocale(locale);
        }

        @Override
        public Locale getLocale() {
            return res.getLocale();
        }

        public int getStatus() {
            return 200;
        }

        public String getHeader(String string) {
            return "";
        }

        public Collection<String> getHeaders(String string) {
            return new ArrayList<String>();
        }

        public Collection<String> getHeaderNames() {
            return new ArrayList<String>();
        }

        public void setContentLengthLong(long l) {
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