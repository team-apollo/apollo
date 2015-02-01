(ns cljspazzer.client.pages
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljspazzer.client.utils :as utils]
            [cljspazzer.client.services :as services]
            [cljs.core.async :refer [<! put! chan]]
            [secretary.core :as secretary]))

(def nav-seq (concat (map str "#abcdefghijklmnopqrstuvwxyz") ["all"]))
(def audio-node (js/Audio.))
(def track-list (chan))
(def player-ctrl (chan))
(def stream-position (chan))
(def now-playing (chan))

(aset audio-node "onended" (fn [e] (put! player-ctrl :next)
                             false))

(aset audio-node "ontimeupdate" (fn[e] (put! stream-position :changed)))

(defn nav-item [x]
  (let [nav-url (utils/format "#/nav/%s" (utils/encode x))]
    [:li [:a {:href nav-url} x]]))

(defn artist-item [x]
  (let [artist-image (utils/format "/api/artists/%s/image" (utils/encode x))
        artist-url (utils/format "#/artists/%s" (utils/encode x))]
    [:li
     [:a {:href artist-url}
      ;; [:img {:src artist-image}]
      [:div x]]]))


(defn album-item [active-artist album]
  (let [album-name (album "album_canonical")
        album-url (utils/format "#/artists/%s/albums/%s"
                                (utils/encode active-artist)
                                (utils/encode album-name))
        album-image (utils/format "/api/artists/%s/albums/%s/image"
                                  (utils/encode active-artist)
                                  (utils/encode album-name))
        album-label (utils/format "%s" album-name)
        album-zip-url (utils/format "/api/artists/%s/albums/%s/zip"
                                    (utils/encode active-artist)
                                    (utils/encode album-name))]
    [:li
     [:a {:href album-url}
      [:img {:src album-image}]
      album-label]
     [:a.download {:href album-zip-url}
      [:i.fa.fa-download.fa-lg]]
     ]))

(defn format-duration [d]
  (let [hours (quot d 3600)
        r (rem d 3600)
        minutes (quot r 60)
        seconds (rem r 60)]
    (if (> hours 0)
      (utils/format "%i:%02i:%02i" hours minutes seconds)
      (utils/format "%i:%02i" minutes seconds))))

(defn mk-track-url
  ([artist album track-id]
   (utils/format "/api/artists/%s/albums/%s/tracks/%s"
                 (utils/encode artist)
                 (utils/encode album)
                 (utils/encode track-id)))
  ([track]
   (let [t (track "track")
         artist (t "artist_canonical")
         album (t "album_canonical")
         id (t "id")]
     (mk-track-url artist album id))))

(defn track-detail [track compilation?]
  (let [t (track "track")
        track-num (t "track")
        track-title (t "title")
        track-id (t "id")
        artist (t "artist_canonical")
        album (t "album_canonical")
        duration (t "duration")
        track-url (mk-track-url artist album track-id)
        track-label (if compilation?
                      (utils/format "%s. %s by %s" track-num track-title artist)
                      (utils/format "%s. %s" track-num track-title))]
    [:li
     [:a {
          :on-click (fn [e] (put! track-list [track]))}
      track-label
      [:div.right (format-duration duration)]]]))

(defn album-detail [artist album]
  (let [album-name (album "name")
        album-year (album "year")
        album-label (utils/format "%s - (%s)" album-name album-year)
        album-image (utils/format "/api/artists/%s/albums/%s/image"
                                  (utils/encode artist)
                                  (utils/encode album-name))
        album-zip-url (utils/format "/api/artists/%s/albums/%s/zip"
                                    (utils/encode artist)
                                    (utils/encode album-name))
        tracks (album "tracks")
        artist-url (utils/format "#/artists/%s" artist)
        play-album (fn [e] (put! track-list tracks))
        compilation? (album "compilation")]
    (if (and (not (nil? artist)) (not (nil? album)))
      [:div 
       [:a {:href artist-url} [:h1 artist]]
       [:h2 album-label]
       [:a.download {:href album-zip-url}
        [:i.fa.fa-download.fa-lg]]
       [:i.fa.fa-play-circle.fa-lg {:on-click play-album}]
       [:img {:src album-image}]
       [:ul.tracks
        (map (fn [track] (track-detail track compilation?)) tracks)]
       ]))
  )

(defn nav-partial []
  [:div.collection-nav
   [:h3 "Artists"]
   [:ul (map nav-item nav-seq)]])

(defn main-nav-partial []
  (let [browse-url "#/"
        settings-url "#/admin"
        player-url "#/player"]
    [:div.main-nav
     [:a {:href browse-url} [:i.fa.fa-search.active {:title "Browse"}]]
     [:i.fa.fa-play-circle {:title "Play"}]
     [:a {:href player-url} [:i.fa.fa-list {:title "Playlists"}]]
     [:a {:href settings-url} [:i.fa.fa-gear {:title "Settings"}]]]))

(defn artist-list-partial [artists]
  (let [artist-heading (utils/format "%s Artists" (count artists))]
    [:div.artist-list
     [:h3 artist-heading]
     [:ul (map artist-item artists)]]))

(defn album-list-partial [active-artist albums]
  (let [album-heading (utils/format "%s Albums" (count albums))
        render-album (partial album-item active-artist)]
    [:div.album-list
     [:a {:href (utils/format "#/nav/%s" (first active-artist))} "browse"]
     [:h3 album-heading]
     [:ul (map render-album albums)]]))

(defn artist-detail-partial [active-artist]
  (let [artist-image (utils/format "/api/artists/%s/image?force-fetch=1" (utils/encode active-artist))]
    [:div
     [:h3 active-artist]
     [:img.band {:src artist-image}]]))

(defn browse-page [data]
  (let [active-artist (:active-artist data)
        artists (:artists data)
        artist-count (count artists)
        albums (:albums data)
        album-count (count albums)
        active-album (:active-album data)
        artist-image (utils/format "/api/artists/%s/image?force-fetch=1" (utils/encode active-artist))
        artist-image-url (utils/format "url(\"%s\")" artist-image)]
    (html
     [:div.browse
      (main-nav-partial)
      (nav-partial)
      [:div.content.pure-g
       (cond
         (and (nil? active-artist) (nil? active-album))
         [:div.pure-u-1
          (artist-list-partial artists)]
         (and (not (nil? active-artist)) (nil? active-album))
         [:div.pure-u-1
          [:div.pure-g.artist-detail
           [:div.pure-u-17-24
            (album-list-partial active-artist albums)]
           [:div.pure-u-7-24
            (artist-detail-partial active-artist)]
           [:div.artist-bg {:style {:background-image artist-image-url}}]]]
         (not (nil? active-album))
         [:div.pure-u-1
          [:div.album-detail
           (album-detail active-artist active-album)]
          [:div.artist-bg {:style {:background-image artist-image-url}}]])]])))

(defn delete-mount [mount]
  (go
    (<! (services/delete-mount mount))
    (secretary/dispatch! "#/admin")))

(defn mount-item [m]
  (let [path (m "mount")]
    [:li path [:a {
                   :on-click (fn [e]
                               (delete-mount path)
                               false)}
               "delete"]]))

(defn view-admin [data owner]
  (reify
    om/IInitState
    (init-state [this]
      {:mount-input-value nil})
    om/IRender
    (render [this]
      (let [mounts ((:mounts data) "mounts")
            on-add-mount (fn [e]
                           (go (let [v (.-value (om/get-node owner "new-mount"))
                                     result (<! (services/add-mount v))]
                                 (secretary/dispatch! "#/admin")))
                           false)]
        (html [:div.admin
               (main-nav-partial)
               [:div.content.pure-g
                [:div.pure-u-1
                 [:h1 "scan this..."]]
                [:div.pure-u-1
                 [:ul (map mount-item mounts)]]
                [:div.new-mount.pure-u-1
                 [:input {:type "text" :ref "new-mount"}]
                 [:a.button {:href "#" :on-click on-add-mount} "create"]]
                [:div.scan.pure-u-1
                 [:a.button {:on-click (fn [e] (services/do-scan))} "(re)scan"]]]])))))


(defn view-browse [data]
  (om/component (browse-page data)))

(defn view-player [data owner]
  (reify
    om/IRender
    (render [this]
      (let [tracks (data :play-list [])
            playlist-item (fn [r] [:li (r "title")])]
        (html
         [:div.player
          (main-nav-partial)
          [:div.content.pure-g
           [:div.pure-u-1
            [:h1 "player goes here"]
            [:ul (map  playlist-item tracks)]
            [:a.button
             {:on-click (fn [e] (om/transact! data :play-list
                                              (fn [v]
                                                (conj v "xxx"))))
              } "add"]]]])))))


(defn ctrl-audio-node [action]
  (let [seek-offset 5]
    (case action
      :stop (do (.pause audio-node)
                (aset audio-node "currentTime" 0))
      :play (.play audio-node)
      :pause (.pause audio-node)
      :seek-backward (aset audio-node "currentTime" (- (.-currentTime audio-node) 1))
      :seek-forward (aset audio-node "currentTime" (+ (.-currentTime audio-node) 1))
      (.pause audio-node))))

(defn audio-elem [data owner]
  (reify
    om/IInitState
    (init-state [this]
      
      
      
      {:current-src ""
       :current-offset 0
       :ctrl :stop
       :track-src []
       :current-position 0
       :end-position 0})
    om/IWillMount
    (will-mount [this]
      (let [set-track (fn [track offset]
                        (om/set-state! owner :current-offset offset)
                        (aset audio-node "src" (mk-track-url track))
                        (ctrl-audio-node :play)
                        (put! now-playing track))]
        (go (loop []
              (let [track-src (<! track-list)]
                (set-track (first track-src) 1)
                (om/set-state! owner :current-src track-src))
              (recur)))
        (go (loop []
              (let [ctrl (<! player-ctrl)
                    current-offset (or (om/get-state owner :current-offset) 0)
                    previous-offset (dec current-offset)
                    next-offset (inc current-offset)
                    track-src (om/get-state owner :current-src)
                    next-track (last (take next-offset track-src))
                    previous-track (last (take previous-offset track-src))
                    ]
                (om/set-state! owner :ctrl ctrl)
                (cond
                  (and (= ctrl :next) (> next-offset (count track-src)))
                  (ctrl-audio-node :stop)
                  (and (= ctrl :next) (not (nil? next-track)))
                  (set-track next-track next-offset)
                  (and (= ctrl :previous) (not (nil? previous-track)))
                  (set-track previous-track previous-offset)
                  :else (ctrl-audio-node ctrl)))
              (recur)))
        (go (loop []
              (<! stream-position)
              (let [c (.-currentTime audio-node)
                    e (.-duration audio-node)]
                (if (= 0 c)
                  (do
                    (om/set-state! owner :current-position 0)
                    (om/set-state! owner :end-position 0))
                  (do
                    (om/set-state! owner :current-position c)
                    (om/set-state! owner :end-position e))))
              (recur)))
        ))
    om/IRender
    (render [this]
      (let [ctrl-current (om/get-state owner :ctrl)
            is-playing? (not (.-paused audio-node))
            current-position (om/get-state owner :current-position)
            end-position (om/get-state owner :end-position)]
        (html [:div
               [:progress {:max end-position :value current-position}]
               [:div (utils/format "%s/%s"
                                   (format-duration current-position)
                                   (format-duration end-position))]
               [:ul
                [:li {:on-click (fn [e] (put! player-ctrl :previous))}
                 [:i.fa.fa-fast-backward]]
                [:li {:on-click (fn [e] (put! player-ctrl :seek-backward))}
                 [:i.fa.fa-step-backward]]
                [:li {:on-click (fn [e] (put! player-ctrl (if is-playing? :pause :play)))}
                 (if is-playing?
                   [:i.fa.fa-pause]
                   [:i.fa.fa-play])]
                [:li {:on-click (fn [e] (put! player-ctrl :stop))}
                 [:i.fa.fa-stop]]
                [:li {:on-click (fn [e] (put! player-ctrl :seek-forward))}
                 [:i.fa.fa-step-forward]]
                [:li {:on-click (fn [e] (put! player-ctrl :next))}
                 [:i.fa.fa-fast-forward]]
                ]])))))

(defn view-debug [data owner]
  (reify
    om/IInitState
    (init-state [this]
      (html [:h1 "Debug"]))))


(defn view-now-playing [data owner]
  (reify
    om/IInitState
    (init-state [this]
      {:now-playing {"track" {}}})
    om/IWillMount
    (will-mount [this]
      (go
        (loop []
          (let [current-track (<! now-playing)]
            (om/set-state! owner :now-playing current-track))
          (recur))))
    om/IRender
    (render [this]
      (let [current-track (om/get-state owner :now-playing)
            t (current-track "track")
            artist (t "artist_canonical")
            album (t "album_canonical")
            title (t "title_canonical")
            track-num (t "track")
            year (t "year")
            artist-nav (utils/format "#/artist/%s" (utils/encode artist))
            album-nav (utils/format "%s/albums/%s" artist-nav (utils/encode album))

            album-image (utils/format "/api/artists/%s/albums/%s/image" (utils/encode artist) (utils/encode album))
            artist-image (utils/format "/api/artists/%s/image" (utils/encode artist))
            track-heading (utils/format "%s - %s (%s)" artist title year)]
        (html [:div
               (if (not (nil? artist))
                 [:h1 track-heading])])))))
