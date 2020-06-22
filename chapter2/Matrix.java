package chapter2;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import math.Ordinal;
import sort.QuickSorter;
import sort.Sequence;
import utils.Utensils;

/**
 * 
 * @author Serhii Pylypenko
 * @since 2020-02-15
 * @version 1.5
 * 
 */
public class Matrix <T extends Ordinal<T>> implements Cloneable, Iterable<Matrix<T>.Segment>, Serializable {
	
	private static final long serialVersionUID = -7708934672336335161L;

	public final static int SIDE_COUNT=4;
	
	private static void checkIndex(final String name,final int index,final int from,final int to) {
		if(index<from || index>=to) throw new RuntimeException(String.format("index of %s (%d) must be within [%d,%d)",name,index,from,to));
	}
	
	public enum IndexType { ROW, COLUMN;
		public IndexType getOpposite() {
			return this==ROW?COLUMN:ROW;
		}
	};
	
	public enum Direction { RIGHT_DOWN, LEFT_UP};
	
	public enum Rotation { CLOCKWISE, COUNTERCLOCKWISE;
		public Rotation getOpposite() {
			return this==CLOCKWISE?COUNTERCLOCKWISE:CLOCKWISE;
		}
	};
	
	public enum Angle { _0, _90, _180, _270;
		public boolean isOdd() {
			return ordinal()%2!=0;
		}
		public Angle previous() {
			return this==_0?_270:Angle.values()[ordinal()-1];
		}
		public Angle getOpposite() {
			return Angle.values()[Angle.values().length-ordinal()];
		}
	};
	
	public enum Quadrant { UP, RIGHT, DOWN, LEFT};
	
	private final int dimension;
	private final T[][] data;
	
	@Override
	public Iterator<Segment> iterator() {
		return iterator(IndexType.ROW);
	}
	
	public Iterator<Segment> iterator(final IndexType iType) {
		return new Iterator<Segment>() {
			private int index=0;

			@Override public boolean hasNext() {
				return index<dimension;
			}

			@Override public Segment next() {
				return new Segment(iType,index++,0,dimension);
			}
		};
	}
	
	public final UnaryOperator<T> IDENTITY_OPERATOR = new UnaryOperator<T>() {
		@Override public T apply(final T x) { return x;}		
	};
	
	/**
	 * Returns dimension of matrix
	 * @return dimension of matrix
	 */
	final public int getDimension() {
		return dimension;
	}
	
	private class ValueComparator implements Comparator<Position> {
		@Override
		public int compare(final Position o1,final Position o2) {
			return o1.getValue().compareTo(o2.getValue());
		}
	}
	
	private final ValueComparator VALUE_COMPARATOR=new ValueComparator(); 
	
	/**
	 * Represents position of one element of matrix
	 */
	 public class Position implements Comparable<Position>{
		
		private final int row, column;
		
		public Position(final int row,final int column) {
			checkIndex("row",row,0,dimension);
			checkIndex("column",column,0,dimension);
			this.row=row;
			this.column=column;
		}
		
		public int getRow() { return row;}
		
		public int getColumn() { return column;}
		
		public T getValue() {
			return data[row][column];
		}
		
		public void setValue(final T value) {
			data[row][column]=value;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public boolean equals(final Object o) {
			if(o instanceof Matrix.Position) {
				Position a=(Position)o;
				return this.getRow()==a.getRow() && this.getColumn()==a.getColumn();
			}else {
				return false;
			}
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(row,column);//row*13+column;
		}
		
		/**
		 * Determines if value at given position is local extremum
		 * @param maximum check for local maximum if {@code true}
		 * @return {@code true} if it is local extremum
		 */
		public boolean isLocalExtremum(final boolean maximum) {
			final Comparator<T> comparator=maximum?Comparator.naturalOrder():Comparator.reverseOrder();
			boolean yes=true;
			for(int i=row-1;i<=row+1;i++) {
				if(i>=0 && i<dimension) {
					for(int j=column-1;j<=column+1;j++) {
						if(j>=0 && j<dimension) {
							if(!(i==row && j==column)) {
								yes=yes && comparator.compare(getValue(), data[i][j])>0;
							}
						}
					}
				}
			}
			return yes;
		}
		
		/**
		* Updates current extremum value and list of extremum positions
		* @param list of accumulated extremum value positions
		* @param extremum value found on previous iterations
		* @return position of current extremum value
		*/
		private final Position updateExtremumList(final Set<Position> list, final Position extremum, final boolean maximum) {
			final Comparator<Position> comparator=maximum?VALUE_COMPARATOR:VALUE_COMPARATOR.reversed();
			Position newExtremum=extremum;
			int comparison;
			if((comparison=comparator.compare(this,extremum))>0) {//new extremum found
				newExtremum=this;
				list.clear();
				list.add(this);//start accumulating extremums anew
			}else if(comparison==0) {
				list.add(this);//add second copy of current extremum
			}
			return newExtremum;
		}
		
		@Override public String toString(){
			return String.format("{%d,%d}",row,column);
		}

		@Override
		public int compareTo(Matrix<T>.Position o) {
			return this.row==o.row?
					Integer.compare(this.column, o.column):
						Integer.compare(this.row, o.row);
		}
		
	}
	
	/**
	 * Represents segment of row (if {@code indexType} equals to {@code IndexType.ROW}) or 
	 * column (if {@code indexType} equals to {@code IndexType.COLUMN}) at {@code index} of outer matrix instance
	 * starting from {@code start} up to {@code finish}
	 */
	public class Segment implements Iterable<T> {
		private final IndexType indexType;
		private final int index;
		private final int start, finish;
		private boolean quadrantSpecific=false;
		
		public Segment(final IndexType iType,final int index) {
			this(iType,index,0,dimension);
		}
		
		public Segment(final IndexType iType,final int index,final int start,final int finish) {
			checkIndex("row/column",index,0,dimension);
			checkIndex("segment finish",finish,1,dimension+1);
			checkIndex("segment start",start,0,finish);
			this.indexType=iType;
			this.index=index;
			this.start=start;
			this.finish=finish;
		}
		
		public Segment(final IndexType iType,final int index,final int depth) {
			checkIndex("row/column",index,0,dimension);
			checkIndex("depth",depth,0,dimension);
			this.quadrantSpecific=true;
			this.indexType=iType;
			this.index=index;
			if(ascendingIndex()) {
				this.start=depth;
				this.finish=dimension-1-depth;
			}else {
				this.start=depth+1;
				this.finish=dimension-depth;				
			}
		}
		
		public Position createPosition(final int k) {
			return new Position(getRow(k),getColumn(k));
		}
		
		private T getValue(final int k) {
			return indexType==IndexType.COLUMN?
					data[k][index]:
						data[index][k];
		}
		
		private void setValue(final int k,final T value) {
			if(indexType==IndexType.COLUMN) {
				data[k][index]=value;
			}else{
				data[index][k]=value;
			}
		}
		
		public int getRow(final int k) {
			return indexType==IndexType.COLUMN?k:index; 
		}
		
		public int getColumn(final int k) {
			return indexType==IndexType.COLUMN?index:k; 
		}
		
		@Override public boolean equals(final Object o) {
			if(o instanceof Matrix.Segment) {
				@SuppressWarnings("unchecked")
				Matrix<T>.Segment r=(Matrix<T>.Segment)o;
				return 
						(this.indexType==r.indexType) && 
						(this.index==r.index) && 
						(this.start==r.start) && 
						(this.finish==r.finish);
			}else {
				return false;
			}
		}

		@Override public String toString() {
			final StringJoiner joiner=new StringJoiner(",","[","]");
			for(final T value:this) {
				joiner.add(String.format("%s", value.toString()));
			}
			return joiner.toString();
		}
		
		public T sum() {
			return sum(IDENTITY_OPERATOR);
		}
		
		public T sum(final UnaryOperator<T> op) {
			assert dimension>0: "empty segment prohibited";
			T accum=data[0][0].zero();
			for(T value:this) {
				accum=accum.add(op.apply(value));			
			}
			return accum;
		}
		
		public T average() {
			return sum().divide(length());
		}
		
		public int length() {
			return finish-start;
		}
		
		/**
		* Scans segment and collects positions of extremum value within the segment
		* @param extremums of collected positions from previous iteration
		* @param maximum find maximum value if {@code true}, minimum otherwise 
		* @return list of positions of extremum values of the segment merged with {@code list}
		*/
		public void getExtremumList(final Set<Position> extremums, final boolean maximum){
			final Iterator<Position> iterator=extremums.iterator();
			if(iterator.hasNext()) {
				Position extremum = iterator.next();
				for(final SegmentIterator i=listIterator();i.hasNext();){
					extremum = i.nextPosition().updateExtremumList(extremums, extremum, maximum);
				}
			}else {
				throw new RuntimeException("list of accumulated extremums should contain at least one item");
			}
		}
		
		public Set<Position> getExtremums(final boolean maximum){
			if(length()>=1) {
				final Set<Position> extremums=new HashSet<>();
				extremums.add(createPosition(start));
				getExtremumList(extremums, maximum);
				return extremums;
			}else {
				throw new RuntimeException("segment should contain at least one element");
			}
		}
		
		public void copy(final Segment source) {
			int k=startIndex();
			for(final T value:source) {
				setValue(k,value);
				k=nextIndex(k);
			}
		}
		
		public List<T> save() {
			List<T> data=new ArrayList<>(length());
			for(final T value:this) {
				data.add(value);
			}
			return data;
		}
		
		public void restore(final List<T> data) {
			if(data.size()>length()) throw new RuntimeException(String.format("size of parameter list should be equal or less than %d",length()));
			int k=startIndex();
			for(final T value:data) {
				setValue(k,value);
				k=nextIndex(k);
			}
		}
		
		/**
		* Scans the segment and swaps elements with passed {@code segment} 
		*/
		public void swap(final Segment segment){
			int dstIndex=startIndex();
			for(int srcIndex=startIndex();hasNext(srcIndex);srcIndex=nextIndex(srcIndex)){
				final T value=getValue(srcIndex);
				setValue(srcIndex,segment.getValue(dstIndex));
				segment.setValue(dstIndex,value);
				dstIndex=nextIndex(dstIndex);
			}
		}
		
		public Quadrant getQuadrant() {
			Quadrant quadrant=Quadrant.UP;
			switch(indexType) {
			case COLUMN:
				quadrant=index<dimension/2?Quadrant.LEFT:Quadrant.RIGHT;
				break;
			case ROW:
				quadrant=index<dimension/2?Quadrant.UP:Quadrant.DOWN;
				break;
			}
			return quadrant;
		}
		
		public boolean ascendingIndex() {
			final Quadrant quadrant=getQuadrant();
			return 
					!isQuadrantSpecific() ||
					isQuadrantSpecific() && (quadrant==Quadrant.UP || quadrant==Quadrant.RIGHT);
		}
		
		private boolean isQuadrantSpecific() {
			return quadrantSpecific;
		}

		public int startIndex() {
			return ascendingIndex()?start:finish-1;
		}
		
		public boolean hasNext(final int index) {
			return ascendingIndex()? index<finish: index>=start;
		}
		
		public boolean hasPrevious(final int index) {
			return ascendingIndex()? index>=start: index<finish;
		}
		
		public int nextIndex(final int index) {
			return ascendingIndex()? index+1: index-1;
		}
		
		public int previousIndex(final int index) {
			return ascendingIndex()? index-1: index+1;
		}
		
		public class SegmentIterator implements ListIterator<T>{
			private int index=startIndex();//points at 'next' element
			private boolean movingRight=true;//to determine correct index for 'set' operation

			@Override public boolean hasNext() {
				return Segment.this.hasNext(index);
			}

			@Override public int nextIndex() {
				return index;
			}

			@Override public T next() {
				movingRight=true;
				final T value=getValue(index);
				index=Segment.this.nextIndex(index);
				return value;
			}
			
			public Position nextPosition(){
				final int kNext=nextIndex();
				next();
				return createPosition(kNext);
			}
				
			@Override public void set(final T e) {
				setValue(movingRight?Segment.this.previousIndex(index):index,e);
			}

			@Override public boolean hasPrevious() {
				return ascendingIndex()? index>start: index<finish-1;
			}

			@Override public int previousIndex() {
				return Segment.this.previousIndex(index);
			}

			@Override public T previous() {
				movingRight=false;
				index=Segment.this.previousIndex(index);
				return getValue(index);
			}

			@Override public void remove() {
				throw new UnsupportedOperationException("SegmentIterator doesn't support remove operation");
			}

			@Override public void add(T e) {
				throw new UnsupportedOperationException("SegmentIterator doesn't support add operation");
			}
			
		}
		
		@Override
		public Iterator<T> iterator() {
			return listIterator();
		}

		public SegmentIterator listIterator() {
			return new SegmentIterator();
		}
		
		public Segment getNextSegment(final Rotation rotation,Angle angle,final int depth) {
			
			IndexType nextType=angle.isOdd()?
					indexType.getOpposite():indexType;//should we switch from row to column or keep the same?

			int nextIndex=index;
			if(angle.isOdd()) {//odd rotation angle 1 or 3
				if(//do we cross left-down to right-up diagonal?
						(indexType==IndexType.ROW && rotation==Rotation.CLOCKWISE) ||
						(indexType==IndexType.COLUMN && rotation==Rotation.COUNTERCLOCKWISE)
				) {
					nextIndex=dimension-1-nextIndex;
				}
				angle=angle.previous();
			}

			assert !angle.isOdd() : "corrected angle parameter must be even";
			
			if(angle.compareTo(Angle._0)>0) {//reflect row or column index to opposite side of square matrix
				nextIndex=dimension-1-nextIndex;
			}
			
			return new Segment(nextType,nextIndex,depth);
		}
		
		private class SegmentSequence implements Sequence<T>{

			@Override public int size() {
				return length();
			}

			@Override public T getKey(final int index) {
				return getValue(index);
			}

			@Override public void swap(final int from, final int to) {
				final T value=getValue(from);
				setValue(from,getValue(to));
				setValue(to,value);
			}
			
		}

		public void sort(final Comparator<T> comparator) {
			new QuickSorter<T>(comparator).sort(new SegmentSequence());
		}
		
	}
	
	@SuppressWarnings("unchecked")
	private T[][] allocateData(final int dimension){
		return (T[][]) Array.newInstance(Ordinal.class, new int[] {dimension,dimension});
	}
	
	private static void checkDimension(final int dimension) {
		if(dimension<1) throw new RuntimeException(String.format("wrong dimension %d, it should be at least 1 or greater",dimension));		
	}
	
	/**
	 * Creates square matrix
	 * Note: it doesn't initialize elements, so it's been made private
	 * @param dimension number of columns and rows of square matrix
	 */
	private Matrix(final int dimension) {
		checkDimension(dimension);
		this.dimension=dimension;
		data=allocateData(dimension);
	}
	
	/**
	 * Creates square matrix and fills it with random values within [-dimension..dimension]
	 * @param <X> type of {@code initializer} argument produced by {@code producer}
	 * @param dimension number of columns and rows of square matrix
	 * @param initializer function to produce element's value
	 */
	public <X> Matrix(final int dimension, final Function<X,T> initializer,final Function<Position,? extends X> producer) {
		checkDimension(dimension);
		this.dimension=dimension;
		data=allocateData(dimension);
		initBy(initializer,producer);
	}
	
	/**
	 * Creates instance and initialized it with passed data 'matrix'
	 * @param matrix original integer matrix to copy
	 * @param initializer function that takes element of source matrix and produces value to set in new matrix
	 */
	public Matrix(final int[][] matrix, final Function<Long,T> initializer) {
		this(matrix.length);
		int row=0;
		for(int[] line:matrix) {
			int col=0;
			if(line.length!=dimension) throw new RuntimeException(String.format("the matrix should have same dimension %d both for rows and columns",dimension));
			for(long value:line) {
				data[row][col++]=initializer.apply(value);
			}
			row++;
		}
	}
	
	/**
	 * Creates new square matrix as deep copy of passed one
	 * @param original original matrix to deep-copy
	 * @throws ClassNotFoundException if appropriate class for matrix not found
	 * @throws IOException if stream exception occurred
	 */
	public Matrix(final Matrix<T> original) {
		this.dimension=original.dimension;
		this.data=Utensils.copy(original.data);
	}
	
	/**
	 * Creates new square matrix and initializes it with passed argument {@code value} via {@code initializer}
	 * @param <X> type of {@code initializer} argument 
	 * @param dimension of new matrix
	 * @param initializer function that produces value to set
	 * @param value argument of initializer function
	 */
	public <X> Matrix(final int dimension, final Function<X,T> initializer, final X value) {
		this(dimension);
		for(final Segment row:this) {
			for(final ListIterator<T> i=row.listIterator();i.hasNext();) {
				i.next();
				i.set(initializer.apply(value));
			}
		}
	}

	/**
	 * Creates new matrix as copy of {@code org} and then transforms it by applying {@code processor} to every row element and result of {@code rowAccumulator}
	 * @param org matrix to process
	 * @param accum accumulates elements of passed row
	 * @param processor combines original matrix element and extra parameter to produce new matrix element
	 */
	public Matrix(final Matrix<T> org, final Function<Segment,T> accum, final BiFunction<T,T,T> processor) {
		this(org);
		for(final Segment row:this) {
			final T accumulated=accum.apply(row);
			for(final ListIterator<T> i=row.listIterator();i.hasNext();)
				i.set(processor.apply(i.next(), accumulated));
		}
	}
	
	/**
	* Creates matrix by striking out max (dimension-1) rows and columns for every element of {@code positions} of source matrix {@code org} 
	* Note: 
	* 1. all positions that are located on the same row/column are ignored and skipped except first because deleting 2 rows(columns) and one column(row) leads to creation of non-square matrix
	* 2. dimension of new matrix must remain at least 1 or greater  
	*/
	public Matrix(final Matrix<T> org,final Set<Position> positions){
		//form set of unique columns and rows to cross out
		Set<Integer> columns=new HashSet<>();
		Set<Integer> rows=new HashSet<>();
		int max=org.dimension-1;
		for(final Position position:positions){
			if(!rows.contains(position.getRow()) && !columns.contains(position.getColumn())){//both row/column shouldn't be yet marked for deletion to keep matrix square
				if(max--<=0) break;
				rows.add(position.getRow());
				columns.add(position.getColumn());
			}
		}

		this.dimension = Math.max(org.dimension-rows.size(),1);//matrix dimension should remain at least 1 or greater 
		this.data = allocateData(dimension);
		
		//copy data from original matrix to new one except rows and columns marked for deletion
		int dstRow=0;
		for(int i=0;i<org.data.length;i++){
			if(!rows.contains(Integer.valueOf(i))){
				int dstCol=0;
				for(int j=0;j<org.data[i].length;j++){
					if(!columns.contains(Integer.valueOf(j))){
						data[dstRow][dstCol]=org.data[i][j];
						dstCol++;
					}
				}
				dstRow++;
			}
		}
	}

	@Override public Object clone() {
		return new Matrix<T>(this);
	}
	
	public T get(final int row,final int column) {
		return data[row][column];
	}
	
	public void set(final int row,final int column,final T value) {
		data[row][column]=value;
	}
	
	/**
	 * Initializes matrix with appropriate row or column number of every cell
	 * @param byRow fill in row number, if {@code true}, column number, if {@code false} 
	 */
	@SuppressWarnings("unused")
	private void initByRowColumn(final IndexType iType,final Function<Long,T> initializer) {
		for(int i=0;i<dimension;i++) {
			for(int j=0;j<dimension;j++) {
				data[i][j]=initializer.apply((long)(iType==IndexType.ROW?i:j));
			}
		}
	}

	/**
	 * Initializes matrix with random numbers within range [-dimension..dimension]
	 */
	private <X> void initBy(final Function<X,T> initializer,final Function<Position,? extends X> producer) {
		for(int i=0;i<data.length;i++) {
			for(int j=0;j<data[i].length;j++) {
				data[i][j]=initializer.compose(producer).apply(new Position(i,j));
			}
		}
	}
	
	@Override public String toString() {
		final StringJoiner matrixJoiner=new StringJoiner("","{\n","}\n");
		for(final T[] row:data) {
			final StringJoiner rowJoiner=new StringJoiner(",");
			for(final T value:row) {
				rowJoiner.add(String.format("%10s", value.toString()));
			}
			rowJoiner.add("\n");
			matrixJoiner.merge(rowJoiner);
		}
		return matrixJoiner.toString();
	}
	
	@Override public boolean equals(Object o) {
		if(o instanceof Matrix) {
			@SuppressWarnings("unchecked")
			Matrix<T> m=(Matrix<T>)o;
			if(this.dimension==m.dimension) {
				boolean equal=true;
				for(int row=0;row<this.data.length && equal;row++) {
					equal=equal && Arrays.deepEquals(this.data[row], m.data[row]);
				}
				return equal;
			}else return false;
		}
		else return false;
	}
	
	/**
	* Creates list of positions of elements that are equal to {@code checkValue}
	* @param checkValue value to check
	* @return list of positions of elements that are equal to {@code checkValue}
	*/
	public Set<Position> getEqualTo(final T checkValue){
		Set<Position> occurences=new HashSet<>();
		for(final Segment segment:this){
			for(final Segment.SegmentIterator i=segment.listIterator();i.hasNext();){
				final Position p=i.nextPosition();
				if(p.getValue().equals(checkValue)) occurences.add(p);
			}
		}
		return occurences;
	}
	
	/**
	 * Sorts matrix on given column or row
	 * @param byColumn {@code true} if matrix should by sorted by column, {@code false} by row otherwise
	 * @param index denotes column or row that contains sort key
	 */
	public void sort(final IndexType iType,final int index) {
		if(iType==IndexType.COLUMN) sortByColumn(index);
		else sortByRow(index);
	}

	/**
	 *  Sorts matrix by given column
	 * @param column number of column to sort matrix by
	 */
	public void sortByColumn(final int column) {
		Arrays.sort(data, new Comparator<T[]>() {
			@Override
			public int compare(final T[] o1, final T[] o2) {
				return o1[column].compareTo(o2[column]);
			}	
		});
	}
	
	private void swap(final int row,final int col) {
		final T save=data[row][col];
		data[row][col]=data[col][row];
		data[col][row]=save;
	}
	
	/**
	 * Transforms matrix by swapping rows and columns
	 */
	public void transpose() {
		for(int i=0;i<dimension-1;i++) {
			for(int j=i+1;j<dimension;j++) {
				swap(i,j);
			}
		}
	}

	/**
	 *  Sorts matrix by given row.
	 *  This inefficient implementation transposes square matrix, sorts it by row and transposes it back.
	 * @param row number of row to sort matrix by
	 * @deprecated
	 */
	public void sortByRow(final int row) {
		transpose();
		sortByColumn(row);
		transpose();
	}
	
	/**
	 * Quicksorts matrix by given row
	 * @param row number of row to sort matrix by
	 */
	public void quickSortByRow(final int row) {
		new QuickSorter<T>(Comparator.naturalOrder()).
		sort(new Sequence<T>() {
			
			private final List<T> cache=new ArrayList<>(dimension);
			
			@Override public int size() {
				return dimension;
			}
			
			@Override
			public T getKey(final int column) {
				return data[row][column];
			}

			@Override
			public void swap(final int first, final int second) {
				getColumn(first,cache);
				copyColumn(first,second);
				setColumn(second,cache);
			}
		});
	}
	
	/**
	 * Quicksorts matrix by given column
	 * @param column number of column to sort matrix by
	 */
	public void quickSortByColumn(final int column) {
		new QuickSorter<T>(Comparator.naturalOrder()).
		sort(new Sequence<T>() {
			
			private final List<T> cache=new ArrayList<T>(dimension);
			
			@Override public int size() {
				return dimension;
			}
			
			@Override
			public T getKey(final int row) {
				return data[row][column];
			}

			@Override
			public void swap(final int first, final int second) {
				getRow(first,cache);
				copyRow(first,second);
				setRow(second,cache);
			}
		});
	}
	
	/**
	 * Quicksorts matrix by row or column
	 * @param iType {@code COLUMN} if matrix should be sorted by column, {@code ROW} otherwise
	 * @param index column or row index to sort on
	 */
	public void quickSort(final IndexType iType,final int index) {
		if(iType==IndexType.COLUMN) {
			quickSortByColumn(index);
		}else {
			quickSortByRow(index);
		}
	}

	/**
	 * Finds index of source column or row by shifting {@code step} paces in given direction {@direct} from destination column or row
	 * @param destination index of destination column or row
	 * @param direct direction to move left or down if {@code LEFT_DOWN}, right or up if {@code RIGHT_UP}
	 * @param step numbers of columns or rows to skip
	 * @return index of source column or row for given {@code destination} parameter 
	 */
	private int getAdjacentIndex(final int destination,final Direction direct,final int step) {
		int source=destination;
		switch(direct) {
		case RIGHT_DOWN:
			source-=step;
			if(source<0) source+=dimension;
			break;
		case LEFT_UP:
			source+=step;
			if(source>=dimension) source-=dimension;
			break;
		}
		return source;  
	}
	
	/**
	 * Copies whole source column at {@code sourceColumn} to destination column {@code destColumn}
	 * @param destColumn index of destination column
	 * @param sourceColumn index of source column
	 */
	private void copyColumn(final int destColumn,final int sourceColumn) {
		for(int row=0;row<dimension;row++) {
			data[row][destColumn]=data[row][sourceColumn];
		}
	}
	
	/**
	 * Copies whole source row at {@code sourceRow} to destination row {@code destRow}
	 * @param destRow index of destination row
	 * @param sourceRow index of source row
	 */
	private void copyRow(final int destRow,final int sourceRow) {
		System.arraycopy(data[sourceRow], 0, data[destRow], 0, dimension);
	}
	
	/**
	 * Copies row or column from {@code sourceIndex} to {@code destIndex}
	 * @param iType copy row if {@code ROW}, otherwise copy column
	 * @param destIndex destination row or column index
	 * @param sourceIndex source row or column index
	 */
	private void copyRowColumn(final IndexType iType,final int destIndex,final int sourceIndex) {
		if(iType==IndexType.COLUMN) copyColumn(destIndex,sourceIndex);
		else copyRow(destIndex,sourceIndex);
	}
	
	/**
	 * Caches whole column of matrix at {@code column} and returns it
	 * @param column index of column to copy
	 * @return copy of column at {@code column}
	 */
	private List<T> getColumn(final int column) {
		final List<T> copy=new ArrayList<>(dimension);
		for(int row=0;row<dimension;row++) {
			copy.add(data[row][column]);
		}
		return copy;
	}
	
	/**
	 * Caches whole column of matrix at {@code column} and returns it as passed parameter {@code cache}
	 * @param column index of column to copy
	 */
	private void getColumn(final int column,final List<T> cache) {
		cache.clear();
		for(int row=0;row<dimension;row++) {
			cache.add(data[row][column]);
		}
	}
	
	/**
	 * Caches row of matrix at {@code row} and returns it
	 * @param row index of row to copy
	 * @return copy of row at {@code row}
	 */
	private List<T> getRow(final int row) {
		return new ArrayList<T>(Arrays.asList(data[row]));
	}
	
	/**
	 * Caches row to passed parameter {@code cache}
	 * @param row index of row to copy
	 * @param cache shallow copy of row at {@code row}
	 */
	private void getRow(final int row,final List<T> cache) {
		cache.clear();
		for(final T value:data[row]) {
			cache.add(value);
		}
	}
	
	/**
	 * Returns copy of row or column
	 * @param iType if {@code COLUMN} then return copy of column at {@code rowColumn}, otherwise return copy of row at {@code rowColumn}
	 * @param index index of row or column to return
	 * @return copy of row or column
	 */
	private List<T> getRowColumn(final IndexType iType,final int index) {
		if(iType==Matrix.IndexType.COLUMN) return getColumn(index);
		else return getRow(index);
	}
	
	/**
	 * Caches copy of row or column as passed parameter {@code cache}
	 * @param iType if {@code COLUMN} then pass copy of column at {@code rowColumn}, otherwise pass copy of row at {@code rowColumn}
	 * @param index index of row or column to pass
	 */
	@SuppressWarnings("unused")
	private void getRowColumn(final IndexType iType,final int index,final List<T> cache) {
		if(iType==Matrix.IndexType.COLUMN) getColumn(index,cache);
		else getRow(index,cache);
	}
	
	/**
	 * Saves passed data {@code columnData} at specified column {@code column} of matrix 
	 * @param column index of column to save
	 * @param columnData data to replace column
	 */
	private void setColumn(final int column,final List<T> columnData) {
		for(int row=0;row<dimension;row++) {
			data[row][column]=columnData.get(row);
		}
	}
	
	/**
	 * Saves passed data {@code rowData} at specified row {@code row} of matrix 
	 * @param row index of row to save 
	 * @param rowData data to replace row
	 */
	private void setRow(final int row,final List<T> rowData) {
		int col=0;
		for(final T value:rowData) {
			data[row][col++]=value;
		}
	}
	
	/**
	 * Copies passed data {@code data} either as column or row at {@code index}
	 * @param iType saves passed {@code data} as column if equal to {@code COLUMN}, and a row otherwise
	 * @param index index of column or row to set data to
	 * @param data contains data to save as matrix column or row
	 */
	private void setRowColumn(final IndexType iType,final int index,final List<T> data) {
		if(iType==IndexType.COLUMN) setColumn(index,data);
		else setRow(index,data);
	}

	/**
	 * Shifts matrix content {@code step} paces in specified direction {@code direct}
	 * @param iType {@code ROW} if rows should be shifted up/down, {@code COLUMN} if columns should be shifted left/right
	 * @param direct shifts left, if {@code LEFT}, right if {@code RIGHT}
	 * @param step number of paces to shift matrix data
	 */
	public void shift(final IndexType iType,final Direction direct,final int step) {
		int destIndex=0;
		int count=dimension;
		do{
			int initialIndex=destIndex;//first index of periodical sequence (period=step)
			List<T> cache=getRowColumn(iType, initialIndex);//cache first column/row
			
			int sourceIndex=destIndex;
			//find source index and copy source column to destination until sequence traversed
			while(count>0 && 
					(sourceIndex=getAdjacentIndex(destIndex, direct, step))!=initialIndex) {
				copyRowColumn(iType,destIndex, sourceIndex);
				count--;
				destIndex=sourceIndex;		
			};
			setRowColumn(iType,destIndex, cache);//save first column as last one
			count--;
			if(count>0){//sequence traversed completely (sourceIndex==initialIndex), so
				destIndex=getAdjacentIndex(destIndex,direct,1);//step aside, take next adjacent column as first one of periodical sequence and proceed
			}
		}while(count>0);
		
	}
	
	/**
	 * Finds biggest continuous block of numbers
	 * @param iType if {@code ROW} seek along every row, if {@code COLUMN} seek along every column
	 * @param ascending numbers must be arranged in ascending order, if {@code true}, otherwise in descending order
	 * @return copy of largest continuous block of numbers
	 */
	public Segment getLargestContinuousBlock(final IndexType iType,final boolean ascending) {
		if(iType==IndexType.ROW)	return getLargestRowContinuouosBlock(ascending);
		else return getLargestColumnContinuouosBlock(ascending);
	}
	
	/**
	 * Finds biggest continuous block of numbers along column
	 * @param ascending numbers must be arranged in ascending order, if {@code true}, otherwise in descending order
	 * @return instance of class {@code Segment} that points at largest continuous block of numbers along column in given matrix
	 */
	private Segment getLargestColumnContinuouosBlock(final boolean ascending) {
		Segment block=new Segment(IndexType.COLUMN,0,0,1);
		int max=1;
		final Comparator<T> comp=ascending?Comparator.naturalOrder():Comparator.reverseOrder();
		for(int column=0;column<dimension;column++) {
			int startIndex=0;
			do{
				int endIndex=startIndex+1;
				while(endIndex<dimension && comp.compare(data[endIndex-1][column], data[endIndex][column])<0) {
					endIndex++;//propagate segment [startIndex..endIndex] one step right
				}
				if(endIndex-startIndex>max) {
					max=endIndex-startIndex;
					block=new Segment(IndexType.COLUMN,column,startIndex,endIndex);
				}
				startIndex=endIndex;
			}while(startIndex<dimension);
		}
		return block;
	}

	/**
	 * Finds biggest continuous block of numbers along row
	 * @param ascending numbers must be arranged in ascending order, if {@code true}, otherwise in descending order
	 * @return instance of class {@code Segment} that points at largest continuous block of numbers along row in given matrix
	 */
	private Segment getLargestRowContinuouosBlock(final boolean ascending) {
		Segment block=new Segment(IndexType.ROW,0,0,1);
		int max=1;
		final Comparator<T> comp=ascending?Comparator.naturalOrder():Comparator.reverseOrder();
		for(int row=0;row<dimension;row++) {
			int startIndex=0;
			do{
				int endIndex=startIndex+1;
				while(endIndex<dimension && comp.compare(data[row][endIndex-1], data[row][endIndex])<0) {
					endIndex++;//propagate segment [startIndex..endIndex] one step right
				}
				if(endIndex-startIndex>max) {
					max=endIndex-startIndex;
					block=new Segment(IndexType.ROW,row,startIndex,endIndex);
				}
				startIndex=endIndex;
			}while(startIndex<dimension);
		}
		return block;
	}
	
	/**
	 * Finds next positive number starting from given column {@code startColumn} within row {@code row} 
	 * @param row row to seek positive number in
	 * @param startColumn start search from this column
	 * @return column number of next positive value in row or {@code dimension}
	 */
	private int findNextPositive(final int row,final int startColumn) {
		int column=startColumn;
		while(column<dimension && !data[row][column].positive()) {
			column++;
		}
		return column;
	}
	
	/**
	 * Accumulates sum of numbers between first and second positive value for every row and collects them in resulting array  
	 * @return array of accumulated sums
	 */
	public List<T> getAccumulatedSumsBetweenFirstAndSecondPositiveElement() {
		final List<T> segments=new ArrayList<>(dimension);
		for(int row=0;row<dimension;row++) {
			final int start=findNextPositive(row, 0);
			final int finish=findNextPositive(row, start+1);
			if(start+1<finish) {
				segments.add(new Segment(IndexType.ROW,row,start+1,finish).sum());
			}
		}
		return segments;
	}
	
	/**
	 * Computes matrix norm
	 * @param iType find maximum value for rows, if {@code ROW}, or for columns, if {@code COLUMN}
	 * @return matrix norm
	 */
	public T getNorm(final IndexType iType) {
		final Iterator<Segment> i=iterator(iType);
		if(i.hasNext()) {
			T norm=i.next().sum(Ordinal::abs);
			while(i.hasNext()) {
				norm=norm.max(i.next().sum(Ordinal::abs));
			}
			return norm;
		}else {
			throw new RuntimeException("matrix dimension should be at least 1 or more");
		}
	}
	
	/**
	 * Rotates matrix in specified direction {@code rotation} by given {@code angle} 
	 * @param rotation rotates clockwise if {@code CLOCKWISE} or {@code COUNTERCLOCKWISE} otherwise
	 * @param angle of rotation (either 0,90,180 or 270)
	 */
	public void rotate(final Rotation rotation,final Angle angle) {
		for(int depth=0;depth<dimension/2;depth++) {//for every row/column from outside to center of matrix
			Segment initialSegment=new Segment(IndexType.ROW,depth,depth);
			int turnCount=SIDE_COUNT-1;
			do {
				List<T> initialSegmentData=initialSegment.save();
				Segment destination=initialSegment;
				Segment source;
				while(turnCount>0 && 
						!initialSegment.equals(source=destination.getNextSegment(rotation, angle.getOpposite(), depth))) {//find source segment 
					destination.copy(source);//and copy it to destination segment  
					destination=source;//move to next segment
					turnCount--;
				};
				destination.restore(initialSegmentData);
				turnCount--;
				if(turnCount>0) {//if angle.ordinal() is even, take adjacent quadrant and repeat until sideCount>0
					initialSegment=initialSegment.getNextSegment(rotation.getOpposite(), Angle._90, depth);
				}
				
			}while(turnCount>0);

		}
	}
	
	/**
	 * Recursively computes determinant of square matrix
	 * @return determinant of square matrix
	 */
	public T getDeterminantRecursive() {
		boolean free[]=new boolean[dimension];//occupied column flags
		Arrays.fill(free, true);
		return getDeterminantRecursive(0,free);
	}
	
	private T getDeterminantRecursive(final int row,final boolean[] free) {
		T accum;
		if(row+1==dimension) {//last row, size of matrix is equal to 1
			int k=0;
			while(k<dimension && !free[k]) k++;//skip occupied columns until free one found
			accum=data[row][k];
		}else {//scan given 'row', compute determinants and combine into final result
			assert dimension>=1: "matrix must contain at least 1 element";
			accum=data[0][0].zero();//matrix size be at least 1 and elements initialized beforehand
			int position=0;
			for(int column=0;column<dimension;column++) {
				if(free[column]) {
					T value=data[row][column];
					free[column]=false;//occupy column
					T subDet=getDeterminantRecursive(row+1,free);
					T component=value.multiply(subDet);
					accum=position%2==0?
							accum.add(component):accum.subtract(component);
					free[column]=true;//free column
					position++;
				}
			}
		}
		return accum;	
	}
	
	/**
	 * Non-recursively computes determinant of square matrix
	 * @return determinant of square matrix
	 */
	public T getDeterminant() {
		
		assert dimension>=1: "matrix must contain at least 1 element";
		final T zero=data[0][0].zero();//size be at least 1 and elements initialized beforehand
		
		class TraversalState {//holds data for matrix traversal
			
			List<T> totals=new ArrayList<T>(dimension);//accumulated totals for every line from top to bottom
			int[] position=new int[dimension];//number of next component
			int[] selectedColumns=new int[dimension];//collection of selected columns from top to bottom
			BitSet occupiedColumns=new BitSet(dimension);//flag set of occupied columns
			
			int line=0;//points at current line
			
			{//reset to initial state
				for(int k=0;k<dimension;k++) totals.add(zero);
				selectedColumns[0]=0;
				position[0]=0;
			}
			
			void moveUpAndRollUp() {//move one line up, compute and add next component to its total 
				line--;
				int selColumn=selectedColumns[line];
				occupiedColumns.clear(selColumn);//free column
				T product=data[line][selColumn].multiply(totals.get(line+1));
				totals.set(line,
						position[line]%2==0?
								totals.get(line).add(product):
									totals.get(line).subtract(product));
				position[line]++;
			}
			
			void moveDownAndInitialize(final int column) {//move down one line and set initial values
				selectedColumns[line]=column;
				occupiedColumns.set(column);
				line++;
				selectedColumns[line]=0;//start from leftmost column
				totals.set(line,zero);//clear line total
				position[line]=0;//start counting components of total				
			}
			
			boolean firstLine() {
				return line==0;
			}
			
			boolean lastLine() {
				return line+1==dimension;
			}

			T getResult() {
				return totals.get(0);//holds computed determinant of whole matrix
			}
			
			void saveTotal(final int column) {
				totals.set(line,data[line][column]);
			}

			int findNextFreeColumn() {
				return occupiedColumns.nextClearBit(selectedColumns[line]);
			}
			
			boolean hasNextColumn() {
				return ++selectedColumns[line]<dimension;
			}
			
		}
		
		TraversalState traversal=new TraversalState();
		
		boolean up=false;
		do {
			
			if(up) {//previous iteration gained determinant of submatrix, moving up
				if(traversal.firstLine()) break;//the whole matrix processed, hence terminate loop
				traversal.moveUpAndRollUp();//move up and roll up intermediate result				
				if(traversal.hasNextColumn()) {//move to next column and dive down otherwise move up
					up=false;
				}			
			}else {
				//select next unoccupied column within [0..dimension)
				int column=traversal.findNextFreeColumn();
				if(column<dimension) {//there is at least one column yet
					if(traversal.lastLine()) {
						traversal.saveTotal(column);//save the only value left in reduced 1x1 matrix
						up=true;//move up
					}else {//move one step down
						traversal.moveDownAndInitialize(column);
						up=false;//move down
					}					
				}else {
					up=true;
				}
			}

		}while(true);
		
		return traversal.getResult();
	}
	
	/**
	* Swaps row and column of position {@code a} with correspondent row/column of position {@code b}
	* @param a first position within matrix to interchange row and column
	* @param b second position within matrix to interchange row and column
	* @return reference to {@code this} matrix
	*/
	public Matrix<T> swapSegments(final Position a,final Position b){
		new Segment(IndexType.ROW,a.getRow()).
			swap(new Segment(IndexType.ROW,b.getRow()));
		new Segment(IndexType.COLUMN,a.getColumn()).
			swap(new Segment(IndexType.COLUMN,b.getColumn()));
		return this;
	}
	
	/**
	 * Orders every row of matrix by provided comparator
	 * @param comparator determines sorting order
	 * @return sorted matrix
	 */
	public Matrix<T> sortRows(final Comparator<T> comparator) {
		for(final Segment segment:this) {
			segment.sort(comparator);
		}
		return this;
	}
	
	/**
	 * Processes every item of matrix by given {@code processor}
	 * @param processor processing method reference
	 * @return processed matrix
	 */
	public Matrix<T> process(final Function<T,T> processor){
		for(final Segment segment:this) {
			for(final Segment.SegmentIterator i=segment.listIterator();i.hasNext();) {
				i.set(processor.apply(i.next()));
			}
		}
		return this;
	}
	
	/**
	* Collects positions of extremums within the whole matrix
	* @param maximum collect maximums if {@code true} or minimums if {@code false}
	* @return list of extremums
	*/
	public Set<Position> getExtremums(final boolean maximum) {
		checkDimension(dimension);
		final Set<Position> extremums=new HashSet<>();
		extremums.add(new Position(0, 0));
		for(final Segment segment:this) {
			segment.getExtremumList(extremums, maximum);
		}
		return extremums;
	}
	
	/**
	 * Collects positions of extremums within each segment of matrix (either row or column) and computes intersection set
 	 * @param iType determine extremum for row if {@code ROW} or column if {@code COLUMN}
	 * @param maximum collect maximums if {@code true} or minimums if {@code false}
	 * @return list of extremums
	 */
	public Set<Position> getExtremumsForEachSegment(final IndexType iType,final boolean maximum) {
		final Set<Position> extremums=new HashSet<>();
		for(final Iterator<Segment> i=iterator(iType);i.hasNext();) {
			extremums.addAll(i.next().getExtremums(maximum));
		}
		return extremums;
	}
	
	/**
	 * Fetches list of saddle points for given matrix
	 * @return set of positions where saddle points located
	 */
	public Set<Position> getSaddlePoints(){
		Set<Position> minimums=getExtremumsForEachSegment(IndexType.ROW,false);
		Set<Position> maximums=getExtremumsForEachSegment(IndexType.COLUMN,true);
		minimums.retainAll(maximums);
		return minimums;
	}
	
	/**
	 * Quicksorts data that comply with {@code Sequence} interface via passed comparator and methods for size determination, key mapping and element swapping
	 * @param <X> type of key to sort on
	 * @param comparator to order keys of sequence elements
	 * @param sizeFunc function that returns size of sequence to sort
	 * @param keyMapFunc function that maps index of sequence element to key
	 * @param swapFunc function that interchanges two elements of a sequence  
	 */
	public Matrix<T> sortBy(
			final Comparator<? super T> comparator,final Supplier<Integer> sizeFunc,final Function<Integer,? extends T> keyMapFunc,final BiConsumer<Integer,Integer> swapFunc) {

		new QuickSorter<T>(comparator).sort(new Sequence<T>() {
			@Override public int size() {
				return sizeFunc.get();
			}

			@Override public T getKey(final int index) {
				return keyMapFunc.apply(index);
			}

			@Override public void swap(final int from, final int to) {
				swapFunc.accept(from, to);
			}
		});
		
		return this;
	}

	/**
	 * Collects set of local extremums within matrix
	 * @param maximum look for maximums if {@code true} and for minimums if {@code false}
	 * @return set of local extremums
	 */
	public Set<Position> getLocalExtremums(final boolean maximum){
		final Set<Position> extremums=new HashSet<>();
		for(final Segment row:this) {
			for(final Segment.SegmentIterator i=row.listIterator();i.hasNext();) {
				final Position position=i.nextPosition();
				if(position.isLocalExtremum(maximum)) extremums.add(position);
			}
		}
		return extremums;
	}
	
	/**
	 * Collects local extremums within matrix and places them into sorted set of {@code Position}
	 * @param maximum look for maximums if {@code true} and for minimums if {@code false}
	 * @return sorted set of local extremums
	 */
	  public SortedSet<Position> getSortedSetOfLocalExtremums(final boolean maximum){ 
		  final SortedSet<Position> set=new TreeSet<>(VALUE_COMPARATOR);
		  set.addAll(getLocalExtremums(maximum));
		  return set;
	 }
	
	/**
	 * The class provides access to parent matrix as set of pairs of diagonals for further ordering by sorter class
	 * @author Serhii Pylypenko
	 */
	public class DiagonallyScannedSequence implements Sequence<T> {
			  
		@Override public int size() {
			return getDimension()*getDimension();
		}
		
		//maps index of sequence item to row&column 
		private Position mapIndexToPosition(final int index) {
			final int dividend=index/getDimension();
			final int remainder=index%getDimension();
			final int firstPartSize=getDimension()-dividend;
			//final int secondPartSize=dividend;
			final int row=remainder<firstPartSize?
					dividend+remainder:
						remainder-firstPartSize;
			final int column=remainder;
			return new Position(row,column);
		}
	
		@Override public T getKey(final int index) {
			return mapIndexToPosition(index).getValue();
		}
	
		@Override public void swap(final int from, final int to) {
			final Position fromRef=mapIndexToPosition(from);
			final Position toRef=mapIndexToPosition(to);
			
			final T save=fromRef.getValue();
			fromRef.setValue(toRef.getValue());
			toRef.setValue(save);	
		}
		  
	}
	 
}
