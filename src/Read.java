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

import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.cmdline.PerformanceMonitor;
import opennlp.tools.cmdline.postag.POSModelLoader;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.parser.AbstractBottomUpParser;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.Parser;
import opennlp.tools.parser.ParserFactory;
import opennlp.tools.parser.ParserModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSSample;
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
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.store.FSDirectory;

public class Read {

	static String sentences[] = null;
	private static final String OPENNLP_DIR = "./opennlp-models/";

	public static void main(String[] args) throws IOException,
			InvalidTokenOffsetsException {
		//this method indexes the files, runs once only at startup
		readTextFiles();
		
		String inputQuestion = null;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {
			TextFileIndexer.indexFiles(sentences);
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
				int questionType = TextFileIndexer.checkQuestionValidity(inputQuestion);
				//user asked for topic
				if(questionType ==4) {
					
				} 
				//user asked for db
				else if(questionType==5){
					System.out.println("!!!!Knowlegebase!!!!");
					for (int i =0;i< TextFileIndexer.SENT_STRINGS.length;i++){
						System.out.println(i +". " + TextFileIndexer.SENT_STRINGS[i]);
					}
				}
				else {
							
					if (questionType == -1) {
						System.out
								.println("Sorry, I don’t understand your questions.");
					} 
//						else if (questionType == 4){
//						
//					}
					else{
						try {
							inputQuestion = StopWords
									.removeStopWords(inputQuestion);
							ArrayList<String> results = searchIndexedFiles(inputQuestion);
							if (results != null)
								System.out.println("ANSWER:"
										+ analyzeResults(inputQuestion,
												results, questionType));
						} catch (ParseException e) {
							e.printStackTrace();
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} while (!inputQuestion.equalsIgnoreCase("quit"));

		try {
			FileUtils.deleteDirectory(new File(TextFileIndexer.INDEX_DIR));
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static void displayMenu() {
		System.out.println("Welcome to DSS Project 1 Question Answering System.");
		System.out
				.println("Type in a question in english to get an answer from the system.");
		System.out
				.println("Type 'topic' to see the top 5 topics discussed in the documents.");
		System.out
		.println("Type 'db' to see the top 5 topics discussed in the documents.");
		System.out.println("Type 'quit' to exit");
		System.out.print("Q:");
	}

	private static ArrayList<String> searchIndexedFiles(String query)
			throws ParseException, InvalidTokenOffsetsException {
		IndexReader reader = null;
		ArrayList<String> results = new ArrayList<String>();
		try {
			reader = DirectoryReader.open(FSDirectory.open(new File(
					TextFileIndexer.INDEX_DIR)));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		IndexSearcher searcher = new IndexSearcher(reader);
		TopScoreDocCollector collector = TopScoreDocCollector.create(5, true);
		try {
			results = TextFileIndexer.searchIndexForSentence(searcher,
					collector, query);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return results;
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
					sb.append(" ");					// System.out.println(content);

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

		// sentenceDetect
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

	public static String[] tokenizeString(String stringToTokenize)
			throws InvalidFormatException, IOException {
		InputStream is = new FileInputStream(OPENNLP_DIR + "en-token.bin");

		TokenizerModel model = new TokenizerModel(is);

		_tokenizer = new TokenizerME(model);

		String tokens[] = _tokenizer.tokenize(stringToTokenize);

		is.close();
		return tokens;
	}

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

	public static String[] POSTag(String stringToPOSTag) throws IOException {
		POSModel model = new POSModelLoader().load(new File(OPENNLP_DIR
				+ "en-pos-maxent.bin"));
		// PerformanceMonitor perfMon = new PerformanceMonitor(System.err,
		// "sent");
		POSTaggerME tagger = new POSTaggerME(model);

		ObjectStream<String> lineStream = new PlainTextByLineStream(
				new StringReader(stringToPOSTag));

		// perfMon.start();
		String line;
		if ((line = lineStream.read()) != null) {

			String whitespaceTokenizerLine[] = WhitespaceTokenizer.INSTANCE
					.tokenize(line);
			String[] tags = tagger.tag(whitespaceTokenizerLine);

			return tags;
			// POSSample sample = new POSSample(whitespaceTokenizerLine, tags);
			// System.out.println(sample.toString());
			//
			// perfMon.incrementCounter();
		} else
			return null;
		// perfMon.stopAndPrintFinalResult();
	}

	public static String analyzeResults(String query,
			ArrayList<String> answers, int questionType)
			throws InvalidFormatException, IOException {
		String[] tokens = null;
		String[] tags = null;

		try {
			tokens = tokenizeString(query);
		} catch (InvalidFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			tags = POSTag(query);
		} catch (IOException e) {
			e.printStackTrace();
		}

		float bestCount = 0;
		int bestIndex = -1;
		if (questionType != -1 && tags.length > 0 && tokens.length > 0
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