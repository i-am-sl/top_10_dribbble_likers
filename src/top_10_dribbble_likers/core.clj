(ns top-10-dribbble-likers.core
 (:require [clj-http.client :as http]
           [clojure.data.json :as json])
 (:use [slingshot.slingshot :only [try+ throw+]]))

(defn- dribbble_get [url]
  (try+
    (let [{status :status
           :as result} (http/get url {:query-params {"access_token" "7cca0041603433065400ebee27608a06bb2009c78d1d280dea51ba5aa38aae0b"}
                                      :content-type :json})]
      (case status
        200 result       ;;?
        (throw+ {:type ::bad_status :status status :result result}))
    )
    (catch [:status 429] {:keys [headers body]}
      (let [{:strs [X-RateLimit-Reset]} headers
            milliseconds_to_wait (- (* 1000 (read-string X-RateLimit-Reset)) (System/currentTimeMillis))]
        (println "Exceeded number of requests to dribbble server. Server blocked for" (quot milliseconds_to_wait 1000) "seconds. Please wait")
        (Thread/sleep milliseconds_to_wait)
        (dribbble_get url)))))

(defn- dribbble_get_full [url]
  (let [{links :links
         body :body} (dribbble_get url)
        {next :next} links
        {href :href} next,
        decoded-body (json/read-str body)]
    (if (nil? href)
      decoded-body
      (concat decoded-body (dribbble_get_full href)))))

(defn- make_url [path]
  (str "https://api.dribbble.com/v1/" path "?per_page=100")) ;; ! set 100

(defn top10_dribbble_likers [user_id]
  (try+
    (let [
          follower_info_list (dribbble_get_full (make_url (str "users/" user_id "/followers")))
          follower_id_list (map #(get (get % "follower") "id") follower_info_list)
          all_followers_shots (apply concat (pmap
                                              #(dribbble_get_full (make_url (str "users/" % "/shots"))) follower_id_list))
          all_followers_shot_ids (map #(get % "id") all_followers_shots)
          all_likes (apply concat (pmap
                                    #(dribbble_get_full (make_url (str "shots/" % "/likes"))) all_followers_shot_ids))
          top10_users (->> all_likes
                        (map #(let [{:strs [id username likes_received_count]} (get % "user")]
                                {:id id :username username :likes_received_count likes_received_count}))
                        (distinct)
                        (sort-by :likes_received_count #(> %1 %2))
                        (take 10))]
      {:top10_users top10_users})
     (catch [:type :top-10-dribbble-likers.core/bad_status] {:keys [status result]}
       {:error "http_error"})
     (catch [:status 404] _ ;; response
          ;; to do: (log response)
          {:error "not found data"})
     (catch Object _ ;; response
       ;; to do: (log response (:throwable &throw-context))
       {:error "unknown"})))

(defn -main [& args]
  (println "process started:" "\n" "result:" "\n" (top10_dribbble_likers 155719))
  (shutdown-agents))
