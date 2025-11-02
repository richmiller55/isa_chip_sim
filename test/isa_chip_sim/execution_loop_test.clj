(ns isa-chip-sim.execution-loop-test
  (:require [clojure.test :refer :all]
            [isa-chip-sim.top :refer [new-state]]
            [isa-chip-sim.execution-loop :refer :all]
            [isa-chip-sim.register :as reg]))

(deftest pipeline-test
  (testing "single instruction pipeline"
    (let [program [(->Instruction :add [:r1 :r2] {:write-reg :r3})]
          initial-state (-> (new-state :arm)
                            (assoc :program program)
                            (update :registers #(reg/write-reg % :r1 10))
                            (update :registers #(reg/write-reg % :r2 20)))
          final-state (simulate initial-state 5)]
      (is (= 30 (reg/read-reg (:registers final-state) :r3)))))

  (testing "data forwarding for RAW hazard"
    (let [program [(->Instruction :add [:r1 :r2] {:write-reg :r3})
                   NOP
                   NOP
                   (->Instruction :add [:r3 :r4] {:write-reg :r5})]
          initial-state (-> (new-state :arm)
                            (assoc :program program)
                            (update :registers #(reg/write-reg % :r1 10))
                            (update :registers #(reg/write-reg % :r2 20))
                            (update :registers #(reg/write-reg % :r4 5)))
          state (simulate initial-state 8)]
      (is (= 30 (reg/read-reg (:registers state) :r3)))
      (is (= 35 (reg/read-reg (:registers state) :r5)))))

  (testing "stall on busy unit"
    (let [program [(->Instruction :add [:r1 :r2] {:write-reg :r3})
                   (->Instruction :add [:r4 :r5] {:write-reg :r6})]
          initial-state (-> (new-state :arm)
                            (assoc :program program))
          state2 (simulate initial-state 2)
          state3 (run-cycle state2)]
      (is (:pipeline-stall state3)))))

(deftest memory-access-test
  (testing "load and store instructions"
    (let [program [(->Instruction :store [:r1 100] {})
                   (->Instruction :load [100] {:write-reg :r2})]
          initial-state (-> (new-state :arm)
                            (assoc :program program)
                            (update :registers #(reg/write-reg % :r1 123)))
          final-state (simulate initial-state 8)]
      (is (= [123] (isa-chip-sim.memory/read-mem (:memory final-state) 100 1)))
      (is (= 123 (reg/read-reg (:registers final-state) :r2))))))

(deftest branching-logic-test
  (testing "beq instruction - branch taken"
    (let [program [(->Instruction :beq [:r1 :r2 10] {})]
          initial-state (-> (new-state :arm)
                            (assoc :program program)
                            (update :registers #(reg/write-reg % :r1 10))
                            (update :registers #(reg/write-reg % :r2 10)))
          final-state (simulate initial-state 3)]
      (is (= 10 (reg/read-reg (:registers final-state) :pc)))))

  (testing "beq instruction - branch not taken"
    (let [program [(->Instruction :beq [:r1 :r2 10] {})]
          initial-state (-> (new-state :arm)
                            (assoc :program program)
                            (update :registers #(reg/write-reg % :r1 10))
                            (update :registers #(reg/write-reg % :r2 20)))
          final-state (simulate initial-state 3)]
      (is (= 1 (reg/read-reg (:registers final-state) :pc)))))

  (testing "jmp instruction"
    (let [program [(->Instruction :jmp [20] {})]
          initial-state (-> (new-state :arm)
                            (assoc :program program))
          final-state (simulate initial-state 3)]
      (is (= 20 (reg/read-reg (:registers final-state) :pc))))))
