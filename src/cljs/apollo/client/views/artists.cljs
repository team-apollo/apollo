(ns apollo.client.views.artists
  (:require [apollo.client.utils :as utils]
            [apollo.client.state :as state]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(defn mk-artist-image
  ([artist]
   (utils/format "/api/artists/%s/image" (utils/encode artist)))
  ([artist force]
   (utils/format "%s?force-fetch=1" (mk-artist-image artist))))

(defn mk-artist-url [artist]
  (utils/format "#/artists/%s" (utils/encode artist)))

(defn artist-item [artist]
  (let [artist-url (mk-artist-url artist)]
    (om/component
     (html
      [:li
       [:a {:href artist-url}
        [:div artist]]]))))

(defn artist-list-partial [artists owner]
  (reify
   om/IRender
   (render [_]
     (let [artist-heading (utils/format "%s Artists" (count artists))
           post-filter (:value (om/observe owner (state/ref-post-filter)))
           filter-fn (fn [a]
                       (if (or (nil? post-filter) (empty? post-filter))
                         true
                         (utils/str-contains? (str a) post-filter)))]
       (html
      [:div.artist-list
       [:h3 artist-heading]
       [:ul (om/build-all artist-item (filter filter-fn artists))]])))))

(defn artist-detail-partial [artist]
  (let [artist-image (mk-artist-image artist true)]
    (when (not (empty? artist))
      [:div.artist-detail
       [:h2 artist]])))

(defn artist-info-partial [info]
  (om/component
   (html
    [:div
   ;; (let [relations (group-by (fn [i] (i "type")) (info "relations"))
   ;;       members (relations "member of band")]
   ;;   [:ul
   ;;    (let [wiki-link (get-in (first (relations "wikipedia"))["url" "resource"])]
   ;;       [:li
   ;;     [:a {:href wiki-link :target "_blank"} "wikipedia"]])])
   ]))
  )
