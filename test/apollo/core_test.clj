(ns apollo.core-test
  (:require [clojure.test :refer :all]
            [apollo.core :refer :all]))

(defn request [handler method resource & params]
  (handler {:uri resource :request-method method :params params}))

(deftest is-dev-true
  (testing "is dev true"
    (is (= is-dev? true))))

(deftest is-inject-devcode-working
  (testing "given is-dev? is true we should see some injecting of figwheel code"
    (is (boolean (re-find #"('apollo.client.dev')" (:body (render-to-response (page))))))))

(deftest redirects-to-index
  (testing "redirects from / to index.html"
    (let [{status :status {location "Location"} :headers} (request app-handler :get "/")]
      (is (= 302 status))
      (is (= location "/index.html")))))
