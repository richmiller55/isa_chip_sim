(ns isa-chip-sim.ui.register
  (:require [cljfx.api :as fx]
            [isa-chip-sim.register :as reg]))

(defn view [{:keys [registers]}]
  {:fx/type :v-box
   :children (for [[reg-name value] registers]
               {:fx/type :h-box
                :children [{:fx/type :label
                            :text (str (name reg-name) ": ")}
                           {:fx/type :label
                            :text (str value)}]})})
