(ns apollo.client.pages
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [apollo.client.utils :as utils]
            [apollo.client.services :as services]
            [apollo.client.channels :as channels]
            [apollo.client.views.nav :as nav]
            [apollo.client.views.artists :as artists]
            [apollo.client.views.albums :as albums]
            [apollo.client.player :as player]
            [apollo.client.state :as state]
            [apollo.client.components :as ac]
            [apollo.client.events :as events]
            [cljs.core.async :refer [<! put! chan sub dropping-buffer]]
            [cljs-time.extend]
            [cljs-time.coerce :as c]
            [cljs-time.core :as t]))

(defn left-column [data owner]
  (reify
    om/IRender
    (render [this]
      (let [active-artist (:active-artist data)
            sub-view (first (om/observe owner (state/ref-subview)))
            set-subview (fn [k] (om/transact! (state/ref-subview) (fn [p] [k])))]
        (html
         [:div.left-column
          (artists/artist-detail-partial active-artist)
          [:ul
           [:li {:on-click (fn [e]
                             (set-subview :now-playing))} "now playing"]
           [:li {:on-click (fn [e]
                             (set-subview :playlists))} "playlists"]]
          (if (= sub-view :now-playing)
            (om/build player/view-now-playing data)
            (om/build player/view-playlists data))])))))

(defn view-browse [data owner]
  (reify
    om/IRender
    (render [this]
      (let [active-artist (:active-artist data)
            active-nav (:active-nav data)
            artists (:artists data)
            artist-count (count artists)
            albums (:albums data)
            album-count (count albums)
            active-album (:active-album data)
            info (:artist-info data)]
        (html
         [:div.browse
          (om/build nav/main-nav data)
          (om/build left-column data)
          [:div.middle-column.pure-g
           [:div.pure-u-1
            [:div.content
             (om/build nav/nav-partial (or (first active-artist) active-nav))
             (cond
               (and (nil? active-artist) (nil? active-album))
               (om/build artists/artist-list-partial artists)
               (and (not (nil? active-artist)) (nil? active-album))
               [:span
                (om/build albums/album-list-partial {:artist active-artist :albums albums})
                (om/build artists/artist-info-partial info)]
               (not (nil? active-album))
               (om/build albums/album-detail {:artist active-artist :album active-album}))]]]])))))


(def ageing-range [{:date (-> 1 t/days t/ago t/at-midnight) :label "Today"}
                   {:date (-> 1 t/weeks t/ago t/at-midnight) :label "1 - 7 Days"}
                   {:date (-> 1 t/months t/ago t/at-midnight) :label "8 - 30 Days"}
                   {:date (-> 3 t/months t/ago t/at-midnight) :label "1 - 3 Months"}
                   {:date (-> 6 t/months t/ago t/at-midnight) :label "4 - 6 Months"}
                   {:date (-> 1 t/years t/ago t/at-midnight) :label "7 months -  Year"}
                   {:date (-> 3 t/years t/ago t/at-midnight) :label "1 - 3 Years"}
                   {:date (-> 5 t/years t/ago t/at-midnight) :label "4 - 5 Years"}
                   {:date (-> 100 t/years t/ago t/at-midnight) :label "6+ years"}])

(defn date-to-range-val [d]
  (:date (first (filter (fn [x] (or (t/after? d (:date x)) (t/= d (:date x)))) ageing-range))))

(defn date-to-label [d]
  (let [dv (date-to-range-val d)]
    (:label (first (filter (fn [x] (t/= (:date x) dv)) ageing-range)))))


(defn view-bucket [{:keys [bucket-date albums]}]
  (om/component
   (html
    [:span
       [:h2 (date-to-label bucket-date)]
       (om/build albums/album-list-partial {:artist nil :albums albums})
       [:hr]])))

(defn view-recently-added [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:result-count 20
       :scroll-chan (chan (dropping-buffer 1))
       :result-inc 0.01})
    om/IWillMount
    (will-mount [_]
      (let [scroll-chan (om/get-state owner :scroll-chan)]
        (sub events/event-bus :at-bottom scroll-chan)
        (go
          (loop []
            (let [e (<! scroll-chan)]
              (om/set-state! owner
                            :result-count
                            (* (+ 1 (om/get-state owner :result-inc))
                               (om/get-state owner :result-count))))
            (recur)))))
    om/IRender
    (render [this]
      (let [result-count (om/get-state owner :result-count)
            buckets (group-by
                     (fn [x]
                       (let [d (x "scan_date")]
                         (date-to-range-val (c/from-long d))))
                     (take result-count (:albums data)))]
        (html [:div.browse
               (om/build nav/main-nav data)
               (om/build left-column data)
               [:div.middle-column.pure-g
                [:div.pure-u-1
                 [:div.content
                  (om/build-all view-bucket
                                (reverse (sort-by :bucket-date
                                         (map (fn [x] {:bucket-date (first x) :albums (last x)})
                                     buckets))))]]]])))))
