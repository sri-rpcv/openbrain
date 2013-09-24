package openbrain.peoplesearch.wiki.index;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import openbrain.peoplesearch.wiki.index.IndexBuilder.PersonNames;
import openbrain.peoplesearch.wiki.index.IndexBuilder.PersonToken;
import openbrain.peoplesearch.wiki.index.IndexBuilder.ScoredName;

public class Utils {

  private static final String[] STOP_WORDS = new String[] { "i", "me", "my",
          "myself", "we", "our", "ours", "ourselves", "you", "your", "yours",
          "yourself", "yourselves", "he", "him", "his", "himself", "she",
          "her", "hers", "herself", "it", "its", "itself", "they", "them",
          "their", "theirs", "themselves", "what", "which", "who", "whom",
          "this", "that", "these", "those", "am", "are", "were", "be", "been",
          "being", "have", "has", "had", "having", "do", "does", "did",
          "doing", "a", "an", "the", "and", "but", "if", "or", "because", "as",
          "until", "while", "of", "at", "by", "for", "with", "about",
          "against", "between", "into", "through", "during", "before", "after",
          "above", "below", "to", "from", "up", "down", "in", "out", "on",
          "off", "over", "under", "again", "further", "here", "there", "when",
          "where", "why", "how", "all", "any", "both", "each", "few", "more",
          "most", "other", "some", "such", "no", "nor", "not", "only", "own",
          "same", "so", "than", "too", "very", "s", "t", "can", "will", "just",
          "don", "should", "now" };

  private static final String[] STOP_WORDS_ALL = new String[] { "i", "me",
          "my", "myself", "we", "our", "ours", "ourselves", "you", "your",
          "yours", "yourself", "yourselves", "he", "him", "his", "himself",
          "she", "her", "hers", "herself", "it", "its", "itself", "they",
          "them", "their", "theirs", "themselves", "what", "which", "who",
          "whom", "this", "that", "these", "those", "am", "is", "are", "was",
          "were", "be", "been", "being", "have", "has", "had", "having", "do",
          "does", "did", "doing", "a", "an", "the", "and", "but", "if", "or",
          "because", "as", "until", "while", "of", "at", "by", "for", "with",
          "about", "against", "between", "into", "through", "during", "before",
          "after", "above", "below", "to", "from", "up", "down", "in", "out",
          "on", "off", "over", "under", "again", "further", "then", "once",
          "here", "there", "when", "where", "why", "how", "all", "any", "both",
          "each", "few", "more", "most", "other", "some", "such", "no", "nor",
          "not", "only", "own", "same", "so", "than", "too", "very", "s", "t",
          "can", "will", "just", "don", "should", "now" };

  private static final String[] TIME_SENSITIVE_WORDS = new String[] {
          "president", "captain", "ceo", "chairman" };

  public static Set<String> getStopWords() {
    Set<String> stopwords = new HashSet<String>();
    for (int i = 0; i < STOP_WORDS.length; i++) {
      stopwords.add(STOP_WORDS[i]);
    }
    return stopwords;
  }

  public static Set<String> getTimeSensitiveWords() {
    Set<String> stopwords = new HashSet<String>();
    for (int i = 0; i < TIME_SENSITIVE_WORDS.length; i++) {
      stopwords.add(TIME_SENSITIVE_WORDS[i]);
    }
    return stopwords;
  }
  
  public static void printPersonTokenFreq(Map<String, Integer> map) {
    Set<Entry<String, Integer>> entrySet = map.entrySet();
    for (Entry<String, Integer> entry : entrySet) {
      System.out.println(entry.getKey() + "/" + entry.getValue());
    }
  }

  public static void printPersonTokens(List<PersonToken> personTokens) {
    for (PersonToken personToken : personTokens) {
      System.out.println(personToken.word + ":" + personToken.posn);
    }
  }

  public static void printPossibleNames(HashSet<PersonNames> possibleNames) {
    Iterator<PersonNames> iterator = possibleNames.iterator();
    while (iterator.hasNext()) {
      PersonNames personNames = iterator.next();
      StringBuffer sb = new StringBuffer();
      for (String subName : personNames.names) {
        sb.append(subName);
        sb.append(" ");
      }
      System.out.println(sb.toString().trim());
    }
  }

  public static void printScoredNames(List<ScoredName> scoredNames) {

  }

}
