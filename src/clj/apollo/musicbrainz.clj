(ns apollo.musicbrainz
  (:require [clj-http.client :as client]))
;; http://musicbrainz.org/ws/2/release/?query=artist:nirvana&limit=10
(def musicbrainz-base-qry-url "http://musicbrainz.org/ws/2/%s/")

(defn entity-qry [entity qry-phrase params]
  (let [query-params (merge params {:query qry-phrase
                                    :limit 10
                                    :fmt "json"})]
    (client/get (format musicbrainz-base-qry-url entity) {:query-params query-params
                                                        :as :json})))
(defn entity-by-id [entity id num-inc]
  (client/get (format "%s%s" (format musicbrainz-base-qry-url entity) id)
              {:query-params {:fmt "json"
                              :inc num-inc}
               :as :json}))

(defn get-artist [artist-name]
  (let [res (entity-qry "artist" artist-name {})
        artist-id (:id (first (:artists (:body res))))]
    (entity-by-id "artist" artist-id "releases annotation tags url-rels artist-rels")))

(defn get-release [artist-name release]
  (let [response (entity-qry "release" release {:artist artist-name})
        {{releases :releases} :body} response
        release-index (group-by (fn[x] (clojure.string/lower-case (:title x))) releases)]
    (release-index (clojure.string/lower-case release))))

(defn get-release-artwork [artist-name release]
  (let [releases (map :id (get-release artist-name release))
        releases-details (map (fn [x] (Thread/sleep 5000) ;; throttle
                                (:body (entity-qry (str "release/" x) "" {})))
                              releases)
        release-with-artwork (first (filter (fn [x] (:front (:cover-art-archive x)))
                                            releases-details))
        release-id (:id release-with-artwork)]
    (if release-id
      (str "http://coverartarchive.org/release/" release-id  "/front"))))
