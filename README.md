# cljspazzer

A personal music collection app

## Usage

FIXME

## Hacking

### Dependencies

- [Leiningen](http://leiningen.org/)
- [Guard](http://guardgem.org/)

### Running server

```bash
$ lein ring server
```

### compiling client code

```bash
$ lein cljsbuild auto
```

### compiling templates

```bash
$ guard
```

### getting initial tag data

Start a repl

```bash
$ lein repl
```

#### creating initial database

in your repl....

```bash
user=> (use 'cljspazzer.db.schema)
nil
user=> (in-ns 'cljspazzer.db.schema)
#<Namespace cljspazzer.db.schema>
cljspazzer.db.schema=> (create-all-tbls! the-db)
((0) (0))
cljspazzer.db.schema=> 
```

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


## License

Copyright © 2015 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
