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
- [Bower](http://bower.io/)


#### Setting up js libs(reactjs/fontawesome)

```bash
# from the project directory/resources/public
$ bower install
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

Update: in the browser you can now do this from the settings page, ugly as it may be

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

Update: in the browser you can now do this from the settings page, ugly as it may be

in your repl

````bash
user=> (use 'cljspazzer.scanner)
nil
user=> (in-ns 'cljspazzer.scanner)
#<Namespace cljspazzer.scanner>
cljspazzer.scanner=> (process-mounts!)
nil
cljspazzer.scanner=> 
````

There should now be data in the database derived from files contained in one or more mounted directories added in the previous step.

## License

Copyright © 2015 FIXME
