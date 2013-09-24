package openbrain.peoplesearch.wiki.search;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import openbrain.peoplesearch.wiki.index.Utils;

public class QueryAnswer {

  private Set<String> stopWords = Utils.getStopWords();
  private Set<String> timeSensitiveWords = Utils.getTimeSensitiveWords();
  
  private RandomAccessFile indexReader;

  private String[] secIndexWords;
  private Long[] secIndexOffsets;
  private final int blockSize;

  public QueryAnswer(String indexFileName, String secIndexFileName,
          int blockSize) throws Exception {
    this(new File(indexFileName), new File(secIndexFileName), blockSize);
  }

  public QueryAnswer(File indexFile, File secIndexFile, int blockSize)
          throws Exception {
    if (!indexFile.exists())
      throw new IOException(indexFile.getAbsolutePath() + " does not exist");
    if (!secIndexFile.exists())
      throw new IOException(secIndexFile.getAbsolutePath() + " does not exist");

    this.blockSize = blockSize;
    indexReader = new RandomAccessFile(indexFile, "r");

    List<String> words = new ArrayList<String>();
    List<Long> offsets = new ArrayList<Long>();
    RandomAccessFile secIndexReader = new RandomAccessFile(secIndexFile, "r");
    String line = null;
    String[] wordOffsets = null;
    long offset = -1;
    while ((line = secIndexReader.readLine()) != null) {
      wordOffsets = line.trim().split("#");
      words.add(wordOffsets[0]);
      offset = Long.parseLong(wordOffsets[1]);
      offsets.add(offset);
    }
    secIndexReader.close();
    secIndexWords = words.toArray(new String[words.size()]);
    secIndexOffsets = offsets.toArray(new Long[offsets.size()]);
  }

  public void close() throws IOException {
    indexReader.close();
  }

  protected Long binarySearchOnSecIdx(String queryWord) {
    int low = 0;
    int high = secIndexWords.length - 1;
    int mid;
    int comparedValue;

    while (low < high) {
      mid = (low + high) / 2;
      comparedValue = secIndexWords[mid].compareTo(queryWord);
      if (comparedValue < 0)
        low = mid + 1;
      else if (comparedValue > 0)
        high = mid - 1;
      else
        return secIndexOffsets[mid];
    }

    if (secIndexWords[low].compareTo(queryWord) <= 0)
      return secIndexOffsets[low]; // low=high
    else
      return secIndexOffsets[low - 1];
  }

  protected String fetchResultsForWord(String queryWord) throws IOException {
    Long offset = binarySearchOnSecIdx(queryWord);
    indexReader.seek(offset);
    String line = null;
    String[] postings = null;
    int compareValue = 0;
    for (int i = 0; i < blockSize; i++) {
      line = indexReader.readLine();
      if (line == null)
        break;
      postings = line.trim().split(" => ");
      compareValue = postings[0].compareTo(queryWord);
      if (compareValue == 0)
        return postings[1];
      else if (compareValue > 0)
        return "";
    }
    return "";
  }

  public static <T> Set<T> intersection(Set<T> setA, Set<T> setB) {
    Set<T> tmp = new TreeSet<T>();
    for (T x : setA)
      if (setB.contains(x))
        tmp.add(x);
    return tmp;
  }
  
  public class DocInfo {
    Map<String, Float> nameScores = new HashMap<String, Float>();
    int termFreq;
    int docLength;
  }
  
  public class Results {
    String name;
    Float score;
    Integer freq;
    Integer docID;
  }

  public void getResults(String query) throws IOException {
    String[] queryWords = query.split("[^a-zA-Z0-9]");
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < queryWords.length; i++) {
      if (!stopWords.contains(queryWords[i])) {
        sb.append(queryWords[i]);
        sb.append(" ");
      }
//      if (timeSensitiveWords.contains(queryWords[i])) {
//        sb.append("current");
//        sb.append(" ");
//        sb.append("2013");
//        sb.append(" ");
//      }
    }
    queryWords = sb.toString().trim().split(" ");
    String docPostings = null, name = null;
    String[] items = null, names = null;
    Set<Integer> docs = null;
    Map<String, Set<Integer>> queryWordsToDocs = new HashMap<String, Set<Integer>>();
    Map<Integer, DocInfo> docVectors = new HashMap<Integer, DocInfo>();
    int docId = 0, docLength = 0, idx = 0;
    Float score = (float) 0;
    for (int i = 0; i < queryWords.length; i++) {
      docPostings = fetchResultsForWord(queryWords[i]);
      if (docPostings.equals("")) {
        System.out.println("query word \"" + queryWords[i] + "\" not present in index");
        return;
      }
      items = docPostings.split("#");
      docs = new HashSet<Integer>();
      sb = new StringBuffer(queryWords[i] + " => ");
      for (int j = 0; j < items.length; j++) {
        DocInfo info = new DocInfo();
        names = items[j].split(",");
        docId = Integer.valueOf(names[0]);
        docLength = Integer.valueOf(names[1]);
        info.termFreq = Integer.valueOf(names[2]);
        info.docLength = docLength;
        for (int k = 3; k < names.length; k++) {
          idx = names[k].indexOf(":");
          if (idx == -1) // TODO: Debug why it'd be -1 for query "ceo of yahoo" 
            continue;
          name = names[k].substring(0, idx);
          score = Float.valueOf(names[k].substring(idx+1));
          info.nameScores.put(name, score);
        }
        docs.add(Integer.valueOf(items[j].split(",")[0]));
        docVectors.put(docId, info);
      }
      queryWordsToDocs.put(queryWords[i], docs);
//      System.out.println(queryWords[i] + " => " + docPostings);
    }

    List<Entry<String, Set<Integer>>> entrySet = new ArrayList<Map.Entry<String, Set<Integer>>>(
            queryWordsToDocs.entrySet());
    if (entrySet.size() > 0) {
      Set<Integer> commonDocs = new TreeSet<Integer>();
      commonDocs.addAll(entrySet.get(0).getValue());
      for (Entry<String, Set<Integer>> entry : entrySet) {
        Set<Integer> value = entry.getValue();
        commonDocs = intersection(commonDocs, value);
      }
      
      List<Results> results = new ArrayList<Results>();
      Results res = null;
      for (Integer integer : commonDocs) {
        DocInfo docInfo = docVectors.get(integer);
        List<Entry<String,Float>> nameScoreEntrySet = new ArrayList<Map.Entry<String,Float>>(docInfo.nameScores.entrySet());
        Collections.sort(nameScoreEntrySet, new Comparator<Entry<String, Float>>() {

          public int compare(Entry<String, Float> o1, Entry<String, Float> o2) {
            return (int) (o2.getValue() - o1.getValue()); // descending sort
          }
        });
        for (Entry<String, Float> entry : nameScoreEntrySet) {
          res = new Results();
          res.freq = docInfo.termFreq;
          res.docID = integer;
          res.name = entry.getKey();
          res.score = entry.getValue();
          results.add(res);
          break; // only 1 result per doc
        }
      }
      Collections.sort(results, new Comparator<Results>() {

        public int compare(Results o1, Results o2) {
          // descending sort
//          int compareTo = o2.freq.compareTo(o1.freq);
//          return compareTo;
//          if (compareTo != 0)
//            return compareTo;
//          else
            return o2.score.compareTo(o1.score);
        }
      });
//      for (Results val : results) {
//        System.out.println(val.name + " / " + val.score + " / " + val.docID);
//      }
      for (Results val : results) {
        System.out.println(val.name);
      }
    } else {
      System.out.println("No Results Found");
    }
  }

  public static void main(String[] args) {

    if (args.length != 4) {
      System.out
              .println("\nUsage: java -cp people.jar:lib/* search.IndexSearcher <FinalIndexFile> <SecondaryIndexFile> <BlockSize> <query>\n");
      System.exit(1);
    }

    String indexFileName = null, secIndexFileName = null, query = null;
    int blockSize = -1;
    
    blockSize = 256;

    query = "president of united states";
    query = "ceo of yahoo";
    query = "captain of liverpool football club";

     indexFileName = args[0];
     secIndexFileName = args[1];
     blockSize = Integer.valueOf(args[2]);
     query = args[3];
    
    // Sample dataset ...
//    blockSize = 64;
//    query = "ceo of yahoo";
//    query = "captain of liverpool football club";
//    query = "cofounder of apple";
//    query = "captain of england cricket team";

    try {
      QueryAnswer answer = new QueryAnswer(indexFileName, secIndexFileName,
              blockSize);
      answer.getResults(query);
      answer.close();
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

}
