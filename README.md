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
fun cone(radius: Float, height: Float): CSG do
   cylinder(height + 2.0, radius, 0.1)
end      

fun dbl(radius <Float> height <Float>): CSG do
    let c = cone(height, radius) in
       c + c->rot(0.0, 180.0, 0.0)->move(0.0, 020, 25.0)
     end
end
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

Simplex is basically a simple functional language.

### Definitions 

It's got three things you can define in a simplex model: functions, variables,
and tuples. 

A function is something that takes parameters, does something
with them, and returns a result. In Simplex, they look like:

```
fun fact(n: Int): Int do
  cond 
     n == 0 then 1
     else n * fact(n-1)
  end
end  
```

Value definitions are simple:

```
val name :type = expr
```

Variables give a name to a value. But they're not mutable:
there's no way to change the value of a variable.

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

```
let x = f(a), y = g(b, x) in
   something(x, y)
end       
```

#### Conditionals

```
cond
   x == y then  "true"
   else "false"
end   
```

### With

```
with tuple do 
   field1 + field2
end   
```

### Update

```
update tuple set field1=3, field2=4
```

### Method

Methods are operations provides to work on 
built-in objects. For example, if you have
a CSG, you can change its position by calling
its "move" method.

```
cyl->move(10, 10, 5)
```
