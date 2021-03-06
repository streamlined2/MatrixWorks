package chapter2;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Scanner;
import chapter2.Matrix.Angle;
import chapter2.Matrix.IndexType;
import chapter2.Matrix.Position;
import chapter2.Matrix.Rotation;

/**
 * 
 * A collection of algorithms that transforms, inspects matrices and computes related values:
 * 
 * 1. Matrix filled with random values within given range
 * 2. Transposition of matrix
 * 3. Matrix (quick)sorted by specified column/row
 * 4. Matrix shifted by specified steps to the right/left and up/down
 * 5. Largest continuous ascending/descending row/column block of matrix
 * 6. List of accumulated sums between first and second positive number for each row
 * 7. Computation of row/column norm of a matrix
 * 8. Rotation of matrix by 90,180,270 degrees counterclockwise
 * 9. Determinant of square matrix computed recursively and non-recursively
 * 10. Computation of matrix with elements deducted by row average 
 * 11. Creation of matrix without rows and columns where maximums located
 * 12. Creation of matrix without rows and columns where zeroes found
 * 13. Swapping rows and columns of first minimum and appointed element of matrix
 * 14. Ordering matrix row elements so that zeroes placed behind
 * 
 * @author Serhii Pylypenko
 * @since 2020-03-15
 * @version 1.2
 *
 */

public class Runner {

	public static int readValue(String prompt,Scanner scanner,int start,int finish) {
		System.out.printf(prompt);
		final int value=scanner.nextInt();
		if(!(value>=start && value<=finish)) throw new RuntimeException(String.format("value %d should be within [%d,%d]", value, start, finish));
		System.out.printf("You entered %d.\n\n", value);
		return value;
	}

	public static void main(String... args) {
		
		try(Scanner scanner=new Scanner(System.in)){
			int dimension=readValue("Please enter matrix dimension: ",scanner,0,Integer.MAX_VALUE);
			
			Matrix m=new Matrix(dimension);
			Matrix org=(Matrix)m.clone();
			System.out.printf("Initial matrix %dx%d filled with random values within range [%d,%d]:\n%s\n", dimension, dimension, -dimension, dimension, m);
			
			m.transpose();
			System.out.printf("Transposed matrix:\n%s\n", m);
			
			m.transpose();
			System.out.printf("Double transposed matrix is the same (%s) as original one:\n%s\n", m.equals(org), m);

			int sortKey=readValue(String.format("Please enter sort column and row within [%d,%d]: ",0,dimension-1),scanner,0,dimension-1);
			
			org=(Matrix)m.clone();
			m.sort(Matrix.IndexType.COLUMN, sortKey);
			System.out.printf("Matrix sorted by column %d:\n%s\n", sortKey, m);
			m.sort(Matrix.IndexType.ROW, sortKey);
			System.out.printf("Matrix sorted by row %d:\n%s\n", sortKey, m);
			
			m=(Matrix)org.clone();
			m.quickSort(Matrix.IndexType.COLUMN, sortKey);
			System.out.printf("Matrix quicksorted by column %d:\n%s\n", sortKey, m);
			m.quickSort(Matrix.IndexType.ROW, sortKey);
			System.out.printf("Matrix quicksorted by row %d:\n%s\n", sortKey, m);
			
			int shiftCount=readValue(String.format("Please enter shift count within [%d,%d]: ",1,dimension),scanner,1,dimension);
			
			System.out.printf("Original matrix:\n%s\n", m);
			m.shift(Matrix.IndexType.COLUMN,Matrix.Direction.RIGHT_DOWN, shiftCount);
			System.out.printf("Matrix shifted by %d to the right:\n%s\n", shiftCount, m);
			m.shift(Matrix.IndexType.COLUMN,Matrix.Direction.LEFT_UP, shiftCount);
			System.out.printf("Matrix shifted by %d to the left:\n%s\n", shiftCount, m);

			m.shift(Matrix.IndexType.ROW,Matrix.Direction.RIGHT_DOWN, shiftCount);
			System.out.printf("Matrix shifted down by %d:\n%s\n", shiftCount, m);
			m.shift(Matrix.IndexType.ROW,Matrix.Direction.LEFT_UP, shiftCount);
			System.out.printf("Matrix shifted up by %d:\n%s\n", shiftCount, m);
			
			Matrix copy=(Matrix)m.clone();
			System.out.printf("Original matrix:\n%s\n", m);
			System.out.printf("Copy of matrix:\n%s\n", copy);
			System.out.printf("Original matrix and it's copy are equal: %s\n",m.equals(copy));
			copy.set(0, 0, BigInteger.ONE);
			System.out.printf("Changed copy of matrix:\n%s\n", copy);
			System.out.printf("Original matrix and changed copy are equal: %s\n",m.equals(copy));

			System.out.printf("\nMatrix:\n%s\n", m);
			System.out.printf("Largest continuous ascending row block: %s\n", m.getLargestContinuousBlock(IndexType.ROW, true));
			System.out.printf("Largest continuous descending row block: %s\n", m.getLargestContinuousBlock(IndexType.ROW, false));
			System.out.printf("Largest continuous ascending column block: %s\n", m.getLargestContinuousBlock(IndexType.COLUMN, true));
			System.out.printf("Largest continuous descending column block: %s\n", m.getLargestContinuousBlock(IndexType.COLUMN, false));

			System.out.printf("\nList of accumulated sums of numbers between first and second positive number for each row: %s\n",Arrays.toString(m.getAccumulatedSumsBetweenFirstAndSecondPositiveElement()));
			
			System.out.printf("\nRow norm (%s) and column norm (%s) of a matrix: \n%s\n",m.getNorm(IndexType.ROW),m.getNorm(IndexType.COLUMN),m);
			
			copy=(Matrix)m.clone(); 
			System.out.printf("\nOriginal matrix: \n%s",m);
			m.rotate(Rotation.COUNTERCLOCKWISE, Angle._90);
			System.out.printf("\nMatrix rotated 90 degrees counterclockwise: \n%s\n",m);
			m=(Matrix)copy.clone();
			m.rotate(Rotation.COUNTERCLOCKWISE, Angle._180);
			System.out.printf("\nMatrix rotated 180 degrees counterclockwise: \n%s\n",m);
			m=(Matrix)copy.clone();
			m.rotate(Rotation.COUNTERCLOCKWISE, Angle._270);
			System.out.printf("\nMatrix rotated 270 degrees counterclockwise: \n%s\n",m);
			 		
			m=(Matrix)copy.clone();
			System.out.printf("\nDeterminant of matrix:\n%s\n is %s\n", m, m.getDeterminantRecursive());
			System.out.printf("\nDeterminant of matrix:\n%s\n is %s\n", m, m.getDeterminant());

			Matrix averageSubtracted=new Matrix(m,Matrix.Range::average,BigInteger::subtract);
			System.out.printf("\nOriginal matrix:\n%s\nSubtracted average matrix:\n%s\n", m, averageSubtracted);
			
			System.out.printf("List of maximum values in matrix:\n%s\nis %s\nand matrix without deleted rows and columns: %s\n", 
					m,m.getExtremum(Matrix.Position::compareTo),new Matrix(m,m.getExtremum(Matrix.Position::compareTo)));

			System.out.printf("List of zeroes in matrix:\n%s\nis %s\nand matrix without deleted rows and columns: %s\n", m,m.getEqualTo(BigInteger.ZERO),new Matrix(m,m.getEqualTo(BigInteger.ZERO)));
			
			int minRow=readValue("Please enter row to place minimum: ",scanner,0,dimension-1);
			int minCol=readValue("Please enter column to place minimum: ",scanner,0,dimension-1);

			Comparator<Position> reverseComparator=Comparator.reverseOrder();
			System.out.printf("First minimum value in matrix:\n%s\nis %s\nand matrix with swapped rows and columns: %s\n", 
					m,m.getExtremum(reverseComparator::compare).get(0),
					new Matrix(m).swapRanges(
							m.new Position(minRow,minCol),
							m.getExtremum(reverseComparator::compare).get(0)));
			
			Comparator<BigInteger> compareByAbsoluteValueReversed=new Comparator<BigInteger>(){
				@Override public int compare(final BigInteger o1, final BigInteger o2) {
					return o2.abs().compareTo(o1.abs());
				}
			};
			System.out.printf("Order every element of row of source matrix\n%s\nby its absolute value reversed\n%s\n", m, new Matrix(m).sortRows(compareByAbsoluteValueReversed));

		}
				
	}

}
