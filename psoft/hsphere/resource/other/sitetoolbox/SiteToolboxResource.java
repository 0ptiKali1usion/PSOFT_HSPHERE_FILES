package psoft.hsphere.resource.other.sitetoolbox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import org.apache.log4j.Category;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.admin.Settings;
import psoft.util.freemarker.Template2String;

/* loaded from: hsphere.zip:psoft/hsphere/resource/other/sitetoolbox/SiteToolboxResource.class */
public class SiteToolboxResource extends Resource {
    private static Category log = Category.getInstance(SiteToolboxResource.class.getName());

    public SiteToolboxResource(int typeId, Collection initValues) throws Exception {
        super(typeId, initValues);
    }

    public SiteToolboxResource(ResourceId rId) throws Exception {
        super(rId);
    }

    protected String exec(String call) throws IOException {
        URL url = new URL(Settings.get().getValue("toolbox_url") + "/RPC/websitesource");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        PrintWriter out = new PrintWriter(con.getOutputStream());
        out.print(call);
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        StringBuffer buf = new StringBuffer();
        while (true) {
            String inputLine = in.readLine();
            if (inputLine != null) {
                buf.append(inputLine).append('\n');
            } else {
                try {
                    break;
                } catch (Exception e) {
                }
            }
        }
        con.disconnect();
        return buf.toString();
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        exec(Template2String.process(Session.getTemplate("/other/sitetoolbox/create.xml"), Session.getModelRoot()));
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            exec(Template2String.process(Session.getTemplate("/other/sitetoolbox/delete.xml"), Session.getModelRoot()));
        }
    }
}
