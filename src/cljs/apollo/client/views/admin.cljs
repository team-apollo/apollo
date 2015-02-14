(ns apollo.client.views.admin
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [secretary.core :as secretary]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [<! put! chan]]
            [apollo.client.services :as services]
            [apollo.client.views.nav :as nav]))

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
               (om/build nav/main-nav data)
               [:div.content.pure-g
                [:div.pure-u-1
                 [:h2 "Existing Mounts"]
                 [:ul (map mount-item mounts)]
                 [:input {:type "text" :ref "new-mount"}]
                 [:a.button {:href "#" :on-click on-add-mount} "create"]
                 [:a.button {:on-click (fn [e] (services/do-scan))} "(re)scan"]]]])))))
