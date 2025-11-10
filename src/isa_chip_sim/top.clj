(ns isa-chip-sim.top
  (:require [isa-chip-sim.register :refer [new-register-file]]
            [isa-chip-sim.memory :refer [new-memory]]
            [isa-chip-sim.scoreboard :as scoreboard]
            [isa-chip-sim.functional-units :as fu]))
;; In isa-chip-sim.top
(defn new-latch []
  {:status :empty ; or :stalled, :valid
   :data   nil})

(defn new-state [arch-keyword]
  (let [arch-def (get isa-chip-sim.register/architectures arch-keyword)]
    {:registers   (new-register-file arch-keyword)
     :pc-reg      (:pc-reg arch-def)
     :memory      (new-memory)
     :scoreboard  (scoreboard/new-scoreboard fu/functional-units)
     :functional-units fu/functional-units
     :pipeline    {}
     :if-id-latch (new-latch)
     :id-ex-latch (new-latch)
     :ex-mem-latch (new-latch)
     :mem-wb-latch (new-latch)
     :btb         {}
     :program     []
     :clock       0
     :status      :running}))

