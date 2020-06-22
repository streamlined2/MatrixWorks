package utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public final class Utensils {
	
	@SuppressWarnings("unchecked")
	public final static <T extends Serializable> T copy(final T src) {
		final ByteArrayOutputStream baos=new ByteArrayOutputStream();
		try {
			new ObjectOutputStream(baos).writeObject(src);
			return (T)new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())).readObject();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
