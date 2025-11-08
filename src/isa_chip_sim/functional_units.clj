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
      (contains? #{:vadd :vsub :vmul :vdiv :vload :vstore} op)))
  (execute [this instruction operand-values]
    (let [{:keys [opcode metadata]} instruction
          [val1 val2] operand-values]
      (case opcode
        :vadd {:result (mapv + val1 val2)}
        :vsub {:result (mapv - val1 val2)}
        :vmul {:result (mapv * val1 val2)}
        :vdiv {:result (mapv quot val1 val2)}
        :vload (assoc instruction :operands operand-values)
        :vstore (assoc instruction :operands operand-values)
        (throw (ex-info "Unknown VPU opcode" {:opcode opcode}))))))

(defrecord MemoryUnit []
  FunctionalUnit
  (can-execute? [this instruction]
    (let [op (:opcode instruction)]
      (contains? #{:load :store} op)))
  (execute [this instruction operand-values]
    (assoc instruction :operands operand-values)))

(defrecord BranchUnit []
  FunctionalUnit
  (can-execute? [this instruction]
    (let [op (:opcode instruction)]
      (contains? #{:jmp :beq :bne} op)))
  (execute [this instruction operand-values]
    (let [{:keys [opcode]} instruction
          [val1 val2 address] operand-values]
      (case opcode
        :jmp {:branch-taken? true :target-address val1}
        :beq {:branch-taken? (= val1 val2) :target-address address}
        :bne {:branch-taken? (not= val1 val2) :target-address address}
        (throw (ex-info "Unknown Branch opcode" {:opcode opcode}))))))

(def functional-units
  {:alu (->ALU)
   :fpu (->FPU)
   :vpu (->VPU)
   :memory (->MemoryUnit)
   :branch (->BranchUnit)})
