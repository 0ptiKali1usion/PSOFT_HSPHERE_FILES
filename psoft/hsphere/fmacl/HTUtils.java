package psoft.hsphere.fmacl;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.Hashtable;
import psoft.hsphere.Localizer;
import psoft.hsphere.Reseller;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateErrorResult;
import psoft.hsphere.TemplateOKResult;
import psoft.util.HttpUtils;
import psoft.util.URLCheckResult;
import psoft.util.freemarker.TemplateMethodWrapper;

/* loaded from: hsphere.zip:psoft/hsphere/fmacl/HTUtils.class */
public class HTUtils implements TemplateHashModel {
    public TemplateModel get(String key) throws TemplateModelException {
        return TemplateMethodWrapper.getMethod(this, key);
    }

    public TemplateModel FM_checkImageByURL(String imageId, String urlPrefix) throws Exception {
        if (urlPrefix == null) {
            return new TemplateErrorResult(Localizer.translateMessage("error.empty_url"));
        }
        Hashtable image = Session.getDesign().getImage(imageId);
        if (image == null) {
            return new TemplateErrorResult(Localizer.translateMessage("error.unknown_image"));
        }
        if (!urlPrefix.startsWith("http://") && !urlPrefix.startsWith("https://")) {
            if (!urlPrefix.startsWith("/")) {
                urlPrefix = "/" + urlPrefix;
            }
            Reseller resel = Reseller.getReseller(Session.getUser().getResellerId());
            String protocol = resel.getProtocol();
            String host = resel.getURL();
            String port = resel.getPort().trim();
            if (port != null && !"".equals(port)) {
                urlPrefix = protocol + host + ":" + port + urlPrefix;
            } else {
                urlPrefix = protocol + host + urlPrefix;
            }
        }
        String message = checkURLResource(urlPrefix + image.get("file").toString());
        return message == null ? new TemplateOKResult() : new TemplateErrorResult(message);
    }

    public String checkURLResource(String url) {
        URLCheckResult r = HttpUtils.checkURLResource(url);
        switch (r.getResult()) {
            case 0:
                return null;
            case 1:
                return r.getMessage() == null ? Localizer.translateMessage("error.unable_find_url_resource") : r.getMessage();
            case 2:
                return Localizer.translateMessage("error.url_unknown_host");
            case 3:
                return Localizer.translateMessage("error.url_unknown_protocol");
            case 4:
                return Localizer.translateMessage("error.empty_url");
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            default:
                return Localizer.translateMessage("error.unknown_error");
            case 10:
                return r.getMessage();
        }
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }
}
