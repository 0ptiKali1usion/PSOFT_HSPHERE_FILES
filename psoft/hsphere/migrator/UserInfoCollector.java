package psoft.hsphere.migrator;

import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Document;

/* loaded from: hsphere.zip:psoft/hsphere/migrator/UserInfoCollector.class */
public interface UserInfoCollector {
    Document getUsersXML(ArrayList arrayList) throws Exception;

    Document getResellersXML(ArrayList arrayList) throws Exception;

    List getUsersList() throws Exception;

    List getResellersList() throws Exception;
}
