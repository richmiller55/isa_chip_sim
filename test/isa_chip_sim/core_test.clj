(ns isa-chip-sim.core-test
  (:require [clojure.test :refer :all]
            [isa-chip-sim.core :refer :all]
            [isa-chip-sim.memory :refer :all]
            [isa-chip-sim.register :refer :all]))

(deftest register-test
  (testing "new-register-file for ARM"
    (let [rf (new-register-file :arm)]
      (is (instance? isa_chip_sim.register.RegisterFile rf))
      (is (= (:arch rf) arm-arch))
      (is (= (count (:registers rf)) (count (:registers arm-arch))))
      (is (every? zero? (vals (:registers rf))))))

  (testing "new-register-file for x86"
    (let [rf (new-register-file :x86)]
      (is (instance? isa_chip_sim.register.RegisterFile rf))
      (is (= (:arch rf) x86-arch))
      (is (= (count (:registers rf)) (count (:registers x86-arch))))
      (is (every? zero? (vals (:registers rf))))))

  (testing "read-reg and write-reg for ARM"
    (let [rf (new-register-file :arm)
          updated-rf (write-reg rf :r0 123)]
      (is (= 123 (read-reg updated-rf :r0)))))

  (testing "read-reg and write-reg for x86"
    (let [rf (new-register-file :x86)
          updated-rf (write-reg rf :eax 456)]
      (is (= 456 (read-reg updated-rf :eax))))))

(deftest memory-test
  (testing "new-memory initializes correctly"
    (let [mem (new-memory)]
      (is (instance? isa_chip_sim.memory.Memory mem))
      (is (= (count (:text mem)) text-size))
      (is (= (count (:bss mem)) bss-size))
      (is (= (count (:heap mem)) heap-size))
      (is (= (count (:stack mem)) stack-size))
      (is (every? zero? (:text mem)))))

  (testing "read-mem and write-mem in text segment"
    (let [mem (new-memory)
          updated-mem (write-mem mem text-base [1 2 3] 3)]
      (is (= [1 2 3] (read-mem updated-mem text-base 3)))))

  (testing "read-mem and write-mem in bss segment"
    (let [mem (new-memory)
          updated-mem (write-mem mem bss-base [4 5 6] 3)]
      (is (= [4 5 6] (read-mem updated-mem bss-base 3)))))

  (testing "read-mem and write-mem in heap segment"
    (let [mem (new-memory)
          updated-mem (write-mem mem heap-base [7 8 9] 3)]
      (is (= [7 8 9] (read-mem updated-mem heap-base 3)))))

  (testing "read-mem and write-mem in stack segment"
    (let [mem (new-memory)
          ; Stack grows downwards, so address 0x4000 is the top of the stack
          ; Writing [10 11 12] at 0x3ffc (stack-base - 4) should put it at the end of the stack vector
          updated-mem (write-mem mem (- stack-base 3) [10 11 12] 3)]
      (is (= [10 11 12] (read-mem updated-mem (- stack-base 3) 3)))))

  (testing "write-mem at offset and read multiple bytes"
    (let [mem (new-memory)
          offset (+ text-base 5)
          data [10 20 30 40]
          updated-mem (write-mem mem offset data (count data))]
      (is (= data (read-mem updated-mem offset (count data))))))

  (testing "read-mem and write-mem across segment boundaries (should throw error)"
    (let [mem (new-memory)]
      (is (thrown? clojure.lang.ExceptionInfo (read-mem mem (- bss-base 1) 2))))
    (let [mem (new-memory)]
      (is (thrown? clojure.lang.ExceptionInfo (write-mem mem (- bss-base 1) [1] 2))))))


