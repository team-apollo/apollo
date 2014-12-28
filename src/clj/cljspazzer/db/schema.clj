(ns cljspazzer.db.schema
  (:require [clojure.java.jdbc :as sql]))

(def the-db {:classname "org.sqlite.JDBC",
             :subprotocol "sqlite",
             :subname "spazzer.db",
             :make-pool? true}) ;; how to config

(def tables {:tracks {:name "tracks"
                      :columns[[:id :integer "PRIMARY KEY" "AUTOINCREMENT"]
                               [:path :string]
                               [:title :string]
                               [:artist :string]
                               [:year :string]
                               [:disc_no :integer]
                               [:album :string]
                               [:artist_canonical :string]
                               [:album_canonical :string]
                               [:title_canonical :string]
                               [:last_modified :integer]
                               [:genre :integer]]}})

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

(defn insert-track! [db track-info]
  (sql/insert! db :tracks track-info))

(defn last-modified-index
  "{path:last_modified}"
  [db]
  (let [rows (sql/query db ["select path, last_modified from tracks"])
        result (into {} (map (fn [x] (let [k (:path x)
                                           v (:last_modified x)
                                           r (hash-map k v)]
                                       r)) rows))]
    result))
