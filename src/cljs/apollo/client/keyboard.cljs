(ns apollo.client.keyboard
  (:require [apollo.client.events :as events]
            [goog.events :as e])
  (:import [goog.ui KeyboardShortcutHandler]))


;;; keyboard shortcuts wiring
(def kb (new KeyboardShortcutHandler js/window))

(defn register-shortcut [identifier str-shortcut]
  (.registerShortcut kb (name identifier) str-shortcut))

(e/listen kb "shortcut"
               (fn [e]
                 (events/publish events/event-chan :shortcut {:topic :shortcut
                                  :message (keyword (.-identifier e))})))
