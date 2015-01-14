(ns cljspazzer.client.services
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [ajax.core :as ajax]
            [cljspazzer.client.utils :as utils]
            [cljs.core.async :refer [<! put! chan]]))


(defn error-handler [response]
  (.log js/console "something bad happened " (clj->js response)))


(defn artist-list-prefix 
  ([prefix]
   (let [out (chan)]
     (ajax/GET (utils/format "/api/artists/search/%s" prefix)
               {:error-handler error-handler
                :handler (fn [response]
                           (let [not-blank? (fn [x] (not (= x "")))
                                 artists (filter  not-blank? (map (fn [a] (a "artist")) (response "artists")))]
                             (put! out artists)))})
     out)))

(defn artist-detail [artist]
  (let [out (chan)]
    (ajax/GET (utils/format "/api/artists/%s" (utils/encode artist))
              {:error-handler error-handler
               :handler (fn [response] (put! out response))})
    out))

(defn album-detail [artist album]
  (let [out (chan)]
    (ajax/GET (utils/format "/api/artists/%s/albums/%s"
                            (utils/encode artist)
                            (utils/encode album))
              {:error-handler error-handler
               :handler (fn [response] (put! out response))})    
    out))


(defn mounts []
  (let [out (chan)]
    (ajax/GET "/api/mounts"
              {:error-handler error-handler
               :handler (fn [response] (put! out response))})
    out))

(defn add-mount [new-mount]
  (let [out (chan)]
    (ajax/POST "/api/mounts"
               {:params {:new-mount new-mount}
                :keywords? true
                :format :url
                :error-handler error-handler
                :handler (fn [response] (put! out response))})
    out))

(defn delete-mount [mount]
  (let [out (chan)]
    (ajax/DELETE "/api/mounts"
                 {:params {:mount mount}
                  :keywords? true
                  :format :url
                  :error-handler error-handler
                  :handler (fn [response] (put! out response))})
    out))
