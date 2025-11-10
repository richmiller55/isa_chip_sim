# GEMINI.md - isa_chip_sim

## Project Overview

This project is an ISA (Instruction Set Architecture) chip simulator written in Clojure. It is in the early stages of development, but the core components suggest a sophisticated design.

The simulator models a classic 5-stage RISC pipeline: Fetch, Decode, Execute, Memory, and Write Back. A key feature is its apparent design to support multiple ISAs (e.g., ARM, x86) by translating them into a common Intermediate Representation (IR) before execution.

The state of the simulation, including registers, memory, and pipeline status, is managed in a central data structure.

## Building and Running

This project uses Leiningen for dependency management and building.

### Building

To build a standalone JAR file, run:

```sh
lein uberjar
```

This will create a file in the `target/uberjar/` directory.

### Running the Simulator

To run the compiled JAR:

```sh
java -jar target/uberjar/isa_chip_sim-0.1.0-SNAPSHOT-standalone.jar
```
### verification of assembly test code for correctness

to build a x86 assembly file
x86_prime_numbers.asm
'''sh
nasm -f elf resources/asm_source/x86_prime_number.asm
'''

to link assembly object

'''sh
ld -m elf_i386 -s -o resources/asm_source/x86_prime_number resources/asm_source/x86_prime_number.o
'''


**design section
*	 the ui will be multi-screen
*	 the pipeline display will be the centerpiece 
*	 functional units activity will be displayed
*	 the text_segment will contain ISA and orginal ARM or intel based on user keybinding
###This Clojure code implements a classic five-stage pipelined CPU simulator
*	(Fetch, Decode, Execute, Memory, Write-back).
*	It models data hazards, control hazards (branch prediction/handling),
*	and resource management using a scoreboard.

###1. Overall Structure and Design
*     **The code uses Clojure's strengths:**
*      immutability, maps, and functional updates (assoc, update, cond->, case).
*     **State Management:** The entire simulation state is held within a single,
large, immutable map. The run-cycle function takes the current state map
and returns a new state map representing the result of one clock cycle.
This makes the simulation predictable and easier to debug.
*    **Pipelining:** The run-cycle function orchestrates the stages
in reverse order (WB -> Mem -> Ex -> ID -> IF) to avoid using
the "same" clock cycle's modified data in an earlier stage.

*   **Modularity:** It requires several other namespaces
(scoreboard, functional-units, register, memory),
suggesting a well-structured project.
###2. Key Components and Their Roles
###Data Structures
*	**Instruction record:** A simple representation of an instruction
with an opcode, operands, and metadata
(likely where the destination register is stored).
*	**Global State Map:**
* This map implicitly holds everything:
* :registers: Current CPU registers (using the isa-chip-sim.register API).
* :memory: The main memory (using the isa-chip-sim.memory API).
* :program: A map of PC address to Instruction object.
* :scoreboard: Resource management/hazard tracking.
* :if-id-latch, :id-ex-latch, :ex-mem-latch, :mem-wb-latch:
* The actual "pipes" holding instructions between stages.
* :pipeline-stall, :branch-just-taken, :clock: Control flags and metrics.
* **Pipelined Stages (Functions):**
* The core logic is in the defn- (private functions) for each stage:
* write-back (WB):
* Reads from :mem-wb-latch.
* Action: Writes the final :result to the destination register (:registers).
* Resource Management: Clears the corresponding functional unit in the :scoreboard.
* **memory-access (Mem):**
* Reads from :ex-mem-latch.
* Action: Handles :load and :store operations using the mem namespace API.
Stores update :memory, loads fetch data and append it to the
instruction's :result field for the WB stage.
execute (Ex):
Reads from :id-ex-latch.
*     **Data Forwarding/Hazard Avoidance:** This is a critical piece (forward-value).
It checks the next latches (Ex/Mem and Mem/WB) for a result before reading from
the actual register file, implementing classic CPU forwarding logic.
*   **Functional Units:** Uses the fu namespace to calculate the result
based on the instruction type.
*     **Control Hazard Handling (Branches):** This logic is complex.
If a branch is taken, it flushes the subsequent pipeline stages
(:if-id-latch, :id-ex-latch) and updates the PC register immediately.
It sets a flag :branch-just-taken to manage fetching the correct next
instruction in the subsequent fetch stage.
*	    **decode (ID):**
Reads from :if-id-latch.
*     **Data Hazard/Structural Hazard Check:** This is where the
scoreboard/issue-instruction check happens.
*     **Stalling:** If the scoreboard says the
instruction cannot issue (due to a structural hazard or waiting for
a data dependency), it sets :pipeline-stall true.
###fetch (IF)
*	 **Stall Handling:** Only runs if :pipeline-stall is false.
Action: Reads the instruction at the current :pc from the :program map.
PC Update: Increments the PC for the next fetch.

*  **Branch Handling:** Uses the :branch-just-taken flag to decide if it
should behave normally or if it needs to resume fetching after a jump.
###3. Areas for Improvement/Potential Issues
*     **forward-value Implementation:** While it handles forwarding from the
next two stages, a real out-of-order execution engine would need a much more
robust mechanism (e.g., a Reorder Buffer or Reservation Stations) to track
where every pending result is coming from. This is a simple in-order
forwarding mechanism.
*    **Branch Prediction:** The current system uses a very simple form of
branch resolution in the execute stage. This results in a 3-cycle penalty
(IF, ID, EX cycles wasted) whenever a branch is
taken. A real CPU would use a Branch Target Buffer
(BTB) for speculative execution.
*     **Memory Abstraction:** The mem/read-mem returns a collection/sequence
((mem/read-mem ...)), and the code immediately takes the
first element ((first (mem/read-mem ...))). This works for single values
but might be limiting for multi-word access instructions.
Instruction Representation: The Instruction record is simple,
but the dependence on metadata for things like write-reg means
the functions rely heavily on implicit knowledge of how instructions
are constructed.*
***State Management:
Leverage Clojure's immutable data structures and atoms for managing the simulator's
state (register values, memory, pipeline stages). cljfx integrates well with these,
automatically re-rendering parts of the UI when relevant data changes.

***Custom Controls:
Create custom controls to visually represent pipeline stages, registers, and memory blocks
with specific styling and interaction.
***Animations:
Animate instructions moving through pipeline stages or highlight changes in register values.
 * Multi-screen Layout: A multi-screen or multi-pane approach is ideal. Consider a main window with the
     pipeline visualization as the focus. You could then have dockable or separate windows for registers,
     memory, and the code view (showing both the original ISA and the translated IR). This would allow users to
     customize their workspace.

   * Interactive Pipeline: Making the pipeline display the centerpiece is the right call. To make it even more
     effective, consider adding interactivity. For example, hovering over an instruction in a pipeline stage
     could display a tooltip with its current state. Clicking on it could freeze the simulation and highlight
     the corresponding functional units, registers, and memory locations being accessed.

   * Dynamic Visualizations: Animations will be key to making the simulator feel alive. You could use
     color-coding to indicate pipeline stalls, data hazards, and control hazards. For functional units, you
     could visually show which instruction is currently being executed by each unit.

   * Data-Rich Code View: The ability to switch between the source ISA and the internal IR is a fantastic
     feature for debugging and understanding the translation process. Adding syntax highlighting for both would
     be a great touch. You could also highlight the current instruction being fetched in the code view.

   * Performance Dashboards: The idea of using charts is excellent. You could create a "performance dashboard"
     that visualizes key metrics in real-time, such as:
       * Instructions Per Cycle (IPC)
       * Cache hit/miss rates
       * Branch prediction accuracy
       * Resource utilization of functional units

   * Simulation Control: A crucial part of the UI will be the simulation controls. Standard controls like run,
     pause, step-by-step (forward and backward, if possible), and reset are essential for debugging and
     educational purposes.

### Running Tests

To run the test suite:

```sh
lein test
```

## Development Conventions

*   **Language:** Clojure
*   **Core Libraries:** Clojure 1.12.2
*   **Project Structure:** The code is organized into namespaces based on the simulator's components:
    *   `core.clj`: Main entry point of the application.
    *   `top.clj`: Defines the top-level data structure for the simulator state.
    *   `IR_translate.clj`: Handles the translation of different ISAs to the internal IR.
    *   `execution_loop.clj`: Contains the main simulation loop and pipeline logic.
    *   `memory.clj`: Manages memory operations.
    *   `register.clj`: Manages register operations.
*   **Testing:** Tests are located in the `test` directory and can be run with `lein test`.
*   **State Management:** The simulator is designed in a functional style, with the simulation state
passed as an argument to functions that return a new, updated state.
