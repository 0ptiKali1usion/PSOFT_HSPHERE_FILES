package psoft.hsphere.background;

import java.util.Hashtable;

/* loaded from: hsphere.zip:psoft/hsphere/background/Backgroundable.class */
public interface Backgroundable {
    void startJob() throws Exception;

    void interruptJob() throws Exception;

    int getStatus() throws Exception;

    void suspendJob() throws Exception;

    void resumeJob() throws Exception;

    int setProgress(int i, int i2, int i3) throws Exception;

    int addProgress(int i) throws Exception;

    int getProgress();

    long getId();

    String getGroupId();

    void setGroupId(String str);

    boolean isAliveJob();

    boolean isRunning();

    boolean isSuspended();

    String getName();

    void setName(String str);

    void setPriority(int i);

    void setParams(Hashtable hashtable) throws Exception;
}
