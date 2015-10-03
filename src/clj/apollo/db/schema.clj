(ns apollo.db.schema
  (:require [apollo.utils :as utils]
            [clj-time.coerce :as c]
            [clj-time.core :as t]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as sql]
            [clojure.tools.logging :as log]
            [korma.core :as k]))

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
                               [:genre :integer]
                               [:scan_date :integer]]}
             :mounts {:name "mounts"
                      :columns [[:id :integer "PRIMARY KEY" "AUTOINCREMENT"]
                                [:path :string "UNIQUE"]]}
             :playlists {:name "playlists"
                         :columns [[:id :integer "PRIMARY KEY" "AUTOINCREMENT"]
                                   [:name :string "UNIQUE"]
                                   [:body :text]]}})

(k/defentity track (k/table :tracks))

(defn get-count [e f]
  (k/aggregate e (count f) :count))

(def album (k/subselect track
                        (k/fields [:album_canonical :id]
                                  [:album :name]
                                  [:artist_canonical :artist_id]
                                  :year)
                        (k/modifier "DISTINCT")
                        (k/where {:name [not= nil]})
                        (k/order :id)))

(def artist (k/subselect track
                         (k/fields [:artist_canonical :id]
                                   [:artist :name])
                         (k/modifier "DISTINCT")
                         (k/where {:name [not= nil]})
                         (k/order :id)))

(def year (k/subselect track
                       (k/fields :year)
                       (k/modifier "DISTINCT")
                       (k/where {:year [not= nil]})
                       (k/order :year)))


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
  (> (:count (first (-> (k/select* track) (k/where {:path t}) (get-count :id) (k/select)))) 0))

(defn update-track!
  "returns count of rows affected"
  [db track-info]
  (first (sql/update! db :tracks track-info ["path=?" (:path track-info)])))

(defn insert-track!
  "returns last row id which should correspond to id"
  [db raw-track-info scan-date]
  (let [track-info (assoc raw-track-info :scan_date scan-date)]
    ((keyword "last_insert_rowid()") (first (sql/insert! db :tracks track-info)))))

(defn delete-track!
  "returns rows effected which should be > 1 if successful"
  [db path]
  (first (sql/delete! db :tracks ["path=?" path])))

(defn upsert-track!
  "returns {id:??}for insert or {row-count:??} for update"
  [db scan-date track-info]
  (let [update-result (update-track! db track-info)]
    (if (> update-result 0)
      {:row-count update-result :action :update}
      {:id (insert-track! db track-info scan-date) :action :insert})))

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
  (let [normalized-mounts (map (fn [x] (.getAbsolutePath (.getAbsoluteFile (io/file x)))) mounts)]
       (some (partial utils/starts-with? (.getAbsolutePath f)) normalized-mounts)))

(defn prune-tracks!
  "get rid of rows where the files no longer exist or are no longer managed"
  [db]
  (let [fkeys (keys (last-modified-index db))
        mounts (mount-points db)
        should-prune? (fn [f]
                        (do
                          (or (not (.exists f)) (not (managed? f mounts)))))
        files (filter should-prune? (map io/file fkeys))]
    (map (partial delete-track! db) (map (fn [f] (.getAbsolutePath f)) files))))

;; queries for web endpoints
(defn artist-list []
  (-> (k/select* artist)))


(defn artist-search [prefix]
  (cond (= prefix "all") (-> (artist-list) (k/select))
        (= prefix "#")
        (let [db-results (-> (artist-list) (k/where {:id [< "a"]})(k/select)) ;; needs fix for canonical
              results (filter (fn [x] (not (= (:id x) ""))) db-results)]
          (take-while (fn [x] (not (= (subs (str (:id x)) 0 1) "a"))) results))
        :else (-> (artist-list) (k/where {:name [like (format "%s%%" prefix)]}) (k/select))))

(defn album-list-by-artist [artist]
  (-> (k/select* album) (k/where {:artist_id (utils/canonicalize artist)}) (k/order :year) (k/select)))


(defn tracks-by-album [album]
  (-> (k/select* track)
      (k/where {:album_canonical (utils/canonicalize album)})
      (k/order :disc_no)
      (k/order :track)
      (k/order :artist_canonical)
      (k/select)))

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
   (sql/query db [(format "select group_concat(DISTINCT artist) as artist, album_canonical, album, year, scan_date, last_modified from tracks where scan_date > %s group by album order by scan_date desc,last_modified desc,id desc" (c/to-long (-> days-ago t/days t/ago)))]))
  ([db] (get-albums-recently-added db 365)))

(defn get-albums-by-year [db]
  (sql/query the-db ["select group_concat(DISTINCT artist) as artist, album_canonical, album, year, scan_date, last_modified from tracks group by album order by year desc, artist"]))
