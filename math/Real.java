package math;

import java.util.function.Function;

public class Real implements Ordinal<Real> {
	
	private static final long serialVersionUID = 5001227811989659056L;

	public static final Function<Double,Real> DOUBLE_INITIALIZER = x->new Real(x);

	private final double value;
	
	public static final Real ZERO=new Real(0D);
	
	public Real(final double value) {
		this.value=value;
	}

	@Override public String toString() {
		return Double.toString(value);
	}
	
	@Override public Real zero() {
		return ZERO;
	}

	@Override public int compareTo(final Real o) {
		if(value>o.value) return 1;
		else if(value<o.value) return -1;
		return 0;
	}

	@Override public Real add(final Real x) {
		return new Real(value+x.value);
	}

	@Override public Real subtract(final Real x) {
		return new Real(value-x.value);
	}

	@Override public Real multiply(final Real x) {
		return new Real(value*x.value);
	}

	@Override public Real divide(final Real x) {
		return new Real(value/x.value);
	}

	@Override public Real divide(final long x) {
		return divide(new Real(x));
	}

	@Override public boolean positive() {
		return value>0;
	}

	@Override public Real abs() {
		return new Real(Math.abs(value));
	}

	@Override public Real max(final Real a) {
		return new Real(Math.max(value, a.value));
	}

	@Override public Real negate() {
		return new Real(-value);
	}

}
