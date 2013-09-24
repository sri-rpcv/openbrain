package openbrain.peoplesearch.wiki.index.merge;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;

public class SecondaryIndexer {

  public static void buildSecIndex(String inFilename, String outFilename,
          int blockSize) throws IOException {
    RandomAccessFile priIndexFile = new RandomAccessFile(new File(inFilename),
            "r");
    // BufferedReader priIndex= new BufferedReader(new FileReader ((File)
    // priIndexFile));
    BufferedWriter secIndex = new BufferedWriter(new FileWriter(outFilename));

    long offset = priIndexFile.getFilePointer();
    String curLine = priIndexFile.readLine();
    long linenum = 0;
    while (curLine != null) {
      if (linenum % blockSize == 0) {
        // System.out.println("line--> " + curLine);
        // System.out.println("Offset-->" + offset);
        secIndex.write((curLine.split(" => "))[0] + "#" + offset + "\n");
      }
      offset = priIndexFile.getFilePointer();
      curLine = priIndexFile.readLine();
      linenum++;
    }

    priIndexFile.close();
    secIndex.close();

  }

  public static void main(String[] args) throws IOException {
    if (args.length != 3) {
      System.out.println("\nUsage: java -cp people.jar:lib/* merge.SecondaryIndexer <FinalIndexFile> <OutputFile> <BlockSize>");
      System.exit(1);
    }
    buildSecIndex(args[0], args[1], Integer.valueOf(args[2]));
  }
}
