package openbrain.peoplesearch.wiki.index;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;

/**
 * @author vempap
 * 
 * Each index file format on disk :
 * 
 *  word => docID,freq.of.the.key.word,p1:score1,p2:score2
 *  
 * Index file format of merged indexes :
 * 
 *  word => d1,f1,p1:score1,p2:score2#d2,f2,p1:score1,p2:score2 ...
 *
 */
public class IndexBuilder {

  public static final float BOOST_SCORE_TITLE = 500;
  
  public static final int NO_WIKI_PAGES = 13355093;
  
  private String outputDir;
  private String indexFilesDir;
  private TextExtractor textExtractor;
  private CRFClassifier<CoreLabel> nerTagger;
  private Set<String> stopwords;
//  private MaxentTagger posTagger;

  public IndexBuilder(String outputDir) throws Exception {
    if (outputDir.endsWith("/"))
      outputDir = outputDir.substring(0, outputDir.length() - 1);
    this.outputDir = outputDir;
    this.indexFilesDir = this.outputDir + File.separator + "fileindexes";
    File file = new File(this.indexFilesDir);
    if (! (file.isDirectory()) ) {
      file.mkdirs();
    }
    this.textExtractor = new TextExtractor();
    this.stopwords = Utils.getStopWords();
    String nerTaggerModel = "models/english.all.3class.distsim.crf.ser.gz";
    this.nerTagger = CRFClassifier.getClassifier(nerTaggerModel);
//    String posTaggerModel = "models/english-bidirectional-distsim.tagger";
//    this.posTagger = new MaxentTagger(posTaggerModel);
  }
 
  protected String normalizeText(String text) {
    text = text.replaceAll("\\[[0-9]*\\]", " "); // replacing references
    text = text.replaceAll("[^a-zA-Z0-9,.?! \t\n]", " ");
    return text;
  }
  
  public class PersonToken {
    String word;
    int posn;
  }
  
  protected List<PersonToken> getPersonTokens(List<CoreLabel> sentence) {
    List<PersonToken> personTokens = new ArrayList<IndexBuilder.PersonToken>();
    int posn = 0;
    PersonToken personToken = null;
    for (CoreLabel coreLabel : sentence) {
      posn++;
      if (coreLabel.getString(CoreAnnotations.AnswerAnnotation.class).equals("PERSON")) {
        personToken = new PersonToken();
        personToken.word = coreLabel.word();
        personToken.posn = posn;
        personTokens.add(personToken);
      }
    }
    return personTokens;
  }
  
  public class PersonPositionInfo {
    int sen;
    int posn;
  }
  
  protected void addPersonTokenFreq(Map<String, Integer> personTokenFreq, PersonToken personToken) {
    String word = personToken.word;
    if (personTokenFreq.containsKey(word)) {
      Integer freq = personTokenFreq.get(word);
      freq += 1;
      personTokenFreq.put(word, freq);
    } else {
      personTokenFreq.put(word, 1);
    }
  }
  
  protected void addPersonPositionInfo(Map<String, List<PersonPositionInfo>> personPositionInfo, PersonToken personToken, int sen) {
    String word = personToken.word;
    if (personPositionInfo.containsKey(word)) {
      List<PersonPositionInfo> positionInfos = personPositionInfo.get(word);
      PersonPositionInfo positionInfo = new PersonPositionInfo();
      positionInfo.posn = personToken.posn;
      positionInfo.sen = sen;
      positionInfos.add(positionInfo);
    } else {
      List<PersonPositionInfo> positionInfos = new ArrayList<IndexBuilder.PersonPositionInfo>();
      PersonPositionInfo positionInfo = new PersonPositionInfo();
      positionInfo.posn = personToken.posn;
      positionInfo.sen = sen;
      positionInfos.add(positionInfo);
      personPositionInfo.put(word, positionInfos);
    }
  }
  
  protected Map<String, Integer> getTopPersonTokens(Map<String, Integer> personTokenFreq, int i) {
    List<Entry<String, Integer>> toSort = new ArrayList<Map.Entry<String,Integer>>(personTokenFreq.entrySet());
    Map<String, Integer> topPersonTokens = new HashMap<String, Integer>(i);
    int j = 1;
    Collections.sort(toSort, new PersonTokenFreqComparator());
    for (Entry<String, Integer> entry : toSort) {
      if (j > i)
        break;
      topPersonTokens.put(entry.getKey(), entry.getValue());
      j++;
    }
    return topPersonTokens;
  }
  
  private class PersonTokenFreqComparator implements Comparator<Entry<String, Integer>> {

    public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
      // descending order sorting
      int cmp = o2.getValue().compareTo(o1.getValue());
      if (cmp == 0) {
        return o1.getKey().compareTo(o2.getKey());
      }
      return cmp;
    }
    
  }
  
  public class PersonNames {
    List<String> names = new ArrayList<String>();
    
    @Override
    public boolean equals(Object obj) {
      if (obj instanceof PersonNames) {
        PersonNames other = (PersonNames) obj;
        if (names.containsAll(other.names)) {
          return true;
        }
      }
      return false;
    }
    
    @Override
    public int hashCode() {
      return names.hashCode();
    }
    
  }
  
  protected HashSet<PersonNames> formPossibleNames(Map<String, Integer> topPersonTokens, Map<String, List<PersonPositionInfo>> personPositionInfo) {
    HashSet<PersonNames> possibleNames = new HashSet<PersonNames>();
    int size = topPersonTokens.size();
    List<Entry<String,Integer>> topTokens = new ArrayList<Map.Entry<String, Integer>>(topPersonTokens.entrySet());
    if (size == 0) {
      return possibleNames;
    } else if (size == 1) {
      PersonNames personNames = new PersonNames();
      personNames.names.add(topTokens.get(0).getKey());
      possibleNames.add(personNames);
      return possibleNames;
    } else if (size == 2) {
      PersonNames personNames = new PersonNames();
      personNames.names.add(topTokens.get(0).getKey());
      personNames.names.add(topTokens.get(1).getKey());
      possibleNames.add(personNames);
      return possibleNames;
    }
    for (Entry<String, Integer> entry1 : topTokens) {
      String word1 = entry1.getKey();
      for (Entry<String, Integer> entry2 : topTokens) {
        String word2 = entry2.getKey();
        for (Entry<String, Integer> entry3 : topTokens) {
          String word3 = entry3.getKey();
          if ( (!word1.equals(word2)) && (!word1.equals(word3)) && (!word2.equals(word3)) ) {
            HashSet<PersonNames> namesFromWords = getNamesFromWords(word1, word2, word3, personPositionInfo);
            possibleNames.addAll(namesFromWords);
          }
        }
      }
    }
    return possibleNames;
  }
  
  protected int minimum(int posn1, int posn2, int posn3) {
    int min = posn1;
    if (min > posn2)
      min = posn2;
    if (min > posn3)
      min = posn3;
    return min;
  }
  
  protected int maximum(int posn1, int posn2, int posn3) {
    int max = posn1;
    if (max < posn2)
      max = posn2;
    if (max < posn2)
      max = posn3;
    return max;
  }
  
  protected HashSet<PersonNames> getNamesFromWords(String word1, String word2, String word3, Map<String, List<PersonPositionInfo>> personPositionInfo) {
    HashSet<PersonNames> namesFromWords = new HashSet<IndexBuilder.PersonNames>();
    List<PersonPositionInfo> posnInfosWord1 = personPositionInfo.get(word1);
    List<PersonPositionInfo> posnInfosWord2 = personPositionInfo.get(word2);
    List<PersonPositionInfo> posnInfosWord3 = personPositionInfo.get(word3);
    int sen1 = 0,
        sen2 = 0,
        sen3 = 0;
    int posn1 = 0,
        posn2 = 0,
        posn3 = 0;
    PersonNames personNames = null;
    for (PersonPositionInfo info1 : posnInfosWord1) {
      sen1 = info1.sen; posn1 = info1.posn;
      for (PersonPositionInfo info2 : posnInfosWord2) {
        sen2 = info2.sen;   posn2 = info2.posn;
        for (PersonPositionInfo info3 : posnInfosWord3) {
          sen3 = info3.sen; posn3 = info3.posn;
          if (sen1 == sen2 && sen2 == sen3) {
            if ( (minimum(posn1, posn2, posn3) == posn1) && (maximum(posn1, posn2, posn3) == posn3)) {
              if ( (posn2 - posn1 == 1) && (posn3 - posn2 == 1) ) {
                personNames = new PersonNames();
                personNames.names.add(word1);
                personNames.names.add(word2);
                personNames.names.add(word3);
                namesFromWords.add(personNames);
              } else if ( posn2 - posn1 == 1) {
                personNames = new PersonNames();
                personNames.names.add(word1);
                personNames.names.add(word2);
                namesFromWords.add(personNames);
              } else if (posn3 - posn2 == 1) {
                personNames = new PersonNames();
                personNames.names.add(word2);
                personNames.names.add(word3);
                namesFromWords.add(personNames);
              }
            } else if ( (minimum(posn1, posn2, posn3) == posn1) && (maximum(posn1, posn2, posn3) == posn2)) {
              if ( (posn3 - posn1 == 1) && (posn2 - posn3 == 1) ) {
                personNames = new PersonNames();
                personNames.names.add(word1);
                personNames.names.add(word3);
                personNames.names.add(word2);
                namesFromWords.add(personNames);
              } else if ( posn3 - posn1 == 1) {
                personNames = new PersonNames();
                personNames.names.add(word1);
                personNames.names.add(word3);
                namesFromWords.add(personNames);
              } else if (posn2 - posn3 == 1) {
                personNames = new PersonNames();
                personNames.names.add(word3);
                personNames.names.add(word2);
                namesFromWords.add(personNames);
              }
            } else if ( (minimum(posn1, posn2, posn3) == posn2) && (maximum(posn1, posn2, posn3) == posn1)) {
              if ( (posn3 - posn2 == 1) && (posn1 - posn3 == 1) ) {
                personNames = new PersonNames();
                personNames.names.add(word2);
                personNames.names.add(word3);
                personNames.names.add(word1);
                namesFromWords.add(personNames);
              } else if ( posn3 - posn2 == 1) {
                personNames = new PersonNames();
                personNames.names.add(word2);
                personNames.names.add(word3);
                namesFromWords.add(personNames);
              } else if (posn1 - posn3 == 1) {
                personNames = new PersonNames();
                personNames.names.add(word3);
                personNames.names.add(word1);
                namesFromWords.add(personNames);
              }
            } else if ( (minimum(posn1, posn2, posn3) == posn2) && (maximum(posn1, posn2, posn3) == posn3)) {
              if ( (posn1 - posn2 == 1) && (posn3 - posn1 == 1) ) {
                personNames = new PersonNames();
                personNames.names.add(word2);
                personNames.names.add(word1);
                personNames.names.add(word3);
                namesFromWords.add(personNames);
              } else if ( posn1 - posn2 == 1) {
                personNames = new PersonNames();
                personNames.names.add(word2);
                personNames.names.add(word1);
                namesFromWords.add(personNames);
              } else if (posn3 - posn1 == 1) {
                personNames = new PersonNames();
                personNames.names.add(word1);
                personNames.names.add(word3);
                namesFromWords.add(personNames);
              }
            } else if ( (minimum(posn1, posn2, posn3) == posn3) && (maximum(posn1, posn2, posn3) == posn1)) {
              if ( (posn2 - posn3 == 1) && (posn1 - posn2 == 1) ) {
                personNames = new PersonNames();
                personNames.names.add(word3);
                personNames.names.add(word2);
                personNames.names.add(word1);
                namesFromWords.add(personNames);
              } else if ( posn2 - posn3 == 1) {
                personNames = new PersonNames();
                personNames.names.add(word3);
                personNames.names.add(word2);
                namesFromWords.add(personNames);
              } else if (posn1 - posn2 == 1) {
                personNames = new PersonNames();
                personNames.names.add(word2);
                personNames.names.add(word1);
                namesFromWords.add(personNames);
              }
            } else if ( (minimum(posn1, posn2, posn3) == posn3) && (maximum(posn1, posn2, posn3) == posn2)) {
              if ( (posn1 - posn3 == 1) && (posn2 - posn1 == 1) ) {
                personNames = new PersonNames();
                personNames.names.add(word3);
                personNames.names.add(word1);
                personNames.names.add(word2);
                namesFromWords.add(personNames);
              } else if ( posn1 - posn3 == 1) {
                personNames = new PersonNames();
                personNames.names.add(word3);
                personNames.names.add(word1);
                namesFromWords.add(personNames);
              } else if (posn2 - posn1 == 1) {
                personNames = new PersonNames();
                personNames.names.add(word1);
                personNames.names.add(word2);
                namesFromWords.add(personNames);
              }
            }
          } else if (sen1 == sen2 && sen2 != sen3) {
            if (posn1 - posn2 == 1) { // word2 comes before word1
              personNames = new PersonNames();
              personNames.names.add(word2);
              personNames.names.add(word1);
              namesFromWords.add(personNames);
            } else if (posn2 - posn1 == 1) { // word1 before word2
              personNames = new PersonNames();
              personNames.names.add(word1);
              personNames.names.add(word2);
              namesFromWords.add(personNames);
            }
          } else if (sen2 == sen3 && sen1 != sen3) {
            if (posn2 - posn3 == 1) { // word3 comes before word2
              personNames = new PersonNames();
              personNames.names.add(word3);
              personNames.names.add(word2);
              namesFromWords.add(personNames);
            } else if (posn3 - posn2 == 1) { // word2 before word3
              personNames = new PersonNames();
              personNames.names.add(word2);
              personNames.names.add(word3);
              namesFromWords.add(personNames);
            }
          } else if (sen1 == sen3 && sen2 != sen3) {
            if (posn1 - posn3 == 1) { // word3 comes before word1
              personNames = new PersonNames();
              personNames.names.add(word3);
              personNames.names.add(word1);
              namesFromWords.add(personNames);
            } else if (posn3 - posn1 == 1) { // word1 before word3
              personNames = new PersonNames();
              personNames.names.add(word1);
              personNames.names.add(word3);
              namesFromWords.add(personNames);
            }
          }
        }
      }
    }
    return namesFromWords;
  }
  
  public class ScoredName {
    String name;
    Float score;
  }
  
  public void buildIndexOfFile(String filename, int docId) throws Exception {
    String text = textExtractor.extractText(filename);
    System.out.println("####### DOC ID : " + docId + " #######");
    System.out.println("Title :: " + textExtractor.title);
    text = normalizeText(text);
    List<List<CoreLabel>> sentences = nerTagger.classify(text);
    int sen = 0;
    List<PersonToken> personTokens = null;
    Set<String> allPersons = new HashSet<String>();
    Map<String, Integer> personTokenFreq = new HashMap<String, Integer>();
    Map<String, List<PersonPositionInfo>> personPositionInfo = new HashMap<String, List<PersonPositionInfo>>();
    for (List<CoreLabel> sentence : sentences) {
      sen++;
      personTokens = getPersonTokens(sentence);
      if (personTokens.size() > 0) {
        for (PersonToken personToken : personTokens) {
          allPersons.add(personToken.word.toLowerCase(Locale.ENGLISH));
          addPersonTokenFreq(personTokenFreq, personToken);
          addPersonPositionInfo(personPositionInfo, personToken, sen);
        }
      }
    }
    Map<String, Integer> topPersonTokens = getTopPersonTokens(personTokenFreq, 5);
    HashSet<PersonNames> possibleNames = formPossibleNames(topPersonTokens, personPositionInfo);
    if (possibleNames.size() == 0) {
      System.out.println("This document does not have any possible names");
      return;
    }
    
    // free memory before computing scores ...
    personPositionInfo.clear();
    
    // compute scores ...
    List<ScoredName> scoredNames = new ArrayList<ScoredName>();
    ScoredName scoredName = null;
    float score = 0;
    StringBuffer sb = null;
    for (PersonNames personNames : possibleNames) {
      score = 0;
      sb = new StringBuffer();
      for (String subName: personNames.names) {
        score += personTokenFreq.get(subName);
        sb.append(subName);
        sb.append(" ");
      }
      String name = sb.toString().trim();
      if (name.equals(textExtractor.title))
        score += BOOST_SCORE_TITLE;
      scoredName = new ScoredName();
      scoredName.name = name;
      scoredName.score = score;
      scoredNames.add(scoredName);
    }
    
    scoredNames = getTopScoredNames(scoredNames, 5);
    Utils.printScoredNames(scoredNames);
    
    // free memory before building index ...
    personTokenFreq.clear();
    
    // build index for this file ...
    buildIndex(docId, text, scoredNames, allPersons);
    
  }
  
  protected List<ScoredName> getTopScoredNames(List<ScoredName> scoredNames, int n) {
    Collections.sort(scoredNames, new Comparator<ScoredName>() {

      public int compare(ScoredName o1, ScoredName o2) {
        return o2.score.compareTo(o1.score); // descending order
      }
    });
    int min = Math.min(n, scoredNames.size());
    return scoredNames.subList(0, min);
  }
  
  protected String getStringReprPossibleNamesScored(List<ScoredName> scoredNames) {
    StringBuffer sb = new StringBuffer();
    for (ScoredName scoredName : scoredNames) {
      sb.append(scoredName.name);
      sb.append(":");
      sb.append(scoredName.score);
      sb.append(",");
    }
    return sb.substring(0, sb.length() - 1);
  }
  
  protected void buildIndex(int docId, String text, List<ScoredName> scoredNames, Set<String> allPersons) throws Exception {
    String namesScored = getStringReprPossibleNamesScored(scoredNames);
    System.out.println(namesScored); // print as part of log ...
//    String taggedString = posTagger.tagTokenizedString(text);
    String[] tokens = text.split("[^a-zA-Z0-9]");
    String token = null;
//    String postag = null;
//    int start = 0;
    Map<String, Integer> tokenFreq = new HashMap<String, Integer>();
    Integer freq = null;
    for (int i = 0; i < tokens.length; i++) {
      token = tokens[i];
//      start = token.lastIndexOf("_");
//      postag = token.substring(start+1);
//      token = token.substring(0, start);
//      token = token.replaceAll("[^a-zA-Z0-9]", "");
      token = token.toLowerCase(Locale.ENGLISH);
      if ( (!stopwords.contains(token))  && (!token.equals("")) ) {
        if (tokenFreq.containsKey(token)) {
          freq = tokenFreq.get(token);
          freq++;
          tokenFreq.put(token, freq);
        } else {
          freq = new Integer(1);
          tokenFreq.put(token, freq);
        }
      }
    }
    
    // sort the token freq keys ...
    List<String> keys = new ArrayList<String>(tokenFreq.keySet());
    Collections.sort(keys);
    int docLength = keys.size();
    
    // write the index of this file ...
    String fileName = indexFilesDir + File.separator + docId;
    StringBuffer sb = null;
    PrintWriter pw = new PrintWriter(fileName);
    for (String key : keys) {
      tokenFreq.get(key);
      sb = new StringBuffer();
      sb.append(key);
      sb.append(" => ");
      sb.append(docId);
      sb.append(",");
      sb.append(docLength);
      sb.append(",");
      sb.append(tokenFreq.get(key));
      sb.append(",");
      sb.append(namesScored);
      pw.println(sb.toString());

    }
//    Set<Entry<String,Integer>> entrySet = tokenFreq.entrySet();
//    for (Entry<String, Integer> entry : entrySet) {
//      sb = new StringBuffer();
//      sb.append(entry.getKey());
//      sb.append(" => ");
//      sb.append(docId);
//      sb.append(",");
//      sb.append(entry.getValue());
//      sb.append(",");
//      sb.append(namesScored);
//      pw.println(sb.toString());
//    }
    pw.close();
  }
  
  public void buildIndexOfDirectory(String wikiPagesDir, int start, int end) throws Exception {
    if (wikiPagesDir.endsWith("/"))
      wikiPagesDir = wikiPagesDir.substring(0, wikiPagesDir.length() - 1);
//    File wikiPagesDirFile = new File(wikiPagesDir);
//    int noFiles = FileUtils.listFiles(wikiPagesDirFile , null, true).size();
    int n = Math.min(NO_WIKI_PAGES, end);
    String filename = "";
    int i = 1;
    for (i = start; i <= n; i ++) {
      filename = wikiPagesDir + File.separator + Integer.toString(i);
      buildIndexOfFile(filename, i);
    }
  }
  
  public static void main(String[] args) {
    if (args.length != 4) {
      System.out.println("\nUsage: java -cp people.jar:lib/* index.IndexBuilder <wikiPagesDir> <outputDir> <start> <end>\n");
      System.exit(1);
    }
    String wikiPagesDir = null,
           outputDir = null;
    int start = 70,
        end = 70;
    wikiPagesDir = args[0];
    outputDir = args[1];
    start = Integer.valueOf(args[2]);
    end = Integer.valueOf(args[3]);
    try {
      IndexBuilder indexBuilder = new IndexBuilder(outputDir);
      indexBuilder.buildIndexOfDirectory(wikiPagesDir, start, end);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
