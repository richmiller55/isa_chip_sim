(ns isa-chip-sim.assembler
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [isa-chip-sim.execution-loop :refer [->Instruction NOP]]))

(defn read-asm-file [file-path]
  (with-open [reader (io/reader file-path)]
    (->> (line-seq reader)
         (map #(first (str/split % #";")))
         (map str/trim)
         (filter (complement str/blank?))
         (vec))))

(defn first-pass [asm-lines]
  (loop [lines asm-lines
         address 0
         symbol-table {}]
    (if (empty? lines)
      symbol-table
      (let [line (first lines)
            [label-part instruction-part] (str/split line #":" 2)
            new-symbol-table (if instruction-part
                               (assoc symbol-table (keyword (str/trim label-part)) address)
                               symbol-table)
            new-address (if (or instruction-part (not (str/blank? line)))
                          (inc address)
                          address)]
        (recur (rest lines) new-address new-symbol-table)))))

(defn- parse-operand [operand-str symbol-table]
  (let [trimmed-operand (str/trim operand-str)]
    (cond
      (str/starts-with? trimmed-operand "#") (Integer/parseInt (subs trimmed-operand 1))
      (str/starts-with? trimmed-operand ":") (keyword (subs trimmed-operand 1))
      (contains? symbol-table (keyword trimmed-operand)) (get symbol-table (keyword trimmed-operand))
      :else (keyword trimmed-operand))))

(defn second-pass [asm-lines symbol-table]
  (vec
   (for [line asm-lines]
     (let [[_ label-part instruction-part] (re-matches #"^\s*(?:([^:]+):)?\s*(.*)$" line)
           instruction-str (str/trim (or instruction-part label-part))]
       (if (str/blank? instruction-str)
         NOP ; Should not happen with cleaned lines, but as a safeguard
         (let [[opcode-str & raw-operand-strs] (str/split instruction-str #"\s+")
               operand-strs (map #(str/replace % #",+" "") raw-operand-strs)
               opcode (keyword (str/upper-case opcode-str))]
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
             :BNE (->Instruction opcode
                                 [(parse-operand (first operand-strs) symbol-table)]
                                 {})
             :HALT (->Instruction opcode [] {})
             NOP)))))))
