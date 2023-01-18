package psoft.hsphere.migrator.creator;

import org.w3c.dom.Element;
import psoft.hsphere.User;

/* loaded from: hsphere.zip:psoft/hsphere/migrator/creator/UserCreator.class */
public interface UserCreator {
    User createUser(Element element) throws Exception;

    int execute() throws Exception;
}
