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
            [cljs.core.async :refer [<! put! chan sub dropping-buffer unsub]]
            [cljs-time.extend]
            [cljs-time.coerce :as c]
            [cljs-time.core :as t]
            [clojure.string :refer [split]]))

(defn left-column [data owner]
  (reify
    om/IRender
    (render [this]
      (let [sub-view (first (om/observe owner (state/ref-subview)))
            set-subview (fn [k] (om/transact! (state/ref-subview) (fn [p] [k])))]
        (html
         [:div.left-column
          [:ul.toggle
           [:li {:on-click (fn [e]
                             (set-subview :now-playing))
                 :class-name (if (= sub-view :now-playing) "active" "")} "now playing"]
           [:li {:on-click (fn [e]
                             (set-subview :playlists))
                 :class-name (if (= sub-view :playlists) "active" "")} "playlists"]]
          (if (= sub-view :now-playing)
            (om/build player/view-now-playing data)
            (om/build player/view-playlists data))])))))

(defn set-post-filter-value [value]
  (om/transact! (state/ref-post-filter) (fn [p] (assoc p :value value))))

(defn list-filter [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:visible true})
    om/IWillMount
    (will-mount [_]
      (om/set-state! owner :keypress-chan (chan (dropping-buffer 1)))
      (sub events/event-bus :keypress (om/get-state owner :keypress-chan))
      (go
        (loop []
          (let [e (<! (om/get-state owner :keypress-chan))
                key-code (.-keyCode (:event (:message e)))
                n (om/get-node owner "filter-input")]
            (when (= key-code 191)
              (om/set-state! owner :visible true)
              (.focus n)))
          (recur))))
    om/IWillUnmount
    (will-unmount [_]
      (unsub events/event-bus :keypress (om/get-state owner :keypress-chan)))
    om/IRender
    (render [_]
      (let [value (:value (om/observe owner (state/ref-post-filter)))]
        (html
         [:div {:style {:display (if (om/get-state owner :visible)
                                   "block"
                                   "none")}}
          [:input {:type "text"
                   :value value
                   :on-change (fn [e] (set-post-filter-value (.-value (.-target e))))
                   :placeholder "Press / to activate search..."
                   :ref "filter-input"}]])))))

(defn view-browse [data owner]
  (reify
    om/IRender
    (render [this]
      (let [{:keys [active-artist active-nav artists albums active-album artist-info]} data
            artist-count (count artists)
            album-count (count albums)]
        (html
         [:div.browse
          (om/build nav/main-nav data)
          (om/build left-column data)
          [:div.middle-column.pure-g
           [:div.pure-u-1
            [:div.content
             (om/build nav/nav-partial (or (first active-artist) active-nav))
             (cond
                (nil? active-album)
                (om/build list-filter {}))
             (artists/artist-detail-partial active-artist)
             (cond
               (and (nil? active-artist) (nil? active-album))
               (om/build artists/artist-list-partial artists)
               (and (not (nil? active-artist)) (nil? active-album))
               [:span
                (om/build albums/album-list-partial {:artist active-artist
                                                     :albums (sort-by (fn [a] (first (split (:year a) "-"))) albums)})
                (om/build artists/artist-info-partial artist-info)]
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

(defn view-bucket-2 [{:keys [bucket-date albums]}]
  (om/component
   (html
    [:span
       [:h2 bucket-date]
       (om/build albums/album-list-partial {:artist nil :albums albums})
     [:hr]])))

(defn view-recently-added [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:result-count 25
       :scroll-chan (chan (dropping-buffer 1))
       :keypress-chan (chan (dropping-buffer 1))
       :result-inc 25})
    om/IWillMount
    (will-mount [_]
      (let [scroll-chan (om/get-state owner :scroll-chan)]
        (sub events/event-bus :at-bottom scroll-chan)
        (go
          (loop []
            (let [e (<! (events/throttle scroll-chan 100))
                  old-result-count (om/get-state owner :result-count)
                  i (+ (om/get-state owner :result-inc) old-result-count)]
              (om/set-state! owner :result-count i))
            (recur)))))
    om/IWillUnmount
    (will-unmount [_]
      (unsub events/event-bus :at-bottom (om/get-state owner :scroll-chan)))
    om/IRender
    (render [this]
      (let [result-count (om/get-state owner :result-count)
            post-filter (:value (om/observe owner (state/ref-post-filter)))
            filtered-albums (filter (fn [a]  (if (or (nil? post-filter)
                                                (empty? post-filter))
                                              true
                                              (let [c-f post-filter
                                                    c-a-b (str (:name a))
                                                    c-a-a (str (:artist a))]
                                                (or (utils/str-contains? c-a-b c-f)
                                                    (utils/str-contains? c-a-a c-f)))))
                                    (:albums data))
            buckets (group-by
                     (fn [x]
                       (let [d (:scan_date x)]
                         (date-to-range-val (c/from-long d))))
                     (take result-count filtered-albums))]
        (html [:div.browse
               (om/build nav/main-nav data)
               (om/build left-column data)
               [:div.middle-column.pure-g
                [:div.pure-u-1
                 [:div.content
                  (om/build list-filter {})
                  (om/build-all view-bucket
                                (reverse (sort-by :bucket-date
                                         (map (fn [x] {:bucket-date (first x) :albums (last x)  :filtered true})
                                     buckets))))]]]])))))

(defn view-by-year [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:result-count 25
       :scroll-chan (chan (dropping-buffer 1))
       :keypress-chan (chan (dropping-buffer 1))
       :result-inc 25})
    om/IWillMount
    (will-mount [_]
      (let [scroll-chan (om/get-state owner :scroll-chan)]
        (sub events/event-bus :at-bottom scroll-chan)
        (go
          (loop []
            (let [e (<! (events/throttle scroll-chan 100))
                  old-result-count (om/get-state owner :result-count)
                  i (+ (om/get-state owner :result-inc) old-result-count)]
              (om/set-state! owner :result-count i))
            (recur)))))
    om/IWillUnmount
    (will-unmount [_]
      (unsub events/event-bus :at-bottom (om/get-state owner :scroll-chan)))
    om/IRender
    (render [this]
      (let [result-count (om/get-state owner :result-count)
            post-filter (:value (om/observe owner (state/ref-post-filter)))
            filtered-albums (filter (fn [a]  (if (or (nil? post-filter)
                                                     (empty? post-filter))
                                               true
                                               (let [c-f post-filter
                                                     c-a-b (str (:name a))
                                                     c-a-a (str (:artist a))]
                                                 (or (utils/str-contains? c-a-b c-f)
                                                     (utils/str-contains? c-a-a c-f)))))
                                    (:albums data))
            buckets (group-by
                     (fn [x]
                       (first (clojure.string/split (:year x) "-")))
                     (take result-count (reverse (sort-by (fn [x] (first (clojure.string/split (:year x) "-"))) filtered-albums))))]
        (html [:div.browse
             (om/build nav/main-nav data)
             (om/build left-column data)
             [:div.middle-column.pure-g
              [:div.pure-u-1
               [:div.content
                (om/build list-filter {})
                (om/build-all view-bucket-2
                              (reverse (sort-by (fn [x] (str (x :bucket-date)))
                                                (map (fn [x] {:bucket-date (first x) :albums (last x) :filtered true}) buckets))))]]]])))))
