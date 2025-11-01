(ns isa-chip-sim.core
  (:gen-class)
  (:require [clojure.edn :as edn]
            [isa-chip-sim.top :as top]
            [isa-chip-sim.execution-loop :as sim]
            [clojure.pprint :as pprint]))

(defn -main
  "Reads a configuration file, initializes the simulator, and runs it."
  [& args]
  (if-let [config-path (first args)]
    (let [config (edn/read-string (slurp config-path))
          initial-state (-> (top/new-state (:architecture config))
                              (update :registers #(isa-chip-sim.register/write-reg % :r1 10))
                              (update :registers #(isa-chip-sim.register/write-reg % :r2 20)))
          final-state (sim/simulate initial-state (:cycles config))]
      (println "Simulation complete.")
      (println "Final state:")
      (pprint/pprint final-state))
    (println "Usage: lein run <path-to-config.edn>")))
