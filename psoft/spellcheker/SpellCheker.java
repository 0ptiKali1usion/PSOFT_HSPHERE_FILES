package psoft.spellcheker;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplatePair;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/spellcheker/SpellCheker.class */
public class SpellCheker implements TemplateMethodModel {
    protected List totalResult;
    protected int oldOffset;
    protected int totalOffset;

    /* renamed from: p */
    Process f248p;
    BufferedReader reader;
    BufferedWriter writer;

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel exec(List l) {
        try {
            List l2 = HTMLEncoder.decode(l);
            init();
            TemplateList templateList = new TemplateList(spellCheck((String) l2.get(0)));
            finish();
            return templateList;
        } catch (IOException e) {
            finish();
            return null;
        } catch (Throwable th) {
            finish();
            throw th;
        }
    }

    public List spellCheck(String str) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(str));
        this.totalResult = new ArrayList();
        this.totalOffset = 0;
        while (true) {
            String line = reader.readLine();
            if (line != null) {
                this.oldOffset = 0;
                checkLine(line);
                this.totalOffset = this.totalOffset + line.length() + 2;
            } else {
                return this.totalResult;
            }
        }
    }

    public void processResult(Result result, String line) {
        switch (result.getType()) {
            case 1:
            case 2:
            default:
                return;
            case 3:
            case 4:
                int wordOffset = result.getOffset() - 1;
                if (wordOffset > this.oldOffset) {
                    this.totalResult.add(new TemplatePair("1", line.substring(this.oldOffset, wordOffset)));
                }
                this.totalResult.add(result);
                this.oldOffset = wordOffset + result.getOriginalWord().length();
                result.setOffset(this.totalOffset + wordOffset);
                return;
        }
    }

    public static void main(String[] args) throws IOException {
        SpellCheker spell = new SpellCheker();
        Iterator i = spell.spellCheck("Hello to thes wosdfsdfsdnderful world\nHello and godbye").iterator();
        while (i.hasNext()) {
            System.out.println("-->" + i.next());
        }
    }

    public void init() throws IOException {
        this.f248p = Runtime.getRuntime().exec("aspell pipe");
        this.reader = new BufferedReader(new InputStreamReader(this.f248p.getInputStream()));
        this.writer = new BufferedWriter(new OutputStreamWriter(this.f248p.getOutputStream()));
        this.reader.readLine();
    }

    public void checkLine(String line) throws IOException {
        this.writer.write(94);
        this.writer.write(line);
        this.writer.newLine();
        this.writer.flush();
        String readLine = this.reader.readLine();
        while (true) {
            String response = readLine;
            if (response == null || response.length() <= 0) {
                break;
            }
            Result result = new Result(response);
            processResult(result, line);
            readLine = this.reader.readLine();
        }
        if (line.length() > this.oldOffset) {
            System.err.println("-->" + this.oldOffset);
            this.totalResult.add(new TemplatePair("1", line.substring(this.oldOffset)));
        }
        this.totalResult.add(new TemplatePair("1", "\n"));
    }

    public void finish() {
        this.f248p.destroy();
    }
}
