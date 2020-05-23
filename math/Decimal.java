package math;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Function;

public class Decimal implements Ordinal<Decimal> {
	
	private static final long serialVersionUID = 6640482170464834497L;
	
	public static final Function<Long,Decimal> LONG_INITIALIZER = (x)->new Decimal(x);
	public static final Function<Double,Decimal> DOUBLE_INITIALIZER = (x)->new Decimal(x);

	public static final Decimal ZERO=new Decimal(0);
	
	private final BigDecimal value;

	public Decimal(final long val) {
		value=BigDecimal.valueOf(val);
	}
	
	public Decimal(final double val) {
		value=BigDecimal.valueOf(val);
	}

	public Decimal() {
		this(0L);
	}
	
	private Decimal(final BigDecimal val) {
		value=val;
	}
	
	@Override public String toString() {
		return value.toString();
	}
	
	@Override public Decimal zero() {
		return ZERO;
	}
	
	@Override
	public Decimal add(final Decimal x) {
		return new Decimal(value.add(x.value));
	}

	@Override
	public Decimal subtract(final Decimal x) {
		return new Decimal(value.subtract(x.value));
	}

	@Override
	public Decimal multiply(final Decimal x) {
		return new Decimal(value.multiply(x.value));
	}

	@Override
	public Decimal divide(final Decimal x) {
		return new Decimal(value.divide(x.value));
	}

	@Override
	public Decimal divide(final long x) {
		return new Decimal(value.divide(BigDecimal.valueOf(x)));
	}
	
	@Override
	public boolean positive() {
		return value.compareTo(BigDecimal.ZERO)>0;
	}
	
	@Override
	public Decimal abs() {
		return new Decimal(value.abs());
	}
	
	@Override
	public Decimal max(final Decimal a) {
		return new Decimal(value.max(a.value));
	}
	
	@Override public Decimal negate() {
		return new Decimal(value.negate());
	}
	
	public Decimal round() {
		return new Decimal(value.setScale(0,RoundingMode.HALF_UP));
	}

	@Override
	public int compareTo(final Decimal o) {
		return value.compareTo(o.value);
	}
	
	@Override public boolean equals(final Object o) {
		if(!(o instanceof Decimal)) return false;
		return value.equals(((Decimal)o).value);
	}
}
