package psoft.knowledgebase;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/knowledgebase/KBEntry.class */
public class KBEntry implements TemplateHashModel {

    /* renamed from: id */
    int f242id;
    int categoryId;
    String question;
    String answer;

    public KBEntry(int id, int categoryId, String question, String answer) {
        this.f242id = id;
        this.categoryId = categoryId;
        this.question = question;
        this.answer = answer;
    }

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) {
        if ("id".equals(key)) {
            return new TemplateString(key);
        }
        if ("q".equals(key)) {
            return new TemplateString(this.question);
        }
        if ("a".equals(key)) {
            return new TemplateString(this.answer);
        }
        if ("cat".equals(key)) {
            return new TemplateString(this.categoryId);
        }
        return null;
    }

    public int getId() {
        return this.f242id;
    }

    public String getAnswer() {
        return this.answer;
    }

    public String getQuestion() {
        return this.question;
    }

    public int getCategoryId() {
        return this.categoryId;
    }

    public Document getDocument() {
        Document d = new Document();
        d.add(Field.Keyword("id", Integer.toString(this.f242id)));
        d.add(Field.Keyword("cat", Integer.toString(this.categoryId)));
        d.add(Field.UnStored("q", this.question));
        d.add(Field.UnStored("a", this.answer));
        return d;
    }

    public void setCategoryId(int newCatId) {
        this.categoryId = newCatId;
    }
}
