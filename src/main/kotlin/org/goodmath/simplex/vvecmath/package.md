# Package org.goodmath.simplex.vvecmath

This contains a port of Michael Hoffman's vvecmath  to
Kotlin, in order to use it as part of the Simplex modeling
language. The reason for the Kotlin port is that I'd like
to be able to use the JCSG types directly as Simplex value
types, and they have strong dependencies on the underlying
vvecmath types. So to make things work smoothly in
Simplex, I did this port.

The code is, for the most part, operationally unchanged.
The only changes I made were:

* Porting to Kotlin (obviously);
* Using kotlin operator overrides for arithmetic.
* Cleaning up places where Kotlin could do things more simply than
  java (like streams and collection mapping)
* Editing some of the documentation for clarity;
* Replacing varargs with list parameters (since this port is for
  use in simplex, I don't need the varargs versions.)
* Integrating the types into the Simplex value type system.
