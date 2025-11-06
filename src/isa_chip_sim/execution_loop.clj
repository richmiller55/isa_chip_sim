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
        (cond-> (-> state
                    (update :scoreboard #(scoreboard/clear-unit % unit-keyword)))
          write-reg (update :registers #(reg/write-reg % write-reg result))))
      state)))

(defn- memory-access [state]
  (let [instruction (:ex-mem-latch state)]
    (if instruction
      (case (:opcode instruction)
        :load
        (let [address (first (:operands instruction))
              value (first (mem/read-mem (:memory state) address 4)) ; Extract the first element
              new-instruction (assoc instruction :result value)]
          (assoc state :mem-wb-latch new-instruction))

        :store
        (let [address (second (:operands instruction))
              value (first (:operands instruction))
              new-memory (mem/write-mem (:memory state) address [value] 4)]
          (-> state
              (assoc :memory new-memory)
              (assoc :mem-wb-latch instruction)))

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
              operands (:operands instruction)
              operand-values (mapv #(if (keyword? %) (forward-value % state) %) operands)
              result (fu/execute unit instruction operand-values)
              write-reg (get-in instruction [:metadata :write-reg])
              original-pc (get-in instruction [:metadata :original-pc])
              current-pc (reg/read-reg (:registers state) :pc)]
          (if (= unit-keyword :branch)
            (let [branch-taken? (:branch-taken? result)
                  target-address (:target-address result)
                  actual-next-pc (if branch-taken? target-address (inc original-pc))
                  predicted-pc-from-btb (get (:btb state) original-pc)
                  misprediction? (and (:btb-prediction-taken state)
                                      (not= actual-next-pc predicted-pc-from-btb))]
              (cond
                misprediction?
                (-> state
                    (assoc :if-id-latch nil)
                    (assoc :id-ex-latch nil)
                    (update :registers #(reg/write-reg % :pc actual-next-pc))
                    (assoc :ex-mem-latch nil)
                    (assoc :btb-prediction-taken false))

                branch-taken?
                (-> state
                    (assoc :if-id-latch nil)
                    (assoc :id-ex-latch nil)
                    (update :registers #(reg/write-reg % :pc target-address))
                    (assoc :ex-mem-latch nil)
                    (assoc :branch-just-taken true)
                    (update :btb assoc original-pc target-address))

                :else
                (assoc state :ex-mem-latch nil)))
            (assoc state :ex-mem-latch (assoc result :write-reg write-reg :executed-unit unit-keyword)))))
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
          (assoc state :pipeline-stall true)))
      (assoc state :id-ex-latch nil))))

(defn- fetch [state]
  (if-not (:pipeline-stall state)
    (let [pc (reg/read-reg (:registers state) :pc)
          btb-entry (get (:btb state) pc)
          predicted-pc (if btb-entry btb-entry pc)
          instruction (get (:program state) predicted-pc)]
      (if instruction
        (if (:branch-just-taken state)
          (assoc state :if-id-latch (assoc instruction :metadata (assoc (:metadata instruction) :original-pc pc)))
          (-> state
              (assoc :if-id-latch (assoc instruction :metadata (assoc (:metadata instruction) :original-pc pc)))
              (update-in [:registers :registers :pc] (fn [p] (if p (inc p) 1)))
              (assoc :btb-prediction-taken (boolean btb-entry))))
        (assoc state :if-id-latch nil)))
    state))

(defn run-cycle [current-state]
  (let [state-no-stall (-> current-state
                           (assoc :pipeline-stall false)
                           (assoc :branch-just-taken false))
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