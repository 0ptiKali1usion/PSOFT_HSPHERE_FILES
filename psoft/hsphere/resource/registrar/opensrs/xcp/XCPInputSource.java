package psoft.hsphere.resource.registrar.opensrs.xcp;

import java.io.Reader;
import java.io.StringReader;
import org.xml.sax.InputSource;

/* loaded from: hsphere.zip:psoft/hsphere/resource/registrar/opensrs/xcp/XCPInputSource.class */
public class XCPInputSource extends InputSource {
    protected Reader reader;
    public static String DTD_INPUT_SOURCE = "<?xml encoding=\"UTF-8\"?>\n<!ELEMENT OPS_envelope (header,body)>\n<!ELEMENT header (version, msg_id?, msg_type?)>\n<!ELEMENT body (data_block)>\n<!ELEMENT data_block (dt_assoc|dt_array|dt_scalar|dt_scalarrerf)>\n<!ELEMENT dt_assoc (dt_assoc|dt_array|dt_scalar|dt_scalarref|(item)*)>\n<!ELEMENT dt_array (dt_assoc|dt_array|dt_scalar|dt_scalarref|(item)*)>\n<!ELEMENT dt_scalar (#PCDATA |dt_assoc |dt_array |dt_scalar |dt_scalarref)*>\n<!ELEMENT dt_scalarref (#PCDATA |dt_assoc|dt_array|dt_scalar|dt_scalarref)*>\n<!ELEMENT item (#PCDATA | dt_assoc | dt_array | dt_scalar | dt_scalarref)*>\n<!ATTLIST item key CDATA #REQUIRED>\n<!ATTLIST item class CDATA \"\">\n<!ELEMENT msg_id (#PCDATA)>\n<!ELEMENT msg_type (#PCDATA)>\n<!ELEMENT version (#PCDATA)>\n<!ENTITY author \"Victor Magdic\">\n<!ENTITY company \"Tucows\">\n<!ENTITY copyright \"2000, Tucows\">";

    public XCPInputSource(String xml, String id) {
        super(id);
        this.reader = new StringReader(xml);
        setCharacterStream(this.reader);
    }

    public XCPInputSource(String xml) {
        this(xml, "xcp");
    }
}
