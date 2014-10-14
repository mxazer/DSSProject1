import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;

import opennlp.tools.cmdline.postag.POSModelLoader;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.tokenize.WhitespaceTokenizer;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.misc.HighFreqTerms;
import org.apache.lucene.misc.TermStats;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.store.FSDirectory;

public class Read {

	public static final String OPENNLP_DIR = "./opennlp-models/";
	static String sentences[] = null;

	public static void main(String[] args) throws Exception {
		// this method indexes the files, runs once only at startup
		try {
			//read the files and store an array of sentences using sentenceDetect
			sentences = sentenceDetect(HelperMethods.readTextFiles());
		} catch (InvalidFormatException e) {
			e.printStackTrace();
		}		
		ArrayList<String> results = new ArrayList<String>();
		String inputQuestion = null;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {//index the sentences using Lucene
			TextFileIndexer.indexSentencesAndDocuments(sentences);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		do {
			try {
				displayMenu();
				inputQuestion = br.readLine();
				if (inputQuestion.equalsIgnoreCase("quit")) {//exit option
					break;
				}
				int questionType = HelperMethods
						.checkQuestionValidity(inputQuestion);
				// user asked for topic
				if (questionType == 4) {
					getTopics();
				}
				// user asked for db
				else if (questionType == 5) {
					System.out.println("\nKnowlegebase:");
					getDb();
				} else if (questionType == -1) {// a question type was not
												// identified
					System.out
							.println("\nSorry, I don’t understand your questions.");
				} else if (questionType == 6) {// typed "search Y"
					inputQuestion = inputQuestion.substring(7);
					searchIndexedFiles(inputQuestion,
							TextFileIndexer.INDEX_DIR_DOCS);
				} else {//begins with "Who", "What", or "When"
					try {
						inputQuestion = HelperMethods
								.removeStopWords(inputQuestion);
						
						results = searchIndexedFiles(inputQuestion,//search the sentence index
								TextFileIndexer.INDEX_DIR);
						
						//if a result is found, the answers are sent to be processed
						if (results.size() != 0)
							System.out.println("\nANSWER: "
									+ analyzeResults(inputQuestion, results,
											questionType));
					} catch (ParseException e) {
						e.printStackTrace();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} while (!inputQuestion.equalsIgnoreCase("quit"));

		try {
			//delete indexed files on close
			FileUtils.deleteDirectory(new File(TextFileIndexer.INDEX_DIR));
			FileUtils.deleteDirectory(new File(TextFileIndexer.INDEX_DIR_DOCS));
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static void displayMenu() {
		System.out
				.println("\nWelcome to DSS Project 1 Question Answering System.\n");
		System.out
				.println("________________________________________MENU________________________________________\n");
		System.out
				.println("Type in a question in english to get an answer from the system.");
		System.out
				.println("Type 'topic' to see the top 5 topics discussed in the documents.");
		System.out
				.println("Type 'db' to see the storage format used by the system.");
		System.out
				.println("Type 'search Y' to query term 'Y' and retrieve document names containing Y in order of relevancy.");
		System.out.println("Type 'quit' to exit");
		System.out.print("Q:");
	}

	//takes the stopword-less query and directory to search and searches the specified directory indexed files for
	//any results that are returned as an ArrayList
	private static ArrayList<String> searchIndexedFiles(String query,
			String indexDirectory) throws ParseException,
			InvalidTokenOffsetsException {
		IndexReader reader = null;
		ArrayList<String> results = new ArrayList<String>();
		try {
			reader = DirectoryReader.open(FSDirectory.open(new File(
					indexDirectory)));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		IndexSearcher searcher = new IndexSearcher(reader);
		TopScoreDocCollector collector = TopScoreDocCollector.create(6, true);
		try {
			if (indexDirectory.equals(TextFileIndexer.INDEX_DIR))//sentences
				results = TextFileIndexer.searchIndexForSentence(searcher,
						collector, query);
			if (indexDirectory.equals(TextFileIndexer.INDEX_DIR_DOCS))//documents
				TextFileIndexer.searchIndexForDocuments(searcher, collector,
						query);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return results;
	}

	//get the top 5 most frequently found terms 
	private static ArrayList<String> getTopics() throws Exception {
		IndexReader reader = null;

		// read the indexed files
		try {
			reader = DirectoryReader.open(FSDirectory.open(new File(
					TextFileIndexer.INDEX_DIR)));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		TermStats[] topFreqWords = null;
		try {
			//get the top 5 most frequent terms indexed 
			Comparator<TermStats> c = new HighFreqTerms.TotalTermFreqComparator();
			topFreqWords = HighFreqTerms.getHighFreqTerms(reader, 5, "word", c);
		} catch (IOException e) {
			e.printStackTrace();
		}
		ArrayList<String> results = new ArrayList<String>();
		System.out.println("\nTop Topics in Descending Order:");
		for (int i = 0; i < topFreqWords.length; i++) {
			System.out.println(i + 1 + " "
					+ topFreqWords[i].termtext.utf8ToString());//gets the term text and displays it as a String
		}
		return results;
	}

	//user typed "db"
	//returns a list of all the terms as stored in the indexed files
	private static void getDb() throws IOException {
		IndexReader reader = null;

		// read the indexed files
		try {
			reader = DirectoryReader.open(FSDirectory.open(new File(
					TextFileIndexer.INDEX_DIR)));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		Fields fields = MultiFields.getFields(reader);
		for (String field : fields) {
			Terms terms = fields.terms(field);
			TermsEnum termsEnum = terms.iterator(null);
			int count = 0;
			while (termsEnum.next() != null) {
				count++;
				System.out.println(termsEnum.term().utf8ToString());
			}
			System.out.println(count);
		}
	}

	//OpenNLP sentenceDetect
	public static String[] sentenceDetect(String stringToSentenceSplit)
			throws InvalidFormatException, IOException {

		// always start with a model, a model is learned from training data
		InputStream is = new FileInputStream(OPENNLP_DIR + "en-sent.bin");
		SentenceModel model = new SentenceModel(is);
		SentenceDetectorME sdetector = new SentenceDetectorME(model);

		String sentences[] = sdetector.sentDetect(stringToSentenceSplit);

		is.close();
		return sentences;
	}

	//OpenNLP tokenizer
	public static Tokenizer _tokenizer = null;

	// Method for tokenizing sentence strings
	public static String[] tokenizeString(String stringToTokenize)
			throws InvalidFormatException, IOException {
		InputStream is = new FileInputStream(OPENNLP_DIR + "en-token.bin");

		TokenizerModel model = new TokenizerModel(is);

		_tokenizer = new TokenizerME(model);

		String tokens[] = _tokenizer.tokenize(stringToTokenize);

		is.close();
		return tokens;
	}

	// receives a string and outputs an array of POS Tags
	public static String[] POSTag(String stringToPOSTag) throws IOException {
		POSModel model = new POSModelLoader().load(new File(OPENNLP_DIR
				+ "en-pos-maxent.bin"));
		POSTaggerME tagger = new POSTaggerME(model);

		ObjectStream<String> lineStream = new PlainTextByLineStream(
				new StringReader(stringToPOSTag));

		String line;
		if ((line = lineStream.read()) != null) {
			String whitespaceTokenizerLine[] = WhitespaceTokenizer.INSTANCE
					.tokenize(line);
			String[] tags = tagger.tag(whitespaceTokenizerLine);

			return tags;
		} else
			return null;
	}

	// main algorithm for determining answer
	public static String analyzeResults(String query,
			ArrayList<String> answers, int questionType)
			throws InvalidFormatException, IOException {

		String[] qtokens = tokenizeString(query);
		String[] qtags = POSTag(query);

		float bestCount = 0;
		int bestIndex = -1;
		
		if (questionType != -1 && qtags.length > 0 && qtokens.length > 0
				&& answers.size() != 0) {
			for (int j = 0; j < answers.size(); j++) {
				float count = 0;
				String[] answerTags = POSTag(HelperMethods.removeStopWords(answers
						.get(j)));
				for (int i = 0; i < answerTags.length; i++) {
					// who question
					if (questionType == 1) {
						if (answerTags[i].equals("NNP")) {
							count++;
						}
						// what question
					} else if (questionType == 2) {
						if ((answerTags[i].equals("NNP") || answerTags[i]
								.equals("NN"))) {
							count++;
						}
						// when question
					} else if (questionType == 3) {
						if (answerTags[i].equals("CD")) {
							count++;
						}
					}
				}
				// gets answer with most relevant tags
				if (count > bestCount) {
					bestCount = count;
					bestIndex = j;
				}
			}

		}
		if (bestIndex == -1)
			return "\nSorry I couldn't find that information";
		return answers.get(bestIndex);
	}

}