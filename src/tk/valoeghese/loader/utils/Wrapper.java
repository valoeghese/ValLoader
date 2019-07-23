package tk.valoeghese.loader.utils;

public class Wrapper<T> {
	private T value;
	public Wrapper (T defaultValue) {
		value = defaultValue;
	}
	public T setT(T arg0) {
		value = arg0;
		return arg0;
	}
	public T getT() {
		return value;
	}
	public static <E> Wrapper<E> wrap(E defaultValue) {
		return new Wrapper<E>(defaultValue);
	}
}
