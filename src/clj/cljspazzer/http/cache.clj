(ns cljspazzer.http.cache
  (:require [clojure.java.io :as io]
            [clj-http.client :as client]
            [cljspazzer.utils :as utils]))

(defn make-key [& keys]
  (let [result (utils/chk-sum-str (interpose ":" keys))]
    (prn {:result result :keys keys})
    result))

(defn cache-root []
  (let [result (io/file ".images")]
    (if (or (and (.exists result) (.isDirectory result)) (.mkdir result))
      result
      nil)))

(defn cache-response [url & name]
  (prn (format "caching %s for %s" url name))
  (let [res (client/get url {:as :byte-array
                             :throw-exceptions false})
        status (:status res)
        content-type ((:headers res) "Content-Type")
        body (:body res)
        cache-path (.getAbsolutePath (cache-root))
        file-name (format "%s/%s%s" cache-path (apply make-key name) (utils/get-extension-for-mime content-type))]
    (if (= 200 status)
      (with-open [w (io/output-stream file-name)]
        (.write w body)
        (io/file file-name))
      nil)))
