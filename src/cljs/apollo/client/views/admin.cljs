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
    [:li path
      [:a.button.subtle {
            :on-click (fn [e]
              (delete-mount path)
                false)}
        [:i.fa.fa-trash]]]))

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
                 [:a.button {:on-click (fn [e] (services/do-scan))}
                  [:i.fa.fa-refresh] "(Re)scan Collection"]
                 [:hr]
                 [:h2 "Add New Mount"]
                 [:input.has-btn {:type "text" :ref "new-mount" :placeholder "Copy a folder location here..."}]
                 [:a.button {:href "#" :on-click on-add-mount}
                  [:i.fa.fa-plus] "Add Mount"]
                 ]]])))))
