import java.util.function.Predicate;

//Abstract class to represent Sentinels and Nodes 
abstract class ANode<T> {
  ANode<T> next;
  ANode<T> prev;

  ANode(ANode<T> next, ANode<T> prev) {
    this.next = next;
    this.prev = prev;
  }

  // Counts the number of nodes in this ANode
  abstract int size();

  // Helper for size method
  abstract int sizeHelp();

  //EFFECT: inserts the given object at the head of the Deque list
  void addAtHead(T first) {
    // if called on a ANode<T> that's not a sentinel,
    // this method should do nothing
  }

  //EFFECT: Inserts the given T at the end of the list 
  void addAtTail(T first) {
    // if called on a ANode<T> that's not a sentinel,
    // this method should do nothing
  }

  // Returns the first node in the list that applies to the given predicate
  abstract ANode<T> find(Predicate<T> pred);

  // Helper for find
  abstract ANode<T> findHelp(Predicate<T> pred);

  // EFFECT: Sets the next of this to the given node
  void setNext(ANode<T> newNext) {
    this.next = newNext;
  }

  // EFFECT: Sets the previous of this to the given node
  void setPrev(ANode<T> newPrev) {
    this.prev = newPrev;
  }

  // EFFECT: Removes this node from the list
  abstract T remove();

}

//Class to represent a Node in a Deque
class Node<T> extends ANode<T> {
  T data;

  Node(T data) {
    super(null, null);
    this.data = data;
  }

  Node(T data, ANode<T> next, ANode<T> prev) {
    super(new Utils().checkNull(next), new Utils().checkNull(prev));
    this.data = data;
    prev.next = this;
    next.prev = this;

  }

  // Counts the size of this node
  public int size() {
    return 1 + this.next.sizeHelp();
  }

  // Helper for size
  public int sizeHelp() {
    return 1 + this.next.sizeHelp();
  }

  // finds the first node in this list that applies to the given predicate
  ANode<T> find(Predicate<T> pred) {
    if (pred.test(this.data)) {
      return this;
    }
    return this.next.findHelp(pred);
  }

  // Helper for find
  ANode<T> findHelp(Predicate<T> pred) {
    if (pred.test(this.data)) {
      return this;
    }
    return this.next.findHelp(pred);
  }

  // EFFECT: Removes this node from the list
  // returns the data of the node removed
  public T remove() {
    this.prev.setNext(this.next);
    this.next.setPrev(this.prev);

    return this.data;
  }

}

//Class to represent a Sentinel in a Deque
class Sentinel<T> extends ANode<T> {
  Sentinel() {
    super(null, null);
    this.next = this;
    this.prev = this;
  }

  // Computes the size of this Sentinel
  public int size() {
    return this.next.sizeHelp();
  }

  // Helper for size method
  public int sizeHelp() {
    return 0;
  }

  // EFFECT: Inserts the given T at the front of this list
  void addAtHead(T first) {
    this.next = new Node<T>(first, this.next, this);
  }

  // EFFECT: Inserts the given T at the end of this list
  void addAtTail(T tail) {
    this.prev = new Node<T>(tail, this, this.prev);
  }

  // EFFECT: Removes the first node from this Deque
  T removeFromHead() {
    return this.next.remove();

  }

  // EFFECT: Removes the last node from this Deque
  T removeFromTail() {
    return this.prev.remove();
  }

  // finds the first node in this list that applies to the given predicate
  ANode<T> find(Predicate<T> pred) {
    return this.next.findHelp(pred);
  }

  // Helper for find method
  ANode<T> findHelp(Predicate<T> pred) {
    return this;
  }

  // When asked to remove this Sentinel returns the Sentinel instead
  // returns null because you can not remove Sentinel from a deque, and
  // Sentinel has no data
  public T remove() {
    return null;
  }

}

//Class to represent a Deque which is a doubl ended queue
class Deque<T> {
  Sentinel<T> header;

  Deque() {
    this.header = new Sentinel<T>();
  }

  Deque(Sentinel<T> header) {
    this.header = header;
  }

  //Computes the size of this Deque
  int size() {
    return this.header.size();
  }

  // Inserts the given T at the front of the list
  // EFFECT: inserts the given object at the head of the Deque list
  void addAtHead(T first) {

    this.header.addAtHead(first);
  }

  // Inserts the given T at the end of the list
  // EFFECT: inserts the given object at the tail of the Deque list
  void addAtTail(T tail) {

    this.header.addAtTail(tail);
  }

  // EFFECT: removes the object from the head of the Deque list
  T removeFromHead() {
    if (this.size() == 0) {
      throw new RuntimeException("Can not remove head from empty list");
    }
    return this.header.removeFromHead();
  }

  // EFFECT: removes the object from the tail of the Deque list
  T removeFromTail() {
    if (this.size() == 0) {
      throw new RuntimeException("Can not remove tail from empty list");
    }
    return this.header.removeFromTail();
  }

  // finds the first node in this list that applies to the given predicate
  ANode<T> find(Predicate<T> pred) {
    return this.header.find(pred);
  }

  // EFFECT: Removes the given Node from this List
  // removes the given node, but returns the data of the removed node
  T removeNode(ANode<T> n) {
    return n.remove();
  }

  /*
   * VERSION of removeNode that takes in the data of the node and removes the 
   * corresponding node
  T removeNode1(T data) {
   return this.removeNode(
     this.find(
         new SameDataAs<T>(data)));
  }*/

}

//Class to represent useful functions
class Utils {

  // Checks to make sure the T value is not null, if its throw and exception
  <T> T checkNull(T t) {
    if (t == null) {
      throw new IllegalArgumentException("Can not accept null value");
    }
    return t;
  }

}

//Class to reprsent a predicate that checks if strings start with the letter "b"
class StartsWithB implements Predicate<String> {
  public boolean test(String s) {
    return s.substring(0, 1).equals("b");
  }
}

//class to represent a predicate that checks if this data is the same as the given data
class SameDataAs<T> implements Predicate<T> {
  T data;

  SameDataAs(T data) {
    this.data = data;
  }

  public boolean test(T t) {
    return t.equals(this.data);
  }
}

//interface to represent types of collections for DFS and BFS
interface ICollection<T> {
  //Is this collection empty?
  boolean isEmpty();

  // what is the size of this collection
  int size();

  // add the given item to this collection
  void add(T t);

  // remove the first item from this collection
  T remove();
}

//class to represent Queue type of collection
class Queue<T> implements ICollection<T> {
  Deque<T> items;

  Queue(Deque<T> i) {
    this.items = i;
  }

  //Is this queue empty?
  public boolean isEmpty() {
    return this.items.size() > 0;
  }

  //what is the size of this queue
  public int size() {
    return this.items.size();
  }

  //add the given item to this queue
  public void add(T t) {
    this.items.addAtTail(t);
  }

  //remove the first item from this queue
  public T remove() {
    return this.items.removeFromHead();
  }
}

//class to represent Stack type of collection
class Stack<T> implements ICollection<T> {
  Deque<T> items;

  Stack(Deque<T> i) {
    this.items = i;
  }

  //Is this queue empty?
  public boolean isEmpty() {
    return this.items.size() > 0;
  }

  //what is the size of this stack?
  public int size() {
    return this.items.size();
  }

  //add the given item to this stack
  public void add(T t) {
    this.items.addAtHead(t);
  }

  //remove the first item from this stack
  public T remove() {
    return this.items.removeFromHead();
  }
}
