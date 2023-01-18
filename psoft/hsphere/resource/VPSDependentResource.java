package psoft.hsphere.resource;

import java.util.List;

/* loaded from: hsphere.zip:psoft/hsphere/resource/VPSDependentResource.class */
public interface VPSDependentResource {
    boolean isPsInitialized() throws Exception;

    void setPsInitialized(boolean z) throws Exception;

    void parseConfig(List list) throws Exception;

    String getConfig() throws Exception;

    String getCronLogger() throws Exception;

    void dropCronLogger() throws Exception;
}
