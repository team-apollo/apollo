(ns apollo.musicbrainz
  (:require [clj-http.client :as client]))
;; http://musicbrainz.org/ws/2/release/?query=artist:nirvana&limit=10
(def musicbrainz-base-qry-url "http://musicbrainz.org/ws/2/%s/")

(defn entity-qry [entity qry-phrase]
  (client/get (format musicbrainz-base-qry-url entity) {:query-params {:query qry-phrase
                                                                       :limit 10
                                                                       :fmt "json"}
                                                        :as :json}))
(defn entity-by-id [entity id inc]
  (client/get (format "%s%s" (format musicbrainz-base-qry-url entity) id)
              {:query-params {:fmt "json"
                              :inc inc}
               :as :json}))

(defn get-artist [artist-name]
  (let [res (entity-qry "artist" artist-name)
        artist-id (:id (first (:artists (:body res))))]
    (entity-by-id "artist" artist-id "release-groups annotation tags url-rels artist-rels")))
