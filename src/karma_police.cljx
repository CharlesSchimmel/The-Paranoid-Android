(ns karma-police
  (:use karma-police.karmadecay reddit reddit.format)
  (:require users))

(declare karma-police)
(defn username [] (-> karma-police :login :name))

(defn reposts [url]
  (map get-link (repost-urls url)))

(defn top-comments [links]
  (->> links
       (map #(-> % :replies first))
       (filter identity)
       (filter #(> (:score %) 1))
       (remove deleted-comment?)))

(defn top-comment [links]
  (->> links top-comments
       (sort-by :score)
       last))

(defn bot-post? [comment]
  (some #(author? comment %) ["Trapped_in_Robot" "Top-Comment-Bot"]))

(defn format-comment [{:keys [body author permalink] :as comment}]
  (p (if (bot-post? comment)
       (quotify body)
       body)
     (i (str "~ " (hyperlink author permalink)))))

;; Number of times posted.

(defn count-string [n]
  (condp = n
    1  "once"
    2  "twice"
    3  "thrice"
    50 "more than fifty times"
    (str n " times")))

;; Time since last post

(defn min-div [n & ds]
  (int (Math/ceil (/ n (apply * ds)))))
(defn days     [t] (min-div t 24 60 60))
(defn hours    [t] (min-div t    60 60))
(defn minutes  [t] (min-div t       60))
(defn seconds  [t] (min-div t         ))

(def time-fns {:hours hours :minutes minutes :seconds seconds})

(defn time-str [t]
  (if-not (> (days t) 1)
    (->> [:hours :minutes :seconds]
         (map #(let [n ((time-fns %) t)]
                 (if (> n 1) (str n " " (name %)))))
         (some identity))))

(defn time-since-last-post [original reposts]
  (let [now (.getTime (java.util.Date.))
        ts  (->> reposts
                 (filter #(= (:subreddit %) (:subreddit original)))
                 (map #(- now (-> % :time .getTime)))
                 (map #(/ % 1000))
                 (filter #(> 0 %)))]
    (if (seq ts)
      (time-str (apply min ts)))))

(defn link-reply [{:keys [url permalink] :as link}]
  (when-let [reposts (-> permalink reposts seq)]
    (when-let [top-comment (top-comment reposts)]
      {:reply (p (format-comment top-comment)
                 ---
                 (i (sn 2 "This image has been submitted "
                           (l (count-string (count reposts))
                              (karmadecay-url permalink))
                           " before"
                           (if-let [t (time-since-last-post link reposts)]
                             (str " (and less than " t " ago - stay classy, OP)"))
                           ". Above is the previous top comment."))
                 (if (bot-post? top-comment)
                   "^ Come on, people, this is just getting ridiculous."))
      :vote :up})))

(def karma-police
  {:handler      link-reply
   :user-agent   "Top Comment Bot by /u/one_more_minute"
   :subreddits   ["funny" "wtf" "pics"]
   :type         :link
   :login        users/trapped-in-robot
   :interval     2
   ; :delay        30
   :retry        true
  })
