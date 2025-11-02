(ns isa-chip-sim.core
  (:require [cljfx.api :as fx])
  (:gen-class))

;; Define a simple UI component
(defn root-view [{:keys [text]}]
  {:fx/type :stage
   :showing true
   :title "cljfx Minimal Example"
   :width 400
   :height 200
   :scene {:fx/type :scene
           :root {:fx/type :v-box
                  :alignment :center
                  :children [{:fx/type :label
                              :text text}]}}
   :on-close-request (fn [_] (System/exit 0))}) ;; Added for graceful exit

;; Initial state
(def *state
  (atom {:text "Hello, cljfx!"}))

;; Mount the UI
(defn -main [& args]
  (fx/create-renderer
    :opts {:fx.opt/map-event-handler (fn [event] (println "Event:" event))}) ;; Added event handler
  (fx/mount-renderer *state root-view))