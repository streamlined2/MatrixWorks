package sort;

import java.util.Comparator;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

public class SelectionSorter<K> {

	private final Comparator<? super K> comparator;

	public SelectionSorter(final Comparator<? super K> comparator) {
		this.comparator=comparator;
	}
	
	private int findExtremum(final ListIterator<K> i){

		if(i.hasNext()){
			int extIndex=i.nextIndex();
			K extremum=i.next();

			while(i.hasNext()){
				int k=i.nextIndex();
				final K key=i.next();

				if(comparator.compare(key,extremum)<0) {//new extremum found
					extremum=key;
					extIndex=k;
				}
			}
			return extIndex;
		
		}else throw new RuntimeException("size of sequence should be greater than 0");
	}
	
	public void sort(final Sequence<K> dataSeq){
		sort(dataSeq,dataSeq.size());
	}
	
	//sort first 'limit' items of 'dataSeq' by swapping current item and extremum item for rest of sequence
	public void sort(final Sequence<K> dataSeq,final int limit) {
		if(dataSeq.size()<1) throw new RuntimeException("sequence to be sorted should contain at least one element");
		if(limit<=0) throw new RuntimeException("number of elements to sort should be 1 or greater");
		
		final int steps=Math.min(limit, dataSeq.size()-1);
		for(int index=0;index<steps;index++) {
			dataSeq.swap(
				index, 
				findExtremum(dataSeq.listIterator(index)));
		}	
	}
	
	//creates list of extremum and then place them in the head of sequence 'dataSeq' according to 'comparator' 
	public void buildExtremumListAndSort(final Sequence<K> dataSeq,final int limit){
		if(dataSeq.size()<1) throw new RuntimeException("sequence to be sorted should contain at least one element");
		if(limit<=0) throw new RuntimeException("number of elements to sort should be 1 or greater");
		
		//collect first 'limit' extremums of sequence
		final Map<Integer,Integer> sortedIndices=new TreeMap<>();  
		final int steps=Math.min(limit, dataSeq.size()-1);
		for(int index=0;index<steps;index++) {
			sortedIndices.put(
				findExtremum(dataSeq.listIterator(sortedIndices)),index);
		}
		
		//place extremums listed in 'sortedIndices' in the head of sequence by interchanging items
		for(Map.Entry<Integer, Integer> entry:sortedIndices.entrySet()) {
				dataSeq.swap(entry.getValue(), entry.getKey());
		}
		
	}

}
