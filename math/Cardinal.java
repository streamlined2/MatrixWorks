package math;

import java.math.BigInteger;
import java.util.function.Function;

public class Cardinal implements Ordinal<Cardinal>{
	
	private static final long serialVersionUID = -2914702771398519579L;

	public static final Function<Long,Cardinal> LONG_INITIALIZER = (x)->new Cardinal(x);
	
	public static final Cardinal ZERO=new Cardinal(0);
	public static final Cardinal ONE=new Cardinal(1);
	
	private final BigInteger value;
	
	public Cardinal(final long numValue) {
		value=BigInteger.valueOf(numValue);
	}
	
	public Cardinal() {
		this(0);
	}
	
	private Cardinal(final BigInteger numValue) {
		value=numValue;
	}
	
	@Override public String toString() {
		return value.toString();
	}
	
	@Override public Cardinal zero() {
		return ZERO;
	}
	
	@Override
	public Cardinal add(final Cardinal x) {
		return new Cardinal(value.add(x.value));
	}

	@Override
	public Cardinal subtract(final Cardinal x) {
		return new Cardinal(value.subtract(x.value));
	}

	@Override
	public Cardinal multiply(final Cardinal x) {
		return new Cardinal(value.multiply(x.value));
	}

	@Override
	public Cardinal divide(final Cardinal x) {
		return new Cardinal(value.divide(x.value));
	}

	@Override
	public Cardinal divide(final long x) {
		return new Cardinal(value.divide(BigInteger.valueOf(x)));
	}
	
	@Override
	public boolean positive() {
		return value.compareTo(BigInteger.ZERO)>0;
	}
	
	@Override
	public Cardinal abs() {
		return new Cardinal(value.abs()); 
	}
	
	@Override
	public Cardinal max(final Cardinal a) {
		return new Cardinal(value.max(a.value));
	}
	
	@Override public Cardinal negate() {
		return new Cardinal(value.negate());
	}

	@Override
	public int compareTo(final Cardinal o) {
		return value.compareTo(o.value);
	}

	@Override public boolean equals(final Object o) {
		if(!(o instanceof Cardinal)) return false;
		return value.equals(((Cardinal)o).value);
	}
}
