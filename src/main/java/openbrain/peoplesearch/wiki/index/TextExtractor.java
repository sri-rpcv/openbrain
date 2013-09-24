package openbrain.peoplesearch.wiki.index;

import java.io.BufferedReader;
import java.io.FileReader;

public class TextExtractor {

  public String text = "";
  public String title = "";
  private boolean start = false;
  private boolean end = false;

  public String getTitle(String title) {
    title = title.replaceAll("\t", " ");
    title = title.replaceAll("\n", " ");
    String[] arr = title.split(" ");
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < arr.length; i++) {
      if (arr[i].trim().equals(""))
        continue;
      sb.append(arr[i]);
      sb.append(" ");
    }
    title = sb.toString().trim();
    sb.delete(0, sb.length()); // clear memory
    return title;
  }
  
  public boolean ignore(String line) {
    line = line.trim();
    if (line.startsWith("<title>"))
      start = true;
    if (line.endsWith("</title>"))
      end = true;
    if (start && end) {
      line = line.replaceAll("<title>", "");
      line = line.replaceAll("</title>", "");
      title = title + " " + line + " ";
      title = getTitle(title);
      start = end = false;
      return true;
    } else if (start) {
      line = line.replaceAll("<title>", "");
      title = title + " " + line + " ";
      return true;
    } else if (end) {
      line = line.replaceAll("</title>", "");
      title = title + " " + line + " ";
      title = getTitle(title);
      start = end = false;
      return true;
    }
    if (line.startsWith("<page>") || line.endsWith("</page>"))
      return true;
    if (line.startsWith("<id>"))
      return true;
    if (line.startsWith("<text>") || line.endsWith("</text>"))
      return true;
    return false;
  }
  
  public String extractText(String filename) throws Exception {
    text = "";
    title = "";
    BufferedReader br = new BufferedReader(new FileReader(filename));
    String line = "";
    while( (line = br.readLine())  != null) {
      if (! (ignore(line)) )
        text = text + " " + line + " ";
    }
    br.close();
    return text;
  }
  
}
