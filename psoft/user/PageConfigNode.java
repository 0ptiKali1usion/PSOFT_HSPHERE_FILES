package psoft.user;

import java.util.StringTokenizer;
import java.util.Vector;

/* loaded from: hsphere.zip:psoft/user/PageConfigNode.class */
public class PageConfigNode {
    protected String pageName;
    protected String pageTemplate;
    protected String nextPage;
    protected Vector fields = new Vector();

    public PageConfigNode(String name, String template, String next, String fields) {
        this.pageName = name;
        this.pageTemplate = template;
        this.nextPage = next;
        StringTokenizer st = new StringTokenizer(fields, " ");
        while (st.hasMoreTokens()) {
            this.fields.add(st.nextToken());
        }
    }

    public String getName() {
        return this.pageName;
    }

    public String getTemplate() {
        return this.pageTemplate;
    }

    public String getNext() {
        return this.nextPage;
    }

    public Vector getFields() {
        return this.fields;
    }
}
