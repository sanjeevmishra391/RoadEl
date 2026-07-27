# Command Pattern

## Intent

Encapsulate a request as an object, thereby letting you parameterize clients with different requests, queue or log requests, and support undoable operations.

---

## Problem

Without the Command pattern:
- Undo/redo is difficult — you have no record of what operations were performed or how to reverse them.
- Request queuing is impossible — you cannot store a "call `delete(text)`" for later execution.
- Operation logging requires scattered logging calls across many methods.
- Macro operations (execute a sequence as one unit) require custom, tightly coupled code.

The root cause is that operations are expressed as **method calls**, which are transient and carry no state once they return.

---

## Structure

```
+-------------+          +-------------------+
|   Client    |--------->|  <<interface>>    |
+-------------+          |     Command       |
      |                  |-------------------|
      |                  | + execute()       |
      |                  | + undo()          |
      v                  +-------------------+
+------------+                    ^
|  Invoker   |                    |
|------------|         +----------+----------+
| - history  |         |                     |
| + execute()|  +------+-------+  +----------+------+
| + undo()   |  | TypeTextCmd  |  | DeleteTextCmd   |
+------------+  |--------------|  |-----------------|
      |         | - receiver   |  | - receiver      |
      |         | - text       |  | - deletedText   |
      |         | + execute()  |  | + execute()     |
      v         | + undo()     |  | + undo()        |
+------------+  +--------------+  +-----------------+
|  Receiver  |
|  (TextDoc) |
|------------|
| + type()   |
| + delete() |
+------------+
```

---

## Implementation

### Domain: Text Editor with Undo/Redo

```java
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.List;
import java.util.ArrayList;

// ---------------------------------------------------------------
// Command interface
// ---------------------------------------------------------------
public interface Command {
    void execute();
    void undo();
}

// ---------------------------------------------------------------
// Receiver — the object that knows how to perform the actual work
// ---------------------------------------------------------------
public class TextDocument {

    private final StringBuilder content = new StringBuilder();

    public void type(String text) {
        content.append(text);
    }

    public void delete(int length) {
        int start = Math.max(0, content.length() - length);
        content.delete(start, content.length());
    }

    // Insert at specific position (used by undo of delete)
    public void insert(int position, String text) {
        content.insert(position, text);
    }

    public String getContent() {
        return content.toString();
    }

    public int length() {
        return content.length();
    }
}

// ---------------------------------------------------------------
// ConcreteCommand 1 — TypeTextCommand
// ---------------------------------------------------------------
public class TypeTextCommand implements Command {

    private final TextDocument document;
    private final String text;
    private int insertionPosition; // recorded at execute time for undo

    public TypeTextCommand(TextDocument document, String text) {
        this.document = document;
        this.text = text;
    }

    @Override
    public void execute() {
        insertionPosition = document.length();
        document.type(text);
        System.out.println("Execute: Typed \"" + text + "\"");
    }

    @Override
    public void undo() {
        document.delete(text.length());
        System.out.println("Undo:    Removed \"" + text + "\"");
    }
}

// ---------------------------------------------------------------
// ConcreteCommand 2 — DeleteTextCommand
// ---------------------------------------------------------------
public class DeleteTextCommand implements Command {

    private final TextDocument document;
    private final int length;
    private String deletedText; // captured during execute for undo
    private int deletionPosition;

    public DeleteTextCommand(TextDocument document, int length) {
        this.document = document;
        this.length = length;
    }

    @Override
    public void execute() {
        String content = document.getContent();
        int start = Math.max(0, content.length() - length);
        deletedText = content.substring(start);
        deletionPosition = start;
        document.delete(length);
        System.out.println("Execute: Deleted \"" + deletedText + "\"");
    }

    @Override
    public void undo() {
        document.insert(deletionPosition, deletedText);
        System.out.println("Undo:    Restored \"" + deletedText + "\"");
    }
}

// ---------------------------------------------------------------
// MacroCommand (Composite Command)
// Executes multiple commands as a single atomic unit.
// Undo reverses them in reverse order.
// ---------------------------------------------------------------
public class MacroCommand implements Command {

    private final List<Command> commands;

    public MacroCommand(List<Command> commands) {
        this.commands = new ArrayList<>(commands);
    }

    @Override
    public void execute() {
        System.out.println("MacroCommand: Executing " + commands.size() + " commands");
        for (Command command : commands) {
            command.execute();
        }
    }

    @Override
    public void undo() {
        System.out.println("MacroCommand: Undoing " + commands.size() + " commands (reverse)");
        // Undo in reverse order
        for (int i = commands.size() - 1; i >= 0; i--) {
            commands.get(i).undo();
        }
    }
}

// ---------------------------------------------------------------
// Invoker — CommandHistory
// Manages undo/redo stacks.
// ---------------------------------------------------------------
public class CommandHistory {

    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();

    public void execute(Command command) {
        command.execute();
        undoStack.push(command);
        // Any new command clears the redo stack (standard editor behavior)
        redoStack.clear();
    }

    public void undo() {
        if (undoStack.isEmpty()) {
            System.out.println("Nothing to undo.");
            return;
        }
        Command command = undoStack.pop();
        command.undo();
        redoStack.push(command);
    }

    public void redo() {
        if (redoStack.isEmpty()) {
            System.out.println("Nothing to redo.");
            return;
        }
        Command command = redoStack.pop();
        command.execute();
        undoStack.push(command);
    }

    public int undoDepth()  { return undoStack.size(); }
    public int redoDepth()  { return redoStack.size(); }
}

// ---------------------------------------------------------------
// Request Queuing Use Case
// Commands are placed on a BlockingQueue and consumed by a
// background worker thread. The producer never knows when or
// by whom the command is executed.
// ---------------------------------------------------------------
public class CommandQueue {

    private final BlockingQueue<Command> queue = new LinkedBlockingQueue<>();
    private volatile boolean running = true;

    // Producer: submits commands for async execution
    public void submit(Command command) throws InterruptedException {
        queue.put(command);
    }

    // Consumer: background worker drains the queue
    public void startWorker() {
        Thread worker = new Thread(() -> {
            while (running || !queue.isEmpty()) {
                try {
                    Command command = queue.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS);
                    if (command != null) {
                        command.execute();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "command-worker");
        worker.setDaemon(true);
        worker.start();
    }

    public void shutdown() { running = false; }
}

// ---------------------------------------------------------------
// Client / Demo
// ---------------------------------------------------------------
public class CommandDemo {

    public static void main(String[] args) {
        TextDocument document = new TextDocument();
        CommandHistory history = new CommandHistory();

        System.out.println("=== Text Editor with Undo/Redo ===\n");

        // Execute commands
        history.execute(new TypeTextCommand(document, "Hello"));
        history.execute(new TypeTextCommand(document, ", World"));
        System.out.println("Document: \"" + document.getContent() + "\"\n");

        history.execute(new DeleteTextCommand(document, 6)); // delete ", World"
        System.out.println("Document: \"" + document.getContent() + "\"\n");

        // Undo delete
        history.undo();
        System.out.println("After undo: \"" + document.getContent() + "\"\n");

        // Undo type ", World"
        history.undo();
        System.out.println("After undo: \"" + document.getContent() + "\"\n");

        // Redo type ", World"
        history.redo();
        System.out.println("After redo: \"" + document.getContent() + "\"\n");

        System.out.println("=== Macro Command Demo ===\n");

        TextDocument doc2 = new TextDocument();
        CommandHistory history2 = new CommandHistory();

        List<Command> macroSteps = List.of(
                new TypeTextCommand(doc2, "Dear User,\n"),
                new TypeTextCommand(doc2, "Your order has shipped.\n"),
                new TypeTextCommand(doc2, "Regards, Team")
        );

        MacroCommand insertTemplate = new MacroCommand(macroSteps);
        history2.execute(insertTemplate);
        System.out.println("Document after macro:\n" + doc2.getContent());

        System.out.println("\nUndo entire macro:");
        history2.undo();
        System.out.println("Document after macro undo: \"" + doc2.getContent() + "\"");
    }
}
```

**Expected output:**
```
=== Text Editor with Undo/Redo ===

Execute: Typed "Hello"
Execute: Typed ", World"
Document: "Hello, World"

Execute: Deleted ", World"
Document: "Hello"

Undo:    Restored ", World"
After undo: "Hello, World"

Undo:    Removed ", World"
After undo: "Hello"

Execute: Typed ", World"
After redo: "Hello, World"

=== Macro Command Demo ===

MacroCommand: Executing 3 commands
Execute: Typed "Dear User,\n"
Execute: Typed "Your order has shipped.\n"
Execute: Typed "Regards, Team"
Document after macro:
Dear User,
Your order has shipped.
Regards, Team

Undo entire macro:
MacroCommand: Undoing 3 commands (reverse)
Undo:    Removed "Regards, Team"
Undo:    Removed "Your order has shipped.\n"
Undo:    Removed "Dear User,\n"
Document after macro undo: ""
```

---

## When to Use

- When you need **undo/redo** functionality — encapsulating operations as objects lets you store them on a stack and reverse them on demand.
- When you want to **queue or schedule operations** for later execution (job queues, background task systems, thread pools).
- When you need **operation logging / auditing** — serializable command objects can be written to a log file and replayed.
- When you want to support **macro commands** — composite sequences that execute as one unit and can be undone atomically.

## When NOT to Use

- When operations are simple and undo is not required — the extra indirection adds boilerplate without benefit.
- When the system processes high volumes of fine-grained operations and per-command object creation becomes a memory concern (consider flyweight or batching).
- When command objects would need to capture so much shared mutable state that reasoning about undo becomes harder than a simpler approach.

---

## Variants

### 1. Macro Command (Composite Command)

A `MacroCommand` holds a list of commands and delegates `execute()` / `undo()` to each. Enables recording a user session as a replayable script.

### 2. Command Queue

A `BlockingQueue<Command>` decouples submission from execution. Producers add commands; a pool of worker threads drains the queue. This is the basis for thread pools (`ExecutorService` accepts `Runnable`, which is a degenerate command without undo).

### 3. Transactional Commands

Each command writes to a redo log before executing. On failure, the log is replayed in reverse to roll back. This mirrors how database WAL (Write-Ahead Logging) works.

```java
public interface TransactionalCommand extends Command {
    void rollback();
}
```

---

## Real-World Usage

| API | Notes |
|---|---|
| `java.lang.Runnable` | A degenerate command with only `execute()` (the `run()` method). No receiver reference is required — the lambda captures its context. Used extensively with `ExecutorService`. |
| `javax.swing.Action` | Full Command implementation for Swing. Carries both the action logic and metadata (name, icon, enabled state) for binding to menu items and toolbar buttons. |
| `Spring @Transactional` | Conceptually wraps method calls in a command that knows how to commit or roll back, analogous to a transactional command. |
| `Spring Batch Step` | Each `Step` is effectively a command with execute, skip, and retry semantics, composable into a `Job` (macro command). |

---

## Interview Questions

**Q: How does the Command pattern enable undo/redo?**

Each command captures all state needed to reverse its effect at the moment `execute()` is called (e.g., `DeleteTextCommand` stores the deleted text and its position). The `Invoker` maintains two stacks: an undo stack and a redo stack. `execute()` pushes onto undo. `undo()` pops from undo, calls `command.undo()`, and pushes onto redo. `redo()` pops from redo, calls `command.execute()`, and pushes onto undo. Any new `execute()` clears the redo stack.

**Q: What is the relationship between Command and Runnable?**

`java.lang.Runnable` is a single-method interface (`run()`) that represents a unit of work with no arguments and no return value — a stripped-down command. The key difference is that `Runnable` has no `undo()` and typically captures its receiver via closure rather than holding an explicit reference. `ExecutorService` is the Invoker in this analogy.

**Q: How would you implement a command audit log?**

Before executing each command, serialize it (command type, parameters, timestamp, user ID) to a persistent store (database, append-only log file). On system recovery, replay the log by re-executing commands. This is identical to event sourcing at the application level.
