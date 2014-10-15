#MIE 451 
#Decision Support Systems
#Mohannad Azer
#998192611
#14-Oct-2014


#Instructions

##1. Download from github
 
```
git clone https://github.com/mxazer/DSSProject1.git
```

If you do not have git installed, download the zip file and extract it

##2. Download dependencies using Maven

For instructions on how to quickly download and use maven, please see <http://maven.apache.org/guides/getting-started/maven-in-five-minutes.html>

#####In the project main directory, type the following into terminal
```
mvn clean package
```

##3. Run the program
```
mvn exec:java -Dexec.mainClass="Read" -e
```

#Report
##1. Introduction

This Project contains a simple fact-based question-answering system based on Natural Language Processing. It uses plain text unstructured data stored locally in .txt files and builds an indexed database using Apache Lucene and OpenNLP.

##2. Software Features

###Document Indexing

Starting up the software will read all the files in the "/DSSProject1/text-files" directory, and create two indexed files using Apache Lucene. Lucene performs automatic tokenizing. The first contains an index where each sentence is considered a document(/DSSProject1/tmp/index), and the second indexes the actual documents(/DSSProject1/tmp/index/docs). These indexed files are created and destoryed on each startup of the program.

This begins by using the OpenNLP sentence splitter on all the text as it is read in 

```
sentences = sentenceDetect(HelperMethods.readTextFiles());
```

Next, both sets of indexed documents are created using Lucene

```
TextFileIndexer.indexSentencesAndDocuments(sentences);
```

Sentences are indexed as individual documents in order to facilitate future retrieval, as these will be given as answers. Documents are indexed normally.

```
indexer = new TextFileIndexer(INDEX_DIR);
indexer.indexSentence(sentences);
indexer.closeIndex();
indexer = new TextFileIndexer(INDEX_DIR_DOCS);
indexer.indexFileOrDirectory(TEXT_FILES_DIR);
indexer.closeIndex();

```

###Question Answering

1. User is displayed a menu, and a prompt "Q:"
2. User types in a string and presses enter
3. The string is checked for several critrea to see if it is a valid one (HelperMethods.checkQuestionValidity())
4. If it begins with "Who", "What", or "When", the question is stripped of stopwords and a query is ran against the indexed sentences (Read.searchIndexedFiles()) 
5. If results are returned it is processed as a question in (Read.analyzeResults())
6. In the analysis, each answer that's returned is processed separately and they are all compared to see which one provides the largest number of relevant POS Tags. The specific tags we look for are based on the type of question that has been asked. The specific portion of the algorithm doing this is shown below.
7. A string is printed out with the sentence containing the answer and the menu reappears.

```
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
```

-There are two types of error messages displayed when asking a question.

-The first is ("Sorry, I don’t have that information."), displayed when the search has brought back no results found in TextFileIndexer.searchIndexForSentence()

-The second is ("\nSorry, I don’t understand your questions."), displayed when HelperMethods.checkQuestionValidity() has deemed it invalid

**Note: Although Lucene incorporates scoring, it is not being used directly in comparison when trying to find the proper answer. It is, however, used in the "search Y" option (discussed later)**

###Topic Displaying

1. User types in "topic"
2. The method Read.getTopics() is called. It uses the ""getHighFreqTerms(IndexReader reader, int numTerms, String field, Comparator<TermStats> comparator)" method to get the 5 most frequently found terms in the indexed sentence files. 
3. They are then printed out to the user in descending order.
4. This works because Lucene automatically eliminates stopwords before indexing: <http://lucene.apache.org/core/4_0_0/analyzers-common/org/apache/lucene/analysis/core/StopFilter.html>


###Knowlegebase Displaying

1. User types in "db"
2. Method Read.getDb() is called
3. Returns a list of all the terms as stored in the indexed sentece files
4. Extracting the actual content of the indexed files is not an easy process, and seems to require a software called Luke <https://code.google.com/p/luke/> which is beyond the scope of this project

###Term Querying

1. User types "search Y", where why is a single word term that is to be queried, and Read.searchIndexedFiles(inputQuestion, TextFileIndexer.INDEX_DIR_DOCS) is called. It determines that it is to search the indexed documents (and not individual sentences)

2. This subsequently calls TextFileIndexer.searchIndexForDocuments(searcher, collector, query), which proceeds to search the indexed files, obtain the top results with their score, and print them in descending order.

3. For similarity measuring, I decided to stick to the default one used by Lucene <http://lucene.apache.org/core/4_0_0/core/org/apache/lucene/search/similarities/DefaultSimilarity.html> More information on scoring can be found here <http://www.lucenetutorial.com/advanced-topics/scoring.html>

##3. Design Choices and challenges faced

**Tools used**

The bases for using what the tools I used comes from reading Chapter 8 in the book Taming Text, as well as looking through their code <https://github.com/tamingtext/book>. Although no code was directly used, understanding the logic behind their work allowed me to produce a much simpler version. Their use of Apache OpenNLP, Maven, and Apache Lucene was the inspiration in using them for my project. 

**Returning sentences as answers**

This was a decision I made based on the fact that the system was simple and had a limited source of documents to read from. This turned out to be not very effective despite it being relatively easy to implement, as seen by the results of Question 2.

```
Q:What is Canada’s capital?
Found 2 sentence hits.
10.  score=0.5738205
13.  score=0.5738205
Loading POS Tagger model ... done (0.724s)
Loading POS Tagger model ... done (0.590s)
Loading POS Tagger model ... done (0.627s)

ANSWER: It is the capital city of the province of Ontario.
```
A way around this would be to co-reference sentences in the index with another Field using Lucene, and retrieving the preceding sentence to be checked as well. Unfortunately, implementation is outside the scope of my knowlege and would require extensive time for me.

**Two seperate indexed documents**

The reason for having two separate indexed database was due to some issues faced the way I wanted the program to answer the questions was to return an already formed english sentence within the texts. This required sentences to be indexed as individual documents. The second database containing the six documents was to enable the Term Querying functionality.
##4. Resources Used

```
http://www.lucenetutorial.com/
https://github.com/tamingtext/book
http://opennlp.sourceforge.net/models-1.5/
http://johnmiedema.com/
http://danielmclaren.com/2007/05/11/getting-started-with-opennlp-natural-language-processing

```

##5. Data Content

####Sources
```http://www.ranks.nl/stopwords• http://www.programcreek.com/2012/05/opennlp-­‐‑tutorial/• http://simple.wikipedia.org/wiki/Stephen_Harper• http://simple.wikipedia.org/wiki/Canada• http://simple.wikipedia.org/wiki/Toronto• http://www.teachers.ab.ca/Publications/ATA%20Magazine/Volume%2086/Nu mber%202/Articles/Pages/The%20Founding%20of%20Alberta.aspx• http://umanitoba.ca/about/ourhistory.html• http://www.cbc.ca/history/EPISCONTENTSE1EP10CH4PA4LE.html
```
####Raw Text
1.txt
Stephen Joseph Harper, (born April 30, 1959 in Toronto, Ontario), is the current, and 22nd Prime Minister of Canada. He is a member of the Conservative Party. He was elected in February 2006 and replaced Paul Martin. He has been the Prime Minister of Canada ever since. He was born in 1959 in Toronto, Ontario and lived in Calgary, Alberta. He is married to Laureen Teskey and has two children.
2.txt
Canada is a country in the northern part of North America. It is bordered by the United States of America both to the south and to the west (Alaska). By area, Canada is the second largest country in the world. Canada consists of ten provinces and three territories. Ottawa is the capital of Canada. Canada is the second largest country in the world in land area, after Russia.
3.txt
Toronto is the largest city of Canada. It is the capital city of the province of Ontario. It is found on the northwest side of Lake Ontario. Toronto has a population of over 3,000,000 people. Many large banks of Canada have their main offices in Toronto. It is also a popular destination for tourists.
4.txt
Alberta formally came into being on September 1, 1905, at the same time as Saskatchewan. The Autonomy Acts , which gave the two provinces their places in Confederation, were matters of considerable controversy in Ottawa in the months preceding their passage. Haultain, premier of the Territories, had proposed the establishment of one large province, but Ottawa rejected this plan. Alberta was named after the fourth daughter of Queen Victoria, Princess Alberta Louise, who was married to the governor general of Canada, the Marquis of Lorne. Sir Wilfred Laurier, the prime minister of Canada, journeyed west on the Canadian Pacific Railway to visit the new provincial capitals in September 1905. The terms of Confederation established education as a matter of provincial jurisdiction. Section 93 of the British North America Act , along with the 1901 ordinances of the North-­‐‑West Territories, established the legal basis for public schooling in Alberta. These laws established a system of public and separate schools, similar to the arrangement in Ontario. English was the official language of instruction.
5.txt
In a typical year, the university has an enrolment of approximately 22,000 undergraduate students and 3,000 graduate students. The university offers over 90 degrees, more than 60 at the undergraduate level, in academic programs as diverse as agricultural and food sciences, music, engineering and medicine. Most of our academic units offer graduate studies programs leading to master'ʹs or doctoral degrees. The University of Manitoba is also home to a wide range of research centres and institutes, and Smartpark Research and Technology Park, a community of innovators that forges collaborations between university and industry. The University of Manitoba is Western Canada'ʹs first university, founded on February 28, 1877 just seven years after the province of Manitoba and only four years after the City of Winnipeg. At the time, Manitoba was a small postage stamp province, Winnipeg was hardly more than a town and the University of Manitoba was a university in name only, created to confer degrees on students graduating from its three founding colleges ñ St. Boniface College, St. John'ʹs College, and Manitoba College.
6.txt
Charged with high treason for leading the North West Rebellion, Riel wanted to use the trial as a platform to vindicate himself. "ʺI was not taken prisoner. I surrendered on purpose. I want to be judged on the merits of my actions. ... From the time of my arrival in Saskatchewan, I worked peacefully ... We didnt make any aggressive military moves. ... In Batoche we defended ourselves."ʺ In turn, the government did all it could to muzzle the MÈtis leader. It wished to dispose of the man who had led two uprisings in the countrys brief history. The trial was moved from Winnipeg to Regina when the government discovered that a Manitoba jury could be half MÈtis. Of the six men on the Regina jury -­‐‑ only one spoke French. Prime Minister John A. Macdonald decided to charge Riel with high treason, based on an obscure British law dating to the year 1342. This law carried the death the penalty whereas Canadas treason law did not. Trial began on July 20, 1885. It was a sweltering day made more oppressive by the hordes of people wanting to view the spectacle in Regina.

