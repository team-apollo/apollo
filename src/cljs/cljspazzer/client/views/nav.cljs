(ns cljspazzer.client.views.nav
  (:require [cljspazzer.client.utils :as utils]))

(def nav-seq (concat ["all"] (map str "abcdefghijklmnopqrstuvwxyz#")))

(defn nav-item [x]
  (let [nav-url (utils/format "#/nav/%s" (utils/encode x))]
    [:li [:a {:href nav-url} x]]))

(defn nav-partial []
  [:div.collection-nav
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
