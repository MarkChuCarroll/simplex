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

## Syntactic Basics

A Simplex program has two sections: a collection of definitions, and a collection
of products.

Definitions are where you define the functions, data types, and values that you'll
use to write your model. Products are where you define what objects you want to
output. A given model (source file) can have many different products, and you can
choose which ones to generate when you run simplex on your model.

### Definitions

There are four kinds of definitions: functions, data types, variables, and methods.

#### Function Definitions

A function definition looks like:

```
fun name(arg: type, ...): type {
  expr...
}

```

A function returns the value of its last expression.

#### Data Type Definitions

Data type definitions allow you to define new value types. A value
type is a typed tuple of values.

```
data TypeName {
  field: type...
}

```


#### Variable Definitions

```
var name = expr
var name: Type = expr

```


#### Method Definitions

A method is almost the same thing as a function - the main difference
is syntax. Instead of writing `name(arg1, arg2, arg3)`, you write
`arg1->name(arg2, arg3)`.

There's two advantages to this:
1. For some operations, postfix notation makes the model much easier to read;
2. There are some operations like "size" that make sense for multiple types;
  a method can be implemented for all of those types without collision.

Methods aren't dynamically bound: there's no notion of subtyping
in Simplex, so dynamic binding isn't really possible. But they make
things easier to read.

For example, look at the following snippet:

```
   rotate(90.0, 90.0, 90.0) {
      translate(20, 45, 90) {
          rotate(45, 0, 0) {
              union() {
                 sphere(100, 20, 10)
                 translate(50, 0, 0) {
                    cylinder(200, 40, 20)
                 }
              }
          }
      }
   }

```
What shape does this describe? You have to read through the code to find the innermost
part to figure out what it's making. In contrast, in postfix:
```
   (sphere(100, 20, 10) +
    cylinder(200, 40, 20)
     ->move(50, 0, 0))
     ->rotate(45.0, 0.0, 0.0)
     ->move(25.0, 45.0, 90.0)
     ->rotate(90.0, 90.0, 90.0(
```

The latter puts the most important part right up front, and then
puts the transformations in the order in which they occur.

A method definition is written as:
```
meth TargetType->name(arg0, arg1, ...): Type {
   expr...
}

```

You can define a new method on _any_ type, not just new data types that you define.

### Products

A simplex model can generate  multiple results,
and you can select the results you want it to generate
from the command line.

The way that this is done is by using product
statements. A product statement provides a
name for a collection of values. When you specify
that you want to produce a product, Simplex will
evaluate the model to get the values specified in the product,
and then output those.

A product look like:

```
produce("name") {
   expr
   ...
}
```

The way that outputs work is:
* All values in the product of type Solid are
  unioned together, and the result is output into
  an STL file.
* All values in the product whose value types
  specify that they support text rendering will
  be output into a text file.
* All other values in the product will be rendered
  as twists into a twist file.


### Expressions

Expressions mostly follow familiar syntax patterns.

#### Arithmetic

Simplex supports infix arithmetic. Under the cover, arithmetic is done using
methods - each infix operator is translated into a method call:


| Operator | Method                    | Description             |
|----------|---------------------------|-------------------------|
| +        | plus                      | addition                |
| -        | minus(infix), neg(prefix) | subtraction             |
| *        | times                     | multiplication          |
| /        | div                       | division                |
| %        | mod                       | modulo                  |
| ^        | pow                       | exponent                |
| ==       | eq                        | equal                   |
| <        | compare                   | comparison result == -1 |
| >        | compare                   | comparison result == +1 |
| <=       | compare                   | comparison result <= 0  |
| !=       | compare                   | comparison result >= 0  |

#### Control Flow

##### Conditionals

```
if (cond) expr
elif (cond) expr
...
else expr
```

##### Loops

Simplex has two kinds of loops: for loops, and while loops.

A for loop is slightly different from what you might expect: it iterates over a vector
of values, executing the body of the loop for each element. But it's an expression: it collects
the results of the iteration, and returns them as a new vector. In that sense, it's more like
a map operation in a typical functional language.

```
for idx in collection {
  body
}
```


Whiles, on the other hand, are just completely as you expect.

```
while expr {
  expr
}
```

##### Local Variable Definition

```
let name : type = expr
let name = expr
```

##### Assignment

```
name := value
expr.name := value

expr[expr] := expr
```

#####


## Builtin Types

### Int

* Methods
   *  `->to(max: Int): [Int]`: returns a vector of integers starting with
      the target, and ending with the max parameter, stepping by one.
   * `->float(): Float`: returns a float value equal to (or as close to equal to as possible)
     to the integer target.
   * `->plus(other: Int): Int`
   * `->minus(other: Int): Int`
   * `->times(other: Int): Int`
   * `->div(other: Int): Int`
   * `->mod(other: Int): Int`
   * `->pow(other: Int): Int`
   * `->eq(other: Int): Boolean`
   * `->compare(other: Int): Int`
   * `->neg(): Int`
* Constants:
   * `MAXINT`
   * `MININT`

### Float

* Methods
   * `->isNan(): Boolean`
   * `->truncate(): Int`
   * `->plus(other: Float): Float`
   * `->minus(other: Float): Float`
   * `->times(other: Float): Float`
   * `->div(other: Float): Float`
   * `->mod(other: Float): Float`
   * `->pow(other: Float): Float`
   * `->eq(other: Float): Float`
   * `->compare(other: Float): Int`
   * `->neg(): Float`
   * `->sqrt(): Float`
* Constants
   * `pi = 3.14159....`

### String

* Methods
    * `->length(): Int`
    * `->find(s: String): Int`
    * `->plus(s: String): String`
    * `->eq(s: String): Boolean`
    * `->compare(s: String): Int`
    * `->substring(start): String`
    * `->substring(start, end): String`
    * `->to_upper(): String`
    * `->to_lower(): String`
    * `->replace(from: String, to: String): String`
    * `->replace(from: String, to: String, ignore_case: Boolean): String`
    * `->find(str: String): Int`
    * `->find(str: String, startPos: Int): Int`

### None

None is a way of representing the idea of a function
that doesn't return anything. There's a `None` type,
and a `none` value. The "none" value is unique,
not truthy, and doesn't implement any methods. It cannot
even be compared to itself.

### Solid

* Constructor functions
   * `ovoid(radius:  Float)`: sphere with the specified radius.
   * `ovoid(radius: Float, facets: Int)`: approximate sphere made from circles
      with the specified number of facets.
   * `ovoid(x: Float, y: Float, z: Float, segments: Int)`: ovoid with specified radii,
      made from the specified number of linear segments.
   * `brick(v: Vec3)`: three-dimensional rectangle with edge sizes from the vector,
      centered on the origin.
   * `brick(v: Vec3, center: Boolean)`: three-dimensional rectangle. If `center==true`, then
     the origin will be at the center of the brick, otherwise it will be at the corner.
   * `brick(x: Float, y: Float, z: Float)`
   * `brick(x: Float, y: Float, z: Float, centered: Boolean)`
   * `cylinder(height: Float, radius: Float)`: cylinder of the specified height and radius, with
     the origin at the center of the bottom face.
   * `cylinder(height: Float, radiusLow: Float, radiusHigh: Float)`: cylinder with a varying radius (conic section)
   * `cylinder(height: Float, radiusLow: Float, radiusHigh: Float, facets: Int)`
   * `tetrahedron(size: Float)`: tetrahedron with edges of the specified size.
* Methods
   * `->bounds(): BoundingBox`: return the bounding box of the solid.
   * `->move(x:  Float, y; float, z: Float): Solid`: move the solid.
   * `->move(v: Vec3): Solid`
   * `->scale(x: Float, y: Float, z: Float): Solid`: scale the solid.
   * `->scale(v: Vec3): Solid`
   * `->rotate(x: Float, y: Float, z: Float): Solid`: rotate the solid. Angles are measured in degrees.
   * `->rotate(v: Vec3): Solid`
   * `->mirror(norm: Vec3): Solid`: mirror the solid around the origin.
   * `->plus(other: Solid): Solid` (also infix +): take the union of this solid with a another.
   * `->minus(other: Solid): Solid`: remove any intersecting sections of another solid.
   * `->intersect(other: Solid): Solid`: keep only intersecting sections with another solid.
   * `->genus(): Int`
   * `->surface_area(): Float`: get the total surface area of the solid.
   * `->volume(): Float`: get the volume of a solid.
   * `->split_by_plane(normal: Vec3, origin_offset: Float): [Solid]`: Split a solid into two new
      solids using a plate. The plane is oriented to be perpendicular to the normal vector, at a distance
      specified by the offset along the normal vectors direction.
   * `->split(other: Solid): [Solid]`: split a solid into two solids, using the edge of another
     solid as a dividing line.
   * `->hull()`: taket the convex hull of the current solid.
   * `->slice(height: Float): Slice`: take a horizontal slide of the solid at a height.
   * `->slices(bottom: Float, top: Float, count: Int): [Slice]`: take a series of horizontal slices of
   a solid along a vertical range.
   * `->project(): Slice`: project the solid onto the XY-plane, producing a slice.


### Bounding Box

A bounding box is the minumum 3 dimensional rectangular solid enclosing
a solid.

* `->size(): Vec3`
* `->center(): Vec3`
* `->low(): Vec3`
* `->high(): Vec3`
* `->contains_point(p: Vec3): Boolean`
* `->contains_box(b: BoundingBox): Boolean`
* `->union(b: BoundingBox): BoundingBox`
* `->expand_to(point: Vec3): BoundingBox`
* `->move(pt: Vec3): BoundingBox`
* `->scale(x: Float, y: Float, z: Float): BoundingBox`
* `->scale(v: Vec3): BoundingBox`
* `->does_overlap(other: BoundingBox): Boolean`
* `->is_finite(): Boolean`



### Slice

A slice is a two-dimensional shape that can be
slided from a solid, or extruded into a solid.

* Constructor Functions
   * `circle(radius: Float): Slice`: create a circular slice with the specified radius,
     positioned with its center on the origin.
   * `circle(radius: Float, facets: Int): Slice`: create an approximately circular faceted slice
     with the specified _radius_ and number of _facets_, positioned with its center on the origin.
   * `oval(x:  Float, y: Float): Slice`: create an oval centered on the origin.
   * `oval(x: Float, y: Float, facets: Int): Slice`
   * `rectangle(x: Float, y: Float): Slice`: create a rectangle centered on the origin.
   * `batch_hull(slices: [Slice]): Slice`
* Methods
   * `->area(): Float`
   * `->num_vert(): Int`
   * `->is_empty(): Boolean`
   * `->move(x: Float, y: Float): Slice`
   * `->move(v: Vec2): Slice`
   * `->rotate(degrees: Float): Slice`
   * `->scale(x: Float, y: Float): Slice`
   * `->scale(factor: Float): Slice`
   * `->mirror(norm: Vec3): Slice`
   * `->hull(): Slice`
   * `->plus(): Slice`
   * `->minus(): Slice`
   * `->intersect(): Slice`
   * `->extrude(height: Float, steps: Int): Solid`
   * `->extrude(height: Float, steps: Int, twist_degrees: Float): Solid`
   * `->revolve(segments: Int): Solid`
   * `->revolve(segments: Int, degrees: Float): Solid`
   * `->bounds(): BoundingRect`

### BoundingRect

A bounding rect is the minimum 2d rectangle enclosing a slice.

* `->size(): Vec2`
* `->center(): Vec2`
* `->contains_pt(point: Vec2): Boolean`
* `->contains_rect(rect: BoundingRect): Boolean`
* `->overlaps(other: BoundingRect): Boolean`
* `->is_empty(): Boolean`
* `->is_finite(): Boolean`
* `->plus(other: BoundingRect): BooleanRect`
* `->times(v: Vec2): BoundingRect`

### Vec2 (aka 2 Dimensional Vector or point)

* Constructor functions:
   * `v2(x: Float, y: Float, z: Float): Vec2`
* Methods:
   * `->plus(other: Vec2): Vec2`
   * `->minus(other: Vec2): Vec2`
   * `->times(other: Vec2): Vec2`
   * `->div(other: Vec2): Vec2`
   * `->neg(): Vec2`
   * `->eq(other: Vec2): Vec2`
   * `->compare(other: Vec2): Int`
   * `->dot(other: Vec2): Float`
   * `->at(z: Float): Vec3`
* Global constants
   * `zero_v2 = v2(0.0, 0.0)`

### Vec3 (aka 3 dimensional vector or point)

* Constructor functions
   * `v3(x: Float, y: Float, z: Float): Vec3`
* Methods
   * `->plus(other: Vec3): Vec3`
   * `->minus(other: Vec3): Vec3`
   * `->times(other: Float): Vec3`
   * `->div(other: Float): Vec3`
   * `->negate(): Vec3`
   * `->eq(other: Vec3): Boolean`
   * `->compare(other: Vec3): Int`
   * `->dot(other: Vec3): Float`
* Constants
   * `zero_v3 = Vec3(0.0, 0.0, 0.0)`



### Vectors

Vectors are a variable-length list of values of a single type. A vector of type
`X` is written `[X]`. Vectors support subscripting using "[]", like `x[3]`.

* Methods
   * `->sub(idx: Int): X`: subscript, returning the element at the index.
   * `->length(): Int`
   * `plus(other: [X]): [X]`: add with another vector of the same element
     type and length, returning the piecewise sum.
   * `->minus(other: [X]): [X]`: subtract another vector of the same element
       typenus and length, returning the piecewise difference.
   * `->eq(other: [X]): Boolean`: compare with another vector,
     returning true if their elements are equal.
   * `->map(op: (X): X): [X]`: apply a function to every element of
     the vector, returning a vector of the results. Since Simplex doesn't
     support generic type signatures, the function must return the same type
     as the element type of the vector.
   * `->filter(pred: (X): Boolean): [X]`: return a new vector containing the
      elements of the target where the predicate function returns true.
   * `->append(other: [X]): [X]`
   * `->push(val: X): [X]`: return a _new_ vector containing the elements
     of this vector with the new element added to the end.
   * `->insert(val: X): [X]`: return a _new_ vector containing the elements
    of the target, with the new element inserted as the first element.
   * `->insert_at(val: X, idx: Int): [X]`: return a new vector with the
    new value insert at the specified index.
   * `->sort(): [X]`. Sort the list, using the comparison operator of
     its element type. Results in an error if the element type doesn't
     provide a comparison method.

## The Simplex Command Line

Simplex runs as a standard command line application.
The syntax is:

```bash
   simplex --prefix=output-prefix --products=product,product,... --verbosity=value model-file.s3d
```

Details about the arguments:
* `prefix`: simplex will generate output files with names
  starting with the prefix. For a product named "p", it will output
  the solid in a file named `prefix-p.stl`. If no prefix is
  specified, then it will use "modelname-out".
* `renders`: a comma-separated list of the products to generate. If
  no value is specified, then all products will be generated.
* `verbosity`: a setting for how much output it should generate on stdout while
  evaluating the model. THe default value is 1; 2 and 3 will each produce
  more debug information; 0 will produce no output on stdout.
