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

(defn track-exists? [t]
  (> (:count (first
              (-> (k/select* track)
                  (k/where {:path t})
                  (get-count :id)
                  (k/select))))
     0))

(defn update-track!
  "returns count of rows affected"
  [track-info]
  (k/update track
            (k/set-fields track-info)
            (k/where {:path (:path track-info)})))

(defn insert-track!
  "returns last row id which should correspond to id"
  [raw-track-info scan-date]
  (let [track-info (assoc raw-track-info :scan_date scan-date)]
    (k/insert track (k/values track-info))))

(defn delete-track!
  "returns rows effected which should be > 1 if successful"
  [path]
  (k/delete track (k/where {:path path})))

(defn upsert-track!
  "returns {id:??}for insert or {row-count:??} for update"
  [scan-date track-info]
  (let [update-result (update-track! track-info)]
    (if (> update-result 0)
      {:row-count update-result :action :update}
      {:id (insert-track! track-info scan-date) :action :insert})))

(defn insert-mount! [p]
  (k/insert :mounts (k/values {:path p})))

(defn delete-mount!
  "returns rows effected which should be > 1 if successful"
  [path]
  (k/delete :mounts (k/where {:path path})))

(defn mount-points
  "returned with trailing / to denote directory, io/file does the
  normalization I think"
  []
  (map (fn [x] (str (.getAbsolutePath (io/file (:path x))) "/"))
       (k/select :mounts (k/fields :path))))

(defn last-modified-index
  "{path:last_modified}"
  []
  (let [rows (k/select track (k/fields :last_modified :path))
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
  []
  (let [fkeys (keys (last-modified-index))
        mounts (mount-points)
        should-prune? (fn [f]
                        (do
                          (or (not (.exists f)) (not (managed? f mounts)))))
        files (filter should-prune? (map io/file fkeys))]
    (map delete-track! (map (fn [f] (.getAbsolutePath f)) files))))

;; queries for web endpoints
(defn artist-list []
  (-> (k/select* artist)))


(defn artist-search [prefix]
  (cond (= prefix "all") (-> (artist-list) (k/select))
        (= prefix "#")
        (let [db-results (-> (artist-list) (k/where {:id [< "a"]})(k/select)) ;; needs fix for canonical
              results (filter (fn [x] (not (= (:id x) ""))) db-results)]
          (take-while (fn [x] (not (= (subs (str (:id x)) 0 1) "a"))) results))
        :else (-> (artist-list) (k/where {:id [like (format "%s%%" prefix)]}) (k/select))))

(defn album-list-by-artist [artist]
  (-> (k/select* album) (k/where {:artist_id (utils/canonicalize artist)}) (k/order :year) (k/select)))


(defn tracks-by-album [album]
  (-> (k/select* track)
      (k/where {:album_canonical (utils/canonicalize album)})
      (k/order :disc_no)
      (k/order :track)
      (k/order :artist_canonical)
      (k/select)))

(defn problem-tracks []
  (-> (k/select* track)
      (k/where {:artist_canonical ""
                :album_canonical ""
                :title_canonical ""})
      (k/order :path)
      (k/select)))

(defn track-by-artist-by-album [artist album id]
  (first (-> (k/select* track)
             (k/where {:artist_canonical (utils/canonicalize artist)
                       :album_canonical (utils/canonicalize album)
                       :id id})
             (k/select))))

(defn get-album-and-artist-by-path [path]
  (let [f (io/file path)
        p (if (.isDirectory f) path (.getParent f))]
    (-> (k/select* track)
        (k/fields :artist_canonical :album_canonical)
        (k/modifier "DISTINCT")
        (k/order :artist_canonical)
        (k/order :album_canonical)
        (k/where {:path [like (format "%s%%" p)]}))))

(defn get-albums-recently-added
  ([days-ago]
   (-> (k/select* track)
       (k/fields ["group_concat(DISTINCT artist_canonical)" :artist_id]
                 ["count(DISTINCT artist_canonical)" :artist_count]
                 [:album_canonical :id]
                 [:album :name]
                 :artist :year :scan_date :last_modified)
       (k/where {:scan_date [> (c/to-long (-> days-ago t/days t/ago))]})
       (k/group :album_canonical :year)
       (k/order :last_modified :desc)
       (k/order :scan_date :desc)
       (k/order :id :desc)
       (k/select)))
  ([] (get-albums-recently-added 365)))

(defn get-albums-by-year []
  (-> (k/select* track)
      (k/fields ["group_concat(DISTINCT artist_canonical)" :artist_id]
                ["count(DISTINCT artist_canonical)" :artist_count]
                [:album_canonical :id]
                [:album :name]
                :artist :year :scan_date :last_modified)
      (k/group :album_canonical)
      (k/order :year :desc)
      (k/order :artist)
      (k/select)))
