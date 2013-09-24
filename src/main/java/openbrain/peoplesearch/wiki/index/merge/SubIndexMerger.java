package openbrain.peoplesearch.wiki.index.merge;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.commons.io.FileUtils;

public class SubIndexMerger {
  
  private String mergeIndexDir;
  
  public static final int DEFAULT_BUFFER_SIZE = 10000;  //Number of lines to read
  
  public static final int DEFAULT_BATCH_SIZE = 1000000;
//  public static final int DEFAULT_BATCH_SIZE = 50;
  
  public SubIndexMerger(String indexesDir) throws Exception {
    if (indexesDir.endsWith("/"))
      indexesDir = indexesDir.substring(0, indexesDir.length() - 1);
    if (indexesDir.endsWith("fileindexes"))
      throw new Exception("indexes dir should be the parent directory of \"fileindexes\"");
    this.mergeIndexDir = indexesDir + File.separator + "mergeindexes";
  }
  
  public LinkedList<String> readLinesfromFile(BufferedReader reader)
          throws IOException {
    LinkedList<String> lines = new LinkedList<String>();
    String line;
    for (int i = 0; i < DEFAULT_BUFFER_SIZE; i++) {
      line = reader.readLine();
      if (line == null)
        break;
      lines.add(line);
      // System.out.println(line);
    }
    if (lines.size() == 0)
      return null;
    return lines;
  }
  
  protected String[] getFilesFromMergeDir(File directory) {
    Collection<File> listFiles = FileUtils.listFiles(directory, null, true);
    String files[] = new String[listFiles.size()];
    int i = 0;
    for (File file : listFiles) {
      files[i] = file.getAbsolutePath();
      i++;
    }
    return files;
  }
  
  public void mergeIndexes(String srcDir) throws Exception {
    
    String[] files = null;
    files = getFilesFromMergeDir(new File(srcDir));
    
    // merge indexes into a single index ...
    int iteration = 0;
    int index1 = 0, index2 = 0;
    String mergedDirPrefix = this.mergeIndexDir + File.separator + "merged.iteration-";
    while (files.length != 1) {
      String mergedDirString = mergedDirPrefix + Integer.toString(iteration);
      File mergedDir = new File(mergedDirString);
      if (!mergedDir.exists())
        mergedDir.mkdirs();
      else if (!mergedDir.isDirectory()) {
        System.out.println(mergedDirString + " is not a Directory");
        return;
      }
      System.out.println("Iteration number " + Integer.toString(iteration));
      System.out.println("Merging " + Integer.toString(files.length)
              + " blocks :-)");
      for (int num = 0; num < files.length / 2; num++) {
        BufferedReader block1 = new BufferedReader(new FileReader(new File(
                files[2 * num])));
        BufferedReader block2 = new BufferedReader(new FileReader(new File(
                files[2 * num + 1])));
        BufferedWriter mergedBlock = new BufferedWriter(new FileWriter(
                mergedDirString + File.separator + Integer.toString(num) + ".mergedblk"));

        LinkedList<String> lines1, lines2, mergedLines;
        lines1 = new LinkedList<String>();
        lines2 = new LinkedList<String>();
        mergedLines = new LinkedList<String>();
        int size1 = 0, size2 = 0, size3 = 0, compare;
        String line1, line2;
        String key1, key2;
        while (true) {
          if (size1 == 0) {
            lines1 = readLinesfromFile(block1);
            if (lines1 == null)
              break;
            size1 = lines1.size();
          }

          if (size2 == 0) {
            lines2 = readLinesfromFile(block2);
            if (lines2 == null)
              break;
            size2 = lines2.size();
          }

          while (size1 != 0 && size2 != 0) {

            line1 = lines1.get(0).trim();
            line2 = lines2.get(0).trim();
            index1 = line1.indexOf(" => ");
            index2 = line2.indexOf(" => ");
            key1 = line1.substring(0, index1);
            key2 = line2.substring(0, index2);
            compare = key1.compareTo(key2);

            if (compare == 0) {
              mergedLines.add(key1 + " => " + line1.substring(index1+4) + "#" + line2.substring(index2+4));
              lines1.removeFirst();
              lines2.removeFirst();
              size1 += -1;
              size2 += -1;
              size3 += 1;
            } else if (compare < 0) {
              mergedLines.add(line1);
              lines1.removeFirst();
              size1 += -1;
              size3 += 1;
            } else {
              mergedLines.add(line2);
              lines2.removeFirst();
              size2 += -1;
              size3 += 1;
            }
          }

          if (size3 > 2 * DEFAULT_BUFFER_SIZE) {
            for (String line : mergedLines) {
              mergedBlock.write(line + "\n");
            }
            size3 = 0;
            mergedLines = new LinkedList<String>();
          }
        }

        for (String line : mergedLines) {
          mergedBlock.write(line + "\n");
        }

        while (lines1 != null) {
          for (String line : lines1) {
            mergedBlock.write(line + "\n");
          }
          lines1 = readLinesfromFile(block1);
        }
        while (lines2 != null) {
          for (String line : lines2) {
            mergedBlock.write(line + "\n");
          }
          lines2 = readLinesfromFile(block2);
        }

        mergedLines = null;
        mergedBlock.close();
        block1.close();
        block2.close();
      }

      if (files.length % 2 == 1) {
        FileUtils.copyFileToDirectory((new File(files[files.length - 1])),
                mergedDir);
      }

      FileUtils.deleteDirectory(new File(mergedDirPrefix
              + Integer.toString(iteration - 1)));
      files = getFilesFromMergeDir(mergedDir);
      iteration += 1;
    }

    String finalMergedIndex = this.mergeIndexDir + File.separator + "final.idx";
    String command = "mv " + mergedDirPrefix + Integer.toString(iteration - 1)
            + "/0.mergedblk " + finalMergedIndex;
    System.out.println(command);
    Runtime.getRuntime().exec(command);
  }

  public static void main(String[] args) {
    
    if (args.length != 2) {
      System.out.println("\nUsage: java -cp people.jar:lib/* merge.SubIndexMerger <indexesDir> <srcDir>\n");
      System.exit(1);
    }
    
    String indexesDir = null, srcDir = null;
    indexesDir = "";

    indexesDir = args[0];
    srcDir = args[1];
    
    SubIndexMerger merger = null;
    try {
      merger = new SubIndexMerger(indexesDir);
      merger.mergeIndexes(srcDir);
    } catch (Exception e) {
      e.printStackTrace();
    }
    
  }
  
}
