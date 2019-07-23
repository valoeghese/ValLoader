package tk.valoeghese.loader.utils;

import java.util.function.Consumer;
import java.util.function.Predicate;

public final class FunctionalUtils {
	private FunctionalUtils() {
	}
	
	public static <T> void forEachItem(final T[] array, Consumer<T> consumer) {
		for (T t : array) {
			consumer.accept(t);
		}
	}
	
	public static <T> boolean forEachItemTest(final T[] array, Predicate<T> predicate) {
		for (T t : array) {
			if (predicate.test(t)) {
				return true;
			}
		}
		return false;
	}
	
	public static <T> void accept(final T item, Consumer<T> consumer) {
		consumer.accept(item);
	}
}
