(ns isa-chip-sim.IR-translate)

;; Example IR structure
(defrecord IRInstruction [opcode operands metadata])

;; Helper functions for creating common IR instructions
(defn ir-add [dest-reg src1-reg src2-reg]
  (->IRInstruction :add [src1-reg src2-reg dest-reg] {:write-reg dest-reg}))

(defn ir-mul [dest-reg src1-reg src2-reg]
  (->IRInstruction :mul [src1-reg src2-reg dest-reg] {:write-reg dest-reg}))

(defn ir-div [dest-reg src1-reg src2-reg]
  (->IRInstruction :div [src1-reg src2-reg dest-reg] {:write-reg dest-reg}))

(defn ir-or [dest-reg src1-reg src2-reg]
  (->IRInstruction :or [src1-reg src2-reg dest-reg] {:write-reg dest-reg}))

(defn ir-and [dest-reg src1-reg src2-reg]
  (->IRInstruction :and [src1-reg src2-reg dest-reg] {:write-reg dest-reg}))

(defn ir-xor [dest-reg src1-reg src2-reg]
  (->IRInstruction :xor [src1-reg src2-reg dest-reg] {:write-reg dest-reg}))

(defn ir-mov [dest-reg src-reg]
  (->IRInstruction :mov [src-reg dest-reg] {:write-reg dest-reg}))

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

(defn arm->ir [arm-instruction]
  ;; logic to translate ARM to IR
  )

(defn x86->ir [x86-instruction]
  ;; logic to translate x86 to IR
  )
