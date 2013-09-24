package openbrain.peoplesearch.wiki;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class WikiPageExtractor {

  private static String outputDir = null;
  private static int pageCount = 0;

  public WikiPageExtractor() {
  }

  public static void addWikiPage(WikiPage wikiPage) {
    try {
      wikiPage.setId(pageCount+1);
      writeWikiPageToFileInDir(outputDir, wikiPage);
      pageCount++;
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  protected static void writeWikiPageToFileInDir(String outputDir,
          WikiPage wikiPage) throws FileNotFoundException {
    String filename = null;
    if (outputDir.endsWith("/")) {
      filename = outputDir + wikiPage.getId();
    } else {
      filename = outputDir + "/" + wikiPage.getId();
    }
    PrintWriter pw = new PrintWriter(filename);
    pw.println(wikiPage);
    pw.close();
  }

  public void extractPages(String wikiXmlDumpPath, String outputDir)
          throws ParserConfigurationException, SAXException, IOException {
    extractPages(new File(wikiXmlDumpPath), outputDir);
  }

  public void extractPages(File wikiXmlDumpFile, String outputDir)
          throws ParserConfigurationException, SAXException, IOException {
    if (!new File(outputDir).exists()) {
      new File(outputDir).mkdir();
    }
    WikiPageExtractor.outputDir = outputDir;
    SAXParserFactory factory = SAXParserFactory.newInstance();
    SAXParser saxParser = factory.newSAXParser();
    DefaultHandler dh = new WikiPageHandler();
    InputStream inputStream = new FileInputStream(wikiXmlDumpFile);
    Reader reader = new InputStreamReader(inputStream, "UTF-8");
    InputSource is = new InputSource(reader);
    is.setEncoding("UTF-8");
    saxParser.parse(is, dh);
    System.out.println("Total Number of pages extracted :: " + pageCount);
  }

  public static void main(String[] args) {
    WikiPageExtractor wpe = new WikiPageExtractor();
    String wikiXmlDumpPath = null;
    String outputDir = null;
    wikiXmlDumpPath = "data/in/sample.xml";
    outputDir = "data/wikipages/";
    if (args.length != 2) {
      System.out.println("Usage: java WikiPageExtractor <wiki_dump_file> <output_dir>");
      System.exit(1);
    }
    wikiXmlDumpPath = args[0];
    outputDir = args[1];
    try {
      wpe.extractPages(wikiXmlDumpPath, outputDir);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
