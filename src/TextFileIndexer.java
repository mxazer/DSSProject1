import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/**
 * This terminal application creates an Apache Lucene index in a folder and adds
 * files into this index based on the input of the user.
 */
public class TextFileIndexer {
	public static final String TEXT_FILES_DIR = "./text-files";
	public static final String INDEX_DIR = "./tmp/index";
	public static String[] SENT_STRINGS = null;
	public static final String INDEX_DIR_DOCS = "./tmp/index/docs";

	private static StandardAnalyzer analyzer = new StandardAnalyzer(
			Version.LUCENE_42);

	private IndexWriter writer;
	private ArrayList<File> queue = new ArrayList<File>();

	public static void indexSentences(String[] sentences) throws IOException {
		SENT_STRINGS = sentences;
		TextFileIndexer indexer = null;
		try {
			FileUtils.deleteDirectory(new File(TextFileIndexer.INDEX_DIR));
			FileUtils.deleteDirectory(new File(TextFileIndexer.INDEX_DIR_DOCS));
		} catch (IOException e) {
			e.printStackTrace();
		}
		indexer = new TextFileIndexer(INDEX_DIR);
		indexer.indexSentence(sentences);
		indexer.closeIndex();
		indexer = new TextFileIndexer(INDEX_DIR_DOCS);
		indexer.indexFileOrDirectory(TEXT_FILES_DIR);
		indexer.closeIndex();

	}

	public static ArrayList<String> searchIndexForSentence(
			IndexSearcher searcher, TopScoreDocCollector collector, String query)
			throws IOException, InvalidTokenOffsetsException {
		// highLighter();
		Query q = null;
		ArrayList<String> results = new ArrayList<String>();
		try {
			QueryParser parser = new QueryParser(Version.LUCENE_42, "word",
					analyzer);
			q = parser.parse(QueryParser.escape(query));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		searcher.search(q, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;

		// collect results
		if (hits.length != 0) {
			System.out.println("Found " + hits.length + " sentence hits.");
			for (int i = 0; i < hits.length; ++i) {
				int docId = hits[i].doc;
				float score = hits[i].score;
				System.out.println( docId +". " + " score=" + score);
				results.add(SENT_STRINGS[docId]);
				if (i == 2)
					break;
			}
		} else {
			System.out.println("Sorry, I don’t have that information.");
		}
		return results;
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

	public void indexSentence(String[] sentences) throws IOException {
		// ===================================================
		// gets the list of files in a folder (if user has submitted
		// the name of a folder) or gets a single file name (is user
		// has submitted only the file name)
		// ===================================================

		int originalNumDocs = writer.numDocs();

		for (int i = 0; i < sentences.length; i++) {
			Document docTest = new Document();
			docTest.add(new TextField("word", sentences[i], Field.Store.YES));
			docTest.add(new IntField("index", i, Field.Store.YES));
			writer.addDocument(docTest);
//			System.out.println("Added: " + sentences[i]);

		}

		int newNumDocs = writer.numDocs();
		System.out.println("");
		System.out.println("************************");
		System.out
				.println((newNumDocs - originalNumDocs) + " sentences added.");
		System.out.println("************************");

		queue.clear();
	}

	/**
	 * Close the index.
	 * 
	 * @throws java.io.IOException
	 *             when exception closing
	 */
	public void closeIndex() throws IOException {
		writer.close();
	}

	public static int checkQuestionValidity(String query) {
		if (query.indexOf("Who") == 0) {
			return 1;
		} else if (query.indexOf("What") == 0) {
			return 2;
		} else if (query.indexOf("When") == 0) {
			return 3;
		} else if (query.indexOf("topic") == 0) {
			return 4;
		} else if (query.indexOf("db") == 0) {
			return 5;
		} else if (query.indexOf("search") == 0){
			return 6;
		} else {
			return -1;
		}
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
public void indexFileOrDirectory(String fileName) throws IOException {
	// ===================================================
	// gets the list of files in a folder (if user has submitted
	// the name of a folder) or gets a single file name (is user
	// has submitted only the file name)
	// ===================================================
	addFiles(new File(fileName));

	int originalNumDocs = writer.numDocs();
	for (File f : queue) {
		FileReader fr = null;
		try {
			Document doc = new Document();

			// ===================================================
			// add contents of file
			// ===================================================
			fr = new FileReader(f);
			doc.add(new TextField("contents", fr));
			doc.add(new StringField("path", f.getPath(), Field.Store.YES));
			doc.add(new StringField("filename", f.getName(),
					Field.Store.YES));

			writer.addDocument(doc);
			System.out.println("Added: " + f);
		} catch (Exception e) {
			System.out.println("Could not add: " + f);
		} finally {
			fr.close();
		}
	}

	int newNumDocs = writer.numDocs();
	System.out.println("");
	System.out.println("************************");
	System.out
			.println((newNumDocs - originalNumDocs) + " documents added.");
	System.out.println("************************");

	queue.clear();
}

private void addFiles(File file) {

	if (!file.exists()) {
		System.out.println(file + " does not exist.");
	}
	if (file.isDirectory()) {
		for (File f : file.listFiles()) {
			addFiles(f);
		}
	} else {
		String filename = file.getName().toLowerCase();
		// ===================================================
		// Only index text files
		// ===================================================
		if (filename.endsWith(".htm") || filename.endsWith(".html")
				|| filename.endsWith(".xml") || filename.endsWith(".txt")) {
			queue.add(file);
		} else {
			System.out.println("Skipped " + filename);
		}
	}
}

	public static void searchIndexForDocuments(IndexSearcher searcher,
		TopScoreDocCollector collector, String query) throws IOException {
			Query q = null;
			try {
				q = new QueryParser(Version.LUCENE_42, "contents",
						analyzer).parse(query);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			searcher.search(q, collector);
			ScoreDoc[] hits = collector.topDocs().scoreDocs;

			// 4. display results
			System.out.println("Found " + hits.length + " hits.");
			for (int i = 0; i < hits.length; ++i) {
				int docId = hits[i].doc;
				Document d = searcher.doc(docId);
				System.out.println((i + 1) + ". " + d.get("path")
						+ " score=" + hits[i].score);
			}
	}
}
