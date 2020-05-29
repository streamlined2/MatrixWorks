package chapter2;

import java.util.Comparator;
import java.util.Scanner;
import chapter2.Matrix.Angle;
import chapter2.Matrix.IndexType;
import chapter2.Matrix.Rotation;
import math.Cardinal;
import math.Decimal;

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
 * 15. Decimal matrix rounding
 * 16. Fetching list of saddling points
 * 
 * @author Serhii Pylypenko
 * @since 2020-03-15
 * @version 1.4
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
			
			Matrix<Cardinal> m=new Matrix<>(dimension,Cardinal.LONG_INITIALIZER,(x)->Math.round((Math.random()*2*dimension-dimension)));
			Matrix<Cardinal> org=(Matrix<Cardinal>)m.clone();
			System.out.printf("Initial matrix %dx%d filled with random values within range [%d,%d]:\n%s\n", dimension, dimension, -dimension, dimension, m);
			
			m.transpose();
			System.out.printf("Transposed matrix:\n%s\n", m);
			
			m.transpose();
			System.out.printf("Double transposed matrix is the same (%s) as original one:\n%s\n", m.equals(org), m);

			int sortKey=readValue(String.format("Please enter sort column and row within [%d,%d]: ",0,dimension-1),scanner,0,dimension-1);
			
			org=(Matrix<Cardinal>)m.clone();
			m.sort(Matrix.IndexType.COLUMN, sortKey);
			System.out.printf("Matrix sorted by column %d:\n%s\n", sortKey, m);
			m.sort(Matrix.IndexType.ROW, sortKey);
			System.out.printf("Matrix sorted by row %d:\n%s\n", sortKey, m);
			
			m=(Matrix<Cardinal>)org.clone();
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
			
			Matrix<Cardinal> copy=(Matrix<Cardinal>)m.clone();
			System.out.printf("Original matrix:\n%s\n", m);
			System.out.printf("Copy of matrix:\n%s\n", copy);
			System.out.printf("Original matrix and it's copy are equal: %s\n",m.equals(copy));
			copy.set(0, 0, Cardinal.ONE);
			System.out.printf("Changed copy of matrix:\n%s\n", copy);
			System.out.printf("Original matrix and changed copy are equal: %s\n",m.equals(copy));

			System.out.printf("\nMatrix:\n%s\n", m);
			System.out.printf("Largest continuous ascending row block: %s\n", m.getLargestContinuousBlock(IndexType.ROW, true));
			System.out.printf("Largest continuous descending row block: %s\n", m.getLargestContinuousBlock(IndexType.ROW, false));
			System.out.printf("Largest continuous ascending column block: %s\n", m.getLargestContinuousBlock(IndexType.COLUMN, true));
			System.out.printf("Largest continuous descending column block: %s\n", m.getLargestContinuousBlock(IndexType.COLUMN, false));

			System.out.printf("\nList of accumulated sums of numbers between first and second positive number for each row: %s\n",m.getAccumulatedSumsBetweenFirstAndSecondPositiveElement());
			
			System.out.printf("\nRow norm (%s) and column norm (%s) of a matrix: \n%s\n",m.getNorm(IndexType.ROW),m.getNorm(IndexType.COLUMN),m);
			
			copy=(Matrix<Cardinal>)m.clone(); 
			System.out.printf("\nOriginal matrix: \n%s",m);
			m.rotate(Rotation.COUNTERCLOCKWISE, Angle._90);
			System.out.printf("\nMatrix rotated 90 degrees counterclockwise: \n%s\n",m);
			m=(Matrix<Cardinal>)copy.clone();
			m.rotate(Rotation.COUNTERCLOCKWISE, Angle._180);
			System.out.printf("\nMatrix rotated 180 degrees counterclockwise: \n%s\n",m);
			m=(Matrix<Cardinal>)copy.clone();
			m.rotate(Rotation.COUNTERCLOCKWISE, Angle._270);
			System.out.printf("\nMatrix rotated 270 degrees counterclockwise: \n%s\n",m);
			 		
			m=(Matrix<Cardinal>)copy.clone();
			System.out.printf("\nDeterminant of matrix:\n%s\n is %s\n", m, m.getDeterminantRecursive());
			System.out.printf("\nDeterminant of matrix:\n%s\n is %s\n", m, m.getDeterminant());

			Matrix<Cardinal> averageSubtracted=new Matrix<>(m,Matrix<Cardinal>.Segment::average,Cardinal::subtract);
			System.out.printf("\nOriginal matrix:\n%s\nSubtracted average matrix:\n%s\n", m, averageSubtracted);
			
			System.out.printf("List of maximum values in matrix:\n%s\nis %s\nand matrix without deleted rows and columns: %s\n", 
					m,m.getExtremums(true),new Matrix<Cardinal>(m,m.getExtremums(true)));

			System.out.printf("List of zeroes in matrix:\n%s\nis %s\nand matrix without deleted rows and columns: %s\n", m,m.getEqualTo(Cardinal.ZERO),new Matrix<Cardinal>(m,m.getEqualTo(Cardinal.ZERO)));
			
			int minRow=readValue("Please enter row to place minimum: ",scanner,0,dimension-1);
			int minCol=readValue("Please enter column to place minimum: ",scanner,0,dimension-1);

			System.out.printf("First minimum value in matrix:\n%s\nis %s\nand matrix with swapped rows and columns: %s\n", 
					m,m.getExtremums(false).iterator().next(),
					new Matrix<Cardinal>(m).swapSegments(
							m.new Position(minRow,minCol),
							m.getExtremums(false).iterator().next()));
			
			Comparator<Cardinal> compareByAbsoluteValueReversed=new Comparator<Cardinal>(){
				@Override public int compare(final Cardinal o1, final Cardinal o2) {
					return o2.abs().compareTo(o1.abs());
				}
			};
			System.out.printf("Order elements of row in source matrix\n%s\nby absolute value reversed\n%s\n", m, new Matrix<Cardinal>(m).sortRows(compareByAbsoluteValueReversed));

			Matrix<Decimal> zeroMatrix=new Matrix<>(dimension, Decimal.LONG_INITIALIZER, 0L);
			System.out.printf("Decimal matrix %dx%d filled with zeroes:\n%s\n", dimension, dimension, zeroMatrix);
			Matrix<Decimal> randomMatrix=new Matrix<>(dimension, Decimal.DOUBLE_INITIALIZER,(x)->(Math.random()*2*dimension-dimension));
			System.out.printf("Decimal matrix %dx%d filled with random values within range [%d,%d]:\n%s\n", dimension, dimension, -dimension, dimension, randomMatrix);
			Matrix<Decimal> roundedMatrix=randomMatrix.process(Decimal::round);
			System.out.printf("Decimal matrix %dx%d filled with rounded values:\n%s\n", dimension, dimension, roundedMatrix);
			
			//m=new Matrix<>(dimension,Cardinal.LONG_INITIALIZER,x->(long)(x.getRow()+x.getColumn()));
			System.out.printf(
					"Matrix:\n%s\nRow minimums and columns maximums:\n%s\n%s\nList of saddle points (%d): %s\n", 
					m,
					m.getExtremumsForEachSegment(IndexType.ROW,false),
					m.getExtremumsForEachSegment(IndexType.COLUMN,true),
					m.getSaddlePoints().size(),
					m.getSaddlePoints());

		}
				
	}

}
