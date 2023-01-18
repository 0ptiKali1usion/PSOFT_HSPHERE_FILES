package psoft.hsphere.resource;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Vector;
import org.apache.log4j.Category;
import psoft.util.IOUtils;

/* loaded from: hsphere.zip:psoft/hsphere/resource/StreamReadingThread.class */
public class StreamReadingThread extends Thread {
    private static final Category log = Category.getInstance(StreamReadingThread.class.getName());
    public static final int BUF_SIZE = 4096;
    public static final long DEFAULT_LOOP_TIMEOUT = 500;
    private boolean processDone = false;
    long sleepInterval = 500;
    byte[] buf = new byte[BUF_SIZE];
    final Vector streams = new Vector();
    HashMap results = new HashMap();
    int position = 0;
    boolean working = true;

    public StreamReadingThread() {
        setDaemon(true);
        setName("StreamReadingThread(" + getName() + ")");
    }

    /* loaded from: hsphere.zip:psoft/hsphere/resource/StreamReadingThread$StreamWrapper.class */
    public class StreamWrapper {
        StringBuffer buffer = new StringBuffer();

        /* renamed from: is */
        InputStream f160is;

        /* renamed from: ie */
        IOException f161ie;
        boolean done;

        StreamWrapper(InputStream is) {
            StreamReadingThread.this = r5;
            this.f160is = is;
        }

        InputStream getInputStream() {
            return this.f160is;
        }

        public String getResult() {
            return this.buffer.toString();
        }

        public Collection getResultAsCollection() throws IOException {
            Collection col = new LinkedList();
            BufferedReader br = new BufferedReader(new StringReader(this.buffer.toString()));
            while (true) {
                String tmp = br.readLine();
                if (null != tmp) {
                    col.add(tmp);
                } else {
                    return col;
                }
            }
        }

        public IOException getError() {
            return this.f161ie;
        }

        void setError(IOException ie) {
            this.f161ie = ie;
        }

        void append(String s) {
            this.buffer.append(s);
        }

        public void done() {
            this.done = true;
        }

        boolean isDone() {
            return this.done;
        }
    }

    void readStream(StreamWrapper sw) throws IOException {
        synchronized (sw) {
            if (!sw.isDone()) {
                InputStream is = sw.getInputStream();
                int size = is.available();
                if (size > 0) {
                    if (size > 4096) {
                        size = 4096;
                    }
                    int size2 = is.read(this.buf, 0, size);
                    if (size2 < 0) {
                        finish(sw, null);
                    }
                    sw.append(new String(this.buf, 0, size2));
                    this.working = true;
                    this.sleepInterval = 500L;
                }
            }
        }
    }

    @Override // java.lang.Thread, java.lang.Runnable
    public void run() {
        while (true) {
            StreamWrapper sw = getNextStream();
            if (sw != null && !this.processDone) {
                try {
                    readStream(sw);
                } catch (IOException e) {
                    finish(sw, e);
                }
            } else {
                return;
            }
        }
    }

    public void done() {
        log.info("READING IS DONE");
        this.processDone = true;
    }

    public synchronized StreamWrapper retrieveResult(InputStream is) throws IOException {
        StreamWrapper sw;
        synchronized (this.streams) {
            sw = (StreamWrapper) this.results.get(is);
            boolean isDone = sw.isDone();
            if (!isDone) {
                readStream(sw);
            }
            finish(sw, null);
            this.results.remove(is);
        }
        return sw;
    }

    void finish(StreamWrapper sw, IOException ie) {
        synchronized (this.streams) {
            try {
                sw.getInputStream().close();
            } catch (IOException e) {
            }
            this.streams.remove(sw);
            sw.done();
        }
    }

    synchronized StreamWrapper getNextStream() {
        synchronized (this.streams) {
            if (this.streams.size() == 0) {
                return null;
            }
            if (this.position >= this.streams.size()) {
                this.position = 0;
                if (!this.working) {
                    try {
                        sleep(this.sleepInterval);
                        this.sleepInterval *= 2;
                    } catch (InterruptedException e) {
                        return null;
                    }
                } else {
                    this.working = false;
                }
            }
            Vector vector = this.streams;
            int i = this.position;
            this.position = i + 1;
            return (StreamWrapper) vector.get(i);
        }
    }

    public void addStream(InputStream is) {
        StreamWrapper sw = new StreamWrapper(is);
        synchronized (this.streams) {
            this.streams.add(sw);
            this.results.put(is, sw);
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        StreamReadingThread t = new StreamReadingThread();
        StringBuffer cmd = new StringBuffer("ssh  -a -x ");
        cmd.append("root@192.168.114.136 ");
        cmd.append(args[1]).append(" ");
        for (int i = 2; i < args.length; i++) {
            cmd.append(args[i]).append(" ");
        }
        System.out.println("Executing command:");
        System.out.println(cmd.toString());
        System.out.println();
        Process p = Runtime.getRuntime().exec(cmd.toString());
        t.addStream(p.getInputStream());
        t.addStream(p.getErrorStream());
        if (!t.isAlive()) {
            t.start();
        }
        FileInputStream is = new FileInputStream(args[0]);
        IOUtils.copy(is, p.getOutputStream());
        System.out.println("Copied InputStream to OutputStream;");
        p.getOutputStream().close();
        System.out.println("Closed OutputStream;");
        int exit = p.waitFor();
        System.out.println("exit = " + exit);
        InputStream ioo = p.getInputStream();
        System.out.println("main. after getting ioo");
        Collection output = t.retrieveResult(ioo).getResultAsCollection();
        System.out.println("main. after retrieve result.");
        System.out.println("OUTPUT:" + output);
        if (0 != exit) {
            String error = t.retrieveResult(p.getErrorStream()).getResult();
            System.err.println("ERROR: " + error);
        }
    }
}
