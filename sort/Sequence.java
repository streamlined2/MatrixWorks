package sort;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;

/**
 * The interface provides access to data container
 * 
 * @author Serhii Pylypenko
 * @param <K> type of sequence key
 * @version 1.5
 */
public interface Sequence<K> extends Iterable<K> {
	
	int size();
	K getKey(final int index);
	void swap(final int from,final int to);
	
	default Iterator<K> iterator(){
		return listIterator(0);
	}
	
	default ListIterator<K> listIterator(final int initialIndex) {
		return new ListIterator<K>() {
			private int index=initialIndex;
			
			@Override public boolean hasNext() {
				return index<size();
			}

			@Override public K next() {
				return getKey(index++);
			}

			@Override public int nextIndex() {
				return index;
			}

			@Override public boolean hasPrevious() {
				return index>0;
			}

			@Override public K previous() {
				return getKey(--index);
			}

			@Override public int previousIndex() {
				return index-1;
			}

			@Override public void remove() {
				throw new UnsupportedOperationException("remove operation isn't supported for Sequence interface iterator");
			}

			@Override public void set(K e) {
				throw new UnsupportedOperationException("set operation isn't supported for Sequence interface iterator");
			}

			@Override public void add(K e) {
				throw new UnsupportedOperationException("add operation isn't supported for Sequence interface iterator");
			}
		};
	}
	
	default ListIterator<K> listIterator(final Map<Integer,Integer> selectedIndices) {
		return new ListIterator<K>() {
			private static final int UNDEFINED=-1;
			private int index=-1;
			private int rightNeighbour=UNDEFINED;
			private int leftNeighbour=UNDEFINED;
			
			private boolean itemPresent(final boolean right,final int index) {
				return right?index<size():index>=0;
			}
			
			private int findFirstNonSelectedIndex(final boolean right) {
				int probe=right?index+1:index-1;
				while(itemPresent(right,probe) && selectedIndices.containsKey(probe)) {
					if(right) probe++; else probe--;
				}
				return probe;
			}

			@Override public boolean hasNext() {//should be called before nextIndex/next to produce consistent result
				final int probe=rightNeighbour=findFirstNonSelectedIndex(true);
				return itemPresent(true,probe);
			}

			@Override public int nextIndex() {
				return rightNeighbour;//findFirstNonSelectedIndex(true);
			}

			@Override public K next() {
				leftNeighbour=index;
				index=rightNeighbour;//findFirstNonSelectedIndex(true);
				rightNeighbour=UNDEFINED;
				return getKey(index);
			}

			@Override public boolean hasPrevious() {
				final int probe=leftNeighbour=findFirstNonSelectedIndex(false);
				return itemPresent(false,probe);
			}

			@Override public int previousIndex() {
				return leftNeighbour;//findFirstNonSelectedIndex(false);
			}

			@Override public K previous() {
				rightNeighbour=index;
				index=leftNeighbour;//findFirstNonSelectedIndex(false);
				leftNeighbour=UNDEFINED;
				return getKey(index);
			}

			@Override public void remove() {
				throw new UnsupportedOperationException("remove operation isn't supported for Sequence interface iterator");			
			}

			@Override public void set(K e) {
				throw new UnsupportedOperationException("set operation isn't supported for Sequence interface iterator");
			}

			@Override public void add(K e) {
				throw new UnsupportedOperationException("add operation isn't supported for Sequence interface iterator");
			}
		};

	}
	
}
