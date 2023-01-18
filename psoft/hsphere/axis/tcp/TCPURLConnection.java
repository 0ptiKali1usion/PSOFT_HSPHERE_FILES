package psoft.hsphere.axis.tcp;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

/* loaded from: hsphere.zip:psoft/hsphere/axis/tcp/TCPURLConnection.class */
public class TCPURLConnection extends URLConnection {
    public TCPURLConnection(URL u) {
        super(u);
    }

    @Override // java.net.URLConnection
    public void connect() throws IOException {
    }
}
