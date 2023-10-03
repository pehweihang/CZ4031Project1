import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

public class NBADatabase {
  private Disk disk;

  private Node root;
  private int numberOfLayers;

  public NBADatabase() {
    this.disk = new Disk();
  }

  public void loadFromFile(String path) {
    this.disk.initWithData(path);
  }

  public void bulkLoad() {
    System.out.println("\nBulk Loading in progress...");
    ArrayList<Record> allRecords = this.disk.getRecords();

    Collections.sort(allRecords, new SortingFunction());

    ArrayList<Node> NodeArrayList = new ArrayList<Node>();
    LeafNode prev = null;

    for (int i = 0; i < allRecords.size(); i = i + 39) {
      int blockSize = Math.min(39, allRecords.size() - i);
      short[] keys = new short[blockSize];
      Record[] records = new Record[blockSize];
      for (int j = 0; j < blockSize; j++) {
        keys[j] = PctCompressor.compress(allRecords.get(i + j).getFgPctHome());
        records[j] = allRecords.get(i + j);
      }
      LeafNode cur = new LeafNode(keys, records, prev, null);
      NodeArrayList.add(cur);
      if (prev != null) prev.setNextLeafNode(cur);
      prev = cur;
    }

    System.out.println("No of leaf nodes: " + NodeArrayList.size() + " leaf nodes");
    this.root = recurseBPlusTree(NodeArrayList);
    System.out.println("Bulk Loading is complete. \n");
  }

  class SortingFunction implements Comparator<Record> {
    public int compare(Record a, Record b) {
      return Double.compare(a.getFgPctHome(), b.getFgPctHome());
    }
  }

  public Node recurseBPlusTree(ArrayList<Node> al) {
    Node returnNode = new Node(new short[1], new Node[1]);
    ArrayList<Node> newArrayList = new ArrayList<Node>();
    for (int i = 0; i < al.size(); i = i + 40) {
      int blockSize = Math.min(40, al.size() - i);
      short[] keys = new short[blockSize - 1];
      Node[] children = new Node[blockSize];
      Node root = new Node(keys, children);
      returnNode = root;
      newArrayList.add(root);

      for (int j = 0; j < blockSize; j++) {
        Node node = al.get(i + j);
        children[j] = node;
        node.setParent(root);
        if (j != 0) {
          Node temp = node;
          while (!temp.getIsLeafNode()) {
            temp = temp.getChildren()[0];
          }
          keys[j - 1] = temp.getKeys()[0];
        }
      }
    }

    if (al.size() > 40) {
      System.out.println(
          "Layer " + this.getNumberOfLayers() + " has " + newArrayList.size() + " nodes.");
      this.incNumberOfLayers();
      return recurseBPlusTree(newArrayList);
    } else {
      this.incNumberOfLayers();
      System.out.println(
          "There are " + this.getNumberOfLayers() + " layers, including the root node layer.");
      return returnNode;
    }
  }

  public void experiment3Linear() {
    final long start = System.nanoTime();
    double totalValue = 0;
    int recordCount = 0;
    int blockCount = 0;

    for (int i = 0; i < this.disk.getBlocks().length; i++) {
      if (this.disk.getBlocks()[i] == null) {
        continue;
      }
      blockCount++;
      for (int j = 0; j < this.disk.getBlocks()[i].getRecords().length; j ++) {
        if (this.disk.getBlocks()[i].getRecords()[j] == null) {
          continue;
        }
        if (this.disk.getBlocks()[i].getRecords()[j].getFgPctHome() == 0.5) {
          recordCount++;
          totalValue += this.disk.getBlocks()[i].getRecords()[j].getFg3PctHome();
        }
      }
    }

    final long end = System.nanoTime();
    System.out.println("\nUsing linear scan: ");
    System.out.println("Number of data blocks accessed: " + blockCount);
    System.out.println("Linear scan time taken: " + ((end - start) * Math.pow(10, -6)));
    System.out.println("'FG3_PCT_home' average: " + totalValue/recordCount);
  }




  public int getNumberOfLayers() {
    return this.numberOfLayers;
  }

  public Node getRoot() {
    return this.root;
  }

  public int getNumberOfBlocks() {
    return this.disk.getNumberOfBlocks();
  }

  public int getNumberOfRecords() {
    return this.disk.getNumberOfRecords();
  }

  public int getSizeOfRecord() {
    return this.disk.getSizeOfRecord();
  }

  public int getRecordsInBlock() {
    return this.disk.getRecordsInBlock();
  }

  public void getRootNodeKeys() {
    System.out.print("Root Node Keys: ");
    for (int key : this.root.getKeys()) {
      if (key != 0)
        System.out.print(key + " ");
    }
    System.out.println();
  }

  // setters
  public void incNumberOfLayers() {
    this.numberOfLayers++;
  }
}
