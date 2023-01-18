package psoft.hsphere.axis;

import java.io.IOException;
import java.util.Date;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/* loaded from: hsphere.zip:psoft/hsphere/axis/AxisAccessFilter.class */
public class AxisAccessFilter implements Filter {
    FilterConfig config = null;
    String accessAllowedFrom = null;

    public void init(FilterConfig filterConfig) throws ServletException {
        this.config = filterConfig;
        try {
            ResourceBundle b = PropertyResourceBundle.getBundle("psoft_config.allow_access");
            this.accessAllowedFrom = b.getString("ACCESS_ALLOW");
        } catch (MissingResourceException e) {
            System.out.println("No allow_access.properties found, axis access will denied");
        }
        System.out.println("AXIS servlet filter has been initialized.");
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        String remoteIp = servletRequest.getRemoteAddr();
        new Date();
        if (this.accessAllowedFrom != null && ("ALL".equals(this.accessAllowedFrom.toUpperCase()) || this.accessAllowedFrom.contains(remoteIp))) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }
        ((HttpServletResponse) servletResponse).sendError(403);
        System.out.println(new Date().toString() + " Unathorized axis access attempt detected REMOTE_IP=" + remoteIp + " URL=" + ((Object) ((HttpServletRequest) servletRequest).getRequestURL()));
    }

    public void destroy() {
        this.config = null;
        this.accessAllowedFrom = null;
    }
}
