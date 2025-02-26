# Simplex Syntax

A Simplex program consists of a bunch of code that creates 3d objects,
and a set of _products_, which define how to write the generated object
out into STL files.

In the code, Simplex allows you to write both simple expressions, and
definitions of functions, data types, and values that you'll use to write
your model.


## Definitions

There are four kinds of definitions: functions, data types, variables, and methods.

### Function Definitions

A function definition looks like:

```
fun name(arg: type, ...): type {
  expr...
}
```

A function returns the value of its last expression. A function must define the types of
its parameters, and the type of value that it returns. Every function returns some
value; if there's no reasonable value, it will return a special "none" value.

### Data Type Definitions

Data type definitions allow you to define new value types. A value
type is a typed tuple of values.

```
data TypeName {
  field: type...
}
```

For example, if you wanted to represent a cylinder using its height and radii, you could
do that with:
```
data Cylinder {
   height: Float
   lowerRadius: Float
   upperRadius: Float
}
```


### Variable Definitions

```
let name = expr
let name: Type = expr
```

A "let" expression defines a new variable in a scope. It can be used either at the
top-level of a program, or inside a function or product. A let expression
can declare the type of its value; or it can omit the value type, and Simplex
will use type inference. One subtlety of "let" is that it's an expression,
which returns the value assigned to the new variable.

So, for example, the following is perfectly valid code:
```
  foo(let x = 3, 7)
```
The main place where this can be confusing is inside a product block. A
product outputs the union of all the solid objects produced by expressions
within its block. So if there's a let expression in a product block, it
will define a new local variable, and _also_ add its value to the set of
things that will be output by the product.

### Method Definitions

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

## Products

A single simplex model can generate  multiple outputs. When you run simplex,
it can either generate all the outputs  defined in the program, or you can
specify on the command line which products you want to generate.

The way that this is done is by using product statements. A product statement provides a
name for a collection of values. When you specify that you want to produce a product, Simplex will
evaluate the model to get the values specified in the product, and then output those.

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

The output files are named as "prefix-productname.suffix". For example, if you
ran a simple program called "model.s3d", and specified "mout" as the prefix,
then a product "prod" would generate files:
* `mout-prod.stl` for the STL of the solids defined in the product;
* `mout-prod.twist` for the twist text of any complex values defined in the product;
* `mout-prod.txt` for text renderings of any text objects produced in the product.



## Expressions

Expressions mostly follow familiar syntax patterns.

### Arithmetic

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

### Control Flow

#### Conditionals

```
if (cond) expr
elif (cond) expr
...
else expr
```

#### Loops

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

#### Assignment

```
name := value
expr.name := value

expr[expr] := expr
```

## Imports

A simplex model can import code from a _library file_. A library
is just a simplex source file that _only_ contains definitions, but
no products. When you import a library, its definitions become 
accessible as _scoped names_.

An import statement is written:
```
import "source-file.s3d" as scopename
```

Definitions from the imported module can be accessed as `scopename::name`.

