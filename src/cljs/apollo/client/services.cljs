(ns apollo.client.services
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [ajax.core :as ajax]
            [apollo.client.utils :as utils]
            [cljs.core.async :refer [<! put! chan]]
            [secretary.core :as secretary]))


(defn error-handler [response]
  (.log js/console "something bad happened " (clj->js response))
  (cond
    (= 404 (:status response))
    (secretary/dispatch! "#/not-found")
    (= 500 (:status response))
    (secretary/dispatch! "#/error")))


(defn artist-list-prefix
  ([prefix]
   (let [out (chan)]
     (ajax/GET (utils/format "/api/artists/search/%s" (utils/encode prefix))
               {:response-format (ajax/json-response-format {:keywords? true})
                :keywords? true
                :error-handler error-handler
                :handler (fn [response]
                           (let [not-blank? (fn [x] (not (= x "")))
                                 artists (filter  not-blank? (map (fn [a] (a :name)) (response :artists)))]
                             (put! out artists)))})
     out)))

(defn artist-detail [artist]
  (let [out (chan)]
    (ajax/GET (utils/format "/api/artists/%s" (utils/encode artist))
              {:response-format (ajax/json-response-format {:keywords? true})
               :keywords? true
               :error-handler error-handler
               :handler (fn [response] (put! out response))})
    out))

(defn artist-info [artist]
  (let [out (chan)]
    (ajax/GET (utils/format "/api/artists/%s/info" (utils/encode artist))
              {:response-format (ajax/json-response-format {:keywords? true})
               :keywords? true
               :error-handler error-handler
               :handler (fn [response] (put! out response))})
    out))


(defn album-detail [artist album]
  (let [out (chan)]
    (ajax/GET (utils/format "/api/artists/%s/albums/%s"
                            (utils/encode artist)
                            (utils/encode album))
              {:response-format (ajax/json-response-format {:keywords? true})
               :keywords? true
               :error-handler error-handler
               :handler (fn [response] (put! out response))})
    out))


(defn mounts []
  (let [out (chan)]
    (ajax/GET "/api/mounts"
              {:response-format (ajax/json-response-format {:keywords? true})
               :keywords? true
               :error-handler error-handler
               :handler (fn [response] (put! out response))})
    out))

(defn add-mount [new-mount]
  (let [out (chan)]
    (ajax/POST "/api/mounts"
               {:params {:new-mount new-mount}
               :response-format (ajax/json-response-format {:keywords? true})
                :keywords? true
                :format :url
                :error-handler error-handler
                :handler (fn [response] (put! out response))})
    out))

(defn delete-mount [mount]
  (let [out (chan)]
    (ajax/DELETE "/api/mounts"
                 {:params {:mount mount}
               :response-format (ajax/json-response-format {:keywords? true})
                  :keywords? true
                  :format :url
                  :error-handler error-handler
                  :handler (fn [response] (put! out response))})
    out))

(defn do-scan []
  (let [out (chan)]
    (ajax/POST "/api/do-scan"
               {:error-handler error-handler
                :handler (fn [response] (put! out response))})
    chan))

(defn recently-added []
  (let [out (chan)]
    (ajax/GET "/api/recently-added"
              {:error-handler error-handler
               :response-format (ajax/json-response-format {:keywords? true})
               :keywords? true
               :handler (fn [response] (put! out response))})
    out))

(defn by-year []
  (let [out (chan)]
    (ajax/GET "/api/by-year"
              {:error-handler error-handler
               :response-format (ajax/json-response-format {:keywords? true})
               :keywords? true
               :handler (fn [response] (put! out response))})
    out))
