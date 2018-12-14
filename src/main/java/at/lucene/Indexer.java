package at.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;

public class Indexer {

  public static void main(String[] args) throws Exception {

    if (args.length != 2) {
      throw new IllegalArgumentException(
          "Usage: java " + Indexer.class.getName() + " <index dir> <data dir>");
    }

    final String indexDir = args[0];
    final String dataDir = args[1];

    final long start = System.currentTimeMillis();
    Indexer indexer = new Indexer(indexDir);

    int numIndexed;
    try {
      numIndexed = indexer.index(dataDir, new TextFilesFilter());
    } finally {
      indexer.close();
    }

    final long end = System.currentTimeMillis();
    final long time = end - start;

    System.out.println("Indexing " + numIndexed + " files took " + time + " milliseconds");
  }

  private final IndexWriter writer;

  public Indexer(final String indexDir) throws IOException {
    final Directory dir = FSDirectory.open(new File(indexDir).toPath());
    final Analyzer analyzer = new StandardAnalyzer();
    final IndexWriterConfig config = new IndexWriterConfig(analyzer);

    this.writer = new IndexWriter(dir, config);
  }

  public void close() throws IOException {
    writer.close();
  }

  public int index(final String dataDir, final FileFilter filter) throws Exception {
    final File[] files = new File(dataDir).listFiles();

    for (final File f : files) {
      if (!f.isDirectory() && !f.isHidden() && f.exists() && f.canRead() &&
          (filter == null || filter.accept(f))) {
        indexFile(f);
      }
    }

    return writer.numDocs();
  }

  private static class TextFilesFilter implements FileFilter {
    public boolean accept(final File path) {
      return path.getName().toLowerCase().endsWith(".txt");
    }
  }

  protected Document getDocument(final File file) throws Exception {
    final Document doc = new Document();

    doc.add(new TextField("contents", new FileReader(file)));
    doc.add(new StringField("filename", file.getName(), Field.Store.YES));
    doc.add(new StringField("fullpath", file.getCanonicalPath(), Field.Store.YES));

    return doc;
  }

  private void indexFile(final File file) throws Exception {
    System.out.println("Indexing " + file.getCanonicalPath());
    final Document doc = getDocument(file);
    writer.addDocument(doc);
  }
}
