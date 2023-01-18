package psoft.hsphere.payment;

import freemarker.template.Template;
import freemarker.template.TemplateModelRoot;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Enumeration;
import java.util.HashMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Category;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Language;
import psoft.hsphere.Session;
import psoft.hsphere.UnknownResellerException;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.hsphere.util.XMLManager;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/payment/ExternalPaymentEngine.class */
public class ExternalPaymentEngine extends HttpServlet {
    protected Category LOG;
    public static short EXCEPTION = 1;
    public static short UNKNOWNRESELLEREXCEPTION = 2;

    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setDefaultReseller();
        request.getServletPath();
        WebPayment webprocessor = null;
        String gatewayName = "";
        short result = 0;
        String error = "";
        try {
            HashMap info = getProcessorInfo(request);
            gatewayName = (String) info.get("name");
            Class c = Class.forName((String) info.get("class"));
            webprocessor = (WebPayment) c.newInstance();
            webprocessor.init(gatewayName, request);
        } catch (UnknownResellerException re) {
            error = "Web Processor Initialization problem. An incorrect URL is used to add payment. Web Processor Type: " + gatewayName;
            Session.getLog().error(error, re);
            result = UNKNOWNRESELLEREXCEPTION;
        } catch (Exception ex) {
            error = "Web Processor Initialization problem. Web Processor Type: " + gatewayName + " Error message:" + ex.getMessage();
            Session.getLog().error(error, ex);
            result = EXCEPTION;
        }
        if (result == 0) {
            try {
                webprocessor.processRequest(request, response);
            } catch (Exception ex2) {
                error = "Problem processing request. Web Processor Type: " + gatewayName + " Error message:" + ex2.getMessage();
                Session.getLog().error(error, ex2);
                result = EXCEPTION;
            }
        }
        if (webprocessor.getRequestType() == 4) {
            webprocessor.displayDemoRedirectPage(response);
        } else if (result != 0) {
            processResponse(gatewayName, webprocessor, result, error, response);
        } else if (webprocessor.getRequestType() != 1) {
            processResponse(gatewayName, webprocessor, result, error, response);
        }
        if (webprocessor.getRequestType() != 3) {
            writeLog(request, gatewayName, webprocessor, result, error);
        }
    }

    private void writeLog(HttpServletRequest request, String processorName, WebPayment webPayment, short transResult, String errorMessage) {
        PreparedStatement ps1;
        long transId = 0;
        long accountId = 0;
        long requestId = 0;
        int requestType = 0;
        long resellerId = 0;
        double amount = 0.0d;
        if (webPayment != null) {
            try {
                resellerId = webPayment.getResellerId();
            } catch (Exception re) {
                Session.getLog().error("ExternalPaymentEngine.writeLog(). Problem getting reseller Id", re);
            }
        }
        if (resellerId == 0) {
            resellerId = Session.getResellerId();
        }
        try {
            HashMap requestData = new HashMap();
            Enumeration pNames = request.getParameterNames();
            while (pNames.hasMoreElements()) {
                String pName = (String) pNames.nextElement();
                requestData.put(pName, request.getParameter(pName));
            }
            requestData.put("RemoteAddr", request.getRemoteAddr());
            if (webPayment != null) {
                transId = webPayment.getTransactionId();
                accountId = webPayment.getAccountId();
                requestId = webPayment.getRequestId();
                requestType = webPayment.getRequestType();
                amount = webPayment.getAmount();
            }
            if (requestType == 0) {
                if ("redirect".equals(request.getParameter("action"))) {
                    requestType = 1;
                } else {
                    requestType = 2;
                }
            }
            if (transId == 0) {
                transId = Session.getNewId("extern_pm_seq");
            }
            Connection con_db = Session.getDb();
            PreparedStatement ps = null;
            try {
                ps = con_db.prepareStatement("SELECT id FROM extern_pm_log WHERE id=?");
                ps.setLong(1, transId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    ps1 = con_db.prepareStatement("UPDATE extern_pm_log SET account_id=?, request_id=?, reseller_id=?, created=?, gateway_name=?, request_type=?," + (requestType == 2 ? "result_amount=?" : "requested_amount=?") + ", error_message=?, result=?, request_info=?  WHERE id=?");
                } else {
                    ps1 = con_db.prepareStatement("INSERT INTO extern_pm_log (account_id, request_id, reseller_id, created, gateway_name, request_type, " + (requestType == 2 ? "result_amount" : "requested_amount") + ", error_message, result, request_info, id) VALUES (?,?,?,?,?,?,?,?,?,?,?)");
                }
                ps1.setLong(1, accountId);
                ps1.setLong(2, requestId);
                ps1.setLong(3, resellerId);
                ps1.setTimestamp(4, TimeUtils.getSQLTimestamp());
                ps1.setString(5, processorName);
                ps1.setInt(6, requestType);
                ps1.setDouble(7, amount);
                ps1.setString(8, errorMessage);
                ps1.setInt(9, transResult);
                ps1.setString(10, requestData.toString());
                ps1.setLong(11, transId);
                ps1.executeUpdate();
                Session.closeStatement(ps);
                con_db.close();
            } catch (Exception ex) {
                Session.getLog().error("Problem updating extern_pm_log", ex);
                Session.closeStatement(ps);
                con_db.close();
            }
        } catch (Exception ex2) {
            Session.getLog().error("Unabe to update transaction info ", ex2);
        }
    }

    private void setDefaultReseller() {
        C0004CP.getCP().setConfig();
        Session.setLanguage(new Language(null));
        try {
            Session.setResellerId(1L);
        } catch (UnknownResellerException ex) {
            Session.getLog().error("Error setting default reseller ID: ", ex);
        }
    }

    public static HashMap getProcessorInfo(HttpServletRequest request) throws Exception {
        String servletPath = request.getServletPath();
        HashMap res = new HashMap();
        Document doc = XMLManager.getXML("MERCHANT_GATEWAYS_CONF");
        Element processors = (Element) doc.getElementsByTagName("processors").item(0);
        NodeList proclist = processors.getElementsByTagName("processor");
        Session.getLog().debug("Getting info for the " + servletPath + " servlet request");
        int i = 0;
        while (true) {
            if (i >= proclist.getLength()) {
                break;
            }
            Element processor = (Element) proclist.item(i);
            if (servletPath.indexOf(processor.getAttribute("servlet")) < 0) {
                i++;
            } else {
                Session.getLog().debug("Initializing a web processor: " + processor.getAttribute("name"));
                res.put("template", processor.getAttribute("template"));
                res.put("servlet", processor.getAttribute("servlet"));
                res.put("class", processor.getAttribute("class"));
                res.put("name", processor.getAttribute("name"));
                break;
            }
        }
        return res;
    }

    public void processResponse(String processorName, WebPayment webPayment, short transResult, String errorMessage, HttpServletResponse response) {
        try {
            response.setContentType("text/html");
            if (webPayment == null || !webPayment.isInitialized()) {
                Session.getLog().debug("Displayung the default teplate for unknown reseller.");
                TemplateModelRoot defaultRoot = Session.getModelRoot();
                Template template = Session.getTemplate("billing/paymentresult.html");
                defaultRoot.put("processorName", new TemplateString(processorName));
                defaultRoot.put("errorMessage", new TemplateString(WebPayment.isEmpty(errorMessage) ? "Web processor can't be initialized die to Incorrect parameters included into the request." : errorMessage));
                template.process(defaultRoot, response.getWriter());
            } else {
                webPayment.processResponse(response, transResult, errorMessage);
            }
        } catch (Exception ex) {
            Session.getLog().error("Problem displaying results. ", ex);
            Ticket.create(ex, null);
        }
    }
}
