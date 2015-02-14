(ns apollo.client.views.nav
  (:require [apollo.client.utils :as utils]
            [sablono.core :as html :refer-macros [html]]
            [om.core :as om :include-macros true]))

(def nav-seq (concat ["all"] (map str "abcdefghijklmnopqrstuvwxyz#")))


(defn get-up-nav [prefix]
  (let [nav-str (apply str nav-seq)
        _ (.log js/console prefix)]
    (cond
      (= prefix "all") "all"
      (utils/s-contains? nav-str prefix) prefix
      :else "#")))

(defn nav-item [x active-nav]
  (let [nav-url (utils/format "#/nav/%s" (utils/encode x))
        active? (= x (get-up-nav active-nav))]
    [:li {:class-name (when active? "active")} [:a {:href nav-url} x]]))

(defn nav-partial [active-nav]
  [:div.collection-nav
   [:ul (map nav-item nav-seq (repeat active-nav))]])

(defn main-nav [data]
  (reify
    om/IRender
    (render [this]
      (let [browse-url "#/"
            settings-url "#/admin"
            player-url "#/player"
            token (utils/format "#%s" (:current-token data)) 
            active-item (cond
                          (= token settings-url) {:settings true}
                          (= token player-url) {:player true}
                          :else {:browse true})
            browse-class-name (utils/format "fa fa-search %s" (if (:browse active-item) "active" ""))
            player-class-name (utils/format "fa fa-list %s" (if (:player active-item) "active" ""))
            settings-class-name (utils/format "fa fa-gear %s" (if (:settings active-item) "active" ""))]
        (html
         [:div.main-nav
          [:a {:href browse-url} [:i {:title "Browse"
                                      :class-name browse-class-name}]]
          [:i.fa.fa-play-circle {:title "Play"}]
          [:a {:href player-url} [:i {:title "Playlists"
                                                 :class-name player-class-name}]]
          [:a {:href settings-url} [:i {:title "Settings"
                                        :class-name settings-class-name}]]])))))
