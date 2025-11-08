(ns isa-chip-sim.IR-translate)

;; Example IR structure
(defrecord IRInstruction [opcode operands metadata])

;; Helper functions for creating common IR instructions
(defn ir-add ^IRInstruction [dest-reg src1-reg src2-reg]
  (->IRInstruction :add [src1-reg src2-reg dest-reg] {:write-reg dest-reg}))

(defn ir-sub ^IRInstruction [dest-reg src1-reg src2-reg]
  (->IRInstruction :sub [src1-reg src2-reg dest-reg] {:write-reg dest-reg}))

(defn ir-mul ^IRInstruction [dest-reg src1-reg src2-reg]
  (->IRInstruction :mul [src1-reg src2-reg dest-reg] {:write-reg dest-reg}))

(defn ir-div ^IRInstruction [dest-reg src1-reg src2-reg]
  (->IRInstruction :div [src1-reg src2-reg dest-reg] {:write-reg dest-reg}))

(defn ir-or ^IRInstruction [dest-reg src1-reg src2-reg]
  (->IRInstruction :or [src1-reg src2-reg dest-reg] {:write-reg dest-reg}))

(defn ir-and ^IRInstruction [dest-reg src1-reg src2-reg]
  (->IRInstruction :and [src1-reg src2-reg dest-reg] {:write-reg dest-reg}))

(defn ir-xor ^IRInstruction [dest-reg src1-reg src2-reg]
  (->IRInstruction :xor [src1-reg src2-reg dest-reg] {:write-reg dest-reg}))

(defn ir-mov ^IRInstruction [dest-reg src-reg]
  (->IRInstruction :mov [src-reg dest-reg] {:write-reg dest-reg}))

(defn ir-udiv ^IRInstruction [dest-reg src1-reg src2-reg]
  (->IRInstruction :udiv [src1-reg src2-reg dest-reg] {:write-reg dest-reg}))

(defn ir-inc ^IRInstruction [reg]
  (->IRInstruction :inc [reg] {:write-reg reg}))

(defn ir-dec ^IRInstruction [reg]
  (->IRInstruction :dec [reg] {:write-reg reg}))

;; Comparison IR instruction
(defn ir-cmp [op1 op2]
  (->IRInstruction :cmp [op1 op2] {}))

;; New Conditional Branch IR instructions
(defn ir-bgt [address]
  (->IRInstruction :bgt [address] {}))

(defn ir-bge [address]
  (->IRInstruction :bge [address] {}))

(defn ir-beq [address]
  (->IRInstruction :beq [address] {}))

(defn ir-je [address]
  (->IRInstruction :je [address] {}))

(defn ir-jg [address]
  (->IRInstruction :jg [address] {}))

(defn ir-jge [address]
  (->IRInstruction :jge [address] {}))

;; Stack Operations IR instructions
(defn ir-push [value]
  (->IRInstruction :push [value] {}))

(defn ir-pop [dest-reg]
  (->IRInstruction :pop [dest-reg] {:write-reg dest-reg}))

;; Subroutine Call/Return IR instructions
(defn ir-call [address]
  (->IRInstruction :call [address] {}))

(defn ir-ret []
  (->IRInstruction :ret [] {}))

;; System Call IR instruction
(defn ir-syscall [syscall-num arg1 arg2 arg3]
  (->IRInstruction :syscall [syscall-num arg1 arg2 arg3] {}))

;; Helper functions for creating common FPU IR instructions
(defn ir-fadd [dest-reg src1-reg src2-reg]
  (->IRInstruction :fadd [src1-reg src2-reg dest-reg] {:write-reg dest-reg}))

(defn ir-fsub [dest-reg src1-reg src2-reg]
  (->IRInstruction :fsub [src1-reg src2-reg dest-reg] {:write-reg dest-reg}))

(defn ir-fmul [dest-reg src1-reg src2-reg]
  (->IRInstruction :fmul [src1-reg src2-reg dest-reg] {:write-reg dest-reg}))

(defn ir-fdiv [dest-reg src1-reg src2-reg]
  (->IRInstruction :fdiv [src1-reg src2-reg dest-reg] {:write-reg dest-reg}))

(defn ir-load [dest-reg address]
  (->IRInstruction :load [address] {:write-reg dest-reg}))

(defn ir-store [src-reg address]
  (->IRInstruction :store [src-reg address] {}))

(defn ir-jmp [address]
  (->IRInstruction :jmp [address] {}))

(defn ir-beq [src1-reg src2-reg address]
  (->IRInstruction :beq [src1-reg src2-reg address] {}))

(defn ir-bne [src1-reg src2-reg address]
  (->IRInstruction :bne [src1-reg src2-reg address] {}))

;; Helper functions for creating common VPU IR instructions
(defn ir-vadd [dest-reg src1-reg src2-reg vec-size]
  (->IRInstruction :vadd [src1-reg src2-reg dest-reg] {:write-reg dest-reg :vec-size vec-size}))

(defn ir-vsub [dest-reg src1-reg src2-reg vec-size]
  (->IRInstruction :vsub [src1-reg src2-reg dest-reg] {:write-reg dest-reg :vec-size vec-size}))

(defn ir-vmul [dest-reg src1-reg src2-reg vec-size]
  (->IRInstruction :vmul [src1-reg src2-reg dest-reg] {:write-reg dest-reg :vec-size vec-size}))

(defn ir-vdiv [dest-reg src1-reg src2-reg vec-size]
  (->IRInstruction :vdiv [src1-reg src2-reg dest-reg] {:write-reg dest-reg :vec-size vec-size}))

(defn ir-vload [dest-reg address vec-size]
  (->IRInstruction :vload [address] {:write-reg dest-reg :vec-size vec-size}))

(defn ir-vstore [src-reg address vec-size]
  (->IRInstruction :vstore [src-reg address] {:vec-size vec-size}))

(defn arm->ir [arm-instruction]
  (let [{:keys [opcode operands]} arm-instruction]
    (case opcode
      :MOV (let [[dest src] operands]
             (if (number? src)
               (ir-mov dest src)
               (ir-mov dest src))) ; Handle immediate and register moves
      :ADD (let [[dest src1 src2] operands] (ir-add dest src1 src2))
      :SUB (let [[dest src1 src2] operands] (ir-sub dest src1 src2))
      :CMP (let [[op1 op2] operands] (ir-cmp op1 op2))
      :BGT (ir-bgt (first operands))
      :BGE (ir-bge (first operands))
      :BEQ (ir-beq (first operands))
      :UDIV (let [[dest src1 src2] operands] (ir-udiv dest src1 src2))
      :MUL (let [[dest src1 src2] operands] (ir-mul dest src1 src2))
      :LDR (ir-load (first operands) (second operands))
      :SWI (let [[syscall-num] operands] (ir-syscall syscall-num nil nil nil)) ; Assuming 4 args for syscall
      :VADD.I32 (let [[dest src1 src2] operands]
                  (ir-vadd dest src1 src2 128)) ; NEON is 128-bit
      ;; Add more ARM instruction translations here
      (throw (ex-info "Unknown ARM opcode" {:instruction arm-instruction}))))) 

(defn x86->ir [x86-instruction]
  (let [{:keys [opcode operands]} x86-instruction]
    (case opcode
      :MOV (let [[dest src] operands]
             (if (number? src)
               (ir-mov dest src)
               (ir-mov dest src))) ; Handle immediate and register moves
      :ADD (let [[dest src1 src2] operands] (ir-add dest src1 src2))
      :CMP (let [[op1 op2] operands] (ir-cmp op1 op2))
      :JG (ir-jg (first operands))
      :JGE (ir-jge (first operands))
      :JE (ir-je (first operands))
      :XOR (let [[dest src1 src2] operands] (ir-xor dest src1 src2))
      :DIV (let [[src] operands] (ir-div :eax :edx src)) ; x86 div is special, edx:eax / src
      :INC (ir-inc (first operands))
      :JMP (ir-jmp (first operands))
      :CALL (ir-call (first operands))
      :PUSH (ir-push (first operands))
      :POP (ir-pop (first operands))
      :RET (ir-ret)
      :INT (let [[syscall-num] operands] (ir-syscall syscall-num nil nil nil)) ; Assuming 4 args for syscall
      :VADDPS (let [[dest src1 src2] operands]
                 (ir-vadd dest src1 src2 256)) ; AVX is 256-bit
      ;; Example of a complex x86 instruction that translates to multiple IR instructions
      :ADD_MEM_REG
      (let [[dest-reg mem-addr] operands]
        [(ir-load :tmp-reg mem-addr) ; 1. Load from memory into a temporary register
         (ir-add dest-reg dest-reg :tmp-reg)]) ; 2. Add to the destination register
      ;; Add more x86 instruction translations here
      (throw (ex-info "Unknown x86 opcode" {:instruction x86-instruction})))))

