package psoft.hsphere.axis.tcp;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/* loaded from: hsphere.zip:psoft/hsphere/axis/tcp/TCPStreamHandler.class */
public class TCPStreamHandler extends URLStreamHandler {
    @Override // java.net.URLStreamHandler
    protected URLConnection openConnection(URL u) throws IOException {
        return new TCPURLConnection(u);
    }
}
