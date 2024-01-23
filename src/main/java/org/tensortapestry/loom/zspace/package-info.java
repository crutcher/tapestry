/**
 * ZSpace (N-Dimensional Integer Space) Libs
 *
 * <p>The primary purpose of these classes and interfaces is to permit the description and
 * manipulation of ZSpace selections and projection maps as part of tensor expression graphs.
 *
 * <p>As this library exists to make the behavior of tensor expressions more understandable; and to
 * make the implementation of a correct compiler more tractable; the focus is on explict and simple
 * implementations; over performance.
 *
 * <h2>n-d array libs</h2>
 *
 * <p>I investigated several n-d array implementations, hoping to simplify the math implementations.
 *
 * <p>Unfortunately, I could not find an implementation which meaningfully reduced the complexity.
 *
 * <p>At issue is that the existing ndarry implementations were either mathematically complete, but
 * required session managers for a (potential) GPU backend; or were merely in-memory N-D arrays;
 * without no math support.
 *
 * <p>Libs I looked at:
 *
 * <ul>
 *   <li>Tensorflow NdArray - no math support.
 *   <li>Tensorflow Java Tensors - weird session management.
 *   <li>ND4J - seems dead in 2020
 *   <li>ai.djl - weird session management.
 *   <li>apache common math linear - no support for int vectors / matrices.
 * </ul>
 */
package org.tensortapestry.loom.zspace;
