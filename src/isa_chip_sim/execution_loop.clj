(ns isa-chip-sim.execution-loop
  (:require [isa-chip-sim.scoreboard :as scoreboard]
            [isa-chip-sim.functional-units :as fu]
            [isa-chip-sim.register :as reg]
            [isa-chip-sim.memory :as mem]))

(defrecord Instruction [opcode operands metadata])
(def NOP (->Instruction :nop [] {}))

(defn- write-back [state]
  (let [instruction (:data (:mem-wb-latch state))]
    (if instruction
      (let [{:keys [result write-reg]} instruction
            unit-keyword (:executed-unit instruction)]
        (cond-> (-> state
                    (update :scoreboard #(scoreboard/clear-unit % unit-keyword)))
          write-reg (update :registers #(reg/write-reg % write-reg result))))
      state)))

(defn- memory-access [state]
  (let [instruction (:data (:ex-mem-latch state))]
    (if instruction
      (case (:opcode instruction)
        :load
        (let [address (first (:operands instruction))
              value (first (mem/read-mem (:memory state) address 4))
                                        ; Extract the first element
              new-instruction (assoc instruction :result value)]
          (assoc-in state [:mem-wb-latch :data] new-instruction))

        :store
        (let [address (second (:operands instruction))
              value (first (:operands instruction))
              new-memory (mem/write-mem (:memory state) address [value] 4)]
          (-> state
              (assoc :memory new-memory)
              (assoc-in [:mem-wb-latch :data] instruction)))

        (assoc-in state [:mem-wb-latch :data] instruction))
      (assoc-in state [:mem-wb-latch :data] nil))))

(defn- forward-value [reg-name state]
  (let [ex-mem-instruction (:data (:ex-mem-latch state))
        mem-wb-instruction (:data (:mem-wb-latch state))]
    (cond
      (= reg-name (:write-reg ex-mem-instruction))
      (:result ex-mem-instruction)

      (= reg-name (:write-reg mem-wb-instruction))
      (:result mem-wb-instruction)

      :else
      (reg/read-reg (:registers state) reg-name))))

(defn- flush-pipeline-from [state stage-keyword]
  ;; This is a simplified example; you would implement the logic to clear all stages upstream of a given point
  (-> state
      (assoc-in [:if-id-latch :data] nil)
      (assoc-in [:id-ex-latch :data] nil)
      ; ... maybe clear more latches depending on where the flush starts ...
      ))

(defn- execute [state]
  (let [instruction (:data (:id-ex-latch state))]
    (if instruction
      (if (= (:opcode instruction) :nop)
        (assoc-in state [:ex-mem-latch :data] nil)
        (let [unit-keyword (scoreboard/find-unit-for-instruction instruction fu/functional-units)] ; Find the keyword
          (if-not unit-keyword ; <--- Guard added here
            (throw (ex-info (str "No functional unit found for opcode: " (:opcode instruction))
                            {:instruction instruction}))
            (let [unit (get fu/functional-units unit-keyword)
                  operands (:operands instruction)
                  operand-values (mapv #(if (keyword? %) (forward-value % state) %) operands)
                  result (fu/execute unit instruction operand-values)
                  write-reg (get-in instruction [:metadata :write-reg])
                  original-pc (get-in instruction [:metadata :original-pc])
                  pc-reg (:pc-reg state)
                  current-pc (reg/read-reg (:registers state) pc-reg)]
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
                        (flush-pipeline-from :if-id-latch) 
                        (flush-pipeline-from :id-ex-latch) 
                        (update :registers #(reg/write-reg % pc-reg actual-next-pc))
                        (assoc-in [:ex-mem-latch :data] nil)
                        (assoc :btb-prediction-taken false))

                    branch-taken?
                    (-> state
                        (assoc-in [:if-id-latch :data] nil)
                        (assoc-in [:id-ex-latch :data] nil)
                        (update :registers #(reg/write-reg % pc-reg target-address))
                        (assoc-in [:ex-mem-latch :data] nil)
                        (assoc :branch-just-taken true)
                        (update :btb assoc original-pc target-address))

                    :else
                    (assoc-in state [:ex-mem-latch :data] nil)))
                (assoc-in state [:ex-mem-latch :data] (assoc result :write-reg write-reg :executed-unit unit-keyword)))))))
      (assoc-in state [:ex-mem-latch :data] nil))))

(defn- decode [state]
  (let [instruction (:data (:if-id-latch state))]
    (if instruction
      (if (= (:opcode instruction) :nop)
        (assoc-in state [:id-ex-latch :data] instruction)
        (do (prn "Decoding instruction:" instruction) 
        (if-let [new-scoreboard (scoreboard/issue-instruction (:scoreboard state) instruction fu/functional-units)]
          (-> state
              (assoc-in [:id-ex-latch :data] instruction)
              (assoc :scoreboard new-scoreboard))
          (assoc state :pipeline-stall true))))
      (assoc-in state [:id-ex-latch :data] nil))))

(defn- fetch [state]
  (if-not (:pipeline-stall state)
    (let [pc-reg (:pc-reg state)
          pc (reg/read-reg (:registers state) pc-reg)
          btb-entry (get (:btb state) pc)
          predicted-pc (if btb-entry btb-entry pc)
          instruction (get (:program state) predicted-pc)]
      (if instruction
        (if (:branch-just-taken state)
          (assoc-in state [:if-id-latch :data] (assoc instruction :metadata (assoc (:metadata instruction) :original-pc pc)))
          (-> state
              (assoc-in [:if-id-latch :data] (assoc instruction :metadata (assoc (:metadata instruction) :original-pc pc)))
              (update-in [:registers :registers pc-reg] (fn [p] (if p (inc p) 1)))
              (assoc :btb-prediction-taken (boolean btb-entry))))
        (assoc-in state [:if-id-latch :data] nil)))
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
    (if (and (< cycle cycles) (not= (:status state) :halted)) ; <--- ADD halt check here
      (let [next-state (run-cycle state)]
        (recur next-state (inc cycle)))
      state)))
