# Running a Simplex model 

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
* `products`: a comma-separated list of the products to generate. If
  no value is specified, then all products will be generated.
* `verbosity`: a setting for how much output it should generate on stdout while
  evaluating the model. THe default value is 1; 2 and 3 will each produce
  more debug information; 0 will produce no output on stdout.
