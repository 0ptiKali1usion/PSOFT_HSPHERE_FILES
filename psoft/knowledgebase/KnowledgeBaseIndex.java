package psoft.knowledgebase;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.log4j.Category;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.FSDirectory;

/* loaded from: hsphere.zip:psoft/knowledgebase/KnowledgeBaseIndex.class */
public class KnowledgeBaseIndex {
    private static Category log = Category.getInstance(KnowledgeBaseIndex.class.getName());
    private static final Analyzer analyzer = new StopAnalyzer();
    private static final char[] spSymb = {'+', '-', '!', '(', ')', '{', '}', '[', ']', '^', '\"', '~', '*', '?', ':', '\\'};
    private FSDirectory fsDir;

    static {
        Arrays.sort(spSymb);
    }

    public IndexWriter getIndexWriter() throws IOException {
        return new IndexWriter(this.fsDir, analyzer, false);
    }

    private IndexReader getIndexReader() throws IOException {
        return IndexReader.open(this.fsDir);
    }

    private IndexSearcher getIndexSearcher() throws IOException {
        return new IndexSearcher(this.fsDir);
    }

    private synchronized void remove(Term t) throws IOException {
        IndexReader reader = null;
        try {
            reader = getIndexReader();
            reader.delete(t);
            if (reader != null) {
                reader.close();
            }
        } catch (Throwable th) {
            if (reader != null) {
                reader.close();
            }
            throw th;
        }
    }

    public static IndexWriter init(File root) throws IOException {
        IndexWriter writer = new IndexWriter(FSDirectory.getDirectory(root, true), analyzer, true);
        return writer;
    }

    public KnowledgeBaseIndex(File root) throws IOException {
        this.fsDir = FSDirectory.getDirectory(root, false);
    }

    public synchronized void add(Document d) throws IOException {
        IndexWriter writer = null;
        try {
            writer = getIndexWriter();
            writer.addDocument(d);
            if (writer != null) {
                writer.close();
            }
        } catch (Throwable th) {
            if (writer != null) {
                writer.close();
            }
            throw th;
        }
    }

    public void removeCategory(int catId) throws IOException {
        remove(new Term("cat", Integer.toString(catId)));
    }

    public void remove(int id) throws IOException {
        remove(new Term("id", Integer.toString(id)));
    }

    public void update(int id, Document d) throws IOException {
        remove(id);
        add(d);
    }

    public List search(String queryStr) throws Exception {
        return search(queryStr, Integer.MAX_VALUE);
    }

    public List search(String queryStr, int max) throws Exception {
        return search(queryStr, max, 0);
    }

    public List search(String queryStr, int max, int cat) {
        String queryStr2;
        List list = new ArrayList();
        if (queryStr == null || queryStr.trim().length() < 3) {
            return list;
        }
        try {
            String queryStr3 = encodeQuery(queryStr);
            if (cat != 0) {
                String strCat = String.valueOf(cat) + "~";
                queryStr2 = "(cat:" + strCat + ") AND (q:\"" + queryStr3 + "\"^2 OR a:\"" + queryStr3 + "\")";
            } else {
                queryStr2 = "q:(" + queryStr3 + ")^2 OR a:(" + queryStr3 + ")";
            }
            Query query = QueryParser.parse(queryStr2, "q", analyzer);
            IndexSearcher indexSearcher = null;
            try {
                indexSearcher = getIndexSearcher();
                Hits hits = indexSearcher.search(query);
                for (int i = 0; i < hits.length() && i < max; i++) {
                    Document doc = hits.doc(i);
                    list.add(doc.get("id"));
                }
                if (indexSearcher != null) {
                    indexSearcher.close();
                }
                return list;
            } catch (Exception e) {
                log.info("--SEARCH QUERY: [" + queryStr2 + "]");
                log.warn("Problem searching", e);
                if (indexSearcher != null) {
                    indexSearcher.close();
                }
                return list;
            }
        } catch (Exception e2) {
            return list;
        }
    }

    public synchronized void optimize() throws IOException {
        IndexWriter writer = null;
        try {
            writer = getIndexWriter();
            writer.optimize();
            if (writer != null) {
                writer.close();
            }
        } catch (Throwable th) {
            if (writer != null) {
                writer.close();
            }
            throw th;
        }
    }

    public String rmZero(String val) {
        if (val == null) {
            return "null";
        }
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < val.length(); i++) {
            if (val.charAt(i) != 0) {
                buffer.append(val.charAt(i));
            }
        }
        return buffer.toString();
    }

    public String encodeQuery(String query) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < query.length(); i++) {
            char curC = query.charAt(i);
            if (Arrays.binarySearch(spSymb, curC) >= 0) {
                buffer.append('\\');
            }
            buffer.append(curC);
        }
        return buffer.toString();
    }
}
