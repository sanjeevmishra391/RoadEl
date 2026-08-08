Synchronization in Java is the capability to control the access of multiple threads to any shared resource.

## Why use Synchronization?
The synchronization is mainly used to

1. To prevent thread interference.
2. To prevent consistency problem.

## Types of Synchronization
There are two types of synchronization

1. Process Synchronization
2. Thread Synchronization

## Thread Synchronization
There are two types of thread synchronization: Mutual Exclusive and Inter-thread communication.

1. Mutual Exclusive: helps keep threads from interfering with one another while sharing data.  
    a. Synchronized method.  
    b. Synchronized block.  
    c. Static synchronization.
2. Cooperation (Inter-thread communication in java)

## Concept of Lock in Java
Synchronization is built around an internal entity known as the lock or monitor. Every object has a lock associated with it. By convention, a thread that needs consistent access to an object's fields has to acquire the object's lock before accessing them, and then release the lock when it's done with them.

[Without Synchronization](./WithoutSynchronization.java)  
[Synchronization](./Synchronization.java)

## Synchronized Block in Java

Synchronized block can be used to perform synchronization on any specific resource of the method.  
Suppose we have 50 lines of code in our method, but we want to synchronize only 5 lines, in such cases, we can use synchronized block.

- Synchronized block is used to lock an object for any shared resource.
- Scope of synchronized block is smaller than the method.
- A Java synchronized block doesn't allow more than one JVM, to provide access control to a shared resource.
- The system performance may degrade because of the slower working of synchronized keyword.
- Java synchronized block is more efficient than Java synchronized method.

**Syntax:**
```java
synchronized (object reference expression) {
    // code block
}
```

## Static Synchronization

[Example](./StaticSynchronization.java)

If you make any **static method as synchronized, the lock will be on the class not on object.**

Ref: https://www.javatpoint.com/static-synchronization-example


## Deadlock in Java
[Example](./Deadlock.java)

Deadlock in Java is a part of multithreading. Deadlock can occur in a situation when a thread is waiting for an object lock, that is acquired by another thread and second thread is waiting for an object lock that is acquired by first thread. Since, both threads are waiting for each other to release the lock, the condition is called deadlock.

## Inter-thread Communication in Java

[Example](./InterThreadCommunication.java)

Inter-thread communication or Co-operation is all about allowing synchronized threads to communicate with each other.

Cooperation (Inter-thread communication) is a mechanism in which a thread is paused running in its critical section and another thread is allowed to enter (or lock) in the same critical section to be executed.It is implemented by following methods of Object class:

1. ```wait()``` method  

    The ```wait()``` method causes current thread to release the lock and wait until either another thread invokes the ```notify()``` method or the ```notifyAll()``` method for this object, or a specified amount of time has elapsed.

    The current thread must own this object's monitor, so it must be called from the synchronized method only otherwise it will throw exception.

    **Syntax:**
    ```java
    // waits until notified
    public final void wait() throws InterruptedException
    // waits for specific time
    public final void wait(long timeout) throws InterruptedException
    ```

2. ```notify()``` method

    The notify() method wakes up a single thread that is waiting on this object's monitor. If any threads are waiting on this object, one of them is chosen to be awakened. The choice is arbitrary and occurs at the discretion of the implementation.

    **Syntax:**
    ```java
    public final void notify()
    ```

3. ```notifyAll()``` method

    Wakes up all threads that are waiting on this object's monitor.

    **Syntax:**
    ```java
    public final void notifyAll()
    ```

## Understanding the process of inter-thread communication

1. Thread enters `synchronized` block → acquires the object's lock (monitor).
2. Thread calls `wait()` → **releases the lock** and moves to WAITING state.
3. Another thread acquires the same lock, does its work, calls `notify()` or `notifyAll()`.
4. Waiting thread moves to BLOCKED state (competing to re-acquire the lock).
5. Waiting thread re-acquires the lock → returns from `wait()` → continues.

```java
// Classic producer-consumer with wait/notify
class Buffer {
    private final Queue<Integer> queue = new LinkedList<>();
    private final int capacity;

    synchronized void produce(int item) throws InterruptedException {
        while (queue.size() == capacity) {  // WHILE not IF — see below
            wait();                          // releases lock, waits for space
        }
        queue.add(item);
        notifyAll();                         // wake consumers
    }

    synchronized int consume() throws InterruptedException {
        while (queue.isEmpty()) {            // WHILE not IF
            wait();                          // releases lock, waits for item
        }
        int item = queue.poll();
        notifyAll();                         // wake producers
        return item;
    }
}
```

### Why `while` and not `if` before `wait()`

**Spurious wakeups** — a thread can wake up from `wait()` without being notified. This is allowed by the Java specification (and happens on some JVMs/OS implementations). If you use `if`, the thread skips re-checking the condition and proceeds on a false assumption:

```java
// WRONG — if condition was true when we called wait(),
// but a spurious wakeup fires, we proceed with empty queue → crash
if (queue.isEmpty()) {
    wait();
}
int item = queue.poll();   // queue might still be empty!

// CORRECT — always re-check after waking
while (queue.isEmpty()) {
    wait();   // if spurious wakeup: loop back, check again, sleep again
}
int item = queue.poll();   // guaranteed: queue is not empty
```

Also, with `notifyAll()`, multiple threads wake up but only one can proceed (only one has the item). The `while` loop puts the others back to sleep.

### `notify()` vs `notifyAll()`

```java
notify();     // wakes ONE arbitrary waiting thread — risk: wrong thread wakes up
notifyAll();  // wakes ALL waiting threads — all compete for lock, only one proceeds
```

**Prefer `notifyAll()`** unless you are certain only one waiting thread should ever wake, and all waiting threads are identical. `notify()` with multiple consumer types can cause one type to permanently hog the wakeups while the other starves.

## Why `wait()`, `notify()`, `notifyAll()` are on `Object`, not `Thread`

Because the lock belongs to the **object** (the monitor), not to a specific thread. Any thread that enters `synchronized(obj)` acquires `obj`'s lock. `wait()` says "release *this object's* lock and sleep." It's the object being waited on that matters, not which thread. Every object having these methods lets any object be used as a condition variable.

## Difference between ```wait()``` and ```sleep()``` method
| ```wait()``` | ```sleep()``` | 
| ------------ | ------------- |
| The ```wait()``` method releases the lock. | The ```sleep()``` method doesn't release the lock.|
| It is a method of ```Object``` class. | It is a method of ```Thread``` class. | 
| It is the non-static method. | It is the static method. | 
| It should be notified by ```notify()``` or ```notifyAll()``` methods. | After the specified amount of time, sleep is completed. | 

## Interrupting a Thread

If any thread is in sleeping or waiting state (i.e. sleep() or wait() is invoked), calling the interrupt() method on the thread, breaks out the sleeping or waiting state throwing InterruptedException. If the thread is not in the sleeping or waiting state, calling the interrupt() method performs normal behaviour and doesn't interrupt the thread but sets the interrupt flag to true.

The 3 methods provided by the Thread class for interrupting a thread:

```java
public void interrupt()
public static boolean interrupted()
public boolean isInterrupted()
```

### Example of interrupting a thread that stops working
In this example, after interrupting the thread, we are propagating it, so it will stop working. If we don't want to stop the thread, we can handle it where sleep() or wait() method is invoked. Let's first see the example where we are propagating the exception.

[InterruptingThread](./InterruptingThread.java)

### Example of interrupting a thread that doesn't stop working
In this example, after interrupting the thread, we handle the exception, so it will break out the sleeping but will not stop working.

[InterruptingThreadRunning](./InterruptingThreadRunning.java)

### Example of interrupting thread that behaves normally

If thread is not in sleeping or waiting state, calling the interrupt() method sets the interrupted flag to true that can be used to stop the thread by the java programmer later.

## ```isInterrupted``` and ```interrupted``` method

The isInterrupted() method returns the interrupted flag either true or false. The static interrupted() method returns the interrupted flag after that it sets the flag to false if it is true.

[InterruptedMethod](./InterruptedMethod.java)

## Common Interview Questions

**Q: What is synchronization and why is it needed?**
A: Synchronization controls access to shared resources by multiple threads. Without it, two threads can interleave reads and writes, causing race conditions — corrupted data, lost updates, or inconsistent state. Java synchronization uses intrinsic locks (monitors): only one thread holds the lock at a time, and all writes made while holding the lock are visible to the next thread that acquires it.

**Q: What is the difference between a synchronized method and a synchronized block?**
A: A synchronized method locks `this` (or the Class object for static methods) for its entire duration. A synchronized block locks a specified object for only the enclosed statements. Synchronized blocks are preferred — smaller critical sections mean less contention and better throughput.

**Q: What is a deadlock? Write a minimal example.**
A: Deadlock is when two threads each hold a lock and wait for the other's lock — circular dependency, no progress forever.
```java
synchronized (lockA) {          // Thread 1 holds A, waits for B
    synchronized (lockB) { }    // Thread 2 holds B, waits for A → deadlock
}
```
Prevention: always acquire locks in the same order everywhere in the codebase.

**Q: What is the difference between `wait()` and `sleep()`?**
A: `wait()` releases the lock and moves the thread to WAITING — another thread must call `notify()` to wake it. `sleep()` pauses the thread for a duration but **does not release any locks**. `wait()` is for inter-thread coordination; `sleep()` is for introducing a delay.

**Q: Why must `wait()`, `notify()`, and `notifyAll()` be called from a synchronized block?**
A: Because they operate on the object's monitor. If a thread calls `wait()` without holding the lock, the JVM throws `IllegalMonitorStateException`. The lock is required so the check-then-wait sequence is atomic — otherwise another thread could call `notify()` between your check and your `wait()`, and you'd miss the notification.

**Q: Why should you use `while` instead of `if` before `wait()`?**
A: Two reasons: (1) **Spurious wakeups** — a thread can wake from `wait()` without being notified; re-checking the condition in a loop handles this safely. (2) **notifyAll()** wakes all waiting threads, but the condition may only be true for one of them — the others must go back to sleep.

**Q: What is static synchronization? How does it differ from instance synchronization?**
A: Instance synchronization locks the instance (`this`) — two threads on different instances don't block each other. Static synchronization locks the `Class` object — only one thread across all instances can execute that method at a time. Use static sync when protecting class-level (static) state.

**Q: What is the difference between `notify()` and `notifyAll()`?**
A: `notify()` wakes one arbitrary waiting thread. `notifyAll()` wakes all waiting threads — they all compete for the lock, one proceeds, the rest check their condition and may go back to sleep. Prefer `notifyAll()` unless you are certain all waiting threads are interchangeable and exactly one should proceed.


