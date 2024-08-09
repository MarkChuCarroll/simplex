# simplex: A programming language for building 3d printable models.

## What is it?

Another language for building models for 3d printing.

## Why?

I've been doing a lot of designs for 3d printing using OpenSCAD. I
love the process: programming a model is much easier/more natural to
me than trying to manipulate stuff in a gui using a two-d input device
(a mouse) to manipulate a 2d projection of a 3d object in 3d space.

The problem is, OpenScad is a pretty crummy programming
language. One of my simplest test cases for Simplex is:
make a cone, and union it with itself turned upside down.

```
fun cone(radius: Float, height: Float): CSG {
   cylinder(height + 2.0, radius, 0.1)
}

fun dbl(radius <Float> height <Float>): CSG {
    let c = cone(height, radius)
    c + c->rot(0.0, 180.0, 0.0)->move(0.0, 020, 25.0)
}
```

But you can't do that in OpenSCAD. Because in OpenSCAD, a
3d object isn't a value. So you can't write functions that
take it as a parameter. You can write modules, which are
sort-of like custom functions, except that they can't return
a usable value.

I want a real language - one where I can pass parameters,
and call functions. So, thus, Simplex.

## Why Simplex?

A simplex is the mathematical term for an N-dimensional triangle. So
the two-dimensional simplex is a triangle. A three-dimensional simplex
is a tetrahedron. And so on.

What we're building with Simplex is a 3d model - a volume in 3d space
that's broken down into tetrahedrons. Everything in Simplex is build
from simplexes!

## The Language

Simplex is basically a simple functional language. There are no statements:
it's all definitions and expressions. Every expression always produces
a value as a result.

A model in Simplex is a bunch of definitions, followed by a bunch
of "produce" clauses. Each produce clause describes one set
of objects which the model can output.


### Definitions

It's got three things you can define in a simplex model: functions, variables,
and tuples.

A function is something that takes parameters, does something
with them, and returns a result. Functions return the value of
their last expression.


```
fun fact(n: Int): Int {
  if (n == 0) {
   1
   } else {
     n * fact(n-1)
   }
}
```

Global variable definitions are simple, and the type is optional:

```
let name :type = expr
```

Finally, there are tuples, which are a simple user-defined data
type. They're probably not going to get used much, but I
couldn't resist adding them.

```
tup name { a: Int, b: Float, c: CSG }
```

### Expressions

#### Function Calls and Operators

```
f(1, 2.0, "abc")
f + 3
```


#### Let Expressions

Let expressions introduce a new local variable. They look just
like global variable declarations - but they are expressions, which
return the value assigned to the new variable.

```
let x = f(a)
let y = g(b, x)
something(x, y)
let a = let b = let c = 0
```

#### Conditionals

Conditionals look similar to C, but they allow a python-like elif:

```
if (x == y) "eq"
elif (x > y) "greater"
else "false"
```

### With

With expressions are a convenience for working with tuple types. They
create a local scope where the fields of the tuple are instantiated as
local variables.

```
with tuple {
   field1 + field2
}
```

### Assignments and Tuple Field Updates

```
tup T(one: Int, two: String, three: String)
let x = 3
let y = #T(3, "a", "b")
x := x + 1
y.two := "c"
```

### Method

Methods are just another syntax for functions. They're defined on a
target value, which often makes them easier to read. But since there's
no subtyping or inheritance, there's no dynamic binding or inheritance.

```
method Int->double(): Int {
  self * 2
}

3->double()
cyl->move(10, 10, 5)
```

### For loops

A for loop iterates over an array, performing on operation on each
element. It returns another array containing the result of those
applications - basically like a map in a functional programming language.

```
let arr = [ 1, 2, 3, 4, 5]
let doubled = for i in arr {
   i->double()
}
```

### While Loops

A while loop is just a standard while loop construct. It returns the value
of the last expression in its body on the last iteration of the loop.

```
while x > 3.0 {
  x = x/2.0
}
```

### Block Expressions

Anywhere that you can use a single expression, you can also use
a block, which is a sequence of expressions. A block introduces
a new scope region (so any locals defined with "let" are local to the
block), and returns the value of its last expression.

### Produce Clauses

A produce clause specifies a set of outputs. The produce declares
a name for its outputs, and a list of expressions. The output consists of:

* a CSG file, containing the union of the CSG objects returned by any of
  its member expressions.
* A txt file, containing the values in the produce expressions
  whose type supports pretty-printing;
* a twist file, containing the twisted form of the values in the produce's
  expressions whose types do not support pretty-printing.
```
produce("my-model") {
  my_csg_function(28.2, 3.13, 18.0)
  "This is a text output"
}
```

## Builtins and Modelling Types

### Numbers

Simplex supports 64 bit integers (type Int) and 64 bit floating point values (type Float).
Int and Float are distinct types, and there's no automatic conversion between
them. If you want to multiple a Float by an Int, you have to do an explicit
type conversion:

```
3->float() * 2.0^(0.5)
```

Int and float both implement addition (+), subtraction (-), multiplication (*),
division (/), exponentiation (^), modulo (%), negation (unary-) equality(==, !=), and ordered comparisons (<, <=, >, >=)

Integer also provides a "float" method to convert to float.

Float provides a "truncate" method to convert to an integer, and an "isNaN"
method to check if a float value is NaN (not-a-number).

### Booleans

Simplex has a standard boolean type, with two values: "true" and "false".
In addition, in the boolean operations, it supports a truthiness check,
which converts values of any type to booleans, in a type-specific fashion.
For integer, 0 is false, and everything else is true; for floats, 0.0 and NaN are
false, and everything else is rue.

### Strings

Pretty standard strings, with C-style unicode escapes.

String provides a "length" method.


### TwoDPoints, Polygons, and Extrusion Profiles

Simplex has a set of two-D types and functions that are used as the basis
for extrusions. A two-D shape is called a polygon, and is defined by
a collection of 2-d points arranged clockwise around the perimeter of the
polygon.

#### TwoD points

Two dimension points are implemented by the type `TwoDPoint`. They're defined
in rectangular coordinates. You create a 2D point by calling the builtin
`p2d(x: Float, y: Float)` function.

TwoD points provide operations for addition, subtraction, negation, and comparison.
They also provide the following methods:
* `->mag(): Float`, which returns the length of the diagonal from (0,0) to the point.
* `->multBy(f: Float): TwoDPoint`, which multiplies the point by a float - scaling the
  point.

#### Polygons

Polygons are defined using collections of points. The points have to
be arranged in counter-clockwise order. You can create a polygon from
an array of TwoDPoints using the `polygon` function, but most of the time,
you'll use some combination of builtin polygon functions:

* `circle(radius: Float): Polygon`
* `circleQ(radius: Float, quality: Int): Polygon`
* `squared_circle(diameter: Float, xPad: Float, yPad: Float): Polygon`
* `rectangle(center: 2DPoint, width: Float, height: Float): Polygon`

Polygons also support a large set of methods:
* `->area(): Float` returns the area enclosed by the polygon.
* `->centroid(): TwoDPoint` returns the center of the points of the polygon.
* `->scale(factor: Float): Polygon` scales the polygon by a floating point factor.
* `->scale2(x: Float, y: Float): Polygon` scales the polygon by one factor in
  the X dimension, and a different factor in the Y.
* `->with_area(area: Float): Polygon` scales the polygon so that it has a
  desired area.
* `->with_diam(diam: Float): Polygon` scales the polygon so that the average
  distance from its centroid to its points is a desired value.
* `->with_circumference(circ: Float): Polygon` scales the polygon so that its
  perimeter is a desired value.
* `->move(x: Float, y: Float): Polygon` moves the polygon by a distance.
* `->flipX(): Polygon` mirrors the polygon around its vertical center.
* `->flipY(): Polygon` mirrors the polygon around its horizontal center.
* `extrude(profile: ExtrusionProfile): Csg` extrude the polygon into 3d using
  a profile. (See the extrusion profile section for details.)
* `extrude(profiles: [ExtrusionProfile]): Csg` extrude the polygon into 3d using
  an array of profiles.

#### Extrusion Profiles and Slices

An extrusion profile describes how to extrude a basic polygon into
a 3D shape. Conceptually, the profile defines how to transform
a polygon into a series of slices and then applies a skin over
those slices.

Each slice of a profile provides a position (the position of the
slice in the Z axis), and a pair of diameters describing the
diameter of the polygon at the bottom of the section defined by the slice,
and the diameter of the polygon at the top of the section defined by the slice.

Slices are defined using the `slice(pos: Float, low: Float, high: Float): ProfileSlice`
function.

Profiles are stacks of slices, defined using `profile(slices: [ProfileSlice]): ExtrusionProfile`.

Extrusion Profiles can be added, subtracted, and negated using arithmetic operators. I
Profiles provide methods:

* `->clipped(low: Float, high: Float): ExtrusionProfile`: extends or clips a profile.
* `->move(distance: Float): ExtrusionProfile`: move a profile in the Z axis.
* `->append(p: ExtrusionProfile): ExtrusionProfile`: create a new profile by appending
  another profile on top of the current profile. The Z position of the top slice
  of the target profile is treated as the z=0 point for the appended profile.
* `->stepped(stepSize: Float): ExtrusionProfile`: smooth a profile by introducing
  new step slices between the profile's slices at a distance.


### ThreeDPoints and CSGs

ThreeDPoints are implemented by the type `ThreeDPoint.`. You can create a
point using `point(x: Float, y: Float, z: Float): ThreeDPoint`.

`Point3D`s can be added and subtracted using standard arithmetic operations.
The also provide the following  methods:

* `->scale(factor: Float): ThreeDPoint`
* `->scale3(x: Float, y: Float, z: Float): ThreeDPoint`
* `->rot(xAngle: Float, yAngle: Float, zAngle: Float): ThreeDPoint`
* `->multBy(scalar: Float): ThreeDPoint`
* `->divBy(scalar: Float): ThreeDPoint`

CSGs are the whole point of Simplex. They're threeD bodies that
can be rendered as STL files. You can create CSG values either by
extruding a polygon, or by using one of the built-in CSG functions.

The built-in 3d creation functions are:
* `block(x: Float, y: Float, z: Float): Csg`
* `sphere(radius: Float): Csg`
* `cylinder(height: Float, bottomRadius: Float, topRadius: Float): Csg`

For manipulating CSGs, the arithmetic operators are overloaded:
* `a + b` takes the union of two CSGs;
* `a - b` takes the difference of the CSGs;
* `a / b` takes the intersection of the CSGs

CSGs also provide the following methods:
* `->bounds(): [ThreeDPoint]` return the minimum and maximum points of the bounding box of
   the CSG.
* `->centroid(): ThreeDPoint` return the centroid of the CSG.
* `->hull(): Csg` return the convex hull of the CSG.
* `->move_to(x: Float, y: Float, z: Float): CSG` move the centroid of the CSG to the specified location.
* `->move(x: Float, y: Float, z: Float): CSG` move the CSG by a distance in each of the dimensions.

