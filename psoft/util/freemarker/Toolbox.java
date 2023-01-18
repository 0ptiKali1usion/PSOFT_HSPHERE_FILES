package psoft.util.freemarker;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import java.util.List;
import psoft.spellcheker.SpellCheker;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/util/freemarker/Toolbox.class */
public class Toolbox implements TemplateHashModel {
    public static final TemplateModel toolbox = new Toolbox();

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) {
        if (key.equals("compose")) {
            return StringComposer.compose;
        }
        if (key.equals("html_encode")) {
            return HTMLEncodeMethod.encode;
        }
        if (key.equals("csv_escape")) {
            return CSVEscapeMethod.escape;
        }
        if (key.equals("url_escape")) {
            return URLEscapeMethod.escape;
        }
        if ("includeURL".equals(key)) {
            return URLInclude.includeURL;
        }
        if (key.equals("js_encode")) {
            return JavaScriptEncoderMethod.encode;
        }
        if ("strlit_encode".equals(key)) {
            return StringLiteralEncodeMethod.encode;
        }
        if (key.equals("counter")) {
            return new SimpleCounter();
        }
        if (key.equals("startsWith")) {
            return StartsWithMethod.start;
        }
        if (key.equals("endsWith")) {
            return EndsWithMethod.start;
        }
        if (key.equals("consists")) {
            return ConsistsMethod.consists;
        }
        if (key.equals("substring")) {
            return SubstringMethod.substring;
        }
        if (key.equals("tokenizer")) {
            return TokenizerMethod.tokenizer;
        }
        if (key.equals("add")) {
            return MathMethod.add;
        }
        if (key.equals("sub")) {
            return MathMethod.sub;
        }
        if (key.equals("mul")) {
            return MathMethod.mul;
        }
        if (key.equals("div")) {
            return MathMethod.div;
        }
        if (key.equals("mod")) {
            return MathMethod.mod;
        }
        if (key.equals("gt")) {
            return MathMethod.f268gt;
        }
        if (key.equals("lt")) {
            return MathMethod.f269lt;
        }
        if (key.equals("ge")) {
            return MathMethod.f270ge;
        }
        if (key.equals("le")) {
            return MathMethod.f271le;
        }
        if (key.equals("max")) {
            return MathMethod.max;
        }
        if (key.equals("min")) {
            return MathMethod.min;
        }
        if (key.equals("eq")) {
            return MathMethod.f272eq;
        }
        if (key.equals("ceil")) {
            return MathMethod.ceil;
        }
        if (key.equals("percent")) {
            return MathMethod.percent;
        }
        if (key.equals("countries")) {
            return CountryList.list;
        }
        if (key.equals("currencies")) {
            return CurrencyList.list;
        }
        if (key.equals("dateman")) {
            return DateManager.dateman;
        }
        if (key.equals("currency")) {
            return LocaleMethod.currency;
        }
        if (key.equals("states")) {
            return StatesList.list;
        }
        if (key.equals("statesCanada")) {
            return StatesCanadaList.list;
        }
        if (key.equals("new_list")) {
            return new TemplateList();
        }
        if (key.equals("new_hash")) {
            return new TemplateHash();
        }
        if (key.equals("shrink_string")) {
            return StringShrink.shrink;
        }
        if (key.equals("spellcheck")) {
            return new SpellCheker();
        }
        if (key.equals("date")) {
            return new TemplateString(TimeUtils.getDate().toString());
        }
        return null;
    }

    public static String getArg(List list, int index) {
        if (index < 0 || index >= list.size()) {
            return null;
        }
        Object o = list.get(index);
        if (o instanceof String) {
            return (String) o;
        }
        return null;
    }
}
