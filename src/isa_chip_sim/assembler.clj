(ns isa-chip-sim.assembler
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [isa-chip-sim.execution-loop :refer [->Instruction NOP]]
            [isa-chip-sim.IR-translate :as ir]))

(defn read-asm-file [file-path]
  (with-open [reader (java.io.BufferedReader. (java.io.FileReader. file-path))]
    (->> (line-seq reader)
         (map #(first (str/split % #";"))) ; simpler regex literal
         (map str/trim)
         (filter (complement str/blank?)) ; Filter blank lines
         (vec))))

(defn first-pass [asm-lines]
  (loop [lines asm-lines
         address 0
         symbol-table {}]
    (if (empty? lines)
      symbol-table
      (let [line (first lines)
            label-match (re-matches #"\s*([a-zA-Z_][a-zA-Z0-9_]*):(?:\s*.*)?" line)
            _ (println "Label match result:" label-match) ; Debugging line
            label-name (when label-match (second label-match))
            new-symbol-table (if label-name
                               (assoc symbol-table (keyword label-name) address)
                               symbol-table)
            ;; Increment address for every processed line, as they are all "meaningful"
            ;; after read-asm-file has filtered out comments and blank lines.
            new-address (inc address)]
        (recur (rest lines) new-address new-symbol-table)))))

(defn- parse-operand [operand-str symbol-table]
  (let [trimmed-operand (str/trim operand-str)]
    (cond
      ;; Use #"" syntax for cleaner regex literals
      (re-matches #"^\[.*\]$" trimmed-operand)
        (keyword (str/replace trimmed-operand #"[\[\]\s]" ""))

      (str/starts-with? trimmed-operand "#")
        (Integer/parseInt (subs trimmed-operand 1))

      (str/starts-with? trimmed-operand ":")
        (keyword (subs trimmed-operand 1))

      (contains? symbol-table (keyword trimmed-operand))
        (get symbol-table (keyword trimmed-operand))

      :else (keyword trimmed-operand))))

;; --- Polymorphic Instruction Assembler (Visitor Pattern) ---

(defmulti assemble-instruction
  "Dispatches assembly logic based on the opcode keyword."
  (fn [opcode _ _] opcode)) ; Dispatch on the first argument (the opcode)

;; --- Define assembly methods for each instruction ---

;; General Register-to-Register/Immediate operations (ADD, SUB, XOR, MOV)
(defmethod assemble-instruction :ADD [opcode operands symbol-table]
  (let [[dest src1 src2] (map #(parse-operand % symbol-table) operands)]
    (->Instruction opcode [src1 src2] {:write-reg dest})))

(defmethod assemble-instruction :SUB [opcode operands symbol-table]
   (let [[dest src1 src2] (map #(parse-operand % symbol-table) operands)]
    (->Instruction opcode [src1 src2] {:write-reg dest})))

(defmethod assemble-instruction :XOR [opcode operands symbol-table]
   (let [[dest src1 src2] (map #(parse-operand % symbol-table) operands)]
    (->Instruction opcode [src1 src2] {:write-reg dest})))

(defmethod assemble-instruction :MOV [opcode operands symbol-table]
   (let [[dest src] (map #(parse-operand % symbol-table) operands)]
    (->Instruction opcode [src] {:write-reg dest})))


;; Compare instructions (CMP)
(defmethod assemble-instruction :CMP [opcode operands symbol-table]
  (let [[src1 src2] (map #(parse-operand % symbol-table) operands)]
    (->Instruction opcode [src1 src2] {})))

;; Jump/Branch instructions (JG, JE, JMP, BNE, CALL) - take one label/address operand
(doseq [opcode [:JG :JE :JMP :BNE :CALL :JGE]]
  (defmethod assemble-instruction opcode [opc operands symbol-table]
    (let [target (parse-operand (first operands) symbol-table)]
      (->Instruction opc [target] {}))))

;; Single Operand instructions (INC, PUSH, POP, DIV, INT)
(defmethod assemble-instruction :INC [opcode operands symbol-table]
  (let [target (parse-operand (first operands) symbol-table)]
    (->Instruction opcode [target] {:write-reg target})))

(defmethod assemble-instruction :PUSH [opcode operands symbol-table]
  (let [src (parse-operand (first operands) symbol-table)]
    (->Instruction opcode [src] {})))

(defmethod assemble-instruction :POP [opcode operands symbol-table]
   (let [dest (parse-operand (first operands) symbol-table)]
     (->Instruction opcode [] {:write-reg dest}))) ; POP writes to a register, reads from memory implicitly

(defmethod assemble-instruction :DIV [opcode operands symbol-table]
  (let [src (parse-operand (first operands) symbol-table)]
    (->Instruction opcode [src] {})))

(defmethod assemble-instruction :INT [opcode operands symbol-table]
  (let [interrupt-num (parse-operand (first operands) symbol-table)]
    (->Instruction opcode [interrupt-num] {})))

;; No Operand instructions (RET, HALT)
(doseq [opcode [:RET :HALT]]
  (defmethod assemble-instruction opcode [opc _ _]
    (->Instruction opc [] {})))

;; Default case for unimplemented instructions
(defmethod assemble-instruction :default [opcode operands symbol-table]
  (println (str "Warning: Unimplemented opcode " opcode))
  (->Instruction :NOP [] {}))

(defn second-pass [asm-lines symbol-table]
  (vec
   (filter some?
           (for [line asm-lines]
             (let [[_ label-part instruction-part] (re-matches #"\s*(?:([^:]+):)?\s*(.*)$" line)
                   instruction-str (some-> (or instruction-part label-part) str/trim)]
               
               (if (str/blank? instruction-str)
                 nil ; Skip blank lines that might have slipped through
                 (let [[opcode-str & raw-operand-strs] (str/split instruction-str #"\s+")
                       operand-strs (map #(str/replace % #",+" "") raw-operand-strs)
                       opcode (keyword (str/upper-case opcode-str))]

                   ;; Delegate assembly to the multimethod
                   (assemble-instruction opcode operand-strs symbol-table))))))))

(defn third-pass [instructions isa]
  (let [translate-fn (case isa
                       :x86 ir/x86->ir
                       :arm ir/arm->ir)]
    (vec (map translate-fn instructions))))

(defn assemble-file [file-path isa]
  (let [asm-lines    (read-asm-file file-path)
        symbol-table (first-pass asm-lines)
        instructions (second-pass asm-lines symbol-table)
        ir-instructions (third-pass instructions isa)]
    ir-instructions))
