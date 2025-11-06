(ns isa-chip-sim.assembler-test
  (:require [clojure.test :refer :all]
            [isa-chip-sim.assembler :refer :all]))

(deftest x86-prime-number-assembly-test
  (testing "First pass: generating the symbol table for x86_prime_number.asm"
    (let [asm-lines (read-asm-file "resources/asm_source/x86_prime_number.asm")
          _ (println "ASM lines passed to first-pass:" asm-lines)
          symbol-table (first-pass asm-lines)
          expected-symbol-table {:_start 7,
                                   :prime_loop 9,
                                   :test_divisor_loop 14,
                                   :is_prime 23,
                                   :not_prime 26,
                                   :end_program 29,
                                   :print_number 35,
                                   :print_number_loop 44,
                                   :print_digit_loop 55,
                                   :printed_number 63}]
      (println "Generated symbol table:" symbol-table)
      (is (= expected-symbol-table symbol-table)))))