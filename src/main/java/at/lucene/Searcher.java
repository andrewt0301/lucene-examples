package at.lucene;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;

public class Searcher {

  public static void main(String[] args) throws IllegalArgumentException, IOException, ParseException {

    if (args.length != 2) {
      throw new IllegalArgumentException(
          "Usage: java " + Searcher.class.getName() + " <index dir> <query>");
    }

    final String indexDir = args[0];
    final String q = args[1];

    search(indexDir, q);
  }

  public static void search(
      final String indexDir,
      final String q) throws IOException, ParseException {

    final Directory dir = FSDirectory.open(new File(indexDir).toPath());
    final IndexReader indexReader = DirectoryReader.open(dir);
    final IndexSearcher searcher = new IndexSearcher(indexReader);

    final QueryParser parser = new QueryParser("contents", new StandardAnalyzer());
    final Query query = parser.parse(q);

    final long start = System.currentTimeMillis();
    TopDocs hits = searcher.search(query, 10);
    final long end = System.currentTimeMillis();

    final long time = end - start;
    System.out.println(
        "Found " + hits.totalHits + " document(s) (in " + time +
        " milliseconds) that matched query '" +  q + "':");

    for(ScoreDoc scoreDoc : hits.scoreDocs) {
      Document doc = searcher.doc(scoreDoc.doc);
      System.out.println(doc.get("fullpath"));
    }

    indexReader.close();
  }

}
