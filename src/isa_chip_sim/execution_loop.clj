(ns isa-chip-sim.execution-loop
  (:require [isa-chip-sim.scoreboard :as scoreboard]
            [isa-chip-sim.functional-units :as fu]
            [isa-chip-sim.register :as reg]
            [isa-chip-sim.memory :as mem]))

(defrecord Instruction [opcode operands metadata])
(def NOP (->Instruction :nop [] {}))

(defn- write-back [state]
  (let [instruction (:mem-wb-latch state)]
    (if instruction
      (let [{:keys [result write-reg]} instruction
            unit-keyword (:executed-unit instruction)]
        (-> state
            (update :registers #(reg/write-reg % write-reg result))
            (update :scoreboard #(scoreboard/clear-unit % unit-keyword))))
      state)))

(defn- memory-access [state]
  (let [instruction (:ex-mem-latch state)]
    (if instruction
      (case (:opcode instruction)
        :load
        (let [address (first (:operands instruction))
              value (mem/read-mem (:memory state) address 4) ; Assuming 4-byte reads
              new-instruction (assoc instruction :result value)]
          (assoc state :mem-wb-latch new-instruction))

        :store
        (let [address (second (:operands instruction))
              value (first (:operands instruction))
              new-memory (mem/write-mem (:memory state) address [value] 4)] ; Assuming 4-byte writes
          (-> state
              (assoc :memory new-memory)
              (assoc :mem-wb-latch nil)))

        (assoc state :mem-wb-latch instruction))
      (assoc state :mem-wb-latch nil))))

(defn- forward-value [reg-name state]
  (let [ex-mem-latch (:ex-mem-latch state)
        mem-wb-latch (:mem-wb-latch state)]
    (cond
      (= reg-name (:write-reg ex-mem-latch))
      (:result ex-mem-latch)

      (= reg-name (:write-reg mem-wb-latch))
      (:result mem-wb-latch)

      :else
      (reg/read-reg (:registers state) reg-name))))

(defn- execute [state]
  (let [instruction (:id-ex-latch state)]
    (if instruction
      (if (= (:opcode instruction) :nop)
        (assoc state :ex-mem-latch nil)
        (let [unit-keyword (scoreboard/find-unit-for-instruction instruction fu/functional-units)
              unit (get fu/functional-units unit-keyword)
              [src1 src2] (:operands instruction)
              val1 (if (keyword? src1) (forward-value src1 state) src1)
              val2 (if (keyword? src2) (forward-value src2 state) src2)
              result (fu/execute unit instruction [val1 val2])
              write-reg (get-in instruction [:metadata :write-reg])]
          (assoc state :ex-mem-latch (assoc result :write-reg write-reg :executed-unit unit-keyword))))
      (assoc state :ex-mem-latch nil))))

(defn- decode [state]
  (let [instruction (:if-id-latch state)]
    (if instruction
      (if (= (:opcode instruction) :nop)
        (assoc state :id-ex-latch instruction)
        (if-let [new-scoreboard (scoreboard/issue-instruction (:scoreboard state) instruction fu/functional-units)]
          (-> state
              (assoc :id-ex-latch instruction)
              (assoc :scoreboard new-scoreboard))
          (assoc state :pipeline-stall true :id-ex-latch nil)))
      (assoc state :id-ex-latch nil))))

(defn- fetch [state]
  (if-not (:pipeline-stall state)
    (let [pc (reg/read-reg (:registers state) :pc)
          instruction (get (:program state) pc)]
      (-> state
          (assoc :if-id-latch instruction)
          (update-in [:registers :registers :pc] (fn [p] (if p (inc p) 1)))))
    (assoc state :if-id-latch nil)))

(defn run-cycle [current-state]
  (let [state-no-stall (assoc current-state :pipeline-stall false)
        wb-state (write-back state-no-stall)
        mem-state (memory-access wb-state)
        ex-state (execute mem-state)
        id-state (decode ex-state)
        if-state (fetch id-state)]
    (-> if-state
        (update :clock inc))))

(defn simulate [initial-state cycles]
  (loop [state initial-state cycle 0]
    (if (< cycle cycles)
      (let [next-state (run-cycle state)]
        (recur next-state (inc cycle)))
      state)))