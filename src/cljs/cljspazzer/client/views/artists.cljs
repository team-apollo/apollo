(ns cljspazzer.client.views.artists
  (:require [cljspazzer.client.utils :as utils]))

(defn mk-artist-image
  ([artist]
   (utils/format "/api/artists/%s/image" (utils/encode artist)))
  ([artist force]
   (utils/format "%s?force-fetch=1" (mk-artist-image artist))))

(defn mk-artist-url [artist]
  (utils/format "#/artists/%s" (utils/encode artist)))

(defn artist-item [artist]
  (let [artist-url (mk-artist-url artist)]
    [:li
     [:a {:href artist-url}
      [:div artist]]]))

(defn artist-list-partial [artists]
  (let [artist-heading (utils/format "%s Artists" (count artists))]
    [:div.artist-list
     [:h3 artist-heading]
     [:ul (map artist-item artists)]]))

(defn artist-detail-partial [artist]
  (let [artist-image (mk-artist-image artist true)]
    (when (not (empty? artist))
     [:div
      [:h2 artist]])))
