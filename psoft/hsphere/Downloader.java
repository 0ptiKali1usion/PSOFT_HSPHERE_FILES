package psoft.hsphere;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/* loaded from: hsphere.zip:psoft/hsphere/Downloader.class */
public class Downloader extends HttpServlet {
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Downloadable d = Session.getDownload(request.getParameter("id"));
        if (d == null) {
            return;
        }
        d.process(response);
    }
}
