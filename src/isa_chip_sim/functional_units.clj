(ns isa-chip-sim.functional-units)

(defprotocol FunctionalUnit
  (can-execute? [this instruction])
  (execute [this instruction operand-values]))

(defrecord ALU []
  FunctionalUnit
  (can-execute? [this instruction]
    (let [op (:opcode instruction)]
      (contains? #{:add :sub :mul :div :and :or :xor :mov} op)))
  (execute [this instruction operand-values]
    (let [{:keys [opcode]} instruction
          [val1 val2] operand-values]
      (case opcode
        :add {:result (+ val1 val2)}
        :sub {:result (- val1 val2)}
        :mul {:result (* val1 val2)}
        :div {:result (quot val1 val2)}
        :and {:result (bit-and val1 val2)}
        :or  {:result (bit-or val1 val2)}
        :xor {:result (bit-xor val1 val2)}
        :mov {:result val1}
        (throw (ex-info "Unknown ALU opcode" {:opcode opcode}))))))

(defrecord FPU []
  FunctionalUnit
  (can-execute? [this instruction]
    (let [op (:opcode instruction)]
      (contains? #{:fadd :fsub :fmul :fdiv} op)))
  (execute [this instruction operand-values]
    (let [{:keys [opcode]} instruction
          [val1 val2] operand-values]
      (case opcode
        :fadd {:result (+ val1 val2)}
        :fsub {:result (- val1 val2)}
        :fmul {:result (* val1 val2)}
        :fdiv {:result (/ val1 val2)}
        (throw (ex-info "Unknown FPU opcode" {:opcode opcode}))))))

(defrecord VPU []
  FunctionalUnit
  (can-execute? [this instruction]
    (let [op (:opcode instruction)]
      (contains? #{:vadd :vsub :vmul} op)))
  (execute [this instruction operand-values]
    ;; Dummy implementation
    {:result [1 2 3 4] :write-reg :v0}))

(defrecord MemoryUnit []
  FunctionalUnit
  (can-execute? [this instruction]
    (let [op (:opcode instruction)]
      (contains? #{:load :store} op)))
  (execute [this instruction operand-values]
    (assoc instruction :operands operand-values)))

(def functional-units
  {:alu (->ALU)
   :fpu (->FPU)
   :vpu (->VPU)
   :memory (->MemoryUnit)})
