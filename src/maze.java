import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

/* INSTRUCTIONS/GUIDE TO OUR GAME:
 * - you are red, your 'snail trail' is pink
 * - use up, down, left, right keys to move
 * - you're goal is to reach the blue cell at the end of the maze (bottom right corner)
 * - press a to see the answer path in orange
 * - press b to show the breadth first search in blue gray
 * - press d to show the depth first search in blue gray
 * - as long as the maze isn't solved, press r to restart a new maze
 * !!GOOD LUCK!!
 */

/* TEST GAMES AVAILABLE FOR PLAY
 * - testGame : 27 x 14
 * - testGame1 : 10 x 5
 * - testGame2 : 100 x 60
 * - testGame3 : 3 x 4
 * - testGame : 2 x 2
 */


// Class to represent the users game play
class User {
  // Users current cell
  Cell current;
  // List of cells the user visited
  ArrayList<Cell> visited;

  // General constructor
  User(Cell current, ArrayList<Cell> visited) {
    this.current = current;
    this.visited = visited;
  }

  // Convenience Constructor
  User() {
    this.current = null;
    this.visited = new ArrayList<Cell>();
  }

  // EFFECT: update the current position of this user given a cell
  // EFFECT: update the given cell to be visited
  void updateCurrent(Cell newCurrent) {
    // if the current position is not in visited, add it
    if (!this.visited.contains(this.current)) {
      this.visited.add(this.current);
    }
    // add the new position to visited and mutate current position
    this.visited.add(0, newCurrent);
    this.current = newCurrent;
    newCurrent.isVisted = true;
  }
}

// Class to represent the cells in our game 
class Cell {
  // In logical coordinates, with the origin at the top-left corner of the screen
  int x;
  int y;
  // The four adjacent edges to this one
  Edge left;
  Edge top;
  Edge right;
  Edge bottom;
  // Color of the cell
  Color color;
  // the edges exiting this cell
  ArrayList<Edge> outEdges;

  // whether the right and bottom edges are blocked
  boolean rightWall = true;
  boolean bottomWall = true;
  // has this cell been visited by the user?
  boolean isVisted = false;

  Cell(int x, int y) {
    this.x = x;
    this.y = y;
    this.color = Color.gray;
    this.outEdges = new ArrayList<Edge>();
  }

  // EFFECT: Sets the adjacent edges for this cell
  void setAdjacents(Edge left, Edge top, Edge right, Edge bottom) {
    this.left = left;
    this.top = top;
    this.right = right;
    this.bottom = bottom;
  }

  // EFFECT: Sets the color of this cell
  void setColor(Color c) {
    this.color = c;
  }

  // Draws this cell scaled up by the given size
  WorldImage drawCell(int cellSize) {

    LineImage rightWallImg = new LineImage(new Posn(0, cellSize), Color.black);
    LineImage bottomWallImg = new LineImage(new Posn(cellSize, 0), Color.black);

    RectangleImage cellImg = new RectangleImage(cellSize, cellSize, OutlineMode.SOLID, this.color);
    WorldImage result = cellImg;

    // if the right edge is blocked, add a wall image
    if (this.rightWall) {
      result = new OverlayOffsetImage(rightWallImg, (cellSize * -0.5) + 1, 0, result)
          .movePinholeTo(new Posn(0, 0));

    }

    // if the bottom edge is blocked, add a wall image
    if (this.bottomWall) {
      result = new OverlayOffsetImage(bottomWallImg, 0, (cellSize * -0.5) + 1, result)
          .movePinholeTo(new Posn(0, 0));

    }

    return result.movePinholeTo(new Posn(0, 0));
  }
}

// Class to represent our edges
class Edge {
  // Where the edge of the cell is from
  Cell from;
  // Where the edge of the cell is going to
  Cell to;
  int weight;

  Edge(Cell from, Cell to) {
    this.from = from;
    this.to = to;
  }

  // EFFECT: sets the weight of each edge to the given value
  void setWeight(int w) {
    this.weight = w;
  }
}

// Class to represent our game world 
class Maze extends World {
  // list of cells
  ArrayList<Cell> cells;
  // Width of game
  int width;
  // Height of game
  int height;
  // Cell size for the maze
  int cellSize;

  // Colors for this game
  Color trailColor = new Color(255, 153, 153);
  Color currentColor = new Color(255, 51, 51);

  // HashMap used for union find algorithm
  HashMap<Cell, Cell> ufMap;
  // spanning tree produced by union find/krusgal's algorithm
  ArrayList<Edge> ufTree;
  // to represent the player
  User user;
  // to represent the right and bottom edges connecting the cells in this maze
  ArrayList<Edge> edges;
  // path for the answer
  ArrayList<Cell> answerPath;

  // ArrayList representing the search path for DFS and BFS, respectively
  ArrayList<Cell> dfsPath;
  ArrayList<Cell> bfsPath;

  // booleans representing whether the user wants to see the search paths for
  // DFS and BFS, respectively
  boolean showDFS;
  boolean showBFS;
  boolean showAnswer;

  Maze(int width, int height, User user) {
    this.width = width;
    this.height = height;
    this.user = user;
    // set the cell size for this maze
    // NOTE: FIT FOR 2019 MACBOOK PRO 13-INCH
    // (MAY NOT WORK FOR DIFFERENT YEAR MACS AND OTHER DEVICE SIZES)
    // ADJUST CELL SIZE OF 13 TO SMALLER IF NEEDED
    if (this.height <= 15 && this.width <= 28) {
      this.cellSize = 50;
    }
    else if (this.height > 15 || this.width > 28) {
      this.cellSize = 13;
    }

    // initialize the maze attributes and build the game
    this.initMaze();

    // initialize the player to the start cell of the maze
    this.initPlayer();
  }

  // reset/initialize the maze attributes and build the game
  void initMaze() {
    // for resetting purposes, making everything empty again
    this.ufMap = new HashMap<Cell, Cell>();
    this.ufTree = new ArrayList<Edge>();
    this.edges = new ArrayList<Edge>();

    // for resetting purposes, make everything false again just in case
    this.showDFS = false;
    this.showBFS = false;
    this.showAnswer = false;

    // build all the cells (blocks) for this maze
    this.cells = this.buildCells(this.width, this.height);
    // connect all the cells in this maze
    this.setAdjacents();
    // set random weights to the edges connecting the cells
    this.setRandomWeights();
    // find the MST to connect all the cells in this maze
    this.unionFind();
    // if the edge is not in the MST, draw a wall to cut off the edge
    this.setCellWalls();
    // if the edge is not in the MST, add it to the cell's outEdges
    this.setCellOutEdges();

    // set the colors of the start and goal cells
    this.cells.get(this.cells.size() - 1).color = Color.blue;
  }

  // initialize the player to the start cell of the maze
  void initPlayer() {
    this.user = new User();
    this.user.updateCurrent(this.cells.get(0));
  }

  // builds the board for this game given the size
  ArrayList<Cell> buildCells(int width, int height) {
    ArrayList<Cell> result = new ArrayList<Cell>(width * height);
    // Build the board
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        Cell c = new Cell(x, y);
        result.add(c);
      }
    }
    return result;
  }

  // EFFECT: Sets the adjacent cells for all cells on this board & adds down and
  // right edges for each cell to this game's list of edges
  void setAdjacents() {
    int currentIndex = 0;
    Cell currentCell;

    for (int y = 0; y < this.height; y++) {
      for (int x = 0; x < this.width; x++) {
        currentIndex = (y * this.width) + x;
        currentCell = this.cells.get(currentIndex);

        // if not in the leftmost column
        if (currentCell.x > 0) {
          Edge left = new Edge(currentCell, this.cells.get(currentIndex - 1));
          currentCell.left = left;
        }
        // if not in the topmost row
        if (currentCell.y > 0) {
          Edge top = new Edge(currentCell, this.cells.get(currentIndex - this.width));
          currentCell.top = top;
        }
        // if not in the rightmost column
        if (currentCell.x < this.width - 1) {
          Edge right = new Edge(currentCell, this.cells.get(currentIndex + 1));
          currentCell.right = right;

          this.edges.add(currentCell.right);
        }
        // if not on the bottom most row
        if (currentCell.y < this.height - 1) {
          Edge bottom = new Edge(currentCell, this.cells.get(currentIndex + this.width));
          currentCell.bottom = bottom;

          this.edges.add(currentCell.bottom);
        }
      }
    }
  }

  // EFFECT: assigns appropriate boolean value to each cell's right and bottom
  // wall attribute
  void setCellWalls() {
    // if the edge is in the MST, add to the cell's outEdges
    for (Cell c : this.cells) {
      // if the edge from the current cell to it's right cell is in the spanning tree,
      // there should not be a wall
      if (this.ufTree.contains(c.right)) {
        c.rightWall = false;
      }

      // if the edge from the current cell to it's bottom cell is in the spanning
      // tree,
      // there should not be a wall
      if (this.ufTree.contains(c.bottom)) {
        c.bottomWall = false;
      }
    }
  }

  // EFFECT: if the adjacent edges of the cell is not in the MST, add it to it's
  // outEdges
  void setCellOutEdges() {
    for (Cell c : this.cells) {
      // if the MST contains this cells right edge, add it to it's outEdges
      // also, add the opposite edge to the toCell's outEdges.
      if (this.ufTree.contains(c.right)) {
        Cell toCell = c.right.to;
        c.outEdges.add(c.right);
        toCell.outEdges.add(toCell.left);
      }
      // if the MST contains this cells bottom edge, add it to it's outEdges
      // also, add the opposite edge to the toCell's outEdges.
      if (this.ufTree.contains(c.bottom)) {
        Cell toCell = c.bottom.to;
        c.outEdges.add(c.bottom);
        toCell.outEdges.add(toCell.top);
      }
    }
  }

  // EFFECT: Randomly sets the weight of each edge
  void setRandomWeights() {
    // FOR TESTING!!!!!!!, we use a Random seed of 5
    // Random r = new Random(5);
    Random r = new Random();
    for (Edge e : this.edges) {
      e.setWeight(r.nextInt(50));
    }
  }

  // Makes a hash map using this games cells where each cell references itself
  HashMap<Cell, Cell> initMap(ArrayList<Edge> wl) {
    HashMap<Cell, Cell> result = new HashMap<Cell, Cell>();
    // for every single edge in this maze, add it's cells to the map
    for (int i = 0; i < wl.size(); i++) {
      Cell from = wl.get(i).from;
      Cell to = wl.get(i).to;

      result.put(from, from);
      result.put(to, to);
    }
    return result;
  }

  // Effect: Creates a spanning tree using the union find and krusgal's algorithm
  void unionFind() {
    // define a work list for the unionFind, assign to it a copy of our list of
    // edges, but sorted by weight
    ArrayList<Edge> worklist = new MazeUtils().sortEdges(this.edges);
    this.ufMap = this.initMap(worklist);
    int i = 0;
    // While number of edges in the tree is less than the number of cells - 1 &&
    // they are still edges to look through
    while (this.ufTree.size() < this.cells.size() - 1 && i < worklist.size()) {
      // Get the from and to cells from the current edge
      Cell from = worklist.get(i).from;
      Cell to = worklist.get(i).to;

      // If the Cells are in the same group move on to the next edge
      if (this.find(this.ufMap, to).equals(this.find(this.ufMap, from))) {
        i += 1;
      }

      // if not, add this current edge to the spanning tree && and set cells to and
      // from to be in the same group
      else {
        this.ufTree.add(worklist.remove(i));
        // set to's hash map value to from's hash map value
        this.unionGroup(this.ufMap, to, from);
      }
    }
  }

  // Find the Cell of the given key in the given map
  Cell find(HashMap<Cell, Cell> hm, Cell key) {
    Cell rep = hm.get(key);
    while (!hm.get(rep).equals(rep)) {
      rep = hm.get(rep);
    }
    return rep;
  }

  // EFFECT: set the given first cell's representative to the given second cell's
  // representative
  // in the given map
  void unionGroup(HashMap<Cell, Cell> hm, Cell first, Cell second) {
    hm.put(this.find(hm, first), this.find(hm, second));
  }

  // Return a search path using dfs or bfs using the given ICollection<Cell> data
  // type
  ArrayList<Cell> searchPath(Cell start, Cell end, ICollection<Cell> worklist) {
    HashMap<Cell, Cell> cameFromEdge = new HashMap<Cell, Cell>();
    ArrayList<Cell> alreadySeen = new ArrayList<Cell>();
    worklist.add(start);

    while (worklist.size() > 0) {
      Cell next = worklist.remove();

      if (alreadySeen.contains(next)) {
        // do nothing
      }
      else if (next == end) {
        break;
      }
      else {
        for (Edge e : next.outEdges) {
          worklist.add(e.to);
          cameFromEdge.put(next, e.to);
        }
        alreadySeen.add(next);
      }
    }
    // how do animate this and convert our cameFromEdge into an ArrayList<Cell>
    return alreadySeen;

  }

  // Find the answer path using dijkstra's algorithm - referencing lecture 31
  ArrayList<Cell> answerPath(Cell start, Cell end) {
    ArrayList<Cell> unvisited = new ArrayList<Cell>();
    HashMap<Cell, Integer> distances = new HashMap<Cell, Integer>();
    HashMap<Cell, Cell> predecessors = new HashMap<Cell, Cell>();

    // set the distance of the start cell to 0
    unvisited.add(start);
    distances.put(start, 0);

    // while there are still unvisited cells to look through
    while (unvisited.size() > 0) {
      Cell c = unvisited.remove(0);

      // For edges coming out of the current cell
      for (Edge e : c.outEdges) {
        // if it is unvisited or has a larger distance than this potential path
        if (distances.get(e.to) == null || distances.get(e.to) > distances.get(c) + e.weight) {

          // update the distance and update predecessor
          distances.put(e.to, distances.get(c) + e.weight);
          predecessors.put(e.to, c);

          // add to be examined later on
          unvisited.add(e.to);
        }
      }
    }
    ArrayList<Cell> answer = new ArrayList<Cell>();
    Cell step = end;
    // if there was not a path to the end yet
    if (predecessors.get(step) == null) {
      // return empty list
      return answer;

    }

    answer.add(step);

    // while we haven't reached the start
    while (step != start) {
      // add the cell before to the answer path
      step = predecessors.get(step);
      answer.add(0, step);
    }
    return answer;
  }

  // visualizes the current game scene
  public WorldScene makeScene() {

    WorldScene current = new WorldScene(this.width * this.cellSize, this.height * this.cellSize);

    WorldImage curMaze = this.drawCurrentBoard();

    current.placeImageXY(curMaze, (this.cellSize * (this.width)) / 2,
        (this.cellSize * (this.height)) / 2);
    return current;
  }

  // visualizes the ending scene of the game
  public WorldScene makeEndScene() {
    // show the answer path
    this.answerPath = this.answerPath(this.cells.get(0), this.cells.get(this.cells.size() - 1));
    for (Cell c : this.answerPath) {
      c.setColor(Color.orange);
    }
    WorldScene current = this.makeScene();

    WorldImage message = new TextImage("The maze is solved!", 20, Color.black)
        .movePinholeTo(new Posn(0, 0));

    current.placeImageXY(message, (this.cellSize * (this.width)) / 2,
        (this.cellSize * (this.height)) / 2);

    return current;
  }

  // Draws this current cell configuration
  WorldImage drawCurrentBoard() {
    // Accumulator: the board image so far
    WorldImage boardAcc = new EmptyImage();
    // Accumulator: the row image so far
    WorldImage rowAcc = new EmptyImage();
    for (int y = 0; y < height; y++) {
      rowAcc = new EmptyImage();
      for (int x = 0; x < width; x++) {
        int currentIndex = (y * width) + x;
        Cell currentCell = this.cells.get(currentIndex);

        rowAcc = new BesideImage(rowAcc, currentCell.drawCell(this.cellSize));
      }
      boardAcc = new AboveImage(boardAcc, rowAcc);
    }

    return boardAcc.movePinholeTo(new Posn(0, 0));
  }

  // show the answer path on the board
  void showAnswer() {
    this.answerPath = this.answerPath(this.cells.get(0), this.cells.get(this.cells.size() - 1));
    this.showAnswer = true;
  }

  // animate and show the breadth first search
  void showBreadthFirst() {
    this.bfsPath = this.searchPath(this.cells.get(0), this.cells.get(this.cells.size() - 1),
        new Queue<Cell>(new Deque<Cell>()));
    this.showBFS = true;
  }

  // animate and show the depth first search
  void showDepthFirst() {
    this.dfsPath = this.searchPath(this.cells.get(0), this.cells.get(this.cells.size() - 1),
        new Stack<Cell>(new Deque<Cell>()));
    this.showDFS = true;
  }

  // EFFECT: update the visited cell colors
  void updateVisitedColors() {
    this.cells.get(this.cells.size() - 1).setColor(Color.blue);
    for (Cell c : this.cells) {
      if (c.isVisted) {
        c.setColor(this.trailColor);
      }
    }
    this.user.current.setColor(this.currentColor);
  }

  // EFFECT: update the current position of the user to the given Cell
  void moveUser(Cell next, Edge connection) {
    if (this.ufTree.contains(connection)) {
      this.user.updateCurrent(next);
    }
  }

  // EFFECT: update the status of this maze
  void updateMaze() {

    // update the visited cell colors
    this.updateVisitedColors();

    // if showBFS is true
    if (this.showBFS) {
      Cell temp = this.bfsPath.remove(0);
      temp.setColor(new Color(102, 153, 153));
      if (this.bfsPath.size() == 0) {
        this.showBFS = false;
      }
    }

    // if showDFS is true
    if (this.showDFS) {
      Cell temp = this.dfsPath.remove(0);
      temp.setColor(new Color(102, 153, 153));
      if (this.dfsPath.size() == 0) {
        this.showDFS = false;
      }
    }

    // if showAnswer is true
    if (this.showAnswer) {
      Cell temp = this.answerPath.remove(0);
      temp.setColor(Color.orange);
      if (this.answerPath.size() == 0) {
        this.showAnswer = false;
      }
    }
  }

  // on tick big-bang method
  public void onTick() {
    this.updateMaze();
  }

  // EFFECT: inflict the appropriate change to this maze given the pressed key
  public void onKeyEvent(String key) {
    int currentIndex = (this.user.current.y * this.width) + this.user.current.x;
    Cell choice;
    Edge edge;
    if (key.equals("a")) {
      this.showAnswer();
    }
    if (key.equals("b")) {
      this.showBreadthFirst();
    }
    if (key.equals("d")) {
      this.showDepthFirst();
    }
    if (key.equals("r")) {
      this.initMaze();
      this.initPlayer();
    }
    if (key.equals("up") && this.user.current.y > 0) {
      choice = this.cells.get(currentIndex - this.width);
      edge = choice.bottom;
      this.moveUser(choice, edge);
    }
    if (key.equals("down") && this.user.current.y < this.height - 1) {
      choice = this.cells.get(currentIndex + this.width);
      edge = this.user.current.bottom;
      this.moveUser(choice, edge);
    }
    if (key.equals("right") && this.user.current.x < this.width - 1) {
      choice = this.cells.get(currentIndex + 1);
      edge = this.user.current.right;
      this.moveUser(choice, edge);
    }
    if (key.equals("left") && this.user.current.x > 0) {
      choice = this.cells.get(currentIndex - 1);
      edge = choice.right;
      this.moveUser(choice, edge);
    }
    else {
      // ignore the key
    }
  }

  // EFFECT: ends the game when the user reaches the goal
  public WorldEnd worldEnds() {
    if (this.user.current == this.cells.get(this.cells.size() - 1)) {
      return new WorldEnd(true, this.makeEndScene());
    }
    else {
      return new WorldEnd(false, this.makeScene());
    }
  }
}

// class to hold utility methods for this gameWorld
class MazeUtils {
  // returns the given list but sorts the edges in the list of edges by their
  // weight from smallest to largest (using quick sort and does not mutate the
  // given list!)
  ArrayList<Edge> sortEdges(ArrayList<Edge> sourceEdges) {
    ArrayList<Edge> result = this.copy(sourceEdges);
    sortEdgesHelp(result, 0, result.size());
    return result;
  }

  // EFFECT: sorts the list of edges by weight, in the range of indices
  // [low, high)
  void sortEdgesHelp(ArrayList<Edge> edges, int low, int high) {
    // check for completion
    if (low >= high) {
      return; // no items to sort
    }

    // select pivot
    Edge pivot = edges.get(low);

    // partition edges to lower or upper portions
    int pivotIdx = partition(edges, low, high, pivot);

    // sort both halves of the list
    sortEdgesHelp(edges, low, pivotIdx);
    sortEdgesHelp(edges, pivotIdx + 1, high);
  }

  // Returns the index where the pivot edge ends up in the sorted list of edges
  // EFFECT: makes it so all edges on the left of the pivot are 'lighter' than the
  // edges to the right
  int partition(ArrayList<Edge> edges, int low, int high, Edge pivot) {
    int curLow = low;
    int curHigh = high - 1;

    while (curLow < curHigh) {
      // increment curLo until we find an Edge that is too big
      while (curLow < high && edges.get(curLow).weight <= pivot.weight) {
        curLow += 1;
      }

      // increment curHigh until we find an edge that is too small
      while (curHigh >= low && edges.get(curHigh).weight > pivot.weight) {
        curHigh -= 1;
      }

      if (curLow < curHigh) {
        Collections.swap(edges, curLow, curHigh);
      }
    }
    Collections.swap(edges, low, curHigh);
    return curHigh;
  }

  // creates a copy of the given list
  <T> ArrayList<T> copy(ArrayList<T> source) {
    ArrayList<T> result = new ArrayList<T>();
    for (T t : source) {
      result.add(t);
    }
    return result;
  }
}

class ExamplesMaze {

  ArrayList<Edge> edges1;
  Edge e1;
  Edge e2;
  Edge e3;
  Edge e4;

  Cell c1;
  Cell c2;

  Maze testGame;
  Maze testGame1;
  Maze testGame2;
  Maze testGame3;
  Maze testGame4;

  User user;

  void initData() {
    // Initializing the user
    this.user = new User(null, null);

    // Initializing the game
    this.testGame = new Maze(27, 14, this.user);
    this.testGame1 = new Maze(10, 5, this.user);
    this.testGame2 = new Maze(100, 60, this.user);
    this.testGame3 = new Maze(3, 4, this.user);
    this.testGame4 = new Maze(2, 2, this.user);

    // Initializing the edges
    this.e1 = new Edge(null, null);
    this.e1.weight = 5;
    this.e2 = new Edge(null, null);
    this.e2.weight = 2;
    this.e3 = new Edge(null, null);
    this.e3.weight = 10;
    this.e4 = new Edge(null, null);
    this.e4.weight = 3;

    edges1 = new ArrayList<Edge>(Arrays.asList(this.e1, this.e2, this.e3, this.e4));

    // Initializing the cells
    this.c1 = new Cell(0, 0);
    this.c2 = new Cell(4, 5);
  }

  // test for updateCurrentCell
  void testUpdateCurrentCell(Tester t) {
    this.initData();

    t.checkExpect(this.testGame4.user.current, this.testGame4.cells.get(0));

    this.testGame4.user.updateCurrent(this.testGame4.cells.get(1));

    t.checkExpect(this.testGame4.user.current, this.testGame4.cells.get(1));
  }

  // test for setColor
  void testSetColor(Tester t) {
    this.initData();
    Cell testCell = new Cell(0, 1);
    t.checkExpect(testCell.color, Color.gray);
    testCell.setColor(Color.black);
    t.checkExpect(testCell.color, Color.black);
  }

  // test for setWeight
  void testSetWeight(Tester t) {
    this.initData();
    Edge testEdge = new Edge(this.testGame4.cells.get(0), this.testGame4.cells.get(1));
    t.checkExpect(testEdge.weight, 0);
    testEdge.setWeight(3);
    t.checkExpect(testEdge.weight, 3);
  }

  // test for buildCells
  void testBuildCells(Tester t) {
    this.initData();
    Maze testMaze = new Maze(2, 4, this.user);
    t.checkExpect(testMaze.cells.size(), 2 * 4);
  }

  // test setCellWalls using random seed of 5
  /*void testCellWalls(Tester t) {
    this.initData();
    t.checkExpect(this.testGame4.cells.get(0).rightWall, false);
    t.checkExpect(this.testGame4.cells.get(0).bottomWall, true);
    t.checkExpect(this.testGame4.cells.get(1).rightWall, true);
    t.checkExpect(this.testGame4.cells.get(1).bottomWall, false);
  }*/

  // test setCellOutEdges using random seed of 5
  /*void testSetCellOutEdges(Tester t) {
    this.initData();

    t.checkExpect(this.testGame4.cells.get(0).outEdges.contains(
    this.testGame4.cells.get(0).right), true);
    t.checkExpect(this.testGame4.cells.get(0).outEdges.contains(
    this.testGame4.cells.get(0).bottom), false); 
  }*/

  // test setRandomWeights
  /*void testSetRandomWeights(Tester t) {
    this.initData();

    t.checkExpect(this.testGame4.cells.get(0).right.weight, 37);
    t.checkExpect(this.testGame4.cells.get(1).left.weight, 0);
    t.checkExpect(this.testGame4.cells.get(2).top.weight, 0);

  }*/

  // test moveUser using random seed of 5
  /*void testMoveUser(Tester t) {
    this.initData();

    t.checkExpect(this.testGame4.user.current, this.testGame4.cells.get(0));
    t.checkExpect(this.testGame4.user.visited.contains(this.testGame4.cells.get(0)), true);
    // check that user can't go to a cell blocked by a wall
    this.testGame4.moveUser(this.testGame4.cells.get(2), this.testGame4.cells.get(0).bottom);
    t.checkExpect(this.testGame4.user.current, this.testGame4.cells.get(0));
    t.checkExpect(this.testGame4.user.visited.contains(this.testGame4.cells.get(0)), true);
    // check that user can go to the right 
    this.testGame4.moveUser(this.testGame4.cells.get(1), this.testGame4.cells.get(0).right);
    t.checkExpect(this.testGame4.user.current, this.testGame4.cells.get(1));
    t.checkExpect(this.testGame4.user.visited.contains(this.testGame4.cells.get(1)), true);  
  }*/

  // test makeEndScene using a random seed of 5
  /*void testMakeEndScene(Tester t) {
    this.initData();

    WorldScene result = new WorldScene(this.testGame4.width * this.testGame4.cellSize,
        this.testGame4.height * this.testGame4.cellSize);

    WorldImage black = new RectangleImage(100, 100,
        OutlineMode.OUTLINE,
        Color.black);

    WorldImage resultBoard = new AboveImage(
        new AboveImage(
            new EmptyImage(),
             new BesideImage(
              new BesideImage(
               new EmptyImage(),
                new OverlayOffsetImage(
                 new LineImage(new Posn(50, 0),
                  Color.black),
                 0.0, -24.0,
                 new RectangleImage(50, 50,
                  OutlineMode.SOLID,
                  Color.orange))),
               new OverlayOffsetImage(
                new LineImage(new Posn(0, 50),
                 Color.black),
                -24.0, 0.0,
                new RectangleImage(50, 50,
                 OutlineMode.SOLID,
                 Color.orange)))),
             new BesideImage(
              new BesideImage(
               new EmptyImage(),
                new OverlayOffsetImage(
                 new LineImage(new Posn(50, 0),
                  Color.black),
                 0.0, -24.0,
                 new RectangleImage(50, 50,
                  OutlineMode.SOLID,
                  Color.gray))),
               new OverlayOffsetImage(
                new LineImage(new Posn(50, 0),
                 Color.black),
                0.0, -24.0,
                new OverlayOffsetImage(
                 new LineImage(new Posn(0, 50),
                  Color.black),
                 -24.0, 0.0,
                 new RectangleImage(50, 50,
                  OutlineMode.SOLID,
                  Color.orange)))));

    WorldImage finalText = new TextImage("The maze is solved!",
        20.0, FontStyle.REGULAR,
        Color.black);

    result.placeImageXY(black, 50, 50);
    result.placeImageXY(resultBoard, 50, 50);
    result.placeImageXY(finalText, 50, 50);

    t.checkExpect(this.testGame4.makeEndScene(), result);
  }*/

  // tests for drawCell
  void testDrawCell(Tester t) {  
    // initialize the data
    this.initData();
    // make local variables
    WorldImage cellExample = new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray);
    WorldImage cellExampleRightWall = new OverlayOffsetImage(
        new LineImage(new Posn(0, 50), Color.black),
        (50 * -0.5) + 1, 0, cellExample)
        .movePinholeTo(new Posn(0, 0));
    WorldImage cellExampleRightBottomWall = new OverlayOffsetImage(
        new LineImage(new Posn(50, 0), Color.black), 
        0, (50 * -0.5) + 1, cellExampleRightWall)
        .movePinholeTo(new Posn(0, 0));

    // test before mutation
    t.checkExpect(this.c1.drawCell(50), cellExampleRightBottomWall);
    // mutate the data
    this.c1.rightWall = true;
    this.c1.bottomWall = false;
    // test the change
    t.checkExpect(this.c1.drawCell(50), cellExampleRightWall);

    // mutate the data again
    this.c1.rightWall = false;
    // test the change
    t.checkExpect(this.c1.drawCell(50), cellExample);
  }

  // tests for setAdjacents
  void testSetAdjacents(Tester t) {
    // initialize the data
    this.initData();

    // Check the data before
    t.checkExpect(this.c1.left, null);
    t.checkExpect(this.c2.bottom, null);

    // Run the code to modify the state
    this.c1.setAdjacents(this.e1, null, null, null);
    this.c2.setAdjacents(null, null, null, this.e2);

    // Test the change was made
    t.checkExpect(this.c1.left, this.e1);
    t.checkExpect(this.c2.bottom, this.e2);
  }

  void testSortEdges(Tester t) {
    // initialize the data
    this.initData();

    MazeUtils mu = new MazeUtils();
    // check before the change
    t.checkExpect(this.edges1,
        new ArrayList<Edge>(Arrays.asList(this.e1, this.e2, this.e3, this.e4)));
    // make the change and check that the method returns what is right,
    mu.sortEdges(this.edges1);
    t.checkExpect(mu.sortEdges(this.edges1),
        new ArrayList<Edge>(Arrays.asList(this.e2, this.e4, this.e1, this.e3)));
    // check that the method didn't mutate the original list
    t.checkExpect(this.edges1,
        new ArrayList<Edge>(Arrays.asList(this.e1, this.e2, this.e3, this.e4)));
  }

  // test for answerPath using a random seed of 5
  /*void testAnswerPath(Tester t) {
    // initialize the data
    this.initData();

   t.checkExpect(this.testGame3.answerPath(
   this.testGame3.cells.get(0), 
   this.testGame3.cells.get(this.testGame3.cells.size()-1)), 
        new ArrayList<Cell>(Arrays.asList(
            this.testGame3.cells.get(0),
            this.testGame3.cells.get(1),
            this.testGame3.cells.get(4),
            this.testGame3.cells.get(7),
            this.testGame3.cells.get(10),
            this.testGame3.cells.get(11)))); 
  }*/

  // tests for searchPath using a random seed of 5
  /*void testSearchPath(Tester t) {
    // initialize the data
    this.initData();

    // searchPath for Breadth First Search using random seed of 5
    t.checkExpect(this.testGame4.searchPath(this.testGame4.cells.get(0), 
        this.testGame4.cells.get(this.testGame4.cells.size()-1), 
        new Queue<Cell>(new Deque<Cell>())), new ArrayList<Cell>(Arrays.asList(
            this.testGame4.cells.get(0),
            this.testGame4.cells.get(1),
            this.testGame4.cells.get(3),
            this.testGame4.cells.get(2)
            )));

    // search path for Depth First Search using random seed of 5
    t.checkExpect(this.testGame4.searchPath(this.testGame4.cells.get(0), 
        this.testGame4.cells.get(this.testGame4.cells.size()-1), 
        new Stack<Cell>(new Deque<Cell>())), new ArrayList<Cell>(Arrays.asList(
            this.testGame4.cells.get(0),
            this.testGame4.cells.get(1),
            this.testGame4.cells.get(3),
            this.testGame4.cells.get(2)
            )));
  }*/

  // test for draw current board using a random seed of 5
  /*
  void testDrawCurrentBoard(Tester t) {
    // initialize the data
    this.initData();

    t.checkExpect(this.testGame3.drawCurrentBoard(), new AboveImage(
        new AboveImage(
            new AboveImage(
                new AboveImage(new EmptyImage(), new BesideImage(
                    new BesideImage(
                        new BesideImage(new EmptyImage(),
                            new OverlayOffsetImage(new LineImage(new Posn(50, 0), Color.black), 0.0,
                                -24.0, new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray))),
                        new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray)),
                    new OverlayOffsetImage(new LineImage(new Posn(0, 50), Color.black), -24.0, 0.0,
                        new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray)))),
                new BesideImage(
                    new BesideImage(
                        new BesideImage(new EmptyImage(),
                            new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray)),
                        new OverlayOffsetImage(new LineImage(new Posn(0, 50), Color.black), -24.0,
                            0.0, new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray))),
                    new OverlayOffsetImage(new LineImage(new Posn(0, 50), Color.black), -24.0, 0.0,
                        new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray)))),
            new BesideImage(
                new BesideImage(
                    new BesideImage(new EmptyImage(),
                        new OverlayOffsetImage(new LineImage(new Posn(0, 50), Color.black), -24.0,
                            0.0, new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray))),
                    new OverlayOffsetImage(new LineImage(new Posn(0, 50), Color.black), -24.0, 0.0,
                        new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray))),
                new OverlayOffsetImage(new LineImage(new Posn(50, 0), Color.black), 0.0, -24.0,
                    new OverlayOffsetImage(new LineImage(new Posn(0, 50), Color.black), -24.0, 0.0,
                        new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray))))),
        new BesideImage(
            new BesideImage(
                new BesideImage(new EmptyImage(),
                    new OverlayOffsetImage(new LineImage(new Posn(50, 0), Color.black), 0.0, -24.0,
                        new OverlayOffsetImage(new LineImage(new Posn(0, 50), Color.black), -24.0,
                            0.0, new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray)))),
                new OverlayOffsetImage(new LineImage(new Posn(50, 0), Color.black), 0.0, -24.0,
                    new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray))),
            new OverlayOffsetImage(new LineImage(new Posn(50, 0), Color.black), 0.0, -24.0,
                new OverlayOffsetImage(new LineImage(new Posn(0, 50), Color.black), -24.0, 0.0,
                    new RectangleImage(50, 50, OutlineMode.SOLID, Color.blue))))));
  }*/

  // tests for makeScene using a random seed of 5
  /*
  void testMakeScene(Tester t) {
    // initialize the data
    this.initData();

    // testing
    WorldScene result = new WorldScene(this.testGame3.width * this.testGame3.cellSize,
        this.testGame3.height * this.testGame3.cellSize);

    // WorldImage resultBoard = this.testGame3.drawCurrentBoard();
    WorldImage resultBoard = new AboveImage(
        new AboveImage(
            new AboveImage(
                new AboveImage(new EmptyImage(), new BesideImage(
                    new BesideImage(
                        new BesideImage(new EmptyImage(),
                            new OverlayOffsetImage(new LineImage(new Posn(50, 0), Color.black), 0.0,
                                -24.0, new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray))),
                        new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray)),
                    new OverlayOffsetImage(new LineImage(new Posn(0, 50), Color.black), -24.0, 0.0,
                        new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray)))),
                new BesideImage(
                    new BesideImage(
                        new BesideImage(new EmptyImage(),
                            new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray)),
                        new OverlayOffsetImage(new LineImage(new Posn(0, 50), Color.black), -24.0,
                            0.0, new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray))),
                    new OverlayOffsetImage(new LineImage(new Posn(0, 50), Color.black), -24.0, 0.0,
                        new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray)))),
            new BesideImage(
                new BesideImage(
                    new BesideImage(new EmptyImage(),
                        new OverlayOffsetImage(new LineImage(new Posn(0, 50), Color.black), -24.0,
                            0.0, new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray))),
                    new OverlayOffsetImage(new LineImage(new Posn(0, 50), Color.black), -24.0, 0.0,
                        new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray))),
                new OverlayOffsetImage(new LineImage(new Posn(50, 0), Color.black), 0.0, -24.0,
                    new OverlayOffsetImage(new LineImage(new Posn(0, 50), Color.black), -24.0, 0.0,
                        new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray))))),
        new BesideImage(
            new BesideImage(
                new BesideImage(new EmptyImage(),
                    new OverlayOffsetImage(new LineImage(new Posn(50, 0), Color.black), 0.0, -24.0,
                        new OverlayOffsetImage(new LineImage(new Posn(0, 50), Color.black), -24.0,
                            0.0, new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray)))),
                new OverlayOffsetImage(new LineImage(new Posn(50, 0), Color.black), 0.0, -24.0,
                    new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray))),
            new OverlayOffsetImage(new LineImage(new Posn(50, 0), Color.black), 0.0, -24.0,
                new OverlayOffsetImage(new LineImage(new Posn(0, 50), Color.black), -24.0, 0.0,
                    new RectangleImage(50, 50, OutlineMode.SOLID, Color.blue)))));

    result.placeImageXY(resultBoard, (this.testGame3.cellSize * (this.testGame3.width)) / 2,
        (this.testGame3.cellSize * (this.testGame3.height)) / 2);

    t.checkExpect(this.testGame3.makeScene(), result);
  }*/

  void testBigBang(Tester t) {
    // -- ensure the initial conditions --
    this.initData();
    // make the bigbang world
    Maze m = this.testGame;
    int worldWidth = (this.testGame.cellSize * (this.testGame.width));
    int worldHeight = (this.testGame.cellSize * (this.testGame.height));
    double tickRate = 0.01;
    m.bigBang(worldWidth, worldHeight, tickRate);
  }

}
