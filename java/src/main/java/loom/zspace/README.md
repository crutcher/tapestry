# ZSpace (N-Dimensional Integer Space) Libs

The primary purpose of these classes and interfaces is to permit the description
and manipulation of ZSpace selections and projection maps as part of tensor expression
graphs.

As this library exists to make the behavior of tensor expressions more understandable;
and to make the implementation of a correct compiler more tractable; the focus is on
explict and simple implementations; over performance.


## n-d arrays

I investigated several n-d array implementations, hoping to simplify the math implementations.

Unfortunately, I could not find an implementation which meaningfully reduced the complexity.

At issue is that the existing ndarry implementations were either mathematically complete,
but required session managers for a (potential) GPU backend; or were merely in-memory N-D arrays;
without no math support.

Libs I looked at:

 * Tensorflow NdArray - no math support.
 * Tensorflow Java Tensors - weird session management.
 * ND4J - seems dead in 2020
 * ai.djl - weird session management.
 * apache common math linear - no support for int vectors / matrices.

### 