package chapter2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.BiFunction;
import java.util.function.Function;

import chapter2.Matrix.Range.RangeIterator;
import sort.QuickSorter;
import sort.QuickSorter.Sequence;

/**
 * 
 * @author Serhii Pylypenko
 * @since 2020-02-15
 * @version 1.2
 * 
 */
public class Matrix implements Cloneable, Iterable<Matrix.Range> {
	
	public final static int SIDE_COUNT=4;
	
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
	private final BigInteger[][] data;
	
	@Override
	public Iterator<Range> iterator() {
		return new Iterator<Range>() {
			private int row=0;

			@Override public boolean hasNext() {
				return row<dimension;
			}

			@Override public Range next() {
				return new Range(IndexType.ROW,row++,0,dimension);
			}
		};
	}
	
	/**
	 * Represents position of one element of matrix
	 */
	class Position implements Comparable<Position> {
		private final int row, column;
		
		public Position(final int row,final int column) {
			this.row=row;
			this.column=column;
		}
		
		public int getRow() { return row;}
		
		public int getColumn() { return column;}
		
		public BigInteger getValue() {
			return data[row][column];
		}
		
		@Override public boolean equals(final Object o) {
			if(o instanceof Position) {
				return getValue().equals(((Position)o).getValue());
			}else if(o instanceof BigInteger){
				return getValue().equals((BigInteger)o);
			}else return false;
		}
		
		@Override public int compareTo(final Position p) {
			return getValue().compareTo(p.getValue());
		}

		/**
		* Updates current extremum value and list of extremum positions
		* @param list of accumulated extremum value positions
		* @param extremum value found on previous iterations
		* @return position of current extremum value
		*/
		final Position updateExtremumList(final List<Position> list, final Position extremum, final BiFunction<Position,Position,Integer> compareFunc) {
			Position newExtremum=extremum;
			if(compareFunc.apply(this,extremum)>0) {//new extremum found
				newExtremum=this;
				list.clear();
				list.add(this);//start accumulating extremums anew
			}else if(compareFunc.apply(this,extremum)==0) {
				list.add(this);//add second copy of current extremum
			}
			return newExtremum;
		}
		
		@Override public String toString(){
			return String.format("{%d,%d}", row,column);
		}
		
	}
	
	/**
	 * Points at segment of row (if {@code indexType} equals to {@code IndexType.ROW}) or 
	 * column (if {@code indexType} equals to {@code IndexType.COLUMN}) at {@code index} of outer matrix instance
	 * starting from {@code start} up to {@code finish}
	 */
	public class Range implements Iterable<BigInteger> {
		private final IndexType indexType;
		private final int index;
		private final int start, finish;
		private boolean quadrantSpecific=false;
		
		public Range(final IndexType iType,final int index) {
			this(iType,index,0,dimension);
		}
		
		public Range(final IndexType iType,final int index,final int start,final int finish) {
			this.indexType=iType;
			this.index=index;
			this.start=start;
			this.finish=finish;
		}
		
		public Range(final IndexType iType,final int index,final int depth) {
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
		
		private BigInteger getValue(final int k) {
			return indexType==IndexType.COLUMN?
					data[k][index]:
						data[index][k];
		}
		
		private void setValue(final int k,final BigInteger value) {
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
		
		@Override public boolean equals(Object o) {
			if(o instanceof Range) {
				Range r=(Range)o;
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
			for(BigInteger value:this) {
				joiner.add(String.format("%s", value.toString()));
			}
			return joiner.toString();
		}
		
		public BigInteger sum() {
			BigInteger accum=BigInteger.ZERO;
			for(BigInteger value:this) {
				accum=accum.add(value);
			}
			return accum;
		}
		
		public BigInteger average() {
			return sum().divide(BigInteger.valueOf(length()));
		}
		
		public int length() {
			return finish-start;
		}
		
		/**
		* Scans range and collects positions of extremum value within the range
		* @param list of collected positions from previous iteration
		* @return list of positions of extremum values of the range merged with {@code list}
		*/
		public void getExtremumList(final List<Position> list, final BiFunction<Position,Position,Integer> comparePositionFunc){
			if(list.isEmpty()) throw new RuntimeException("list of accumulated extremums should contain at least one item"); 
			Position extremum = list.get(0);
			for(RangeIterator i=listIterator();i.hasNext();){
				extremum = i.nextPosition().updateExtremumList(list, extremum, comparePositionFunc);
			}
		}
		
		public void copy(final Range source) {
			int k=startIndex();
			for(final BigInteger value:source) {
				setValue(k,value);
				k=nextIndex(k);
			}
		}
		
		public BigInteger[] save() {
			BigInteger[] data=new BigInteger[length()];
			int k=0;
			for(final BigInteger value:this) {
				data[k++]=value;
			}
			return data;
		}
		
		public void restore(final BigInteger[] data) {
			if(data.length>length()) throw new RuntimeException(String.format("size of passed array should be equal or less than %d",length()));
			int k=startIndex();
			for(final BigInteger value:data) {
				setValue(k,value);
				k=nextIndex(k);
			}
		}
		
		/**
		* Scans this range and swaps elements with passed {@code range} 
		*/
		public void swap(final Range range){
			int dstIndex=startIndex();
			for(int srcIndex=startIndex();hasNext(srcIndex);srcIndex=nextIndex(srcIndex)){
				final BigInteger value=getValue(srcIndex);
				setValue(srcIndex,range.getValue(dstIndex));
				range.setValue(dstIndex,value);
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
		
		class RangeIterator implements ListIterator<BigInteger>{
			private int index=startIndex();//points at 'next' element
			private boolean movingRight=true;//to determine correct index for 'set' operation

			@Override public boolean hasNext() {
				return Range.this.hasNext(index);
			}

			@Override public int nextIndex() {
				return index;
			}

			@Override public BigInteger next() {
				movingRight=true;
				final BigInteger value=getValue(index);
				index=Range.this.nextIndex(index);
				return value;
			}
			
			public Position nextPosition(){
				final int kNext=nextIndex();
				next();
				return createPosition(kNext);
			}
				
			@Override public void set(final BigInteger e) {
				setValue(movingRight?Range.this.previousIndex(index):index,e);
			}

			@Override public boolean hasPrevious() {
				return ascendingIndex()? index>start: index<finish-1;
			}

			@Override public int previousIndex() {
				return Range.this.previousIndex(index);
			}

			@Override public BigInteger previous() {
				movingRight=false;
				index=Range.this.previousIndex(index);
				return getValue(index);
			}

			@Override public void remove() {
				throw new UnsupportedOperationException("RangeIterator doesn't support remove operation");
			}

			@Override public void add(BigInteger e) {
				throw new UnsupportedOperationException("RangeIterator doesn't support add operation");
			}
			
		}
		
		@Override
		public Iterator<BigInteger> iterator() {
			return listIterator();
		}

		public RangeIterator listIterator() {
			return new RangeIterator();
		}
		
		public Range getNextRange(final Rotation rotation,Angle angle,final int depth) {
			
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

			assert !angle.isOdd() : "corrected angle parameter should be even";
			
			if(angle.compareTo(Angle._0)>0) {//reflect row or column index to opposite side of square matrix
				nextIndex=dimension-1-nextIndex;
			}
			
			return new Range(nextType,nextIndex,depth);
		}
		
		private class RangeSequence implements Sequence<Range,BigInteger>{

			@Override public int size() {
				return length();
			}

			@Override public BigInteger getKey(final int index) {
				return getValue(index);
			}

			@Override public void swap(final int from, final int to) {
				final BigInteger value=getValue(from);
				setValue(from,getValue(to));
				setValue(to,value);
			}
			
		}

		public void sort(Comparator<BigInteger> comparator) {
			new QuickSorter<Range,BigInteger>(comparator).sort(new RangeSequence());
		}
		
}
	
	/**
	 * Creates square matrix and fills it with random values within [-dimension..dimension]
	 * @param dimension number of columns and rows of square matrix
	 */
	public Matrix(final int dimension) {
		if(dimension<1) throw new RuntimeException(String.format("wrong dimension %d, it should be at least 1 or greater",dimension));
		this.dimension=dimension;
		data=new BigInteger[dimension][dimension];
		initByRandom();
		//initByRowColumn(IndexType.COLUMN);
	}
	
	/**
	 * Creates instance and initialized it with passed data 'matrix'
	 */
	public Matrix(final int[][] matrix) {
		if(matrix.length<1) throw new RuntimeException(String.format("wrong dimension %d, it should be at least 1 or greater",matrix.length));
		this.dimension=matrix.length;
		data=new BigInteger[dimension][dimension];
		int row=0;
		for(int[] line:matrix) {
			int col=0;
			if(line.length!=dimension) throw new RuntimeException(String.format("the matrix should have same dimension %d for rows and columns",dimension));
			for(int value:line) {
				data[row][col++]=BigInteger.valueOf(value);
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
	public Matrix(final Matrix original) {
		this.dimension=original.dimension;
		data=deepCopyData(original);
	}
	
	/**
	 * Creates new matrix as copy of {@code org} and then transforms it by applying {@code processor} to every row element and result of {@code rowAccumulator}
	 * @param org matrix to process
	 * @param rowAccum accumulates elements of passed row
	 * @param processor combines original matrix element and extra parameter to produce new matrix element
	 */
	public Matrix(final Matrix org, Function<Range,BigInteger> accum, BiFunction<BigInteger,BigInteger,BigInteger> processor) {
		this(org);
		for(final Range row:this) {
			BigInteger accumulated=accum.apply(row);
			for(final ListIterator<BigInteger> i=row.listIterator();i.hasNext();)
				i.set(processor.apply(i.next(), accumulated));
		}
	}
	
	/**
	* Creates matrix by striking out max (dimension-1) rows and columns for every element of {@code positions} of source matrix {@code org} 
	* Note: 
	* 1. all positions that are located on the same row/column are ignored and skipped except first because deleting 2 rows(columns) and one column(row) leads to creation of non-square matrix
	* 2. dimension of new matrix must remain at least 1 or greater  
	*/
	public Matrix(final Matrix org,final List<Position> positions){
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
		this.data =new BigInteger[dimension][dimension];
		
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

	private BigInteger[][] deepCopyData(final Matrix original) {
		BigInteger[][] data=null;
		try{
			ByteArrayOutputStream bos=new ByteArrayOutputStream();
			ObjectOutputStream os=new ObjectOutputStream(bos);
			os.writeObject(original.data);
			os.close();
			ObjectInputStream is=new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
			data=(BigInteger[][]) is.readObject();
		}catch(IOException exc) {
			throw new RuntimeException(exc);
		}catch(ClassNotFoundException exc2) {
			throw new RuntimeException(exc2);
		}
		return data;
	}
	
	@Override public Object clone() {
		return new Matrix(this);
	}
	
	public BigInteger get(final int row,final int column) {
		return data[row][column];
	}
	
	public void set(final int row,final int column,final BigInteger value) {
		data[row][column]=value;
	}
	
	/**
	 * Initializes matrix with appropriate row or column number of every cell
	 * @param byRow fill in row number, if {@code true}, column number, if {@code false} 
	 */
	@SuppressWarnings("unused")
	private void initByRowColumn(final IndexType iType) {
		for(int i=0;i<dimension;i++) {
			for(int j=0;j<dimension;j++) {
				data[i][j]=BigInteger.valueOf(iType==IndexType.ROW?i:j);
			}
		}
	}

	/**
	 * Initializes matrix with random numbers within range [-dimension..dimension]
	 */
	private void initByRandom() {
		for(int i=0;i<data.length;i++) {
			for(int j=0;j<data[i].length;j++) {
				data[i][j]=BigInteger.valueOf(
						Math.round((Math.random()*2*dimension-dimension)));
			}
		}
	}
	
	@Override public String toString() {
		final StringJoiner matrixJoiner=new StringJoiner("","{\n","}\n");
		for(BigInteger[] row:data) {
			final StringJoiner rowJoiner=new StringJoiner(",");
			for(BigInteger value:row) {
				rowJoiner.add(String.format("%10s", value.toString()));
			}
			rowJoiner.add("\n");
			matrixJoiner.merge(rowJoiner);
		}
		return matrixJoiner.toString();
	}
	
	@Override public boolean equals(Object o) {
		if(o instanceof Matrix) {
			Matrix m=(Matrix)o;
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
	* Locates positions of extremum value within matrix
	* @return list of extremum value positions
	*/
	public List<Position> getExtremum(final BiFunction<Position,Position,Integer> comparePositionFunc) {
		final List<Position> list=new ArrayList<>(dimension);
		list.add(new Position(0, 0));
		for(final Iterator<Range> i=iterator();i.hasNext();) {
			i.next().getExtremumList(list,comparePositionFunc);
		}
		return list;
	}
	
	/**
	* Creates list of positions of elements that are equal to {@code checkValue}
	* @param checkValue value to check
	* @return list of positions of elements that are equal to {@code checkValue}
	*/
	public List<Position> getEqualTo(final BigInteger checkValue){
		List<Position> list=new ArrayList<>(dimension);
		for(final Range range:this){
			for(final RangeIterator i=range.listIterator();i.hasNext();){
				Position p=i.nextPosition();
				if(p.equals(checkValue)) list.add(p);
			}
		}
		return list;
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
		Arrays.sort(data, new Comparator<BigInteger[]>() {
			@Override
			public int compare(BigInteger[] o1, BigInteger[] o2) {
				return o1[column].compareTo(o2[column]);
			}	
		});
	}
	
	private void swap(final int row,final int col) {
		BigInteger save=data[row][col];
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
		new QuickSorter<BigInteger[],BigInteger>(Comparator.naturalOrder()).
		sort(new Sequence<BigInteger[],BigInteger>() {
			
			private final BigInteger[] cache=new BigInteger[dimension];
			
			@Override public int size() {
				return dimension;
			}
			
			@Override
			public BigInteger getKey(final int column) {
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
		new QuickSorter<BigInteger[],BigInteger>(Comparator.naturalOrder()).
		sort(new Sequence<BigInteger[],BigInteger>() {
			
			private final BigInteger[] cache=new BigInteger[dimension];
			
			@Override public int size() {
				return dimension;
			}
			
			@Override
			public BigInteger getKey(final int row) {
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
	private BigInteger[] getColumn(final int column) {
		final BigInteger[] copy=new BigInteger[dimension];
		for(int row=0;row<dimension;row++) {
			copy[row]=data[row][column];
		}
		return copy;
	}
	
	/**
	 * Caches whole column of matrix at {@code column} and returns it as passed parameter {@code cache}
	 * @param column index of column to copy
	 */
	private void getColumn(final int column,final BigInteger[] cache) {
		for(int row=0;row<dimension;row++) {
			cache[row]=data[row][column];
		}
	}
	
	/**
	 * Caches row of matrix at {@code row} and returns it
	 * @param row index of row to copy
	 * @return copy of row at {@code row}
	 */
	private BigInteger[] getRow(final int row) {
		return Arrays.copyOf(data[row], dimension);
	}
	
	/**
	 * Caches row to passed parameter {@code cache}
	 * @param row index of row to copy
	 * @param cache shallow copy of row at {@code row}
	 */
	private void getRow(final int row,final BigInteger[] cache) {
		System.arraycopy(data[row], 0, cache, 0, dimension);
	}
	
	/**
	 * Returns copy of row or column
	 * @param iType if {@code COLUMN} then return copy of column at {@code rowColumn}, otherwise return copy of row at {@code rowColumn}
	 * @param index index of row or column to return
	 * @return copy of row or column
	 */
	private BigInteger[] getRowColumn(final IndexType iType,final int index) {
		if(iType==Matrix.IndexType.COLUMN) return getColumn(index);
		else return getRow(index);
	}
	
	/**
	 * Caches copy of row or column as passed parameter {@code cache}
	 * @param iType if {@code COLUMN} then pass copy of column at {@code rowColumn}, otherwise pass copy of row at {@code rowColumn}
	 * @param index index of row or column to pass
	 */
	@SuppressWarnings("unused")
	private void getRowColumn(final IndexType iType,final int index,final BigInteger[] cache) {
		if(iType==Matrix.IndexType.COLUMN) getColumn(index,cache);
		else getRow(index,cache);
	}
	
	/**
	 * Saves passed data {@code columnData} at specified column {@code column} of matrix 
	 * @param column index of column to save
	 * @param columnData data to replace column
	 */
	private void setColumn(final int column,final BigInteger[] columnData) {
		for(int row=0;row<dimension;row++) {
			data[row][column]=columnData[row];
		}
	}
	
	/**
	 * Saves passed data {@code rowData} at specified row {@code row} of matrix 
	 * @param row index of row to save 
	 * @param rowData data to replace row
	 */
	private void setRow(final int row,final BigInteger[] rowData) {
		System.arraycopy(rowData, 0, data[row], 0, dimension);
	}
	
	/**
	 * Copies passed data {@code data} either as column or row at {@code index}
	 * @param iType saves passed {@code data} as column if equal to {@code COLUMN}, and a row otherwise
	 * @param index index of column or row to set data to
	 * @param data contains data to save as matrix column or row
	 */
	private void setRowColumn(final IndexType iType,final int index,final BigInteger[] data) {
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
			BigInteger[] cache=getRowColumn(iType, initialIndex);//cache first column/row
			
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
	public Range getLargestContinuousBlock(final IndexType iType,final boolean ascending) {
		if(iType==IndexType.ROW)	return getLargestRowContinuouosBlock(ascending);
		else return getLargestColumnContinuouosBlock(ascending);
	}
	
	/**
	 * Finds biggest continuous block of numbers along column
	 * @param ascending numbers must be arranged in ascending order, if {@code true}, otherwise in descending order
	 * @return instance of class {@code Range} that points at largest continuous block of numbers along column in given matrix
	 */
	private Range getLargestColumnContinuouosBlock(final boolean ascending) {
		Range block=new Range(IndexType.COLUMN,0,0,0);
		int max=1;
		final Comparator<BigInteger> comp=ascending?Comparator.naturalOrder():Comparator.reverseOrder();
		for(int column=0;column<dimension;column++) {
			int startIndex=0;
			do{
				int endIndex=startIndex+1;
				while(endIndex<dimension && comp.compare(data[endIndex-1][column], data[endIndex][column])<0) {
					endIndex++;//propagate range [startIndex..endIndex] one step right
				}
				if(endIndex-startIndex>max) {
					max=endIndex-startIndex;
					block=new Range(IndexType.COLUMN,column,startIndex,endIndex);
				}
				startIndex=endIndex;
			}while(startIndex<dimension);
		}
		return block;
	}

	/**
	 * Finds biggest continuous block of numbers along row
	 * @param ascending numbers must be arranged in ascending order, if {@code true}, otherwise in descending order
	 * @return instance of class {@code Range} that points at largest continuous block of numbers along row in given matrix
	 */
	private Range getLargestRowContinuouosBlock(final boolean ascending) {
		Range block=new Range(IndexType.ROW,0,0,0);
		int max=1;
		final Comparator<BigInteger> comp=ascending?Comparator.naturalOrder():Comparator.reverseOrder();
		for(int row=0;row<dimension;row++) {
			int startIndex=0;
			do{
				int endIndex=startIndex+1;
				while(endIndex<dimension && comp.compare(data[row][endIndex-1], data[row][endIndex])<0) {
					endIndex++;//propagate range [startIndex..endIndex] one step right
				}
				if(endIndex-startIndex>max) {
					max=endIndex-startIndex;
					block=new Range(IndexType.ROW,row,startIndex,endIndex);
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
		while(column<dimension && data[row][column].compareTo(BigInteger.ZERO)<=0) {
			column++;
		}
		return column;
	}
	
	/**
	 * Accumulates sum of numbers between first and second positive value for every row and collects them in resulting array  
	 * @return array of accumulated sums
	 */
	public BigInteger[] getAccumulatedSumsBetweenFirstAndSecondPositiveElement() {
		final BigInteger[] ranges=new BigInteger[dimension];
		for(int row=0;row<dimension;row++) {
			final int start=findNextPositive(row, 0);
			final int finish=findNextPositive(row, start+1);
			ranges[row]=new Range(IndexType.ROW,row,start+1,finish).sum();
		}
		return ranges;
	}
	
	/**
	 * Computes matrix norm
	 * @param iType find maximum value for rows, if {@code ROW}, or for columns, if {@code COLUMN}
	 * @return matrix norm
	 */
	public BigInteger getNorm(final IndexType iType) {
		BigInteger norm=BigInteger.ZERO;
		if(iType==IndexType.ROW) {
			for(BigInteger[] row:data) {
				BigInteger rowAccum=BigInteger.ZERO;
				for(BigInteger value:row) {
					rowAccum=rowAccum.add(value.abs());
				}
				norm=norm.max(rowAccum);
			}
		}else {
			for(int column=0;column<dimension;column++) {
				BigInteger colAccum=BigInteger.ZERO;
				for(int row=0;row<dimension;row++) {
					colAccum=colAccum.add(data[row][column].abs());
				}
				norm=norm.max(colAccum);
			}
		}
		return norm;
	}
	
	/**
	 * Rotates matrix in specified direction {@code rotation} by given {@code angle} 
	 * @param rotation rotates clockwise if {@code CLOCKWISE} or {@code COUNTERCLOCKWISE} otherwise
	 * @param angle of rotation (either 0,90,180 or 270)
	 */
	public void rotate(final Rotation rotation,final Angle angle) {
		for(int depth=0;depth<dimension/2;depth++) {//for every row/column from outside to center of matrix
			Range initialRange=new Range(IndexType.ROW,depth,depth);
			int turnCount=SIDE_COUNT-1;
			do {
				BigInteger[] initialRangeData=initialRange.save();
				Range destination=initialRange;
				Range source;
				while(turnCount>0 && 
						!initialRange.equals(source=destination.getNextRange(rotation, angle.getOpposite(), depth))) {//find source segment 
					destination.copy(source);//and copy it to destination segment  
					destination=source;//move to next segment
					turnCount--;
				};
				destination.restore(initialRangeData);
				turnCount--;
				if(turnCount>0) {//if angle.ordinal() is even, take adjacent quadrant and repeat until sideCount>0
					initialRange=initialRange.getNextRange(rotation.getOpposite(), Angle._90, depth);
				}
				
			}while(turnCount>0);

		}
	}
	
	/**
	 * Recursively computes determinant of square matrix
	 * @return determinant of square matrix
	 */
	public BigInteger getDeterminantRecursive() {
		boolean free[]=new boolean[dimension];//occupied column flags
		Arrays.fill(free, true);
		return getDeterminantRecursive(0,free);
	}
	
	private BigInteger getDeterminantRecursive(final int row,final boolean[] free) {
		BigInteger accum=BigInteger.ZERO;
		if(row+1==dimension) {//last row, size of matrix is equal to 1
			int k=0;
			while(k<dimension && !free[k]) k++;//skip occupied columns until free one found
			accum=accum.add(data[row][k]);
		}else {//scan given 'row', compute determinants and combine into final result
			int position=0;
			for(int column=0;column<dimension;column++) {
				if(free[column]) {
					BigInteger value=data[row][column];
					free[column]=false;//occupy column
					BigInteger subDet=getDeterminantRecursive(row+1,free);
					BigInteger component=value.multiply(subDet);
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
	public BigInteger getDeterminant() {
		
		class TraversalState {//holds data for matrix traversal
			
			BigInteger[] totals=new BigInteger[dimension];//accumulated totals for every line from top to bottom
			int[] position=new int[dimension];//number of next component
			int[] selectedColumns=new int[dimension];//collection of selected columns from top to bottom
			BitSet occupiedColumns=new BitSet(dimension);//flag set of occupied columns
			
			int line=0;//points at current line
			
			{//initialize values
				totals[0]=BigInteger.ZERO;
				selectedColumns[0]=0;
				position[0]=0;
			}
			
			void moveUpAndRollUp() {//move one line up, compute and add next component to its total 
				line--;
				int selColumn=selectedColumns[line];
				occupiedColumns.clear(selColumn);//free column
				BigInteger product=data[line][selColumn].multiply(totals[line+1]);
				totals[line]=
						position[line]%2==0?
								totals[line].add(product):
									totals[line].subtract(product);
				position[line]++;
			}
			
			void moveDownAndInitialize(final int column) {//move down one line and set initial values
				selectedColumns[line]=column;
				occupiedColumns.set(column);
				line++;
				selectedColumns[line]=0;//start from leftmost column
				totals[line]=BigInteger.ZERO;//clear line total
				position[line]=0;//start counting components of total				
			}
			
			boolean firstLine() {
				return line==0;
			}
			
			boolean lastLine() {
				return line+1==dimension;
			}

			BigInteger getResult() {
				return totals[0];//holds computed determinant of whole matrix
			}
			
			void saveTotal(final int column) {
				totals[line]=data[line][column];
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
	* @param b second postiion within matrix to interchange row and column
	* @return reference to {@code this} matrix
	*/
	public Matrix swapRanges(final Position a,final Position b){
		new Range(IndexType.ROW,a.getRow()).
			swap(new Range(IndexType.ROW,b.getRow()));
		new Range(IndexType.COLUMN,a.getColumn()).
			swap(new Range(IndexType.COLUMN,b.getColumn()));
		return this;
	}
	
	/**
	 * Orders every row of matrix by provided comparator
	 * @param comparator determines sorting order
	 * @return this matrix
	 */
	public Matrix sortRows(final Comparator<BigInteger> comparator) {
		for(final Range range:this) {
			range.sort(comparator);
		}
		return this;
	}

}
