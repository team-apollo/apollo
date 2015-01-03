(ns cljspazzer.client.services
  (:require [ajax.core :as ajax]
            [cljspazzer.client.utils :as utils]))


(defn error-handler [response]
  (.log js/console "something bad happened " (clj->js response)))

(defn artist-list [handler]
  (ajax/GET "/api/artists"
            {:error-handler error-handler
             :handler (fn [response]
                        (let [not-blank? (fn [x] (not (= x "")))
                              artists (filter  not-blank? (map (fn [a] (a "artist")) (response "artists")))]
                          (handler artists)))}))

(defn artist-detail [artist handler]
  (ajax/GET (utils/format "/api/artists/%s" (utils/encode artist))
            {:error-handler error-handler
             :handler handler}))

(defn album-detail [artist album handler]
  (ajax/GET (utils/format "/api/artists/%s/albums/%s" (utils/encode artist) (utils/encode album))
            {:error-handler error-handler
             :handler handler}))
