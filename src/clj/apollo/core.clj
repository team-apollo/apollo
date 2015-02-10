(ns apollo.core
  (:require [ring.middleware.resource :as m]
            [compojure.core :refer :all]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.json :as j]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.middleware.partial-content :refer [wrap-partial-content]]
            [apollo.http.admin :refer :all]
            [apollo.http.artist :refer :all]
            [apollo.http.album :refer :all]
            [apollo.http.track :refer :all]))

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
  (GET "/api/mounts" []
       (mounts))
  (POST "/api/mounts" [new-mount]
        (add-new-mount new-mount))
  (DELETE "/api/mounts" [mount]
          (delete-mount mount))
  (POST "/api/do-scan" []
        (do-scan))
  (GET "/" []
       {:status 302
        :headers {"Location" "/index.html"}}))

(def app
  (-> app-handler
      (wrap-params)
      (j/wrap-json-response)
      (m/wrap-resource "public")
      (wrap-not-modified)
      (wrap-content-type)
      (wrap-gzip)
      (wrap-partial-content)))

