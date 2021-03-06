(ns apollo.client.player
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [apollo.client.utils :as utils]
            [apollo.client.channels :as channels]
            [apollo.client.audio :refer [audio-node]]
            [apollo.client.views.albums :as albums]
            [apollo.client.views.artists :as artists]
            [apollo.client.views.tracks :as tracks]
            [apollo.client.views.nav :as nav]
            [apollo.client.state :as state]
            [cljs.core.async :refer [<! put! chan pub]]
            [sablono.core :as html :refer-macros [html]]))

(defn ctrl-audio-node [action]
  (case action
      :stop (do (.pause audio-node)
                (aset audio-node "currentTime" 0))
      :play (.play audio-node)
      :pause (.pause audio-node)
      :seek-backward (aset audio-node "currentTime" (- (.-currentTime audio-node) 1))
      :seek-forward (aset audio-node "currentTime" (+ (.-currentTime audio-node) 1))
      (.pause audio-node)))

(defn set-track [track offset owner]
  (om/set-state! owner :current-offset offset)
  (om/set-state! owner :current-track track)
  (om/transact! (state/ref-player) (fn [p] (assoc p :current-offset offset)))
  (aset audio-node "src" (tracks/mk-track-url track))
  (ctrl-audio-node :play)
  (om/update! (state/ref-now-playing) [track]))

(defn audio-elem [data owner]
  (reify
    om/IInitState
    (init-state [this]
      {:current-offset 0
       :current-track nil
       :ctrl :stop
       :current-position 0
       :end-position 0})
    om/IWillMount
    (will-mount [this]
      (go (loop []
            (let [[track-src offset] (<! channels/track-list)
                  current-track (if (> (or offset 0) 0)
                                  (last (take (inc offset) track-src))
                                  (first track-src))
                  cp (om/get-state owner :current-track)]
              (when (not= (((or current-track {}) :track {}) :id)
                          (((or cp {}) :track {}) :id))
                (set-track current-track (or offset 0) owner))
              (om/transact! (state/ref-player) (fn [p]
                                                 (assoc p
                                                        :current-playlist track-src
                                                        :current-offset (or offset 0)))))
            (recur)))
      (go (loop []
            (let [ctrl (<! channels/player-ctrl)
                  current-offset (or (om/get-state owner :current-offset) 0)
                  previous-offset (dec current-offset)
                  next-offset (inc current-offset)
                  track-src ((state/ref-player) :current-playlist)
                  next-track (last (take (inc next-offset) track-src))
                  previous-track (last (take (inc previous-offset) track-src))
                  current-track (last (take (inc current-offset) track-src))]
              (om/set-state! owner :ctrl ctrl)
              (cond
                (and (= ctrl :next) (> next-offset (dec (count track-src))))
                (ctrl-audio-node :stop)
                (and (= ctrl :next) (not (nil? next-track)))
                (set-track next-track next-offset owner)
                (and (= ctrl :previous) (not (nil? previous-track)))
                (set-track previous-track previous-offset owner)
                :else (do (ctrl-audio-node ctrl)
                          (when (= ctrl :stop)
                            (put! channels/now-playing {"track" {}}))
                          (when (= ctrl :play)
                            (put! channels/now-playing current-track)))))
            (recur)))
      (go (loop []
            (<! channels/stream-position)
            (let [c (.-currentTime audio-node)
                  e (.-duration audio-node)]
              (if (= 0 c)
                (do
                  (om/set-state! owner :current-position 0)
                  (om/set-state! owner :end-position 0))
                (do
                  (om/set-state! owner :current-position c)
                  (om/set-state! owner :end-position e))))
            (recur))))
    om/IRender
    (render [this]
      (let [ctrl-current (om/get-state owner :ctrl)
            is-playing? (not (.-paused audio-node))
            current-position (om/get-state owner :current-position)
            end-position (om/get-state owner :end-position)]
        (html [:div.player
               [:progress {:max end-position :value current-position}]
               [:div (utils/format "%s/%s"
                                   (utils/format-duration current-position)
                                   (utils/format-duration end-position))]
               [:ul
                [:li {:on-click (fn [e] (put! channels/player-ctrl :previous))}
                 [:i.fa.fa-step-backward.fa-fw]]
                [:li {:on-click (fn [e] (put! channels/player-ctrl (if is-playing? :pause :play)))}
                 (if is-playing?
                   [:i.fa.fa-pause.fa-fw]
                   [:i.fa.fa-play.fa-fw])]
                [:li {:on-click (fn [e] (put! channels/player-ctrl :next))}
                 [:i.fa.fa-step-forward.fa-fw]]
                [:li [:i.fa.fa-repeat.fa-fw]]
                [:li {:on-click (fn [e] (om/transact!
                                         (state/ref-player)
                                         (fn [p]
                                           (assoc p :current-playlist (shuffle (:current-playlist p))))))}
                 [:i.fa.fa-random.fa-fw]]]])))))

(defn view-now-playing [data owner]
  (reify
    om/IRender
    (render [this]
      (let [current-track (or (first (om/observe owner (state/ref-now-playing))) {:track {}})
            t (current-track :track)
            artist (t :artist)
            album (t :album)
            title (t :title)
            track-num (t :track)
            year (t :year)
            artist-nav (artists/mk-artist-url (:artist_canonical t))
            album-nav (albums/mk-album-url (:artist_canonical t) (:album_canonical t))
            artist-image (artists/mk-artist-image (:artist_canonical t) true)
            album-image (albums/mk-album-image (:artist_canonical t) (:album_canonical t))
            track-heading (utils/format "%s - %s (%s)" artist title year)
            wiki-link (utils/format "http://www.wikipedia.org/wiki/%s (band)" artist)]
        (html (if (not (nil? artist))
                [:div.now-playing
                 [:a {:href album-nav}
                  [:img {:src album-image}]]
                 [:span
                  [:h2 (utils/format "%s" artist)
                    [:a {:href wiki-link :target "_blank"}
                      [:i.fa.fa-external-link]]]
                  [:label.caption "Album"]
                  [:p album]
                  [:p.year year]
                  [:label.caption "Track"]
                  [:p title]]]))))))

(defn view-playlist-item [{:keys [idx item view-playing? playing playing-offset]}]
  (reify
    om/IRender
    (render [this]
      (html
       [:li {:class-name (when (and view-playing? (= idx playing-offset))  "active") :on-double-click (fn [e] (put! channels/track-list [playing idx]))}
        [:p (tracks/playlist-track item)]
        [:span (tracks/artist item true)]
        [:i.fa.fa-times-circle.fa-2x]]))))

(defn view-playlists [data owner]
  (reify
    om/IInitState
    (init-state [this]
      {:view-playing true})
    om/IRender
    (render [this]
      (let [playing (:current-playlist (om/observe owner (state/ref-player)))
            playing-offset (:current-offset (om/observe owner (state/ref-player)))
            view-playing? (om/get-state owner :view-playing)
            playlist-items (if view-playing? playing [])]
        (html [:div
               ;[:i.fa.fa-plus {:on-click (fn [e] (om/set-state! owner :view-playing false))}]
               [:h3 {:on-click (fn [e] (om/set-state! owner :view-playing true))} "Instant Playlist"]
               [:ul.playlist
                (om/build-all view-playlist-item
                              (map-indexed (fn [idx item] {:idx idx
                                                           :item item
                                                           :view-playing? view-playing?
                                                           :playing playing
                                                           :playing-offset playing-offset}) playlist-items))]])))))
