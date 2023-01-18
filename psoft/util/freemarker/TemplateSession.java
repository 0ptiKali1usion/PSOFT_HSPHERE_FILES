package psoft.util.freemarker;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import java.util.List;

/* loaded from: hsphere.zip:psoft/util/freemarker/TemplateSession.class */
public class TemplateSession implements TemplateHashModel {
    public TemplateList msg = new TemplateList();
    public MessageAdder madd = new MessageAdder();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: hsphere.zip:psoft/util/freemarker/TemplateSession$MessageAdder.class */
    public class MessageAdder implements TemplateMethodModel {
        MessageAdder() {
            TemplateSession.this = r4;
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List l) {
            for (Object obj : l) {
                TemplateSession.this.msg.add(obj);
            }
            return null;
        }
    }

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) {
        if (key.equals("addMessage")) {
            return this.madd;
        }
        if (key.equals("msg")) {
            return this.msg;
        }
        return null;
    }

    public void addMessage(String key) {
        this.msg.add(key);
    }
}
