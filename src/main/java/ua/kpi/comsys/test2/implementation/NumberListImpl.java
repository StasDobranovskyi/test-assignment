/*
 * Copyright (c) 2014, NTUU KPI, Computer systems department and/or its affiliates. All rights reserved.
 * NTUU KPI PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package ua.kpi.comsys.test2.implementation;

import ua.kpi.comsys.test2.NumberList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * Custom implementation of {@link NumberList}.\n
 *
 * <p>Variant is defined by record book number {@link #getRecordBookNumber()}.</p>
 *
 * <p>Author: Dobranovskyi Stanislav, IO-36, 3605</p>
 */
public class NumberListImpl implements NumberList {

    // ===== Variant constants (derived from record book number) =====

    private static final int RECORD_BOOK_NUMBER = 3605;

    private static int baseFromRecordBook() {
        int c5 = RECORD_BOOK_NUMBER % 5;
        return switch (c5) {
            case 0 -> 2;   // binary
            case 1 -> 3;   // ternary
            case 2 -> 8;   // octal
            case 3 -> 10;  // decimal
            case 4 -> 16;  // hexadecimal
            default -> 10;
        };
    }

    private static int additionalBaseFromRecordBook() {
        int idx = (RECORD_BOOK_NUMBER % 5 + 1) % 5;
        return switch (idx) {
            case 0 -> 2;
            case 1 -> 3;
            case 2 -> 8;
            case 3 -> 10;
            case 4 -> 16;
            default -> 10;
        };
    }

    // ===== Internal circular doubly linked list =====

    private static final class Node {
        byte item;
        Node prev;
        Node next;

        Node(byte item) {
            this.item = item;
        }
    }

    private final int base;
    private final int additionalBase;

    private Node head; // sentinel (does not store a digit)
    private int size;

    /**
     * Default constructor. Returns empty {@code NumberListImpl}.
     */
    public NumberListImpl() {
        this(baseFromRecordBook(), additionalBaseFromRecordBook());
    }

    private NumberListImpl(int base, int additionalBase) {
        this.base = base;
        this.additionalBase = additionalBase;
        initEmpty();
    }

    private NumberListImpl(BigInteger value, int base, int additionalBase) {
        this(base, additionalBase);
        fromBigInteger(value);
    }

    private void initEmpty() {
        head = new Node((byte) 0);
        head.next = head;
        head.prev = head;
        size = 0;
    }

    /**
     * Constructs new {@code NumberListImpl} by <b>decimal</b> number
     * from file, defined in string format.
     *
     * @param file file where number is stored.
     */
    public NumberListImpl(File file) {
        this();
        if (file == null || !file.exists() || !file.isFile()) {
            return;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line = br.readLine();
            if (line == null) {
                return;
            }
            line = line.trim();
            if (line.isEmpty()) {
                return;
            }
            parseDecimalAndFill(line);
        } catch (IOException ignored) {
            // If file can't be read -> keep list empty
        }
    }

    /**
     * Constructs new {@code NumberListImpl} by <b>decimal</b> number
     * in string notation.
     *
     * @param value number in string notation.
     */
    public NumberListImpl(String value) {
        this();
        parseDecimalAndFill(value);
    }

    private void parseDecimalAndFill(String value) {
        clear();
        if (value == null) {
            return;
        }
        String s = value.trim();
        if (s.isEmpty()) {
            return;
        }
        // only non-negative decimal integers
        if (s.startsWith("-")) {
            return;
        }
        // fast validation (avoid accepting letters/spaces)
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch < '0' || ch > '9') {
                return;
            }
        }

        try {
            BigInteger dec = new BigInteger(s, 10);
            if (dec.signum() < 0) {
                return;
            }
            fromBigInteger(dec);
        } catch (NumberFormatException ignored) {
            // keep empty on invalid input
        }
    }

    /**
     * Saves the number, stored in the list, into specified file
     * in <b>decimal</b> scale of notation.
     *
     * @param file file where number has to be stored.
     */
    public void saveList(File file) {
        if (file == null) {
            return;
        }
        try (FileWriter fw = new FileWriter(file, false)) {
            fw.write(toDecimalString());
        } catch (IOException ignored) {
        }
    }

    /**
     * Returns student's record book number, which has 4 decimal digits.
     *
     * @return student's record book number.
     */
    public static int getRecordBookNumber() {
        return RECORD_BOOK_NUMBER;
    }

    /**
     * Returns new {@code NumberListImpl} which represents the same number
     * in other scale of notation, defined by personal test assignment.
     *
     * <p>Does not impact the original list.</p>
     *
     * @return {@code NumberListImpl} in other scale of notation.
     */
    public NumberListImpl changeScale() {
        BigInteger dec = toBigInteger();
        return new NumberListImpl(dec, additionalBase, additionalBaseFromRecordBook());
    }

    /**
     * Returns new {@code NumberListImpl} which represents the result of
     * additional operation, defined by personal test assignment.
     *
     * <p>Does not impact the original list.</p>
     *
     * @param arg second argument of additional operation
     * @return result of additional operation.
     */
    public NumberListImpl additionalOperation(NumberList arg) {
        BigInteger a = this.toBigInteger();
        BigInteger b = bigIntegerOf(arg);

        int c7 = RECORD_BOOK_NUMBER % 7;
        BigInteger res = switch (c7) {
            case 0 -> a.add(b);
            case 1 -> a.subtract(b).max(BigInteger.ZERO);
            case 2 -> a.multiply(b);
            case 3 -> (b.equals(BigInteger.ZERO) ? BigInteger.ZERO : a.divide(b));
            case 4 -> (b.equals(BigInteger.ZERO) ? BigInteger.ZERO : a.mod(b));
            case 5 -> a.and(b);
            case 6 -> a.or(b);
            default -> a.add(b);
        };

        return new NumberListImpl(res, this.base, this.additionalBase);
    }

    /**
     * Returns string representation of number, stored in the list
     * in <b>decimal</b> scale of notation.
     *
     * @return string representation in <b>decimal</b> scale.
     */
    public String toDecimalString() {
        if (isEmpty()) {
            return "";
        }
        return toBigInteger().toString(10);
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(size);
        for (Node cur = head.next; cur != head; cur = cur.next) {
            sb.append(digitToChar(cur.item));
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        // List contract: compare element-by-element and size
        if (this == o) {
            return true;
        }
        if (!(o instanceof List<?> other)) {
            return false;
        }
        if (this.size() != other.size()) {
            return false;
        }
        Iterator<Byte> it1 = this.iterator();
        Iterator<?> it2 = other.iterator();
        while (it1.hasNext() && it2.hasNext()) {
            Byte a = it1.next();
            Object b = it2.next();
            if (!(b instanceof Byte bb)) {
                return false;
            }
            if (!a.equals(bb)) {
                return false;
            }
        }
        return !(it1.hasNext() || it2.hasNext());
    }

    @Override
    public int hashCode() {
        int hash = 1;
        for (Byte b : this) {
            hash = 31 * hash + (b == null ? 0 : b.hashCode());
        }
        return hash;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    @Override
    public Iterator<Byte> iterator() {
        return new ListItr(0);
    }

    @Override
    public Object[] toArray() {
        Object[] arr = new Object[size];
        int i = 0;
        for (Node cur = head.next; cur != head; cur = cur.next) {
            arr[i++] = cur.item;
        }
        return arr;
    }

    /**
     * As requested by assignment, this method can remain unimplemented.
     */
    @Override
    public <T> T[] toArray(T[] a) {
        return null;
    }

    @Override
    public boolean add(Byte e) {
        add(size, e);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        int idx = indexOf(o);
        if (idx < 0) {
            return false;
        }
        remove(idx);
        return true;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        if (c == null) {
            return true;
        }
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends Byte> c) {
        if (c == null || c.isEmpty()) {
            return false;
        }
        boolean changed = false;
        for (Byte b : c) {
            add(b);
            changed = true;
        }
        return changed;
    }

    @Override
    public boolean addAll(int index, Collection<? extends Byte> c) {
        checkPositionIndex(index);
        if (c == null || c.isEmpty()) {
            return false;
        }
        int i = index;
        for (Byte b : c) {
            add(i++, b);
        }
        return true;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        if (c == null || c.isEmpty()) {
            return false;
        }
        boolean changed = false;
        Iterator<Byte> it = iterator();
        while (it.hasNext()) {
            Byte v = it.next();
            if (c.contains(v)) {
                it.remove();
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        if (c == null) {
            boolean changed = !isEmpty();
            clear();
            return changed;
        }
        boolean changed = false;
        Iterator<Byte> it = iterator();
        while (it.hasNext()) {
            Byte v = it.next();
            if (!c.contains(v)) {
                it.remove();
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public void clear() {
        if (head == null) {
            return;
        }
        Node cur = head.next;
        while (cur != head) {
            Node nxt = cur.next;
            cur.prev = null;
            cur.next = null;
            cur = nxt;
        }
        head.next = head;
        head.prev = head;
        size = 0;
    }

    @Override
    public Byte get(int index) {
        checkElementIndex(index);
        return node(index).item;
    }

    @Override
    public Byte set(int index, Byte element) {
        checkElementIndex(index);
        byte val = validateDigit(element);
        Node n = node(index);
        byte old = n.item;
        n.item = val;
        return old;
    }

    @Override
    public void add(int index, Byte element) {
        checkPositionIndex(index);
        byte val = validateDigit(element);
        Node succ = (index == size) ? head : node(index);
        linkBefore(new Node(val), succ);
    }

    @Override
    public Byte remove(int index) {
        checkElementIndex(index);
        Node n = node(index);
        byte old = n.item;
        unlink(n);
        return old;
    }

    @Override
    public int indexOf(Object o) {
        if (!(o instanceof Byte b)) {
            return -1;
        }
        int idx = 0;
        for (Node cur = head.next; cur != head; cur = cur.next) {
            if (cur.item == b) {
                return idx;
            }
            idx++;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        if (!(o instanceof Byte b)) {
            return -1;
        }
        int idx = size - 1;
        for (Node cur = head.prev; cur != head; cur = cur.prev) {
            if (cur.item == b) {
                return idx;
            }
            idx--;
        }
        return -1;
    }

    @Override
    public ListIterator<Byte> listIterator() {
        return new ListItr(0);
    }

    @Override
    public ListIterator<Byte> listIterator(int index) {
        checkPositionIndex(index);
        return new ListItr(index);
    }

    @Override
    public List<Byte> subList(int fromIndex, int toIndex) {
        checkPositionIndex(fromIndex);
        checkPositionIndex(toIndex);
        if (fromIndex > toIndex) {
            throw new IndexOutOfBoundsException("fromIndex > toIndex");
        }
        NumberListImpl res = new NumberListImpl(this.base, this.additionalBase);
        for (int i = fromIndex; i < toIndex; i++) {
            res.add(get(i));
        }
        return res;
    }

    // ===== Additional list operations =====

    @Override
    public boolean swap(int index1, int index2) {
        if (index1 == index2) {
            return true;
        }
        if (index1 < 0 || index2 < 0 || index1 >= size || index2 >= size) {
            return false;
        }
        Node a = node(index1);
        Node b = node(index2);
        byte tmp = a.item;
        a.item = b.item;
        b.item = tmp;
        return true;
    }

    @Override
    public void sortAscending() {
        if (size < 2) {
            return;
        }
        // simple stable bubble sort (digits count is small in base <= 16)
        boolean swapped;
        do {
            swapped = false;
            Node cur = head.next;
            while (cur.next != head) {
                if (cur.item > cur.next.item) {
                    byte tmp = cur.item;
                    cur.item = cur.next.item;
                    cur.next.item = tmp;
                    swapped = true;
                }
                cur = cur.next;
            }
        } while (swapped);
    }

    @Override
    public void sortDescending() {
        if (size < 2) {
            return;
        }
        boolean swapped;
        do {
            swapped = false;
            Node cur = head.next;
            while (cur.next != head) {
                if (cur.item < cur.next.item) {
                    byte tmp = cur.item;
                    cur.item = cur.next.item;
                    cur.next.item = tmp;
                    swapped = true;
                }
                cur = cur.next;
            }
        } while (swapped);
    }

    @Override
    public void shiftLeft() {
        if (size < 2) {
            return;
        }
        Node first = head.next;
        unlink(first);
        linkBefore(first, head); // insert before head (at the end)
    }

    @Override
    public void shiftRight() {
        if (size < 2) {
            return;
        }
        Node last = head.prev;
        unlink(last);
        linkBefore(last, head.next); // insert before current first
    }

    // ===== Helpers =====

    private byte validateDigit(Byte element) {
        if (element == null) {
            throw new NullPointerException("Digit can't be null");
        }
        byte v = element;
        if (v < 0 || v >= base) {
            throw new IllegalArgumentException("Digit " + v + " is out of range for base " + base);
        }
        return v;
    }

    private void linkBefore(Node node, Node succ) {
        Node pred = succ.prev;
        node.next = succ;
        node.prev = pred;
        pred.next = node;
        succ.prev = node;
        size++;
    }

    private void unlink(Node node) {
        Node pred = node.prev;
        Node succ = node.next;
        pred.next = succ;
        succ.prev = pred;
        node.prev = null;
        node.next = null;
        size--;
    }

    private Node node(int index) {
        if (index < (size >> 1)) {
            Node cur = head.next;
            for (int i = 0; i < index; i++) {
                cur = cur.next;
            }
            return cur;
        } else {
            Node cur = head.prev;
            for (int i = size - 1; i > index; i--) {
                cur = cur.prev;
            }
            return cur;
        }
    }

    private void checkElementIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
    }

    private void checkPositionIndex(int index) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
    }

    private char digitToChar(byte digit) {
        int d = digit & 0xFF;
        if (d >= 0 && d <= 9) {
            return (char) ('0' + d);
        }
        return (char) ('A' + (d - 10));
    }

    private static byte charToDigit(char ch) {
        if (ch >= '0' && ch <= '9') {
            return (byte) (ch - '0');
        }
        if (ch >= 'A' && ch <= 'F') {
            return (byte) (10 + (ch - 'A'));
        }
        if (ch >= 'a' && ch <= 'f') {
            return (byte) (10 + (ch - 'a'));
        }
        throw new IllegalArgumentException("Invalid digit: " + ch);
    }

    private void fromBigInteger(BigInteger dec) {
        clear();
        if (dec == null) {
            return;
        }
        if (dec.equals(BigInteger.ZERO)) {
            add((byte) 0);
            return;
        }

        String s = dec.toString(base).toUpperCase();
        for (int i = 0; i < s.length(); i++) {
            add(charToDigit(s.charAt(i)));
        }
    }

    private BigInteger bigIntegerOf(NumberList list) {
        if (list == null || list.isEmpty()) {
            return BigInteger.ZERO;
        }
        if (list instanceof NumberListImpl impl) {
            return impl.toBigInteger();
        }
        // assume same base as this list
        BigInteger res = BigInteger.ZERO;
        for (Byte b : list) {
            if (b == null) {
                continue;
            }
            res = res.multiply(BigInteger.valueOf(this.base)).add(BigInteger.valueOf(b));
        }
        return res;
    }

    private BigInteger toBigInteger() {
        if (isEmpty()) {
            return BigInteger.ZERO;
        }
        BigInteger res = BigInteger.ZERO;
        BigInteger b = BigInteger.valueOf(base);
        for (Node cur = head.next; cur != head; cur = cur.next) {
            res = res.multiply(b).add(BigInteger.valueOf(cur.item & 0xFF));
        }
        return res;
    }

    // ===== ListIterator implementation =====

    private final class ListItr implements ListIterator<Byte> {
        private Node lastReturned = null;
        private Node next;
        private int nextIndex;

        ListItr(int index) {
            this.next = (index == size) ? head : node(index);
            this.nextIndex = index;
        }

        @Override
        public boolean hasNext() {
            return nextIndex < size;
        }

        @Override
        public Byte next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            lastReturned = next;
            next = next.next;
            nextIndex++;
            return lastReturned.item;
        }

        @Override
        public boolean hasPrevious() {
            return nextIndex > 0;
        }

        @Override
        public Byte previous() {
            if (!hasPrevious()) {
                throw new NoSuchElementException();
            }
            next = next.prev;
            lastReturned = next;
            nextIndex--;
            return lastReturned.item;
        }

        @Override
        public int nextIndex() {
            return nextIndex;
        }

        @Override
        public int previousIndex() {
            return nextIndex - 1;
        }

        @Override
        public void remove() {
            if (lastReturned == null) {
                throw new IllegalStateException();
            }
            Node lastNext = lastReturned.next;
            unlink(lastReturned);
            if (next == lastReturned) {
                next = lastNext;
            } else {
                nextIndex--;
            }
            lastReturned = null;
        }

        @Override
        public void set(Byte e) {
            if (lastReturned == null) {
                throw new IllegalStateException();
            }
            lastReturned.item = validateDigit(e);
        }

        @Override
        public void add(Byte e) {
            byte val = validateDigit(e);
            linkBefore(new Node(val), next);
            nextIndex++;
            lastReturned = null;
        }
    }
}
