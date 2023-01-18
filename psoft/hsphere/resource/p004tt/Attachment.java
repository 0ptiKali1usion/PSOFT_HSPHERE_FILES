package psoft.hsphere.resource.p004tt;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.StringTokenizer;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Category;
import psoft.hsphere.Downloadable;
import psoft.hsphere.Session;
import psoft.util.IOUtils;
import psoft.util.freemarker.TemplateString;

/* renamed from: psoft.hsphere.resource.tt.Attachment */
/* loaded from: hsphere.zip:psoft/hsphere/resource/tt/Attachment.class */
public class Attachment implements TemplateHashModel, Downloadable {

    /* renamed from: OK */
    public static final int f215OK = 0;
    public static final int DELETED = 0;
    protected static File path;

    /* renamed from: id */
    int f216id;
    String name;
    String mimeType;
    int state;
    private static final Category log = Category.getInstance(Attachment.class.getName());

    public Attachment(int id, String name, String mimeType, int state) {
        this.f216id = id;
        this.name = name;
        this.mimeType = mimeType;
    }

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) {
        if ("id".equals(key)) {
            return new TemplateString(this.f216id);
        }
        if ("state".equals(key)) {
            return new TemplateString(this.state);
        }
        if ("name".equals(key)) {
            return new TemplateString(this.name);
        }
        if ("mimeType".equals(key)) {
            return new TemplateString(this.mimeType);
        }
        if ("exists".equals(key)) {
            return new TemplateString(exists());
        }
        if ("download_key".equals(key)) {
            return new TemplateString(Session.setDownload("at-", this));
        }
        return null;
    }

    @Override // psoft.hsphere.Downloadable
    public void process(HttpServletResponse response) throws IOException {
        File f = getFile();
        if (f != null && f.isFile()) {
            String fileName = this.name == null ? "unknown" : this.name;
            response.setContentType(getMimeType());
            response.setContentLength((int) f.length());
            response.setHeader("Content-disposition", "attachment; filename=\"" + fileName + "\"");
            response.setHeader("Cache-Control", "no-cache, must-revalidate");
            response.setHeader("Connection", "close");
            response.setHeader("Expires", "0");
            IOUtils.copy(new FileInputStream(getFile()), response.getOutputStream());
            response.getOutputStream().flush();
            return;
        }
        log.error("The requested file '" + getName() + "' cannot be obtained.");
    }

    public int getState() {
        return this.state;
    }

    public String getName() {
        return this.name;
    }

    public String getMimeType() {
        if (this.mimeType == null) {
            return "text/plain;";
        }
        boolean first = true;
        StringTokenizer st = new StringTokenizer(this.mimeType, "; \n\t\r");
        StringBuffer result = new StringBuffer();
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            int disp = token.indexOf(61);
            if (disp < 0 || "charset".equals(token.substring(0, disp))) {
                if (!first) {
                    result.append("; ");
                } else {
                    first = false;
                }
                result.append(token);
            }
        }
        return result.toString();
    }

    public static File getPath() {
        if (path == null) {
            String p = Session.getPropertyString("TT_ATTACHMENTS_PATH");
            if ("".equals(p)) {
                p = "/hsphere/local/home/cpanel/.tt-attachments";
            }
            path = new File(p);
            path.mkdirs();
        }
        return path;
    }

    public byte[] getBody() throws IOException {
        File f = getFile(this.f216id);
        byte[] buf = new byte[(int) f.length()];
        FileInputStream in = new FileInputStream(f);
        in.read(buf);
        in.close();
        return buf;
    }

    protected File getFile() {
        return getFile(this.f216id);
    }

    protected static File getFile(int id) {
        return new File(getPath(), id + ".att");
    }

    public static void save(int id, InputStream is) throws IOException {
        File f = getFile(id);
        FileOutputStream out = new FileOutputStream(f);
        IOUtils.copy(is, out);
        out.close();
    }

    public boolean exists() {
        return getFile().exists();
    }

    public Part getAttachmentAsPart() throws Exception {
        getBody();
        MimeBodyPart mbp = new MimeBodyPart();
        FileDataSource fds = new FileDataSource(getFile().getAbsolutePath());
        mbp.setDataHandler(new DataHandler(fds));
        log.debug("Test attachments " + getName());
        mbp.setFileName(getName());
        return mbp;
    }
}
