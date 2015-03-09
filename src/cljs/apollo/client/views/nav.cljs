(ns apollo.client.views.nav
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [secretary.core :as secretary]
            [apollo.client.utils :as utils]
            [apollo.client.keyboard :as keyboard]
            [apollo.client.utils :as utils]
            [apollo.client.events :as e]
            [sablono.core :as html :refer-macros [html]]
            [om.core :as om :include-macros true]
            [cljs.core.async :refer [<! sub chan dropping-buffer]]))

(def nav-seq (concat ["all"] (map str "abcdefghijklmnopqrstuvwxyz#")))

(doall (map (fn[x] (keyboard/register-shortcut
                    (keyword (utils/format "browse-%s" x))
                    (utils/format "n %s" x)))
            (filter (fn [x] (not (= x "all"))) nav-seq)))

(def shortcut-chan (chan (dropping-buffer 1)))
(sub e/event-bus :shortcut shortcut-chan)

(go
  (loop []
    (let [id (:message (:message (<! shortcut-chan)))
          prefix (.replace (name id) "browse-" "")
          route (utils/format "#/nav/%s" prefix)]
      (when (some #{prefix} nav-seq)
        (set! (.-hash js/window.location) route)
        (secretary/dispatch! route)))
    (recur)))

(defn get-up-nav [prefix]
  (let [nav-str (apply str nav-seq)]
    (cond
      (or (= prefix "all") (nil? prefix)) "all"
      (utils/s-contains? nav-str prefix) prefix
      :else "#")))

(defn nav-item [x active-nav]
  (let [nav-url (utils/format "#/nav/%s" (utils/encode x))
        active? (= x (get-up-nav active-nav))]
    [:li {:class-name (when active? "active")} [:a {:href nav-url} x]]))

(defn nav-partial [active-nav]
  (om/component
   (html
    [:div.collection-nav
     [:ul (map nav-item nav-seq (repeat active-nav))]
     [:span.fa-stack
      [:i.fa.fa-circle.fa-stack-2x]
      [:i.fa.fa-ellipsis-h.fa-inverse.fa-stack-1x.fa-lg]]])))

(defn main-nav [data]
  (reify
    om/IRender
    (render [this]
      (let [browse-url "#/"
            settings-url "#/admin"
            recent-url "#/recent"
            token (utils/format "#%s" (:current-token data))
            active-item (cond
                          (= token settings-url) {:settings true}
                          (= token recent-url) {:recent true}
                          :else {:browse true})
            browse-class-name (utils/format "fa fa-search %s"
                                            (if (:browse active-item) "active" ""))
            settings-class-name (utils/format "fa fa-gear %s"
                                              (if (:settings active-item) "active" ""))
            recent-class-name (utils/format "fa fa-clock-o %s"
                                              (if (:recent active-item) "active" ""))]
        (html
         [:div.main-nav
          [:a {:href browse-url} [:i {:title "Browse"
                                      :class-name browse-class-name}]]
          [:a {:href recent-url} [:i {:title "Recent"
                                        :class-name recent-class-name}]]
          [:a {:href settings-url} [:i {:title "Settings"
                                        :class-name settings-class-name}]]])))))
