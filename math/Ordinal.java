package math;

import java.io.Serializable;

public interface Ordinal <T> extends Comparable<T>, Cloneable, Serializable {
	
	T add(final T x);
	T subtract(final T x);
	T multiply(final T x);
	T divide(final T x);
	T divide(final long x);
	boolean positive();
	T abs();
	T max(final T a);
	T negate();
	T zero();//should have been static but must be redefined in descendant classes for variant representation of zero and other values

}
