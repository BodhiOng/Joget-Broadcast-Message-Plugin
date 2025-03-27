package org.joget.dx82.sample;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.joget.apps.app.model.PluginWebFilterAbstract;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.workflow.security.WorkflowUserDetails;
import org.joget.commons.util.LogUtil;
import org.joget.directory.model.Role;
import org.joget.directory.model.User;
import org.joget.directory.model.service.DirectoryManager;
import org.joget.plugin.base.SystemConfigurablePlugin;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;


public class SampleJwtLoginFilter extends PluginWebFilterAbstract implements SystemConfigurablePlugin {
    
    @Override
    public String getName() {
        return "SampleJwtLoginFilter";
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
        return "Sample JWT Login Filter";
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/"+getName()+".json", null, false, null);
    }

    @Override
    public String[] getUrlPatterns() {
        return new String[]{"/**"};
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

        String jwt = request.getParameter("jwt");
        if (jwt != null && !jwt.isEmpty()) {

            String publicKeyString = getPropertyString("publicKey");
            PublicKey publicKey = loadPublicKey(publicKeyString);

            String username = null;
            String firstName = null;
            String lastName = null;
            String email = null;

            try {
                Claims claims = Jwts.parser()
                        .verifyWith(publicKey)
                        .build()
                        .parseClaimsJws(jwt)
                        .getBody();

                LogUtil.info(getClass().getName(), "claims : " + claims);

                username = claims.get("username", String.class);
                firstName = claims.get("firstName", String.class);
                lastName = claims.get("lastName", String.class);
                email = claims.get("email", String.class);

            } catch (Exception e) {
                LogUtil.error(getClass().getName(), e, "error validating jwt");
            }

            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            DirectoryManager directoryManager = (DirectoryManager) AppUtil.getApplicationContext().getBean("directoryManager");
            User user = directoryManager.getUserByUsername(username);

            if(user != null) {
                // get authorities
                UserDetails details = new WorkflowUserDetails(user);
                Collection<Role> roles = directoryManager.getUserRoles(user.getUsername());
                List<GrantedAuthority> gaList = new ArrayList<>();
                if (roles != null && !roles.isEmpty()) {
                    for (Role role : roles) {
                        GrantedAuthority ga = new SimpleGrantedAuthority(role.getId());
                        gaList.add(ga);
                    }
                }

                // login user
                UsernamePasswordAuthenticationToken result = new UsernamePasswordAuthenticationToken(user.getUsername(), "", gaList);
                result.setDetails(details);

                SecurityContext securityContext = SecurityContextHolder.getContext();
                securityContext.setAuthentication(result);

                HttpSession session = httpRequest.getSession(true);
                session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);

                httpResponse.sendRedirect(httpRequest.getRequestURL().toString());
            }
        }

        filterChain.doFilter(request, response);
    }

    protected PublicKey loadPublicKey(String publicKeyString) {
        PublicKey pub = null;
        try {
            X509EncodedKeySpec ks = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyString));
            KeyFactory kf = KeyFactory.getInstance("RSA");
            pub = kf.generatePublic(ks);
        } catch (Exception e) {
            LogUtil.info(getClass().getName(), "error loading public key: " + e.getMessage());
        }
        return pub;
    }
}