# 3d Primitives

* Solid
  * constructors:
    * cuboid(x, y, z)
    * ovoid(r|r,segments|x,y,z|x,y,z,segments)
    * cylinder(height, lowRadius, highRadiusL? segments?)
  * ops:
    * move(x, y, z)
    * rotate(x, y, z)
    * scale(x, y, z)
    * union(other)
    * intersection(other)
    * difference(other)
    * bounds()
    * mirror(x,y,z|v)
    * surfaceArea()
    * volume()
    * split(norm)
    * slice(height)
    * smooth()  -- sets up halfedge tangents, which will then be
                  used when someone calls refine to subdivide the
                  mesh. (Refine introduces new points between the existing
                  ones, positioning the new points according to the
                  halfedge tangents, if present.
    * project
    * hull

* Slice
  * constructors:
    * Rectangle(l,w)
    * Circle(r|x,y|x,y,segments)
    * Polygon([vec2])
  * ops
    * move(x,y)
    * scale(x,y)
    * mirror(norm)
    * simplify(epsilon)
    * union
    * intersect
    * subtract
    * extrude
    * revolve
* Points
  * Vec2
  * Vec3
* Mesh(?)
* Material(?)
* Color
* BoundingBox
* BoundingRect
*
