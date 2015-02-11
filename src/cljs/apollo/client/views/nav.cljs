(ns apollo.client.views.nav
  (:require [apollo.client.utils :as utils]))

(def nav-seq (concat ["all"] (map str "abcdefghijklmnopqrstuvwxyz#")))


(defn get-up-nav [prefix]
  (let [nav-str (apply str nav-seq)]
    (if (utils/s-contains? nav-str prefix)
               prefix
               "#")))

(defn nav-item [x active-nav]
  (let [nav-url (utils/format "#/nav/%s" (utils/encode x))
        active? (= x (get-up-nav active-nav))]
    [:li {:class-name (when active? "active")} [:a {:href nav-url} x]]))

(defn nav-partial [active-nav]
  [:div.collection-nav
   [:ul (map nav-item nav-seq (repeat active-nav))]])

(defn main-nav-partial []
  (let [browse-url "#/"
        settings-url "#/admin"
        player-url "#/player"]
    [:div.main-nav
     [:a {:href browse-url} [:i.fa.fa-search.active {:title "Browse"}]]
     [:i.fa.fa-play-circle {:title "Play"}]
     [:a {:href player-url} [:i.fa.fa-list {:title "Playlists"}]]
     [:a {:href settings-url} [:i.fa.fa-gear {:title "Settings"}]]]))
