(ns apollo.db.schema
  (:require [apollo.utils :as utils]
            [clj-time.coerce :as c]
            [clj-time.core :as t]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [honeysql.core :as sql]
            [honeysql.helpers :as h]))

(def the-db {:classname "org.sqlite.JDBC",
             :subprotocol "sqlite",
             :subname "apollo.db"})

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

(def track-fields (map first (get-in tables [:tracks :columns])))

(def album-fields [[:album_canonical :id]
                   [:album :name]
                   [:artist_canonical :artist_id]
                   :year])

(def artist-fields [[:artist_canonical :id]
                    [:artist :name]])

(def year-fields [:year])

(def q-track (-> (apply h/select track-fields)
                 (h/from :tracks)))

(def q-album (-> (apply h/select album-fields)
                 (h/from :tracks)
                 (h/modifiers :distinct)
                 (h/where [:not= nil :name])
                 (h/order-by :id)))

(def q-artist (-> (apply h/select artist-fields)
                  (h/from :tracks)
                  (h/modifiers :distinct)
                  (h/where [:not= nil :name])
                  (h/order-by :id)))

(def q-year (-> (apply h/select year-fields)
                (h/from :tracks)
                (h/modifiers :distinct)
                (h/where [:not= nil :year])
                (h/order-by :year)))


(defn create-tbl! [db, tbl]
    (let [args (cons (:name tbl) (:columns tbl))]
    (jdbc/db-do-commands db
                      (apply jdbc/create-table-ddl args))))

(defn drop-tbl! [db tbl]
  (jdbc/db-do-commands db (jdbc/drop-table-ddl (:name tbl))))

(defn create-all-tbls! [db]
  (map (partial create-tbl! db) (vals tables)))

(defn drop-all-tbls! [db]
  (map (partial drop-tbl! db) (vals tables)))

(defn column-names [t]
  (map first (:columns t)))

(defn track-exists? [cn t]
  (> (:count (first
              (jdbc/query cn (-> q-track
                                 (h/select [:%count.* :count])
                                 (h/where [:= :path t])
                                 (sql/format)))))
     0))

(defn update-track!
  "returns count of rows affected"
  [cn track-info]
  (first (jdbc/execute! cn (-> (h/update :tracks)
                               (h/sset track-info)
                               (h/where [:= :path (:path track-info)])
                               (sql/format))))) ;; does this return rows effected

(defn insert-track!
  "returns last row id which should correspond to id"
  [cn raw-track-info scan-date]
  (let [track-info (assoc raw-track-info :scan_date scan-date)
        result (jdbc/insert! cn :tracks track-info)]
    ((first result) (keyword "last_insert_rowid()")))) ;; does this last id?

(defn delete-track!
  "returns rows effected which should be > 1 if successful"
  [cn path]
  (jdbc/execute! cn (->
                  (h/delete-from :tracks)
                  (h/where [:= :path path])
                  (sql/format))))

(defn upsert-track!
  "returns {id:??}for insert or {row-count:??} for update"
  [cn scan-date track-info]
  (let [update-result (update-track! cn track-info)]
    (if (> update-result 0)
      {:row-count update-result :action :update}
      {:id (insert-track! cn track-info scan-date) :action :insert})))

(defn insert-mount! [cn p]
  (jdbc/execute! cn (-> (h/insert-into :mounts)
                     (h/values {:path p})
                     (sql/format))))

(defn delete-mount!
  "returns rows effected which should be > 1 if successful"
  [cn path]
  (jdbc/execute! cn (-> (h/delete-from :mounts)
                     (h/where [:= :path path])
                     (sql/format))))

(defn mount-points
  "returned with trailing / to denote directory, io/file does the
  normalization I think"
  [cn]
  (map (fn [x] (str (.getAbsolutePath (io/file (:path x))) "/"))
       (jdbc/query cn (-> (h/select :*)
                          (h/from :mounts)
                          (sql/format)))))

(defn last-modified-index
  "{path:last_modified}"
  [cn]
  (let [rows (jdbc/query cn (-> (h/select :last_modified :path)
                                (h/from :tracks)
                                (sql/format)))
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
  [cn]
  (let [fkeys (keys (last-modified-index cn))
        mounts (mount-points cn)
        should-prune? (fn [f]
                        (do
                          (or (not (.exists f)) (not (managed? f mounts)))))
        files (filter should-prune? (map io/file fkeys))]
    (map (partial delete-track! cn) (map (fn [f] (.getAbsolutePath f)) files))))

;; queries for web endpoints

(defn artist-search [cn prefix]
  (cond (= prefix "all") (jdbc/query cn (-> q-artist (sql/format)))
        (= prefix "#")
        (let [db-results (jdbc/query cn (-> q-artist
                                            (h/where [:< :id "a"])
                                            (sql/format))) ;; needs fix for canonical
              results (filter (fn [x] (not (= (:id x) ""))) db-results)]
          (take-while (fn [x] (not (= (subs (str (:id x)) 0 1) "a"))) results))
        :else (jdbc/query cn (-> q-artist
                                 (h/where [:like :artist_canonical (format "%s%%" prefix)])
                                 (sql/format)))))

(defn album-list-by-artist [cn artist]
  (jdbc/query cn (-> q-album
                     (h/where [:= :artist_id (utils/canonicalize artist)])
                     (h/order-by :year)
                     (sql/format))))


(defn tracks-by-album [cn album]
  (jdbc/query cn (-> q-track
                     (h/where [:= :album_canonical (utils/canonicalize album)])
                     (h/order-by :disc_no :track :artist_canonical)
                     (sql/format))))

(defn problem-tracks [cn]
  (jdbc/query cn (-> q-track
                     (h/where [:= :artist_canonical ""]
                              [:= :album_canonical ""]
                              [:= :title_canonical ""])
                     (h/order-by :path)
                     (sql/format))))

(defn track-by-artist-by-album [cn artist album id]
  (first (jdbc/query cn (-> q-track
                     (h/where [:= :artist_canonical (utils/canonicalize artist)]
                              [:= :album_canonical (utils/canonicalize album)]
                              [:= :id id])
                     (sql/format)))))

(defn get-album-and-artist-by-path [cn path]
  (let [f (io/file path)
        p (if (.isDirectory f) path (.getParent f))]
    (jdbc/query cn (-> q-track
                       (h/select :artist_canonical :album_canonical)
                       (h/modifiers :distinct)
                       (h/where [:like :path (format "%s%%" p)])
                       (h/order-by :artist_canonical :album_canonical)
                       (sql/format)))))

(defn get-albums-recently-added
  ([cn days-ago]
   (jdbc/query cn (-> q-track
                      (h/select [(sql/call :group_concat (keyword "DISTINCT artist_canonical")) :artist_id]
                                [(sql/call :count (keyword "DISTINCT artist_canonical")):artist_count]
                                [:album_canonical :id]
                                [:album :name]
                                :artist
                                :year
                                :scan_date
                                :last_modified)
                      (h/where [:> :scan_date (c/to-long (-> days-ago t/days t/ago))])
                      (h/group :album_canonical :year)
                      (h/order-by [:last_modified :desc]
                                  [:scan_date :desc]
                                  [:id :desc])
                      (sql/format))))
  ([cn] (get-albums-recently-added cn 365)))

(defn get-albums-by-year [cn]
  (jdbc/query cn (-> q-track
                     (h/select [(sql/call :group_concat (keyword "DISTINCT artist_canonical")) :artist_id]
                               [(sql/call :count (keyword "DISTINCT artist_canonical")):artist_count]
                               [:album_canonical :id]
                               [:album :name]
                               :artist
                               :year
                               :scan_date
                               :last_modified)
                     (h/group :album_canonical)
                     (h/order-by [:year :desc] :artist)
                     (sql/format))))
