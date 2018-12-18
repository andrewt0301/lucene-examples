package lia.indexing;

/**
 * Copyright Manning Publications Co.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific lan      
*/

import lia.common.TestUtil;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.index.Term;

import java.io.IOException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// From chapter 2
public class IndexingTest {

  protected final String[]       ids = {"1", "2"};
  protected final String[] unindexed = {"Netherlands", "Italy"};
  protected final String[]  unstored = {"Amsterdam has lots of bridges", "Venice has lots of canals"};
  protected final String[]      text = {"Amsterdam", "Venice"};

  private Directory directory;

  @Before
  public void setUp() throws Exception {
    directory = new RAMDirectory();

    IndexWriter writer = getWriter();
    for (int i = 0; i < ids.length; i++) {
      Document doc = new Document();

      doc.add(new StringField("id", ids[i], Field.Store.YES));
      doc.add(new StoredField("country", unindexed[i]));
      doc.add(new TextField("contents", unstored[i], Field.Store.NO));
      doc.add(new TextField("city", text[i], Field.Store.YES));

      writer.addDocument(doc);
    }

    writer.close();
  }

  private IndexWriter getWriter() throws IOException {
    Analyzer analyzer = new WhitespaceAnalyzer();
    IndexWriterConfig config = new IndexWriterConfig(analyzer);
    return new IndexWriter(directory, config);
  }

  protected long getHitCount(String fieldName, String searchString) throws IOException {
    IndexReader reader = DirectoryReader.open(directory);
    IndexSearcher searcher = new IndexSearcher(reader);

    Term t = new Term(fieldName, searchString);
    Query query = new TermQuery(t);
    long hitCount = TestUtil.hitCount(searcher, query);

    reader.close();
    return hitCount;
  }

  @Test
  public void testIndexWriter() throws IOException {
    IndexWriter writer = getWriter();
    assertEquals(ids.length, writer.numDocs());
    writer.close();
  }

  @Test
  public void testIndexReader() throws IOException {
    IndexReader reader = DirectoryReader.open(directory);
    assertEquals(ids.length, reader.maxDoc());
    assertEquals(ids.length, reader.numDocs());
    reader.close();
  }

  /*
    #1 Run before every test
    #2 Create IndexWriter
    #3 Add documents
    #4 Create new searcher
    #5 Build simple single-term query
    #6 Get number of hits
    #7 Verify writer document count
    #8 Verify reader document count
  */
  @Test
  public void testDeleteBeforeOptimize() throws IOException {
    IndexWriter writer = getWriter();
    assertEquals(2, writer.numDocs());
    writer.deleteDocuments(new Term("id", "1"));
    writer.commit();
    assertTrue(writer.hasDeletions());
    assertEquals(2, writer.maxDoc());
    assertEquals(1, writer.numDocs());
    writer.close();
  }

  /*
    #A Create new document with "Haag" in city field
    #B Replace original document with new version
    #C Verify old document is gone
    #D Verify new document is indexed
  */
  @Test
  public void testUpdate() throws IOException {

    assertEquals(1, getHitCount("city", "Amsterdam"));

    IndexWriter writer = getWriter();

    Document doc = new Document();

    doc.add(new StringField("id", "1", Field.Store.YES));
    doc.add(new StoredField("country", "Netherlands"));
    doc.add(new TextField("contents", "Den Haag has a lot of museums", Field.Store.NO));
    doc.add(new TextField("city", "Den Haag", Field.Store.YES));

    writer.updateDocument(new Term("id", "1"), doc);
    writer.close();

    assertEquals(0, getHitCount("city", "Amsterdam"));
    assertEquals(1, getHitCount("city", "Haag"));
  }

}
