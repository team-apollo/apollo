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
                               [:disc :integer]
                               [:album :string]]}})

(defn create-tbl [db, tbl]
  (let [args (cons (:name tbl) (:columns tbl))]
    (sql/db-do-commands db
                      (apply sql/create-table-ddl args))))

(defn drop-tbl [db tbl]
  (sql/db-do-commands db (sql/drop-table-ddl (:name tbl))))

(defn create-all-tbls [db]
  (map (partial create-tbl db) (vals tables)))

(defn drop-all-tbls [db]
  (map (partial drop-tbl db) (vals tables)))
