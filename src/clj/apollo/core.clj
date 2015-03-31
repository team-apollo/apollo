(ns apollo.core
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]]
            [ring.middleware.resource :as m]
            [compojure.core :refer :all]
            [ring.util.response :as r]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.json :as j]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.middleware.partial-content :refer [wrap-partial-content]]
            [ring.middleware.reload :as reload]
            [net.cgrand.enlive-html :refer [deftemplate]]
            [net.cgrand.enlive-html :refer [set-attr prepend append html content]]
            [apollo.http.admin :refer :all]
            [apollo.http.artist :refer :all]
            [apollo.http.album :refer :all]
            [apollo.http.track :refer :all]
            [apollo.db.schema :as schema]
            [apollo.scanner :as scanner]))

(def is-dev? (env :is-dev))

(def inject-devmode-html
  (comp
     (set-attr :class "is-dev")
     (prepend (html [:script {:type "text/javascript"
                              :src "/javascripts/apollo/goog/base.js"}]))

     (append  (html [:script {:type "text/javascript"}
                     "goog.require('apollo.client.core')"]))
     (append  (html [:script {:type "text/javascript"}
                     "goog.require('apollo.client.dev')"]))))

(deftemplate page
  (io/resource "index.html") [] [:body] (if is-dev? inject-devmode-html identity))

(defn render [t]
  (apply str t))

(def render-to-response
  (comp r/response render))

(defroutes app-handler
  (GET "/api/artists/:artist-id/albums/:album-id/tracks/:track-id"
       [artist-id album-id track-id]
       (track-detail artist-id album-id track-id))
  (GET "/api/artists/:artist-id/albums/:album-id"
       [artist-id album-id]
       (album-detail artist-id album-id))
  (GET "/api/artists/:artist-id/albums/:album-id/zip"
       [artist-id album-id]
       (album-zip artist-id album-id))
  (GET "/api/artists/:artist-id/albums/:album-id/image"
       [artist-id album-id]
       (album-image artist-id album-id))
  (GET "/api/artists/search/:prefix"
       [prefix]
       (artist-search prefix))
  (GET "/api/artists/:artist-id"
       [artist-id]
       (artists-detail artist-id))
  (GET "/api/artists/:artist-id/image" [artist-id force-fetch]
       (artist-image artist-id force-fetch))
  (GET "/api/artists/:artist-id/info" [artist-id]
       (artist-info artist-id))
  (GET "/api/recently-added" []
       (recently-added))
  (GET "/api/by-year" []
       (albums-by-year))
  (GET "/api/mounts" []
       (mounts))
  (POST "/api/mounts" [new-mount]
        (add-new-mount new-mount))
  (DELETE "/api/mounts" [mount]
          (delete-mount mount))
  (POST "/api/do-scan" []
        (do-scan))
  (GET "/index.html" [] (render-to-response (page)))
  (GET "/" []
       {:status 302
        :headers {"Location" "/index.html"}}))

(def http-handler
  (if is-dev?
    (reload/wrap-reload app-handler)
    app-handler))

(def app
  (-> http-handler
      (wrap-params)
      (j/wrap-json-response)
      (m/wrap-resource "public")
      (wrap-not-modified)
      (wrap-content-type)
      (wrap-gzip)
      (wrap-partial-content)))

(let [db-file (io/file (:subname schema/the-db))]
 (if (not (.exists db-file))
   (do
     (log/info (format "%s does not exist so creating." (.getAbsolutePath db-file)))
     (log/info (schema/create-all-tbls! schema/the-db))
     (log/info (format "%s created, you will need to add some mounts from the admin page." (.getAbsolutePath db-file))))
   (log/info (format "database %s exists... we're all good." (.getAbsolutePath db-file)))))

(def scan-job (future (while true
                        (Thread/sleep 60000)
                        (try (scanner/process-mounts!)
                             (catch Exception e (log/error e))))))
