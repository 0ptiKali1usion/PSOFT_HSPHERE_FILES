package psoft.hsphere;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/* loaded from: hsphere.zip:psoft/hsphere/Downloadable.class */
public interface Downloadable {
    void process(HttpServletResponse httpServletResponse) throws ServletException, IOException;
}
