import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;

import opennlp.tools.cmdline.postag.POSModelLoader;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
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
import opennlp.tools.util.Span;

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

	static String sentences[] = null;
	private static final String OPENNLP_DIR = "./opennlp-models/";

	public static void main(String[] args) throws Exception {
		// this method indexes the files, runs once only at startup
		readTextFiles();
		ArrayList<String> results = new ArrayList<String>();
		String inputQuestion = null;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {
			TextFileIndexer.indexSentences(sentences);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		do {
			try {
				displayMenu();
				inputQuestion = br.readLine();
				if (inputQuestion.equalsIgnoreCase("quit")) {
					break;
				}
				int questionType = TextFileIndexer
						.checkQuestionValidity(inputQuestion);
				// user asked for topic
				if (questionType == 4) {
					System.out.println("Top Topics in Descending Order:");
					getTopics();
				}
				// user asked for db
				else if (questionType == 5) {
					System.out.println("Knowlegebase:");
					getDb();
				} else if (questionType == -1) {// a question type was not
												// identified
					System.out
							.println("Sorry, I don’t understand your questions.");
				} else if (questionType == 6) {// number 10, typed "search Y"
					inputQuestion = inputQuestion.substring(7);
					searchIndexedFiles(inputQuestion,
							TextFileIndexer.INDEX_DIR_DOCS);
				} else {
					try {
						inputQuestion = StopWords
								.removeStopWords(inputQuestion);
						results = searchIndexedFiles(inputQuestion,
								TextFileIndexer.INDEX_DIR);
						if (results != null)
							System.out.println("ANSWER: "
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
			FileUtils.deleteDirectory(new File(TextFileIndexer.INDEX_DIR));
			FileUtils.deleteDirectory(new File(TextFileIndexer.INDEX_DIR_DOCS));
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static void displayMenu() {
		System.out
				.println("\nWelcome to DSS Project 1 Question Answering System.\n");
		System.out.println("________________________________________MENU________________________________________\n");
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
			if (indexDirectory.equals(TextFileIndexer.INDEX_DIR))
				results = TextFileIndexer.searchIndexForSentence(searcher,
						collector, query);
			if (indexDirectory.equals(TextFileIndexer.INDEX_DIR_DOCS))
				TextFileIndexer.searchIndexForDocuments(searcher, collector,
						query);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return results;
	}

	private static ArrayList<String> getTopics() throws Exception {
		IndexReader reader = null;

		// read the indexed files
		try {
			reader = DirectoryReader.open(FSDirectory.open(new File(
					TextFileIndexer.INDEX_DIR)));
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// gets the most frequent terms
		// TODO: Currently does not use an index that removes stopwords. Works
		// because of small dataset
		TermStats[] topFreqWords = null;
		try {
			Comparator<TermStats> c = new HighFreqTerms.TotalTermFreqComparator();
			topFreqWords = HighFreqTerms.getHighFreqTerms(reader, 5, "word", c);
		} catch (IOException e) {
			e.printStackTrace();
		}
		ArrayList<String> results = new ArrayList<String>();
		for (int i = 0; i < topFreqWords.length; i++) {
			System.out.println(i + 1 + " "
					+ topFreqWords[i].termtext.utf8ToString());
		}
		return results;
	}

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

	private static void readTextFiles() throws IOException {
		File folder = new File("./text-files");
		File[] listOfFiles = folder.listFiles();
		StringBuilder sb = new StringBuilder();
		String content = null;

		for (int i = 0; i < listOfFiles.length; i++) {
			File file = listOfFiles[i];
			if (file.isFile() && file.getName().endsWith(".txt")) {
				String line = null;

				try {
					// FileReader reads text files in the default encoding.
					FileReader fileReader = new FileReader(file);

					// Always wrap FileReader in BufferedReader.
					BufferedReader bufferedReader = new BufferedReader(
							fileReader);
					while ((line = bufferedReader.readLine()) != null) {
						sb.append(line);
					}
					sb.append(" "); // System.out.println(content);

					// Always close files.
					bufferedReader.close();
				} catch (FileNotFoundException ex) {
					System.out.println("Unable to open file '" + file + "'");
					ex.printStackTrace();
				} catch (IOException ex) {
					System.out.println("Error reading file '" + file + "'");
					// Or we could just do this:
					ex.printStackTrace();
				}
			}
		}
		content = sb.toString();

		// sentenceDetect used to get separate sentences for indexing
		try {
			sentences = sentenceDetect(content);
		} catch (InvalidFormatException e) {
			e.printStackTrace();
		}
	}

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

	private static Tokenizer _tokenizer = null;

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

	// receives an array of tokens and finds out which is a name
	public static void findName(String[] tokens) throws IOException {
		InputStream is = new FileInputStream(OPENNLP_DIR + "en-ner-person.bin");

		TokenNameFinderModel model = new TokenNameFinderModel(is);
		is.close();

		NameFinderME nameFinder = new NameFinderME(model);

		Span nameSpans[] = nameFinder.find(tokens);

		double[] spanProbs = nameFinder.probs(nameSpans);

		System.out.println("!!!!!!!!!!!!NAMEFINDER!!!!!!!!!!!!!!");

		for (int i = 0; i < nameSpans.length; i++) {
			System.out.println("Span: " + nameSpans[i].toString());
			System.out.println("Covered text is: "
					+ tokens[nameSpans[i].getStart()] + " "
					+ tokens[nameSpans[i].getStart() + 1]);
			System.out.println("Probability is: " + spanProbs[i]);
		}
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
				String[] answerTags = POSTag(StopWords.removeStopWords(answers
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
				if (count > bestCount) {
					bestCount = count;
					bestIndex = j;
				}
			}

		}
		if (bestIndex == -1)
			return "Sorry I couldn't find that information";
		return answers.get(bestIndex);
	}

}