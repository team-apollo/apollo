(ns cljspazzer.client.views.artists
  (:require [cljspazzer.client.utils :as utils]))

(defn mk-artist-image
  ([artist]
   (utils/format "/api/artists/%s/image" (utils/encode artist)))
  ([artist force]
   (utils/format "%s?force-fetch=1" (mk-artist-image artist))))

(defn mk-artist-url [artist]
  (utils/format "#/artists/%s" (utils/encode artist)))

(defn artist-item [x]
  (let [artist-url (mk-artist-url x)]
    [:li
     [:a {:href artist-url}
      [:div x]]]))

(defn artist-list-partial [artists]
  (let [artist-heading (utils/format "%s Artists" (count artists))]
    [:div.artist-list
     [:h3 artist-heading]
     [:ul (map artist-item artists)]]))

(defn artist-detail-partial [active-artist]
  (let [artist-image (mk-artist-image active-artist true)]
    [:div
     [:h3 active-artist]
     [:img.band {:src artist-image}]]))