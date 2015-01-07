# cljspazzer

A personal music collection app

## Usage

FIXME

Eventually you should just be able to run a jar, but not now, there's no ui for this yet to speak of. But for future reference...

### Building

```bash
$ lein ring uberjar
```

And wait.....

### Running

```bash
$ java -jar target/cljspazzer-0.1.0-SNAPSHOT-standalone.jar
```

## Hacking

### Dependencies

- [Leiningen](http://leiningen.org/)
- [NodeJs](http://nodejs.org/)
- [Gulp](http://gulpjs.com/)


#### Setting up Gulp

```bash
# from the project directory
$ npm install gulp
$ npm install touch

```

### Running server

```bash
# from the project directory
$ lein ring server
```

Now you can point your browser at http://localhost:5050 to play with the app. 

### compiling client code

```bash
# from the project directory
$ lein cljsbuild auto
```

Now anytime a *.cljs file is updated things will be re-built


### compiling templates

```bash
# from the project directory
$ gulp
```

Now any time you update a file in resources/templates/ gulp will cause
the pages.cljs to be re-compiled.

### getting initial tag data

Start a repl

```bash
# from the project directory
$ lein repl
```

Now you can interactively call clojure functions defined for the application. 

#### creating initial database

In your repl....

```bash
user=> (use 'cljspazzer.db.schema)
nil
user=> (in-ns 'cljspazzer.db.schema)
#<Namespace cljspazzer.db.schema>
cljspazzer.db.schema=> (create-all-tbls! the-db)
((0) (0))
cljspazzer.db.schema=> 
```

You should now have a database located at spazzer.db

#### adding a directory(mount) for scanning

in your repl

````bash
user=> (use 'cljspazzer.db.schema)
nil
user=> (in-ns 'cljspazzer.db.schema)
#<Namespace cljspazzer.db.schema>
cljspazzer.db.schema=> (insert-mount! the-db "/path/to/musicfiles")
1
cljspazzer.db.schema=> 
````

You should now have a directory registered with the application that will be scanned for new music files/updated tags when process-mounts! is called

#### scanning your directories(mounts)
in your repl

````bash
user=> (use 'cljspazzer.db.scanner)
nil
user=> (in-ns 'cljspazzer.db.scanner)
#<Namespace cljspazzer.db.scanner>
cljspazzer.db.scanner=> (process-mounts!)
nil
cljspazzer.db.scanner=> 
````

There should now be data in the database derived from files contained in one or more mounted directories added in the previous step.

## License

Copyright © 2015 FIXME
