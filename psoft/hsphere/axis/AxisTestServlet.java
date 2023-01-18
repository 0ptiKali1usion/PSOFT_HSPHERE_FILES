package psoft.hsphere.axis;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.encoding.XMLType;
import org.apache.log4j.Category;

/* loaded from: hsphere.zip:psoft/hsphere/axis/AxisTestServlet.class */
public class AxisTestServlet extends HttpServlet {
    private static Category log = Category.getInstance(AxisTestServlet.class.getName());

    private String getMapPathFromURI(String uri) {
        int ends = uri.indexOf("AxisTestServlet");
        return uri.substring(0, ends);
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String[] servicesNames = {"AdminServices", "DNSServices", "DomainServices", "FtpServices", "MSSQLServices", "MailServices", "MailServicesIris", "MySQLServices", "PostgreSQLServices", "SupportServices", "UserServices", "WebServices", "PGPServices", "MigrationServices"};
        String host = "127.0.0.1";
        String port = "8180";
        String reqURI = getMapPathFromURI(request.getRequestURI());
        if (request.getParameter("host") != null) {
            host = request.getParameter("host");
        }
        if (request.getParameter("port") != null) {
            port = request.getParameter("port");
        }
        PrintWriter writer = response.getWriter();
        response.setContentType("text/html");
        writer.println("<html>");
        try {
            Service service = new Service();
            service.getEngine();
            writer.println("<h1>Axis engine test successfull.</h1><br>");
            for (String serviceName : servicesNames) {
                String endpoint = "http://" + host + ":" + port + reqURI + serviceName + ".jws";
                testService(service, endpoint, writer, serviceName, "getDescription", endpoint);
            }
            writer.print("</html>");
        } catch (Exception exc) {
            writer.println("<h1>Axis not configured.</h1>Server return : " + exc.toString());
            writer.println("</html>");
        }
    }

    public void testService(Service service, String endpoint, PrintWriter writer, String serviceName, String methodName, String reqURL) {
        try {
            Call call = service.createCall();
            call.setTargetEndpointAddress(endpoint);
            call.setOperationName(new QName(serviceName, methodName));
            call.setReturnType(XMLType.XSD_ANY);
            String result = (String) call.invoke(new Object[0]);
            writer.println("<h3>Check service - " + serviceName + " sucessfully.</h3>Description : " + result + "<br>Request URL - " + reqURL + "<br><hr>");
        } catch (Exception exc) {
            writer.println("<h3>Check service - " + serviceName + " error.</h3><br>Server return : " + exc.toString() + "<br>Request URL - " + reqURL + "<br><hr>");
        }
    }
}
