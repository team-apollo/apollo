(ns apollo.db.schema
  (:require [clojure.java.jdbc :as sql]
            [clojure.java.io :as io]
            [apollo.utils :as utils]
            [clj-time.core :as t]
            [clj-time.coerce :as c]))

(def the-db {:classname "org.sqlite.JDBC",
             :subprotocol "sqlite",
             :subname "apollo.db",
             :make-pool? true}) ;; how to config

(def tables {:tracks {:name "tracks"
                      :columns[[:id :integer "PRIMARY KEY" "AUTOINCREMENT"]
                               [:path :string "UNIQUE"]
                               [:title :string]
                               [:artist :string]
                               [:year :string]
                               [:track :integer]
                               [:disc_no :integer]
                               [:album :string]
                               [:artist_canonical :string]
                               [:album_canonical :string]
                               [:title_canonical :string]
                               [:last_modified :integer]
                               [:duration :integer]
                               [:genre :integer]]}
             :mounts {:name "mounts"
                      :columns [[:id :integer "PRIMARY KEY" "AUTOINCREMENT"]
                                [:path :string "UNIQUE"]]}
             :playlists {:name "playlists"
                         :columns [[:id :integer "PRIMARY KEY" "AUTOINCREMENT"]
                                   [:name :string "UNIQUE"]
                                   [:body :text]]}})

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
  "returns {id:??}for insert or {row-count:??} for update"
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

(defn managed? [f mounts]
  (some (partial utils/starts-with? (.getAbsolutePath f)) mounts))

(defn prune-tracks!
  "get rid of rows where the files no longer exist or are no longer managed"
  [db]
  (let [fkeys (keys (last-modified-index db))
        mounts (mount-points db)
        should-prune? (fn [f] (or (not (.exists f)) (not (managed? f mounts))))
        files (filter should-prune? (map io/file fkeys))]
    (map (partial delete-track! db) (map (fn [f] (.getAbsolutePath f)) files))))

;; queries for web endpoints
(defn artist-list [db]
  (sql/query db ["select distinct artist from tracks where artist is not null order by artist_canonical"]))

(defn artist-search [db prefix]
  (cond (= prefix "all") (artist-list db)
        (= prefix "#")
        (let [db-results (sql/query db [(format "select distinct artist_canonical as artist from tracks where artist_canonical < 'a' order by artist_canonical")]) ;; needs fix for canonical
              results (filter (fn [x] (not (= (:artist x) ""))) db-results)]
          (take-while (fn [x] (not (= (subs (:artist x) 0 1) "a"))) results))
    :else (sql/query db [(format "select distinct artist from tracks where artist_canonical like '%s%%' order by artist_canonical" prefix)])))

(defn album-list-by-artist [db artist]
  (sql/query db ["select distinct album, album_canonical, year from tracks where artist_canonical=? order by year" (utils/canonicalize artist)]))


(defn tracks-by-album [db album]
  (sql/query db ["select * from tracks where album_canonical=? order by disc_no, track, artist_canonical" (utils/canonicalize album)]))

(defn problem-tracks [db]
  (sql/query db ["select path from tracks where artist_canonical='' and album_canonical='' and title_canonical='' order by path"]))

(defn track-by-artist-by-album [db artist album id]
  (first (sql/query db ["select * from tracks where artist_canonical=? and album_canonical=? and id=?"
                 (utils/canonicalize artist)
                 (utils/canonicalize album)
                 id])))

(defn get-album-and-artist-by-path [db path]
  (let [f (io/file path)
        p (if (.isDirectory f) path (.getParent f))]
    (sql/query db [(format "select distinct artist_canonical, album_canonical from tracks where path like '%s%%' order by artist_canonical, album_canonical" p)])))

(defn get-albums-recently-added
  ([db days-ago]
   (sql/query db [(format "select group_concat(DISTINCT artist) as artist, album_canonical, album, year, last_modified from tracks where last_modified > %s group by album order by last_modified desc,id desc" (c/to-long (-> days-ago t/days t/ago)))]))
  ([db] (get-albums-recently-added db 365)))

(defn get-albums-by-year [db]
  (sql/query the-db ["select distinct album, artist, year from tracks order by year, artist"]))
