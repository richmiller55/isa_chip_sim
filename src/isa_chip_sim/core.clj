(ns isa-chip-sim.core
  (:require [isa-chip-sim.ui-ascii :as ui]
            [isa-chip-sim.top :as top]
            [isa-chip-sim.register :as reg]
            [isa-chip-sim.execution-loop :as el]
            [isa-chip-sim.execution-loop :refer [->Instruction]]
            [clojure.string :as str])
  (:import (java.util.concurrent ArrayBlockingQueue TimeUnit))
  (:gen-class))

(defn clear-screen []
  (print (str (char 27) "[2J"))
  (print (str (char 27) "[;H")))

(def program
  [(->Instruction :add [:r1 :r2] {:write-reg :r3})
   (->Instruction :add [:r3 :r4] {:write-reg :r5})])

(defonce app-status (atom :running))
;; --- REQUIRED INPUT FUNCTIONS START HERE ---

(defn start-input-thread!
  "Starts a daemon thread that reads lines from *in* and puts them onto a queue."
  [input-queue]
  (let [keyboard (java.io.BufferedReader. *in*)]
    (doto (Thread.
           (fn []
             (try
               ;; Check the atom's value in the loop condition
               (while (= @app-status :running))
                 (let [line (.readLine keyboard)]
                   (if line
                     (.put input-queue (str/trim line))
                     ;; If input stream ends (e.g. redirected file input ends)
                     (reset! app-status :halted))) ; Signal the main app to stop too

               (catch InterruptedException _
                 (println "Input thread interrupted, shutting down."))
               (catch java.io.IOException e
                 (println "Input thread IO error:" (.getMessage e)))))
           "Input-Reader-Thread")
      (.setDaemon true)
      (.start))))

(defn consume-input [state input-queue]
  "Checks the input queue and processes the next available command if one exists."
  ;; .poll retrieves the head of the queue, or null if the queue is empty (non-blocking)
  (let [input (.poll input-queue)]
    (if input
      (case input
        "q" (assoc state :quit? true)
        "p" (update state :simulation-status #(if (= % :running) :paused :running))
        "s" (assoc state :step? true)
        state)
      state)))

;; --- REQUIRED INPUT FUNCTIONS END HERE ---

(defn -main
  "The entry-point for the application."
  [& args]
  (let [input-queue (ArrayBlockingQueue. 1)]
    (start-input-thread! input-queue)

    (loop [state (-> (top/new-state :arm)
                     (assoc :program (into {} (map-indexed (fn [i v] [i v]) (conj program (->Instruction :halt [] {}))))) ; <--- Append HALT
                     (assoc :simulation-status :running
                            :step? false)
                     (update :registers #(reg/write-reg % :r1 10))
                     (update :registers #(reg/write-reg % :r2 20))
                     (update :registers #(reg/write-reg % :r4 5)))]

      (clear-screen)
      (println (ui/render-ui state))
      (println "Commands: [p]ause/resume, [s]tep, [q]uit")
      
      ;; Add termination messages
      (when (= (:status state) :halted)
        (println "\n--- Program Halted (HALT instruction executed) ---"))
      (when (:quit? state)
        (println "\n--- Simulation Quit by User ---"))

      (Thread/sleep 100)

      (let [input (consume-input state input-queue) ; Use consume-input from previous suggestion
            state-after-input (assoc input :step? false)]

        ;; <--- Main loop termination condition: check for :quit? OR :halted
        (when (and (not (:quit? state-after-input)) 
                   (not= (:status state-after-input) :halted))

          (let [should-run-cycle (or (= (:simulation-status state-after-input) :running)
                                     (:step? input))
                next-state (if should-run-cycle
                             (el/run-cycle state-after-input)
                             state-after-input)]
            (recur next-state)))))))
