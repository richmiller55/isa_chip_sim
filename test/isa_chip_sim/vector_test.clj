(ns isa-chip-sim.vector-test
  (:require [clojure.test :refer :all]
            [isa-chip-sim.register :refer :all]
            [isa-chip-sim.functional-units :refer :all]
            [isa-chip-sim.IR-translate :refer :all]))

(deftest vector-register-test
  (testing "YMM register access"
    (let [rf (new-register-file :x86-64)
          ymm-val (vec (range 8))
          updated-rf (write-reg rf :ymm0 ymm-val)]
      (is (= ymm-val (read-reg updated-rf :ymm0)))))

  (testing "XMM register write and YMM read"
    (let [rf (new-register-file :x86-64)
          xmm-val (vec (range 4))
          updated-rf (write-reg rf :xmm0 xmm-val)
          ymm-val (read-reg updated-rf :ymm0)]
      (is (= (into xmm-val [0 0 0 0]) ymm-val)))) ; Assumes ymm is 8 elements, initialized to 0

  (testing "YMM write and XMM read"
    (let [rf (new-register-file :x86-64)
          ymm-val (vec (range 8))
          updated-rf (write-reg rf :ymm0 ymm-val)
          xmm-val (read-reg updated-rf :xmm0)]
      (is (= (subvec ymm-val 0 4) xmm-val)))))

(deftest vpu-execution-test
  (let [vpu (->VPU)]
    (testing "VADD 128-bit"
      (let [instr (ir-vadd :v0 :v1 :v2 128)
            op1 (vec (range 4))
            op2 (vec (range 4))
            result (execute vpu instr [op1 op2])] 
        (is (= {:result [0 2 4 6]} result))))

    (testing "VSUB 256-bit"
      (let [instr (ir-vsub :ymm0 :ymm1 :ymm2 256)
            op1 (vec (range 8))
            op2 (vec (repeat 8 1))
            result (execute vpu instr [op1 op2])]
        (is (= {:result [-1 0 1 2 3 4 5 6]} result))))))