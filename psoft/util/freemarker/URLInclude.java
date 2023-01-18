package psoft.util.freemarker;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import psoft.util.HttpResponse;
import psoft.util.HttpUtils;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/util/freemarker/URLInclude.class */
public class URLInclude implements TemplateMethodModel {
    public static final URLInclude includeURL = new URLInclude();

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel exec(List l) {
        try {
            String urlStr = HTMLEncoder.decode((String) l.get(0));
            URL url = new URL(urlStr);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            HttpResponse r = HttpUtils.parseResponse(con);
            con.disconnect();
            return new TemplateString(r.getBody());
        } catch (Exception e) {
            return new TemplateString("<!-- Unable to fetch URL -->");
        }
    }
}
