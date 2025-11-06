(ns isa-chip-sim.top
  (:require [isa-chip-sim.register :refer [new-register-file]]
            [isa-chip-sim.memory :refer [new-memory]]
            [isa-chip-sim.scoreboard :as scoreboard]
            [isa-chip-sim.functional-units :as fu]))

(defn new-state [arch-keyword]
  {:registers   (new-register-file arch-keyword)
   :memory      (new-memory)
   :scoreboard  (scoreboard/new-scoreboard fu/functional-units)
   :functional-units fu/functional-units
   :pipeline    {}
   :if-id-latch nil
   :id-ex-latch nil
   :ex-mem-latch nil
   :mem-wb-latch nil
   :btb         {}
   :program     []
   :clock       0
   :status      :running})
