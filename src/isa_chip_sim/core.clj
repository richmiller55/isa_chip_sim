(ns isa-chip-sim.core
  (:require [isa-chip-sim.ui-ascii :as ui]
            [isa-chip-sim.top :as top]
            [isa-chip-sim.register :as reg]
            [isa-chip-sim.execution-loop :as el]
            [isa-chip-sim.execution-loop :refer [->Instruction]])
  (:gen-class))

(defn clear-screen []
  (print (str (char 27) "[2J"))
  (print (str (char 27) "[;H")))

(def program
  [(->Instruction :add [:r1 :r2] {:write-reg :r3})
   (->Instruction :add [:r3 :r4] {:write-reg :r5})])

(defn -main
  "The entry-point for the application."
  [& args]
  (loop [state (-> (top/new-state :arm)
                   (assoc :program program)
                   (update :registers #(reg/write-reg % :r1 10))
                   (update :registers #(reg/write-reg % :r2 20))
                   (update :registers #(reg/write-reg % :r4 5)))]
    (clear-screen)
    (println (ui/render-ui state))
    (Thread/sleep 500)
    (recur (el/run-cycle state))))