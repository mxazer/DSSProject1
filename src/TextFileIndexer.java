import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.FieldInfo;
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
					System.out.println((i + 1) + ". " + docId + " score="
							+ score);
					results.add(SENT_STRINGS[docId]);
					if(i==2)
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
		if (query.indexOf("Who") != -1) {
			return 1;
		} else if (query.indexOf("What") != -1) {
			return 2;
		} else if (query.indexOf("When") != -1) {
			return 3;
		} else {
			return -1;
		}

	}

}
