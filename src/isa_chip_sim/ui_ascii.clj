(ns isa-chip-sim.ui-ascii
  (:require [isa-chip-sim.register :as reg]))

(defn- render-registers
  [state]
  """Renders the register file to an ASCII string."""
  (let [register-file (:registers state)
        registers (:registers register-file)
        reg-names (sort (keys registers))]
    (str
     "--- Registers ---\n"
     (apply str
            (for [reg-name reg-names]
              (format "%s: %s\n" reg-name (reg/read-reg register-file reg-name)))))))

(defn render-ui
  [state]
  """Renders the entire UI to an ASCII string."""
  (render-registers state))
