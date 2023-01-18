package psoft.hsphere.migrator;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.io.BufferedReader;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import psoft.hsphere.Session;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/migrator/PlanChangeRequest.class */
public class PlanChangeRequest implements HttpServletRequest, TemplateHashModel {
    protected Hashtable paramHash;
    protected String remoteAddr;
    protected int remotePort;
    protected String localName;
    protected String localAddr;
    protected int localPort;

    public Object getAttribute(String name) {
        return null;
    }

    public Enumeration getAttributeNames() {
        return null;
    }

    public String getCharacterEncoding() {
        return null;
    }

    public int getContentLength() {
        return -1;
    }

    public ServletInputStream getInputStream() {
        return null;
    }

    public String getParameter(String name) {
        Session.getLog().info("Fake -->" + name + ":" + getParameter1(name));
        return getParameter1(name);
    }

    public String getParameter1(String name) {
        String param = getParameterValue(name);
        if (param == null) {
            return null;
        }
        return param;
    }

    public Enumeration getParameterNames() {
        return this.paramHash.keys();
    }

    public String getParameterValue(String name) {
        String[] values;
        Object obj = this.paramHash.get(name);
        if (obj != null && (values = (String[]) this.paramHash.get(name)) != null && values.length > 0) {
            return values[0];
        }
        return null;
    }

    public String[] getParameterValues(String name) {
        return (String[]) this.paramHash.get(name);
    }

    public String getProtocol() {
        return null;
    }

    public BufferedReader getReader() {
        return null;
    }

    public String getRealPath(String name) {
        return null;
    }

    public String getRemoteAddr() {
        return this.remoteAddr;
    }

    public String getRemoteHost() {
        return null;
    }

    public String getScheme() {
        return null;
    }

    public String getServerName() {
        return null;
    }

    public int getServerPort() {
        return -1;
    }

    public void setAttribute(String key, Object o) {
    }

    public String getContentType() {
        return null;
    }

    public String getAuthType() {
        return null;
    }

    public Cookie[] getCookies() {
        return null;
    }

    public long getDateHeader(String name) {
        return -1L;
    }

    public String getHeader(String name) {
        return null;
    }

    public Enumeration getHeaderNames() {
        return null;
    }

    public int getIntHeader(String name) {
        return -1;
    }

    public String getMethod() {
        return null;
    }

    public String getPathInfo() {
        return null;
    }

    public String getPathTranslated() {
        return null;
    }

    public String getQueryString() {
        return null;
    }

    public String getRemoteUser() {
        return null;
    }

    public String getRequestedSessionId() {
        return null;
    }

    public String getRequestURI() {
        return null;
    }

    public String getServletPath() {
        return null;
    }

    public HttpSession getSession() {
        return null;
    }

    public HttpSession getSession(boolean create) {
        return null;
    }

    public boolean isRequestedSessionIdFromCookie() {
        return false;
    }

    public boolean isRequestedSessionIdFromUrl() {
        return false;
    }

    public boolean isRequestedSessionIdFromURL() {
        return false;
    }

    public boolean isRequestedSessionIdValid() {
        return false;
    }

    public String getContextPath() {
        return null;
    }

    public Enumeration getHeaders(String name) {
        return null;
    }

    public Principal getUserPrincipal() {
        return null;
    }

    public boolean isUserInRole(String role) {
        return true;
    }

    public Locale getLocale() {
        return null;
    }

    public Enumeration getLocales() {
        return null;
    }

    public boolean isSecure() {
        return false;
    }

    public RequestDispatcher getRequestDispatcher(String path) {
        return null;
    }

    public void removeAttribute(String name) {
    }

    public StringBuffer getRequestURL() {
        return null;
    }

    public void setCharacterEncoding(String enc) {
    }

    public Map getParameterMap() {
        return null;
    }

    public void addParameter(Object name, Object value) {
        this.paramHash.put(name, value);
    }

    public PlanChangeRequest(Hashtable paramHash) {
        this.remoteAddr = "127.0.0.1";
        this.remotePort = 0;
        this.localName = null;
        this.localPort = 0;
        this.paramHash = paramHash;
        String remoteAddr = getParameter1("REMOTE_ADDR");
        if (remoteAddr != null) {
            this.remoteAddr = remoteAddr;
        }
    }

    public PlanChangeRequest(HttpServletRequest request) {
        this.remoteAddr = "127.0.0.1";
        this.remotePort = 0;
        this.localName = null;
        this.localPort = 0;
        this.paramHash = new Hashtable();
        this.remoteAddr = request.getRemoteAddr();
        Enumeration e = request.getParameterNames();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            String value = request.getParameter(name);
            this.paramHash.put(name, new String(value));
        }
    }

    public TemplateModel get(String key) throws TemplateModelException {
        String value = (String) this.paramHash.get(key);
        if (value != null && value.length() > 0) {
            return new TemplateString(value);
        }
        return null;
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    public int getRemotePort() {
        return this.remotePort;
    }

    public String getLocalName() {
        return this.localName;
    }

    public String getLocalAddr() {
        return this.localAddr;
    }

    public int getLocalPort() {
        return this.localPort;
    }
}
