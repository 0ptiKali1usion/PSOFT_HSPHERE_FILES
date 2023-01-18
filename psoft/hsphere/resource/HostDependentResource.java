package psoft.hsphere.resource;

/* loaded from: hsphere.zip:psoft/hsphere/resource/HostDependentResource.class */
public interface HostDependentResource {
    boolean canBeMovedTo(long j) throws Exception;

    void physicalCreate(long j) throws Exception;

    void physicalDelete(long j) throws Exception;

    void setHostId(long j) throws Exception;

    long getHostId() throws Exception;
}
