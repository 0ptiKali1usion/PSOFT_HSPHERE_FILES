package psoft.util;

import com.sun.net.ssl.HttpsURLConnection;
import com.sun.net.ssl.KeyManager;
import com.sun.net.ssl.SSLContext;
import com.sun.net.ssl.TrustManager;
import com.sun.net.ssl.X509TrustManager;
import com.sun.net.ssl.internal.ssl.Provider;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import javax.net.ssl.SSLSocketFactory;
import psoft.hsphere.resource.HTTPAuth;
import sun.security.provider.Sun;

/* loaded from: hsphere.zip:psoft/util/HttpUtils.class */
public class HttpUtils {
    protected static SSLSocketFactory fakeSSLSocketFactory;

    static {
        Security.addProvider(new Sun());
        Security.addProvider(new Provider());
        System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
        fakeSSLSocketFactory = null;
    }

    public static HttpURLConnection getRequest(String protocol, String server, int port, String path, String login, String password, Map req) throws Exception {
        Authenticator.setDefault(new HTTPAuth(login, password));
        return getRequest(protocol, server, port, path, req);
    }

    public static HttpURLConnection getRequest(String protocol, String server, int port, String path, Map req) throws Exception {
        String path2;
        if (path.endsWith("?")) {
            path2 = path + makeForm(req);
        } else {
            path2 = path + "?" + makeForm(req);
        }
        URL url = new URL(protocol, server, port, path2);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        return con;
    }

    public static HttpResponse getForm(String protocol, String server, int port, String path, String login, String password, Map req) throws Exception {
        Authenticator.setDefault(new HTTPAuth(login, password));
        return getForm(protocol, server, port, path, req);
    }

    public static HttpResponse getForm(String protocol, String server, int port, String path, Map req) throws Exception {
        String path2;
        if (path.endsWith("?")) {
            path2 = path + makeForm(req);
        } else {
            path2 = path + "?" + makeForm(req);
        }
        URL url = new URL(protocol, server, port, path2);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        return parseResponse(con);
    }

    public static HttpResponse postForm(String protocol, String server, int port, String path, Map req) throws Exception {
        URL url = new URL(protocol, server, port, path);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        PrintWriter out = new PrintWriter(con.getOutputStream());
        out.print(makeForm(req));
        out.close();
        return parseResponse(con);
    }

    public static HttpResponse parseResponse(HttpURLConnection con) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        StringBuffer buf = new StringBuffer();
        while (true) {
            String inputLine = in.readLine();
            if (inputLine != null) {
                buf.append(inputLine).append('\n');
            } else {
                in.close();
                HttpResponse response = new HttpResponse(con.getResponseCode(), con.getResponseMessage(), buf.toString());
                return response;
            }
        }
    }

    public static String makeForm(Map req) {
        StringBuffer buf = new StringBuffer();
        Iterator keys = req.keySet().iterator();
        boolean z = true;
        while (true) {
            boolean first = z;
            if (keys.hasNext()) {
                String key = (String) keys.next();
                addParams(buf, key, req.get(key), first);
                z = false;
            } else {
                return buf.toString();
            }
        }
    }

    private static void addParams(StringBuffer buf, String key, Object values, boolean first) {
        if (values instanceof Collection) {
            for (String str : (Collection) values) {
                addParam(buf, key, str, first);
                first = false;
            }
            return;
        }
        addParam(buf, key, (String) values, first);
    }

    private static void addParam(StringBuffer buf, String key, String value, boolean first) {
        if (!first) {
            buf.append("&");
        }
        buf.append(URLEncoder.encode(key));
        if (value != null) {
            buf.append("=").append(URLEncoder.encode(value));
        }
    }

    public static URLCheckResult checkURLResource(String resourceUrl) {
        if (resourceUrl != null) {
            try {
                if (!"".equals(resourceUrl)) {
                    if (resourceUrl.startsWith("https")) {
                        HttpsURLConnection sslCon = new URL(resourceUrl).openConnection();
                        setFakeSSLSocketFactory(sslCon);
                        try {
                            sslCon.connect();
                            String response = sslCon.getResponseMessage();
                            return "OK".equals(response) ? URLCheckResult.OK_RESULT : new URLCheckResult(response);
                        } finally {
                            sslCon.disconnect();
                        }
                    } else if (resourceUrl.startsWith("http://")) {
                        HttpURLConnection con = (HttpURLConnection) new URL(resourceUrl).openConnection();
                        try {
                            con.connect();
                            String response2 = con.getResponseMessage();
                            return "OK".equals(response2) ? URLCheckResult.OK_RESULT : new URLCheckResult(response2);
                        } finally {
                            con.disconnect();
                        }
                    } else {
                        return new URLCheckResult(3);
                    }
                }
            } catch (FileNotFoundException e) {
                return new URLCheckResult(1);
            } catch (MalformedURLException e2) {
                return new URLCheckResult(3);
            } catch (UnknownHostException e3) {
                return new URLCheckResult(2);
            } catch (Exception e4) {
                String message = e4.getMessage();
                if (message == null) {
                    e4.printStackTrace();
                    return new URLCheckResult(5);
                }
                return new URLCheckResult(message);
            }
        }
        return new URLCheckResult(4);
    }

    protected static void setFakeSSLSocketFactory(HttpsURLConnection con) {
        if (fakeSSLSocketFactory == null) {
            TrustManager[] trustAllCerts = {new X509TrustManager() { // from class: psoft.util.HttpUtils.1
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }

                public boolean isClientTrusted(X509Certificate[] certs) {
                    return true;
                }

                public boolean isServerTrusted(X509Certificate[] certs) {
                    return true;
                }
            }};
            try {
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init((KeyManager[]) null, trustAllCerts, new SecureRandom());
                fakeSSLSocketFactory = sc.getSocketFactory();
            } catch (Exception e) {
                System.err.println("Unable to set a Fake SSL Socket Factory.");
                fakeSSLSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
            }
        }
        con.setSSLSocketFactory(fakeSSLSocketFactory);
    }

    public static void main(String[] argv) throws Exception {
        int port = 443;
        String protocol = "https";
        String server = "psoft.net";
        String path = "";
        int i = 0;
        while (i < argv.length) {
            try {
                if ("--port".equals(argv[i])) {
                    port = Integer.parseInt(argv[i + 1]);
                    i++;
                } else if ("--server".equals(argv[i])) {
                    server = argv[i + 1];
                    i++;
                } else if ("--path".equals(argv[i])) {
                    path = argv[i + 1];
                    i++;
                } else if ("--protocol".equals(argv[i])) {
                    protocol = argv[i + 1];
                    i++;
                }
                i++;
            } catch (Throwable tr) {
                System.out.println("Error: " + tr.getMessage());
                tr.printStackTrace();
            }
        }
        System.out.println("******************Start*****************");
        HttpResponse resp = postForm(protocol, server, port, path, new Hashtable());
        System.out.print("Response code:" + resp.getResponseCode());
        System.out.print("Response message:" + resp.getResponseMessage());
        System.out.println("Response body:\n" + resp.getBody());
        System.out.println("****************OK***************");
        System.exit(0);
    }
}
