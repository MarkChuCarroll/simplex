
# Builtin Types

Simplex has a collection of builtin types that provide basic 
arithmetic, plus types for 2d and 3d object construction.  For each type,
I'll introduce its literal syntax (if any), the constructor functions
for creating an instance, and its methods.

### Int

`Int` is a basic integer value. It's a 64 bit, signed integer value. 
It's literal syntax is normal - a string of digits without any
decimal point.

You can use an `Int` in places where a float is expected; it will
be automatically converted to a float.

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

Floating point numbers. These are standard, 64 bit double-precision IEEE
floating point values. They're written using standard floating point
syntax, and can use exponents.

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

Strings are UTF-8 strings, written inside of double-quotes, and 
using standard C-style escapes.

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

A solid is a 3d object. There are no literals for solids; you have to use
a constructor function to create one. Solids are always created with their
center on the origin.

* Constructor functions
    * `ovoid(radius:  Float)`: sphere with the specified radius.
    * `ovoid(radius: Float, facets: Int)`: approximate sphere made from circles
      with the specified number of facets.
    * `ovoid(x: Float, y: Float, z: Float, segments: Int)`: ovoid with specified radii,
      made from the specified number of linear segments.
    * `cuboid(v: Vec3)`: three-dimensional rectangle with edge sizes from the vector.
    * `cuboid(x: Float, y: Float, z: Float)`
    * `cylinder(height: Float, radius: Float)`: cylinder of the specified height and radius, with
      the origin at the center of the bottom face.
    * `cylinder(height: Float, radiusLow: Float, radiusHigh: Float)`: cylinder with a varying radius (conic section)
    * `cylinder(height: Float, radiusLow: Float, radiusHigh: Float, facets: Int)`
    * `tetrahedron(size: Float)`: tetrahedron with edges of the specified size.
* Methods
    * `->bounds(): BoundingBox`: return the bounding box of the solid.
    * `->move(x:  Float, y; float, z: Float): Solid`: move the solid.
    * `->move(v: Vec3): Solid`
    * `->left(x: Float)`: move distance `x` towards the negative on the x-axis.
    * `->right(x: Float)`: move distance `x` towards the positive on the x-axis.
    * `->backwards(y: Float)`: move distance `y` towards the negative onthe y-axis.
    * `->forwards(y: Float)`: move distance `y` towards the positive on the y-axis.
    * `->down(z: Float)`: move distance `z` towards the negative on the z-axis.
    * `->right(z: Float)`: move distance `z` towards the positive on the z-axis.
    * `->scale(x: Float, y: Float, z: Float): Solid`: scale the solid.
    * `->scale(v: Vec3): Solid`
    * `->rotate(x: Float, y: Float, z: Float): Solid`: rotate the solid. Angles are measured in degrees.
    * `->rotate(v: Vec3): Solid`
    * `->rotx(x: Float)`: rotate by `x` degrees around the x-axis.
    * `->roty(y: Float)`: rotate by `y` degrees around the y-axis.
    * `->rotz(z: Float)`: rotate by `z` degrees around the z-axis.* 
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

A bounding box is the minumum 3-dimensional rectangular shape enclosing
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
sliced from a solid, or extruded into a solid.

* Constructor Functions
    * `circle(radius: Float): Slice`
    * `circle(radius: Float, facets: Int): Slice`
    * `oval(x:  Float, y: Float): Slice`
    * `oval(x: Float, y: Float, facets: Int): Slice`
    * `rectangle(x: Float, y: Float): Slice`
    * `batch_hull(slices: [Slice]): Slice`
    * `polygon_to_slice(polygon: Polygon): Slice`
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

## Polygon

A polygon is a 2d shape formed from a collection of 2d points. The points
of a polygon must be convex, and must be ordered in counter-clockwise order.

**TODO: constructors and methods**   

### BoundingRect

A bounding rect is the minimum 2d rectangle enclosing a slice or a polygon.

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
