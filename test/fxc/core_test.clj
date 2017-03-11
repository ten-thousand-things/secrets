(ns fxc.core-test
  (:use midje.sweet)
  (:require  [fxc.core :refer :all]
             [fxc.random :as r]
             [fxc.secretshare :as ss]
             [fxc.marshalling :as ms]
             [hashids.core :as h]
             [clojure.pprint :as pp]))

(pp/pprint {"------------------------------------------" "FXC_CORE_TESTS"})

;; TODO: get some proper random
(def salt (str (for [x (range 0 2)] (r/digit (Integer/MAX_VALUE)))))
;; generate a random secret
(def password (str (h/encode {:salt salt}
                           (for [x (range 0 3)] (r/digit (Integer/MAX_VALUE))))))

(pp/pprint {:password password
            :salt salt})
;;            :seq (str2seq password)})

(def intseq (str2seq password))

(fact "String to seq"
      (seq2str intseq) => password)

(def secrets (seq2secrets intseq))

;; (pp/pprint {:secrets secrets})

(fact "Seq to secrets"
      (secrets2seq secrets) => intseq
      (fact "Secrets to seq to password"
            (seq2str (secrets2seq secrets)) => password))

(def raw-slices (secrets2slices secrets))
(def encoded-slices (map ms/encode raw-slices))
(def decoded-slices (map ms/decode encoded-slices))

;; (pp/pprint {:raw-slices raw-slices
;;             :encoded-slices encoded-slices
;;             :decoded-slices decoded-slices})

(fact "Create horizontal slices across vertical secrets"
      (fact "are as many as the settings total"
            (count raw-slices) => (:total settings))
      (fact "are as many as the sequence of compressed integers"
            ;; subtract one which is the position
            (dec (count (first raw-slices))) => (count intseq))
      (fact "are decoded correctly to raw numeric format"
            raw-slices => decoded-slices))


(fact "Retrieve vertical secrets from horizontal slices"
      (def decoded-secrets (slices2secrets decoded-slices))
      decoded-secrets => secrets
      (pp/pprint {:back-to-secrets decoded-secrets})

      (fact "then combine secrets into seq"
            (def decoded-seq (secrets2seq decoded-secrets))
            decoded-seq => intseq)

      (fact "then retrieve the password"
            (def decoded-password (seq2str decoded-seq))
            (pp/pprint {:decoded-password decoded-password})
            (seq2str decoded-seq) => password))
      
(fact "Public fxc codec functions"
      (def pub-encoded (encode settings password))
      (pp/pprint {:pub-encoded pub-encoded})
      (fact "work with all shares"
            (decode settings pub-encoded ) => password)
      (fact "work with shuffled shares"
            (decode settings
                    (shuffle
                     (encode settings password))) => password)
      (fact "work with minimum quorum"
            (decode settings
                    (take (:quorum settings)
                          (shuffle (encode settings password)))) => password)
      )

