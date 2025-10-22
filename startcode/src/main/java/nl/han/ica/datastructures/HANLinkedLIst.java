package nl.han.ica.datastructures;

import javax.swing.tree.TreeNode;

public class HANLinkedLIst implements IHANLinkedList<T> {

    private Node<T> head;


    @Override
    public void addFirst(T value) {
        if (head == null){
            head = new Node<>((T) value);
        } else {
            Node<T> newTreeNode = new Node();
            newTreeNode.value = value;
            head = newTreeNode;
        }
    }

    @Override
    public void clear() {
head = null;
    }

    @Override
    public void insert(int index, T value) {
Node<T> addNode = new Node<T>();
addNode.value = value;
    }

    @Override
    public void delete(int pos) {
    }

    @Override
    public T get(int pos) {
        return null;
    }

    @Override
    public void removeFirst() {

    }

    @Override
    public T getFirst() {
        return null;
    }

    @Override
    public int getSize() {
        return 0;
    }
}
