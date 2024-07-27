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
 (def-fun cone <CSG> (radius <Float> height <Float>)
      (cylinder (+ height 2.0) radius 0.1))

  (def-fun dbl <CSG> (radius <Float> height <Float>)
    (let ((c (cone height radius)))
     (+ c
       (-> move
         (-> rot c 0.0 180.0 0.0)
         0.0 0.0 25.0))))
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

Simplex is basically a simple language from the Lisp family.

### Definitions 

It's got three things you can define in a simplex model: functions, variables,
and tuples. 

A function is something that takes parameters, does something
with them, and returns a result. In Simplex, they look like:

```
(def-fun _name_  < return_type > ( param1 < type1 >  ) 
    expr...)
```
This defines a function named "name", which returns a value of type "type1".
It takes a parameter named "param1", which has type "type1". When
you call it, it evaluates the expressions in its body, returning
the value of the last one.

Variable definitions look similar:

```
(def-var name < type > expr)
```

Variables give a name to a value. But they're not mutable:
there's no way to change the value of a variable.

Finally, there are tuples, which are a simple user-defined data
type. They're probably not going to get used much, but I
couldn't resist adding them.

```
(def-tup name {  })
```

### Expressions

#### Function Calls and Operators

```
(f 1 2.0 "abc")
(+ f 3)
```


#### Let Expressions

```
(let ((x (f a)) (y (g b x)))
   (something x y))    
```

#### Conditionals

```
(cond
   ((== x y)  "true")
   (else "false"))
```

### With

```
(with tuple 
   (+ field1 field2))
```

### Update

```
(update tuple set (field1 3))
```

### Method

Methods are operations provides to work on 
built-in objects. For example, if you have
a CSG, you can change its position by calling
its "move" method.

```
(-> move cyl 10 10 5)
```
