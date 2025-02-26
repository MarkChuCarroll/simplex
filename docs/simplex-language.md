# The Simplex 3d Modelling Language

Simplex is a simple language for designing 3d models. It was designed with
a few key goals in mind:

* It uses a roughly lisp-like model of programming, where everything is based
  on functions that return values. It's designed around the idea
  of building things using functions that take 3d objects as arguments,
  and return them as results.
* It's strongly typed.
* It's internally extensible. (The original version of Simplex used a combination
  of JSCG and some ported code from demakein to do 3d modelling; the current
  version uses Manifold. It took 4 evenings of hacking to totally replace that
  internal framework.)

## [Syntax](syntax.md)
## [Types](builtin-types.md)
## [Running](running.md)
