(ns isa-chip-sim.scoreboard
  (:require [isa-chip-sim.functional-units :as fu]))

(defrecord Scoreboard [functional-unit-status register-status])

(defn new-scoreboard [functional-units]
  (->Scoreboard (zipmap (keys functional-units) (repeat {:busy false :instruction nil}))
                  {}))

(defn find-unit-for-instruction [instruction functional-units]
  (when instruction
    (->> functional-units
         (filter (fn [[_ unit]] (fu/can-execute? unit instruction)))
         (first)
         (key))))

(defn issue-instruction [scoreboard instruction functional-units]
  (let [unit-keyword (find-unit-for-instruction instruction functional-units)]
    (if-not unit-keyword
      (throw (ex-info "No functional unit for instruction" {:instruction instruction})))

    (let [unit-status (get-in scoreboard [:functional-unit-status unit-keyword])
          write-reg (get-in instruction [:metadata :write-reg])
          is-reg-busy (get-in scoreboard [:register-status write-reg])]
      (if (:busy unit-status)
        false ; Cannot issue
        (-> scoreboard
            (assoc-in [:functional-unit-status unit-keyword] {:busy true :instruction instruction})
            (assoc-in [:register-status write-reg] unit-keyword))))))

(defn clear-unit [scoreboard unit-keyword]
  (let [instruction (get-in scoreboard [:functional-unit-status unit-keyword :instruction])
        write-reg (get-in instruction [:metadata :write-reg])]
    (-> scoreboard
        (assoc-in [:functional-unit-status unit-keyword] {:busy false :instruction nil})
        (assoc-in [:register-status write-reg] nil))))
