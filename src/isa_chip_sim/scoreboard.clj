(ns isa-chip-sim.scoreboard
  (:require [isa-chip-sim.functional-units :as fu]))

(defrecord Scoreboard [functional-unit-status register-status])

(defn new-scoreboard [functional-units]
  (->Scoreboard (zipmap (keys functional-units) (repeat {:busy false :instruction nil}))
                  {}))

(defn find-unit-for-instruction [instruction functional-units]
  (when instruction
    (let [matching-entry (->> functional-units
                              (filter (fn [[_ unit]] (fu/can-execute? unit instruction)))
                              (first))]
      (when matching-entry ; <--- Add nil check here
        (key matching-entry)))))

(defn issue-instruction [scoreboard instruction functional-units]
  (let [unit-keyword (find-unit-for-instruction instruction functional-units)
        write-reg (get-in instruction [:metadata :write-reg])
        unit-status (get-in scoreboard [:functional-unit-status unit-keyword])
        is-reg-busy (when write-reg (get-in scoreboard [:register-status write-reg]))] ; use when for clarity

    (if-not unit-keyword
      (throw (ex-info "No functional unit for instruction" {:instruction instruction})))

    (cond 
      (:busy unit-status) 
        false ; Structural Hazard
      
      is-reg-busy 
        false ; WAW Hazard (destination register is already waiting for a result)

      :else ; Safe to issue
        (cond-> (-> scoreboard
                    (assoc-in [:functional-unit-status unit-keyword] {:busy true :instruction instruction}))
          write-reg (assoc-in [:register-status write-reg] unit-keyword)))))

(defn clear-unit [scoreboard unit-keyword]
  (let [instruction (get-in scoreboard [:functional-unit-status unit-keyword :instruction])
        write-reg (get-in instruction [:metadata :write-reg])]
    (cond-> (-> scoreboard
                (assoc-in [:functional-unit-status unit-keyword] {:busy false :instruction nil}))
      write-reg (assoc-in [:register-status write-reg] nil))))
