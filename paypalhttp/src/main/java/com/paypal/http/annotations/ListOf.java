package com.paypal.http.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ListOf describes a special case where an API returns "raw" list (e.g [{"key":"value"}])
 * which, due to the nature of type erasure, we would have a hard time deserializing.
 *
 * Objects which use this annotation should be empty subclasses of {@link java.util.ArrayList},
 * contain a public constructor which calls super, and also declare the {@link Model}
 * annotation.
 *
 * Example, given some complex object ComplexObj:
 *
 * <pre>
 * <code>
 * {@literal @}Model
 * {@literal @}ListOf(listClass = ComplexObj.class)
 * public class ComplexObjList extends ArrayListComplexObj {
 *     public ComplexObjList() {
 *         super();
 *     }
 * }
 * </code>
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ListOf {
	Class listClass() default Void.class;
}

