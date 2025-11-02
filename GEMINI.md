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

**design section
*	 using openjfx for our UI
*	 the ui will be multi-screen
*	 the pipeline display will be the centerpiece 
*	 functional units activity will be displayed
*	 the text_segment will contain ISA and orginal ARM or intel based on user keybinding
*
***Declarative UI:
cljfx allows you to define your UI using Clojure data structures, which are then
diffed and applied to the JavaFX scene graph. This declarative approach simplifies managing
complex UI states for the simulator.
***State Management:
Leverage Clojure's immutable data structures and atoms for managing the simulator's
state (register values, memory, pipeline stages). cljfx integrates well with these,
automatically re-rendering parts of the UI when relevant data changes.
***Hot-Reloading:
Clojure's excellent REPL-driven development and cljfx's design often allow for hot-reloading
UI components, significantly speeding up development iterations.
JavaFX's strengths in custom graphics and data visualization are highly beneficial for an ISA chip simulator:

***Custom Controls:
Create custom controls to visually represent pipeline stages, registers, and memory blocks
with specific styling and interaction.
***Animations:
Animate instructions moving through pipeline stages or highlight changes in register values.
***Charts and Graphs:
Integrate existing JavaFX charting libraries or custom drawing to display performance
statistics (IPC, CPI) and memory access patterns.
***Theming:
JavaFX supports CSS for styling, allowing you to create a visually appealing and consistent theme for your simulator.
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
