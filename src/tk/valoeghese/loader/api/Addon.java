package tk.valoeghese.loader.api;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(TYPE)
public @interface Addon {
	/**
	 * The Addon's unique identifier
	 */
	String id();
	
	/**
	 * The Addon Name
	 */
	String name() default "UntitledAddon";
	
	/**
	 * The Addon Version
	 */
	String version() default "1.0.0";
}
