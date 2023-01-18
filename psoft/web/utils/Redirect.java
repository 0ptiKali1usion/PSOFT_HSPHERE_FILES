package psoft.web.utils;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

/* loaded from: hsphere.zip:psoft/web/utils/Redirect.class */
public class Redirect {
    public static void sendRedirect(HttpServletResponse response, String url, boolean type) throws IOException {
        if (!type) {
            response.setContentType("text/html");
            response.getWriter().println("<HTML><HEAD><META HTTP-EQUIV=\"Refresh\" CONTENT=\"0; URL=" + url + "\"></HEAD></HTML>");
            return;
        }
        response.sendRedirect(url);
    }
}
