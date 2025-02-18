# simplex: A programming language for building 3d printable models.

(This file, and all other documentation for Simplex, are Copyright 2024, by Mark C. Chu-Carroll.
Simplex and its documentation are licensed under the Apache License, version 2.0. See
the [LICENSE](./LICENSE) file for details.)

## What is it?

Another language for building models for 3d printing. There's an
introduction to the language [here](docs/simplex-language.md).
For a fairly complicated example model, you can look
at my [tenor guitar](https://github.com/MarkChuCarroll/instruments/blob/main/tenor/mtenor.s3d).


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

## Building Simplex

Building simplex is still messy. It relies on the unofficial Java bindings
for the Manifold library, which aren't very well supported. The only versions
of them online are for Intel based machines running either MacOS or Linux.

So, the first step for building Simplex is to build its dependencies.

1. Build [Manifold](https://github.com/elalish/manifold). Follow the instructions
  on the website, and build and install the dynamic libraries with the MeshIO extension.
2. Build [Clipper2](https://github.com/AngusJohnson/Clipper2). Download the sources,
  and follow the instructions to build and install the C++ DLL.
3. Build [the Java bindings for manifold](https://github.com/SovereignShop/manifold.git). Download
  the source from the link, and cd into `manifold/bindings/java`. Then run `mvn package` to compile.
  This will produce a jarfile, which will contain the Java JNI bindings, and the dynamic libraries
  for manifold, meshio, and clipper. Copy the resulting jar file from `target/manifold3d-1.0.39.jar`
  to `src/main/resources` in the Simplex source tree.
4. Build simplex, by running "gradle shadowJar". This will produce an executable jarfile in
   `build/libs/simplex-0.0.1.jar`.
5. Create an alias to run it, by running:
   ```alias simplex="java -jar $(pwd)/build/libs/simplex-0.0.1.jar```
6. Now you can run simplex models with the simplex command.


## But wait, that's not all!

In addition to the basic Simplex implementation, there's three add-ons in various states
of completeness:

1. In `src/elisp`, there's a very early Emacs mode for editing Simplex files. It's
  It doesn't do much other than very basic syntax highlighting, but that's a nice thing
  to have.
2. In `treesitter-grammar`, there's a (surprise!) treesitter grammar for Simplex. Again,
  it's pretty early-stage code, but it's got the complete simplex grammar.
  It should be possible to use that to set up syntax hightlighting for any editor
  that supports treesitter, including (at least) neovim, helix, and kakoune.
3. In `src/main/kotlin/org/goodmath/simplex/lsp`, I've started the even more
  early-stage code for a language server for Simplex. This doesn't do much of
  anything at the time of this writing, but I hope to gradually start getting bits
  and pieces of it working, which should make it easy to build a VSCode extension
  for it. Ideally, I'd like to set that up with a connection to some kind of STL
  rendering GUI, to give users an OpenSCAD-like experience with a proper editor.

## Contributing

If you're interested in contributing to Simplex, feel free to submit
PRs, or contact me by email. I'm assuming that probably this will remain
a solo project, but if people are interested, I'd be thrilled to have help.
