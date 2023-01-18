package psoft.hsphere.resource.registrar.opensrs.xcp;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/* loaded from: hsphere.zip:psoft/hsphere/resource/registrar/opensrs/xcp/XCPResolver.class */
public class XCPResolver implements EntityResolver {
    @Override // org.xml.sax.EntityResolver
    public InputSource resolveEntity(String publicId, String systemId) {
        if (systemId.endsWith("ops.dtd")) {
            return new XCPInputSource(XCPInputSource.DTD_INPUT_SOURCE, "ops");
        }
        return null;
    }
}
