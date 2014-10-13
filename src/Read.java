import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;

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

public class Read {
	
	public static void main(String[] args) {

		System.out.println("Welcome to DSS Project 1.");
		System.out
				.println("Type in a question in english to get an answer from the system.");
		String inputQuestions = null;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {
			inputQuestions = br.readLine();
			System.out.println(inputQuestions);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block	
			e.printStackTrace();
		}

		readTextFiles();
		Parse parse = parseSentence(inputQuestions);

//		ParseNounPhrases
//				.ParseNounPhrase("Who is the author of The Call of the Wild?");

	}

	private static void readTextFiles() {
		File folder = new File("./TextFiles");
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
					sb.append(" ");
					// System.out.println(content);

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

		String sentences[] = null;
		try {
			sentences = sentenceDetect(content);
			System.out
					.println("!!!!!!!!!!!!!!!!!!!SENTENCESPLITER!!!!!!!!!!!!!!!!");

			for (int i = 0; i < sentences.length; i++) {
				System.out.println(sentences[i]);
			}
		} catch (InvalidFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// tokenizeString
		try {
			String tokens[] = tokenizeString(sentences[6]);

			System.out.println("!!!!!!!!!!!TOKENIZER!!!!!!!!!!");

			for (String a : tokens)
				System.out.println(a);
			findName(tokens);
		} catch (InvalidFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static String[] sentenceDetect(String stringToSentenceSplit)
			throws InvalidFormatException, IOException {

		// always start with a model, a model is learned from training data
		InputStream is = new FileInputStream("en-sent.bin");
		SentenceModel model = new SentenceModel(is);
		SentenceDetectorME sdetector = new SentenceDetectorME(model);

		String sentences[] = sdetector.sentDetect(stringToSentenceSplit);

		is.close();
		return sentences;
	}
	
	private static Tokenizer _tokenizer = null;

	public static String[] tokenizeString(String stringToTokenize)
			throws InvalidFormatException, IOException {
		InputStream is = new FileInputStream("en-token.bin");

		TokenizerModel model = new TokenizerModel(is);

		_tokenizer = new TokenizerME(model);

		String tokens[] = _tokenizer.tokenize(stringToTokenize);

		is.close();
		return tokens;
	}

	public static void findName(String[] tokens) throws IOException {
		InputStream is = new FileInputStream("en-ner-person.bin");

		TokenNameFinderModel model = new TokenNameFinderModel(is);
		is.close();

		NameFinderME nameFinder = new NameFinderME(model);

		// String []sentence = new String[]{
		// "Mike",
		// "Smith",
		// "is",
		// "a",
		// "good",
		// "person"
		// };

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
	
	public static void POSTag(String stringToPOSTag) throws IOException {
		POSModel model = new POSModelLoader()	
			.load(new File("en-pos-maxent.bin"));
		PerformanceMonitor perfMon = new PerformanceMonitor(System.err, "sent");
		POSTaggerME tagger = new POSTaggerME(model);
	 
		String input = "Hi. How are you? This is Mike.";
		ObjectStream<String> lineStream = new PlainTextByLineStream(
				new StringReader(input));
	 
		perfMon.start();
		String line;
		while ((line = lineStream.read()) != null) {
	 
			String whitespaceTokenizerLine[] = WhitespaceTokenizer.INSTANCE
					.tokenize(line);
			String[] tags = tagger.tag(whitespaceTokenizerLine);
	 
			POSSample sample = new POSSample(whitespaceTokenizerLine, tags);
			System.out.println(sample.toString());
	 
			perfMon.incrementCounter();
		}
		perfMon.stopAndPrintFinalResult();
	}

	private static Parse parseSentence (String text) {
		final Parse p = new Parse(text,
		// a new span covering the entire text
				new Span(0, text.length()),
				// the label for the top if an incomplete node
				AbstractBottomUpParser.INC_NODE,
				// the probability of this parse...uhhh...?
				1,
				// the token index of the head of this parse
				0);

		// make sure to initialize the _tokenizer correctly
		final Span[] spans = _tokenizer.tokenizePos(text);

		for (int idx = 0; idx < spans.length; idx++) {
			final Span span = spans[idx];
			// flesh out the parse with individual token sub-parses
			p.insert(new Parse(text, span, AbstractBottomUpParser.TOK_NODE, 0,
					idx));
		}

		Parse actualParse = parse(p);
		return actualParse;
	}

	private static Parser _parser = null;

	private static Parse parse(final Parse p) {
		// lazy initializer
		if (_parser == null) {
			InputStream modelIn = null;
			try {
				// Loading the parser model
				InputStream is = new FileInputStream(
						"en-parser-chunking.bin");
				final ParserModel parseModel = new ParserModel(is);
				is.close();

				_parser = ParserFactory.create(parseModel);
			} catch (final IOException ioe) {
				ioe.printStackTrace();
			} finally {
				if (modelIn != null) {
					try {
						modelIn.close();
					} catch (final IOException e) {
					} // oh well!
				}
			}
		}
		return _parser.parse(p);
	}
	
	public static void chunk() throws IOException {
		POSModel model = new POSModelLoader()
				.load(new File("en-pos-maxent.bin"));
		PerformanceMonitor perfMon = new PerformanceMonitor(System.err, "sent");
		POSTaggerME tagger = new POSTaggerME(model);
	 
		String input = "Hi. How are you? This is Mike.";
		ObjectStream<String> lineStream = new PlainTextByLineStream(
				new StringReader(input));
	 
		perfMon.start();
		String line;
		String whitespaceTokenizerLine[] = null;
	 
		String[] tags = null;
		while ((line = lineStream.read()) != null) {
			whitespaceTokenizerLine = WhitespaceTokenizer.INSTANCE
					.tokenize(line);
			tags = tagger.tag(whitespaceTokenizerLine);
	 
			POSSample sample = new POSSample(whitespaceTokenizerLine, tags);
			System.out.println(sample.toString());
				perfMon.incrementCounter();
		}
		perfMon.stopAndPrintFinalResult();
	 
		// chunker
		InputStream is = new FileInputStream("en-chunker.bin");
		ChunkerModel cModel = new ChunkerModel(is);
	 
		ChunkerME chunkerME = new ChunkerME(cModel);
		String result[] = chunkerME.chunk(whitespaceTokenizerLine, tags);
	 
		for (String s : result)
			System.out.println(s);
	 
		Span[] span = chunkerME.chunkAsSpans(whitespaceTokenizerLine, tags);
		for (Span s : span)
			System.out.println(s.toString());
	}
}