(ns isa-chip-sim.assembler
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [isa-chip-sim.execution-loop :refer [->Instruction NOP]]))

(defn read-asm-file [file-path]
  (with-open [reader (java.io.BufferedReader. (java.io.FileReader. file-path))]
    (->> (line-seq reader)
         (map #(first (str/split % (re-pattern ";")))) ; Remove comments
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
            _ (println "Processing line:" line) ; Debugging line
            ;; Check if the line starts with a label definition
            label-match (re-matches #"^\\s*([a-zA-Z_][a-zA-Z0-9_]*):.*" line)
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
      (str/starts-with? trimmed-operand "[") (keyword (str/replace (str/replace trimmed-operand #"[\\\[|\\\\]]" "") "\\s" "")) ; Memory address
      (str/starts-with? trimmed-operand "#") (Integer/parseInt (subs trimmed-operand 1))
      (str/starts-with? trimmed-operand ":") (keyword (subs trimmed-operand 1))
      (contains? symbol-table (keyword trimmed-operand)) (get symbol-table (keyword trimmed-operand))
      :else (keyword trimmed-operand))))

(defn second-pass [asm-lines symbol-table]
  (vec
   (filter some?
           (for [line asm-lines]
             (let [[_ label-part instruction-part] (re-matches #"^\\s*(?:([^:]+):)?\\s*(.*)$" line)
                   instruction-str (some-> (or instruction-part label-part) str/trim)]
               (println "Instruction string:" instruction-str) ; Debugging line
               (if (str/blank? instruction-str)
                 nil
                 (let [[opcode-str & raw-operand-strs] (str/split instruction-str #"\s+")
                       operand-strs (map #(str/replace % #",+" "") raw-operand-strs)
                       opcode (keyword (str/upper-case opcode-str))]
                   (println "Opcode:" opcode) ; Debugging line
                   (case opcode
                     :MOV (->Instruction opcode
                                         [(parse-operand (first operand-strs) symbol-table)
                                          (parse-operand (second operand-strs) symbol-table)]
                                         {:write-reg (parse-operand (first operand-strs) symbol-table)})
                     :ADD (->Instruction opcode
                                         [(parse-operand (first operand-strs) symbol-table)
                                          (parse-operand (second operand-strs) symbol-table)]
                                         {:write-reg (parse-operand (first operand-strs) symbol-table)})
                     :SUB (->Instruction opcode
                                         [(parse-operand (first operand-strs) symbol-table)
                                          (parse-operand (second operand-strs) symbol-table)]
                                         {:write-reg (parse-operand (first operand-strs) symbol-table)})
                     :CMP (->Instruction opcode
                                         [(parse-operand (first operand-strs) symbol-table)
                                          (parse-operand (second operand-strs) symbol-table)]
                                         {})
                     :JG (->Instruction opcode
                                        [(parse-operand (first operand-strs) symbol-table)]
                                        {})
                     :JGE (->Instruction opcode
                                         [(parse-operand (first operand-strs) symbol-table)]
                                         {})
                     :XOR (->Instruction opcode
                                         [(parse-operand (first operand-strs) symbol-table)
                                          (parse-operand (second operand-strs) symbol-table)]
                                         {:write-reg (parse-operand (first operand-strs) symbol-table)})
                     :DIV (->Instruction opcode
                                         [(parse-operand (first operand-strs) symbol-table)]
                                         {})
                     :JE (->Instruction opcode
                                        [(parse-operand (first operand-strs) symbol-table)]
                                        {})
                     :INC (->Instruction opcode
                                         [(parse-operand (first operand-strs) symbol-table)]
                                         {:write-reg (parse-operand (first operand-strs) symbol-table)})
                     :JMP (->Instruction opcode
                                         [(parse-operand (first operand-strs) symbol-table)]
                                         {})
                     :CALL (->Instruction opcode
                                          [(parse-operand (first operand-strs) symbol-table)]
                                          {})
                     :PUSH (->Instruction opcode
                                          [(parse-operand (first operand-strs) symbol-table)]
                                          {})
                     :POP (->Instruction opcode
                                         [(parse-operand (first operand-strs) symbol-table)]
                                         {:write-reg (parse-operand (first operand-strs) symbol-table)})
                     :RET (->Instruction opcode [] {})
                     :INT (->Instruction opcode
                                         [(parse-operand (first operand-strs) symbol-table)]
                                         {})
                     :BNE (->Instruction opcode
                                         [(parse-operand (first operand-strs) symbol-table)]
                                         {})
                     :HALT (->Instruction opcode [] {})
                     nil))))))))