package psoft.hsphere.fmacl;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.resource.BackupTask;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/fmacl/BackupTaskAccessor.class */
public class BackupTaskAccessor implements TemplateHashModel {
    public static final TemplateString STATUS_OK = new TemplateString("OK");

    public TemplateModel get(String key) throws TemplateModelException {
        if ("status".equals(key)) {
            return STATUS_OK;
        }
        return AccessTemplateMethodWrapper.getMethod(this, key);
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    public TemplateHashModel FM_cancelBackupTask(long taskId) throws Exception {
        BackupTask bt = BackupTask.get(taskId);
        bt.cancelTask();
        return this;
    }

    public TemplateHashModel FM_completeBackupTask(long taskId) throws Exception {
        BackupTask bt = BackupTask.get(taskId);
        bt.completeTask();
        return this;
    }
}
