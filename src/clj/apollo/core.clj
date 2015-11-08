(ns apollo.core
  (:require [apollo.db.schema :as schema]
            [apollo.http.admin :refer :all]
            [apollo.http.album :refer :all]
            [apollo.http.artist :refer :all]
            [apollo.http.track :refer :all]
            [apollo.scanner :as scanner]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [compojure.core :refer :all]
            [environ.core :refer [env]]
            [jdbc.pool.c3p0 :as pool]
            [net.cgrand.enlive-html :refer [deftemplate]]
            [net.cgrand.enlive-html :refer [set-attr prepend append html]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.json :as j]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.partial-content :refer [wrap-partial-content]]
            [ring.middleware.reload :as reload]
            [ring.middleware.resource :as m]
            [ring.util.response :as r]))

(def is-dev? (env :is-dev))

(def spec (pool/make-datasource-spec schema/the-db))

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
  (context "/api" []
           (context "/artists" []
                    (GET "/:artist-id/albums/:album-id/tracks/:track-id"
                         [artist-id album-id track-id]
                         track-detail)
                    (GET "/:artist-id/albums/:album-id"
                         [artist-id album-id]
                         album-detail)
                    (GET "/:artist-id/albums/:album-id/zip"
                         [artist-id album-id]
                         album-zip)
                    (GET "/:artist-id/albums/:album-id/image"
                         [artist-id album-id]
                         album-image)
                    (GET "/search/:prefix"
                         [prefix]
                         artist-search)
                    (GET "/:artist-id"
                         [artist-id]
                         artists-detail)
                    (GET "/:artist-id/image" [artist-id force-fetch]
                         (artist-image artist-id force-fetch))
                    (GET "/:artist-id/info" [artist-id]
                         (artist-info artist-id)))
           (GET "/recently-added" []
                recently-added)
           (GET "/by-year" []
                albums-by-year)
           (GET "/mounts" []
                mounts)
           (POST "/mounts" [new-mount]
                 add-new-mount new-mount)
           (DELETE "/mounts" [mount]
                   delete-mount mount)
           (POST "/do-scan" []
                 do-scan))
  (GET "/index.html" [] (render-to-response (page)))
  (GET "/" []
       {:status 302
        :headers {"Location" "/index.html"}}))

(def http-handler
  (if is-dev?
    (reload/wrap-reload app-handler)
    app-handler))

(defn wrap-db-transaction [h]
  (fn [request]
    (jdbc/with-db-transaction [cn spec]
      (h (assoc request :db-connection cn)))))

(def app
  (-> http-handler
      wrap-db-transaction
      (wrap-params)
      (j/wrap-json-response)
      (m/wrap-resource "public")
      (wrap-not-modified)
      (wrap-content-type)
      (wrap-gzip)
      (wrap-partial-content)))

(defn check-db []
  (let [db-file (io/file (:subname schema/the-db))]
    (if (not (.exists db-file))
      (do
        (log/info (format "%s does not exist so creating." (.getAbsolutePath db-file)))
        (log/info (schema/create-all-tbls! spec))
        (log/info (format "%s created, you will need to add some mounts from the admin page." (.getAbsolutePath db-file))))
      (log/info (format "database %s exists... we're all good." (.getAbsolutePath db-file))))))

(defn initialize []
  (do
    (check-db)
    (let [scan-job (future (while true
                             (Thread/sleep 60000)
                             (log/info "scanning now")
                             (try
                               (jdbc/with-db-connection [cn spec]
                                 (scanner/process-mounts! spec))
                               (catch Exception e (log/error e)))))]
      true)))

(when is-dev? (initialize)) ;; when running through figwheel
