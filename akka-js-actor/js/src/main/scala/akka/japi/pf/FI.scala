/**
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.japi.pf

/**
 * Class that encapsulates all the Functional Interfaces
 * used for creating partial functions.
 *
 * This is an EXPERIMENTAL feature and is subject to change until it has received more real world testing.
 */
object FI {

   /**
   * Functional interface for an application.
   *
   * @param <I> the input type, that this Apply will be applied to
   * @param <R> the return type, that the results of the application will have
   */
  trait Apply[I, R] {
    /**
     * The application to perform.
     *
     * @param i  an instance that the application is performed on
     * @return  the result of the application
     */
    def apply(i: I): R
  }

  /**
   * Functional interface for an application.
   *
   * @param <I1> the first input type, that this Apply will be applied to
   * @param <I2> the second input type, that this Apply will be applied to
   * @param <R> the return type, that the results of the application will have
   */
  trait Apply2[I1, I2, R] {
    /**
     * The application to perform.
     *
     * @param i1  an instance that the application is performed on
     * @param i2  an instance that the application is performed on
     * @return  the result of the application
     */
    def apply(i1: I1, i2: I2): R
  }

	/**
   * Functional interface for an application.
   *
   * @param <I> the input type, that this Apply will be applied to
   */
  trait UnitApply[I] {
    /**
     * The application to perform.
     *
     * @param i  an instance that the application is performed on
     */
    def apply(i: I)
  }

  trait UnitApply2[I1, I2] {
    /**
     * The application to perform.
     *
     * @param i1  an instance that the application is performed on
     * @param i2  an instance that the application is performed on
     */
    def apply(i1: I1, i2: I2)
  }

	/**
   * Package scoped functional interface for a predicate. Used internally to match against arbitrary types.
   */
  trait Predicate {
    /**
     * The predicate to evaluate.
     *
     * @param o  an instance that the predicate is evaluated on.
     * @return  the result of the predicate
     */
    def defined(o: Any): Boolean
  }

  /**
   * Functional interface for a predicate.
   *
   * @param <T> the type that the predicate will operate on.
   * @param <U> the type that the predicate will operate on.
   */
  trait TypedPredicate2[T, U] {
    /**
     * The predicate to evaluate.
     *
     * @param t  an instance that the predicate is evaluated on.
     * @param u  an instance that the predicate is evaluated on.
     * @return  the result of the predicate
     */
    def defined(t: T, u: U): Boolean
  }

}
