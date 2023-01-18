package psoft.hsphere;

import freemarker.template.SimpleHash;
import freemarker.template.SimpleList;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Category;
import org.apache.log4j.PropertyConfigurator;
import psoft.util.NFUCache;
import psoft.util.Salt;
import psoft.util.Toolbox;
import psoft.web.utils.MultipartForm;

/* loaded from: hsphere.zip:psoft/hsphere/Uploader.class */
public class Uploader extends HttpServlet {
    protected static NFUCache cache;
    protected ResourceBundle config;
    protected Category LOG = null;
    protected String templatePath = "misc/";
    private Salt salt;

    public void init(ServletConfig sconfig) throws ServletException {
        super.init(sconfig);
        this.config = PropertyResourceBundle.getBundle("psoft_config.hsphere");
        try {
            PropertyConfigurator.configure(Toolbox.RB2Properties(this.config));
            this.LOG = Category.getInstance(getClass().getName());
            Session.setLog(this.LOG);
        } catch (MissingResourceException mrb) {
            mrb.printStackTrace();
        }
        try {
            this.salt = new Salt();
            cache = new NFUCache(100);
            Session.getLog().info("Uploader initialized");
        } catch (Exception e) {
            throw new UnavailableException(this, "Can't init Salt " + e.toString());
        }
    }

    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Template template;
        Session.setLog(this.LOG);
        try {
            response.setContentType("text/html");
            MultipartForm mp = new MultipartForm(request);
            SimpleHash simpleHash = new SimpleHash();
            SimpleList retvals = new SimpleList();
            SimpleHash params = new SimpleHash();
            String templateName = "uploader.html";
            while (true) {
                String param = mp.nextParameter();
                if (param == null) {
                    break;
                } else if (mp.isFile()) {
                    String fname = mp.getFilename();
                    if (fname == null || "".equals(fname)) {
                        Session.getLog().error("Name of the file to upload was not specified.");
                    } else {
                        Session.getLog().info("File found, name:" + fname);
                        String signKey = getSignature(fname);
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        long written = mp.writeParameter(out);
                        if (written <= 0) {
                            Session.getLog().error("The file to upload '" + fname + "' was empty.");
                        } else {
                            putFile(signKey, out.toByteArray());
                            out.close();
                            SimpleHash pair = new SimpleHash();
                            pair.put("name", new SimpleScalar(param + "_cid"));
                            pair.put("value", new SimpleScalar(signKey));
                            retvals.add(pair);
                        }
                    }
                } else {
                    String line = mp.getParameter();
                    if ("template_name".equals(param)) {
                        templateName = line;
                    } else {
                        params.put(param, new SimpleScalar(line));
                    }
                }
            }
            simpleHash.put("request", params);
            simpleHash.put("retvals", retvals);
            try {
                template = Session.getTemplate(this.templatePath + templateName);
            } catch (NullPointerException e) {
                template = null;
            }
            if (template == null) {
                throw new TemplateException("Unable to get template '" + this.templatePath + templateName + "'.");
            }
            template.process(simpleHash, response.getWriter());
        } catch (Exception ex) {
            Session.getLog().error("Cannot process request. ", ex);
        }
    }

    protected String getSignature(String param) {
        return param + "|" + this.salt.getNext(20);
    }

    protected void putFile(String key, byte[] data) {
        cache.put(key, data);
        Session.getLog().info("File was stored, key=" + key);
    }

    public static InputStream getFile(String key) throws Exception {
        byte[] file = (byte[]) cache.get(key);
        if (file == null) {
            Session.getLog().debug("Unable to get a file by key '" + key + "' from the cache.");
            throw new HSUserException("uploader.file_not_found");
        }
        ByteArrayInputStream out = new ByteArrayInputStream(file);
        cache.remove(key);
        return out;
    }

    public static byte[] getData(String key) throws Exception {
        byte[] data = (byte[]) cache.get(key);
        if (data == null) {
            throw new Exception("File not found, key=" + key);
        }
        cache.remove(key);
        return data;
    }

    public static void setData(String key, byte[] data) throws Exception {
        if (cache == null) {
            cache = new NFUCache(100);
        }
        cache.put(key, data);
    }
}
