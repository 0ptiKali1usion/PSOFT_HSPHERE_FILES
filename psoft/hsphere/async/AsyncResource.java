package psoft.hsphere.async;

/* loaded from: hsphere.zip:psoft/hsphere/async/AsyncResource.class */
public interface AsyncResource {
    boolean isAsyncComplete() throws AsyncDeclinedException, AsyncResourceException;

    String getAsyncDescription();

    boolean isAsyncAutoRemove();

    int getAsyncTimeout();

    int getAsyncInterval();

    void asyncDelete() throws Exception;
}
