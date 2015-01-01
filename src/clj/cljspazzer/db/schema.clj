(ns cljspazzer.db.schema
  (:require [clojure.java.jdbc :as sql]
            [clojure.java.io :as io]))

(def the-db {:classname "org.sqlite.JDBC",
             :subprotocol "sqlite",
             :subname "spazzer.db",
             :make-pool? true}) ;; how to config

(def tables {:tracks {:name "tracks"
                      :columns[[:id :integer "PRIMARY KEY" "AUTOINCREMENT"]
                               [:path :string "UNIQUE"]
                               [:title :string]
                               [:artist :string]
                               [:year :string]
                               [:disc_no :integer]
                               [:album :string]
                               [:artist_canonical :string]
                               [:album_canonical :string]
                               [:title_canonical :string]
                               [:last_modified :integer]
                               [:genre :integer]]}
             :mounts {:name "mounts"
                      :columns [[:id :integer "PRIMARY KEY" "AUTOINCREMENT"]
                                [:path :string "UNIQUE"]]}})

(defn create-tbl! [db, tbl]
  (let [args (cons (:name tbl) (:columns tbl))]
    (sql/db-do-commands db
                      (apply sql/create-table-ddl args))))

(defn drop-tbl! [db tbl]
  (sql/db-do-commands db (sql/drop-table-ddl (:name tbl))))

(defn create-all-tbls! [db]
  (map (partial create-tbl! db) (vals tables)))

(defn drop-all-tbls! [db]
  (map (partial drop-tbl! db) (vals tables)))

(defn column-names [t]
  (map first (:columns t)))

(defn track-exists? [db t]
  (> (:count
      (first
       (sql/query db ["select count(id) as count from tracks where path=?" t])))
     0))

(defn update-track!
  "returns count of rows affected"
  [db track-info]
  (first (sql/update! db :tracks track-info ["path=?" (:path track-info)])))

(defn insert-track!
  "returns last row id which should correspond to id"
  [db track-info]
  ((keyword "last_insert_rowid()") (first (sql/insert! db :tracks track-info))))

(defn delete-track!
  "returns rows effected which should be > 1 if successful"
  [db path]
  (first (sql/delete! db :tracks ["path=?" path])))

(defn upsert-track!
  "returns {id:??}for intert or {row-count:??} for update"
  [db track-info]
  (let [update-result (update-track! db track-info)]
    (if (> update-result 0)
      {:row-count update-result :action :update}
      {:id (insert-track! db track-info) :action :insert})))

(defn insert-mount! [db p]
  ((keyword "last_insert_rowid()") (first (sql/insert! db :mounts {:path p}))))

(defn delete-mount!
  "returns rows effected which should be > 1 if successful"
  [db path]
  (first (sql/delete! db :mounts ["path=?" path])))

(defn mount-points
  "returned with trailing / to denote directory, io/file does the
  normalization I think"
  [db]
  (map (fn [x] (str (.getAbsolutePath (io/file (:path x))) "/"))
       (sql/query db ["select path from mounts"])))

(defn last-modified-index
  "{path:last_modified}"
  [db]
  (let [rows (sql/query db ["select path, last_modified from tracks"])
        result (into {} (map (fn [x] (let [k (:path x)
                                           v (:last_modified x)
                                           r (hash-map k v)]
                                       r)) rows))]
    result))

(defn starts-with? [s prefix]
  (if (> (count prefix) (count s))
    false
    (= prefix (subs s 0 (count prefix)))))

(defn managed? [f mounts]
  (some (partial starts-with? (.getAbsolutePath f)) mounts))

(defn prune-tracks!
  "get rid of rows where the files no longer exist"
  [db]
  (let [fkeys (keys (last-modified-index db))
        mounts (mount-points db)
        should-prune? (fn [f] (or (not (.exists f)) (not (managed? f mounts))))
        files (filter should-prune? (map io/file fkeys))]
    (map (partial delete-track! db) (map (fn [f] (.getAbsolutePath f)) files))))