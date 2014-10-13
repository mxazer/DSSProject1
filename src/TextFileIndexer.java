import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.lucene.index.FieldInfo;

/**
 * This terminal application creates an Apache Lucene index in a folder and adds
 * files into this index based on the input of the user.
 */
public class TextFileIndexer {
	public static final String TEXT_FILES_DIR = "./text-files";
	public static final String INDEX_DIR = "./tmp/index";
	public static String[] SENT_STRINGS = null;

	private static StandardAnalyzer analyzer = new StandardAnalyzer(
			Version.LUCENE_42);

	private IndexWriter writer;
	private ArrayList<File> queue = new ArrayList<File>();

	public static void indexFiles(String[] sentences) throws IOException {
		SENT_STRINGS = sentences;
		// set location to store file index
		TextFileIndexer indexer = null;
		try {
			FileUtils.deleteDirectory(new File(TextFileIndexer.INDEX_DIR));
		} catch (IOException e) {
			e.printStackTrace();
		}
		indexer = new TextFileIndexer(INDEX_DIR);
		// indexer.indexFileOrDirectory(TEXT_FILES_DIR);
		indexer.indexSentence(sentences);
		indexer.closeIndex();

	}

	// public static void searchIndex(IndexSearcher searcher,
	// TopScoreDocCollector collector, String query) throws IOException,
	// ParseException, InvalidTokenOffsetsException {
	// // highLighter();
	// Query q = null;
	// try {
	// q = new QueryParser(Version.LUCENE_42, "contents", analyzer)
	// .parse(query);
	// } catch (ParseException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// searcher.search(q, collector);
	// ScoreDoc[] hits = collector.topDocs().scoreDocs;
	//
	// // 4. display results
	// System.out.println("Found " + hits.length + " document hits.");
	// for (int i = 0; i < hits.length; ++i) {
	// int docId = hits[i].doc;
	// Document d = searcher.doc(docId);
	// System.out.println((i + 1) + ". " + d.get("path") + " score="
	// + hits[i].score);
	// }
	//
	// }

	public static void searchIndexForSentence(IndexSearcher searcher,
			TopScoreDocCollector collector, String query) throws IOException,
			 InvalidTokenOffsetsException {
		// highLighter();
		Query q = null;
		try {
			QueryParser parser = new QueryParser(Version.LUCENE_42, "word", analyzer);
					
			q = parser.parse(QueryParser.escape(query));
			
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		searcher.search(q, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;

		float maxScore = 0;
		int bestHit = -1;
		// 4. display results
		if(hits.length != 0){
		System.out.println("Found " + hits.length + " sentence hits.");
		for (int i = 0; i < hits.length; ++i) {
			int docId = hits[i].doc;
			float score = hits[i].score;
			System.out.println((i + 1) + ". " + docId + " score=" + score);
			if (score > maxScore) {
				maxScore = score;
				bestHit = i;
			}
		}
		// 5. display best result
		int docId = hits[bestHit].doc;
		Document d = searcher.doc(docId);
		System.out.println("Best sentence hit is " + SENT_STRINGS[docId]);
		}else {
			System.out.println("Sorry, I don’t understand your questions.");
		}
	}

	/**
	 * Constructor
	 * 
	 * @param indexDir
	 *            the name of the folder in which the index should be created
	 * @throws java.io.IOException
	 *             when exception creating index.
	 */
	TextFileIndexer(String indexDir) throws IOException {
		// the boolean true parameter means to create a new index everytime,
		// potentially overwriting any existing files there.
		FSDirectory dir = FSDirectory.open(new File(indexDir));

		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_42,
				analyzer);

		writer = new IndexWriter(dir, config);
	}

	/**
	 * Indexes a file or directory
	 * 
	 * @param fileName
	 *            the name of a text file or a folder we wish to add to the
	 *            index
	 * @throws java.io.IOException
	 *             when exception
	 */
	// public void indexFileOrDirectory(String fileName) throws IOException {
	// // ===================================================
	// // gets the list of files in a folder (if user has submitted
	// // the name of a folder) or gets a single file name (is user
	// // has submitted only the file name)
	// // ===================================================
	//
	// /*
	// addFiles(new File(fileName));
	//
	// int originalNumDocs = writer.numDocs();
	//
	// FieldType type = new FieldType();
	// type.setIndexed(true);
	// type.setIndexOptions(FieldInfo.IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
	// type.setStored(true);
	// type.setStoreTermVectors(true);
	// type.setTokenized(true);
	// type.setStoreTermVectorOffsets(true);
	// Document docTest = new Document();
	// docTest.add(new Field("content", "Lucene Highlighter rocks", type));//
	// with
	// // term
	// // vector
	// // enabled
	// docTest.add(new TextField("ncontent", "Lucene Highlighter rocks",
	// Field.Store.YES));
	// writer.addDocument(docTest);
	//
	// for (File f : queue) {
	// FileReader fr = null;
	// try {
	// Document doc = new Document();
	//
	// // ===================================================
	// // add contents of file
	// // ===================================================
	// fr = new FileReader(f);
	//
	// doc.add(new TextField("contents", fr));
	// doc.add(new StringField("path", f.getPath(), Field.Store.YES));
	// doc.add(new StringField("filename", f.getName(),
	// Field.Store.YES));
	//
	// writer.addDocument(doc);
	// System.out.println("Added: " + f);
	// } catch (Exception e) {
	// System.out.println("Could not add: " + f);
	// } finally {
	// fr.close();
	// }
	// }
	//
	// int newNumDocs = writer.numDocs();
	// System.out.println("");
	// System.out.println("************************");
	// System.out
	// .println((newNumDocs - originalNumDocs) + " documents added.");
	// System.out.println("************************");
	//
	// queue.clear();
	// */
	// }

	public void indexSentence(String[] sentences) throws IOException {
		// ===================================================
		// gets the list of files in a folder (if user has submitted
		// the name of a folder) or gets a single file name (is user
		// has submitted only the file name)
		// ===================================================

		int originalNumDocs = writer.numDocs();

		FieldType type = new FieldType();
		type.setIndexed(true);
		type.setIndexOptions(FieldInfo.IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
		type.setStored(true);
		type.setStoreTermVectors(true);
		type.setTokenized(true);
		type.setStoreTermVectorOffsets(true);

		for (int i = 0; i < sentences.length; i++) {
			Document docTest = new Document();
			docTest.add(new TextField("word", sentences[i], Field.Store.YES));
			docTest.add(new IntField("index", i, Field.Store.YES));
			// docTest.add(new TextField("nword", sentences[i],
			// Field.Store.YES));
			writer.addDocument(docTest);
			System.out.println("Added: " + sentences[i]);

		}

		int newNumDocs = writer.numDocs();
		System.out.println("");
		System.out.println("************************");
		System.out
				.println((newNumDocs - originalNumDocs) + " sentences added.");
		System.out.println("************************");

		queue.clear();
	}

	// private void addFiles(File file) {
	//
	// if (!file.exists()) {
	// System.out.println(file + " does not exist.");
	// }
	// if (file.isDirectory()) {
	// for (File f : file.listFiles()) {
	// addFiles(f);
	// }
	// } else {
	// String filename = file.getName().toLowerCase();
	// // ===================================================
	// // Only index text files
	// // ===================================================
	// if (filename.endsWith(".htm") || filename.endsWith(".html")
	// || filename.endsWith(".xml") || filename.endsWith(".txt")) {
	// queue.add(file);
	// } else {
	// System.out.println("Skipped " + filename);
	// }
	// }
	// }

	/**
	 * Close the index.
	 * 
	 * @throws java.io.IOException
	 *             when exception closing
	 */
	public void closeIndex() throws IOException {
		writer.close();
	}

	// public static void highLighter() throws IOException, ParseException,
	// InvalidTokenOffsetsException {
	// IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(
	// INDEX_DIR)));
	// Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_42);
	// IndexSearcher searcher = new IndexSearcher(reader);
	// QueryParser parser = new QueryParser(Version.LUCENE_42, "nword",
	// analyzer);
	// Query query = parser.parse("Mary");
	// TopDocs hits = searcher.search(query, reader.maxDoc());
	// System.out.println("highliter " + hits.totalHits);
	// SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter();
	// Highlighter highlighter = new Highlighter(htmlFormatter,
	// new QueryScorer(query));
	// highlighter.setMaxDocCharsToAnalyze(50000*50000);
	// for (int i = 0; i < hits.totalHits; i++) {
	// int id = hits.scoreDocs[i].doc;
	// ;
	// Document doc = searcher.doc(id);
	// String text = doc.get("nword");
	// TokenStream tokenStream = TokenSources.getAnyTokenStream(
	// searcher.getIndexReader(), id, "nword", analyzer);
	// TextFragment[] frag = highlighter.getBestTextFragments(tokenStream,
	// text, false, 4);
	// for (int j = 0; j < frag.length; j++) {
	// if ((frag[j] != null) && (frag[j].getScore() > 0)) {
	// System.out.println((frag[j].toString()));
	// }
	// }
	// // Term vector
	// text = doc.get("word");
	// tokenStream = TokenSources.getAnyTokenStream(
	// searcher.getIndexReader(), hits.scoreDocs[i].doc,
	// "word", analyzer);
	// frag = highlighter
	// .getBestTextFragments(tokenStream, text, false, 4);
	// for (int j = 0; j < frag.length; j++) {
	// if ((frag[j] != null) && (frag[j].getScore() > 0)) {
	// System.out.println((frag[j].toString()));
	// }
	// }
	// }
	// }
}
