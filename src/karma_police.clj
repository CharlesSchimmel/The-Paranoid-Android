(ns karma-police
  (:use karma-police.karmadecay reddit reddit.format)
  (:require users))

(declare karma-police)
(defn username [] (-> karma-police :login :name))

(defn reposts [url]
  (map link-from-url (repost-urls url)))

(defn top-comments [links]
  (->> links
       (map first-reply)
       (filter identity)
       (remove deleted-comment?)
       ;(remove #(author? % (username))) ; This bit isn't generic
       ))                                ; Also, I'm currently ignoring it for comedic effect.

(defn top-comment [links]
  (->> links top-comments
       (sort-by :score)
       last))

(defn format-comment [{:keys [body author permalink] :as comment}]
  (paragraphs (if (author? comment (username))
                (quotify body)
                body)
              (italic
                (str "~ " (hyperlink author permalink)))))

(defn count-string [n]
  (condp = n
    1 "once"
    2 "twice"
    3 "thrice"
    (str n " times")))

(defn link-reply [url]
  (when-let [reposts (-> url reposts seq)]
    (when-let [top-comment (top-comment reposts)]
      {:reply (paragraphs
                (format-comment top-comment)
                line
                (italic
                  (superscript-n 2
                    "This image has been submitted "
                    (hyperlink (count-string (count reposts)) (karmadecay-url url))
                    " before - above is the previous top comment."))
                (if (author? top-comment (username))
                  "Come on, people, this is just getting ridiculous."))
      :vote :up})))

(def karma-police
  {:handler      (comp link-reply :permalink)
   :user-agent   "Top Comment Bot by /u/one_more_minute"
   :subreddits   ["funny" "wtf" "pics" "aww" "gifs"]
   :type         :link
   :login        users/top-comment
   :log          (comp println str)
   :interval     2
   ; :debug        :true
  })
