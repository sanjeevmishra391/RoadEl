# Multi Threading

## Index

- [Thread](#thread)
- [Java Thread](#what-is-thread-in-java)
- [Thread lifecycle](#life-cycle-of-a-thread-thread-states)
- [Thread state](#life-cycle-of-a-thread-thread-states)
- [Creating Thread in Java](#how-to-create-a-thread-in-java)
- [Thread Schedular](#thread-scheduler-in-java)
- [Sleep Method](#threadsleep)
- [Can We Start a Thread Twice?](#can-we-start-a-thread-twice)
- [What if we call java run method directly instead start method?](#what-if-we-call-java-run-method-directly-instead-start-method)
- [Join Method](#java-join-method)
- [Priority](#priority)
- [Daemon Thread](#daemon-thread-in-java)
- [Thread Pool](#java-thread-pool)
- [Thread Group](#threadgroup-in-java)
- [Shutdown Hook](#shutdown-hook)
- [Garbage Collection](#java-garbage-collection)
- [Runtime Class](#java-runtime-class)
- [!Additional Important](#additional-important)
- [Mini Example](#mini-example)


## Thread
A thread is a lightweight sub-process, the smallest unit of processing. Multiprocessing and multithreading, both are used to achieve multitasking.

However, we use multithreading than multiprocessing because threads use a shared memory area. They don't allocate separate memory area so saves memory, and context-switching between the threads takes less time than process.

## Advantages of Java Multithreading

1. It doesn't block the user because threads are independent and you can perform multiple operations at the same time.  
2. You can perform many operations together, so it saves time.  
3. Threads are independent, so it doesn't affect other threads if an exception occurs in a single thread.

## Multitasking
Multitasking is a process of executing multiple tasks simultaneously. We use multitasking to utilize the CPU. Multitasking can be achieved in two ways:

### 1. Process-based Multitasking (Multiprocessing)

- Each process has an address in memory. In other words, each process allocates a separate memory area.
- A process is heavyweight.
- Cost of communication between the process is high.
- Switching from one process to another requires some time for saving and loading registers, memory maps, updating lists, etc.

### 2. Thread-based Multitasking (Multithreading)

- Threads share the same address space.
- A thread is lightweight.
- Cost of communication between the thread is low.

## What is Thread in java

A thread is a lightweight subprocess, the smallest unit of processing. It is a separate path of execution.

Threads are independent. If there occurs exception in one thread, it doesn't affect other threads. It uses a shared memory area.

There is context-switching between the threads. There can be multiple processes inside the OS, and one process can have multiple threads.

## Java Thread class

Java provides ```Thread``` class to achieve thread programming. Thread class provides constructors and methods to create and perform operations on a thread. Thread class extends Object class and implements Runnable interface.

## Life cycle of a Thread (Thread States)
![alt text](life-cycle-of-a-thread.png)  
In Java, a thread always exists in any one of the following states. These states are:

1. **New** : Whenever a new thread is created, it is always in the new state. For a thread in the new state, the code has not been run yet and thus has not begun its execution.

2. **Active** : When a thread invokes the start() method, it moves from the new state to the active state. The active state contains two states within it: one is runnable, and the other is running.  
    - **Runnable**: A thread, that is ready to run is then moved to the runnable state. In the runnable state, the thread may be running or may be ready to run at any given instant of time. It is the duty of the thread scheduler to provide the thread time to run, i.e., moving the thread the running state.  

        A program implementing multithreading acquires a fixed slice of time to each individual thread. Each and every thread runs for a short span of time and when that allocated time slice is over, the thread voluntarily gives up the CPU to the other thread, so that the other threads can also run for their slice of time. Whenever such a scenario occurs, all those threads that are willing to run, waiting for their turn to run, lie in the runnable state. In the runnable state, there is a queue where the threads lie.

    - **Running**: When the thread gets the CPU, it moves from the runnable to the running state. Generally, the most common change in the state of a thread is from runnable to running and again back to runnable.

3. **Blocked / Waiting** : Whenever a thread is inactive for a span of time (not permanently) then, either the thread is in the blocked state or is in the waiting state.
    > For example, a thread (let's say its name is *A*) may want to print some data from the printer. However, at the same time, the other thread (let's say its name is *B*) is using the printer to print some data. Therefore, thread *A* has to wait for thread *B* to use the printer. Thus, thread *A* is in the blocked state. A thread in the blocked state is unable to perform any execution and thus never consume any cycle of the Central Processing Unit (CPU). Hence, we can say that thread *A* remains idle until the thread scheduler reactivates thread *A*, which is in the waiting or blocked state.

    When the main thread invokes the join() method then, it is said that the main thread is in the waiting state. The main thread then waits for the child threads to complete their tasks. When the child threads complete their job, a notification is sent to the main thread, which again moves the thread from waiting to the active state.

    If there are a lot of threads in the waiting or blocked state, then it is the duty of the thread scheduler to determine which thread to choose and which one to reject, and the chosen thread is then given the opportunity to run.

4. **Timed Waiting** : Sometimes, waiting for leads to starvation. For example, a thread (its name is *A*) has entered the critical section of a code and is not willing to leave that critical section. In such a scenario, another thread (its name is *B*) has to wait forever, which leads to starvation. To avoid such scenario, a timed waiting state is given to thread *B*. Thus, thread lies in the waiting state for a specific span of time, and not forever. A real example of timed waiting is when we invoke the ```sleep()``` method on a specific thread. The ```sleep()``` method puts the thread in the timed wait state. After the time runs out, the thread wakes up and start its execution from when it has left earlier.

5. **Terminated** : A terminated thread means the thread is no more in the system. In other words, the thread is dead, and there is no way one can respawn (active after kill) the dead thread.  

    A thread reaches the termination state because of the following reasons:
    - When a thread has finished its job, then it exists or terminates normally.  
    - **Abnormal termination**: It occurs when some unusual events such as an unhandled exception or segmentation fault.


### Implementation of Thread States

In Java, one can get the current state of a thread using the ```Thread.getState()``` method. The ```java.lang.Thread.State``` class of Java provides the constants ENUM to represent the state of a thread. These constants are:

```java
public static final Thread.State NEW
```
It represents the first state of a thread that is the NEW state.

```java
public static final Thread.State RUNNABLE  
```
It represents the runnable state. It means a thread is waiting in the queue to run.

```java
public static final Thread.State BLOCKED  
```
It represents the blocked state. In this state, the thread is waiting to acquire a lock.

```java
public static final Thread.State WAITING  
```
It represents the waiting state. A thread will go to this state when it invokes the Object.wait() method, or Thread.join() method with no timeout. A thread in the waiting state is waiting for another thread to complete its task.

```java
public static final Thread.State TIMED_WAITING  
```
It represents the timed waiting state.
> The main difference between waiting and timed waiting is the time constraint. Waiting has no time constraint, whereas timed waiting has the time constraint. A thread invoking the following method reaches the timed waiting state.  
    - sleep  
    - join with timeout  
    - wait with timeout  
    - parkUntil  
    - parkNanos  

```java
public static final Thread.State TERMINATED
```
It represents the final state of a thread that is terminated or dead. A terminated thread means it has completed its execution.


## How to create a thread in Java

There are two ways to create a thread:

1. By extending Thread class [↗︎](./UsingThreadClass.java)
2. By implementing Runnable interface. [↗︎](./UsingRunnableInterface.java)

### Thread class:
Thread class provide constructors and methods to create and perform operations on a thread. Thread class extends Object class and implements Runnable interface.

Commonly used Constructors of Thread class:
- Thread()
- Thread(String name)
- Thread(Runnable r)
- Thread(Runnable r,String name)

Commonly used methods of Thread class:
- ```public void run()```: is used to perform action for a thread.
- ```public void start()```: starts the execution of the thread.JVM calls the run() method on the thread.
- ```public void sleep(long miliseconds)```: Causes the currently executing thread to sleep (temporarily cease execution) for the specified number of milliseconds.
- ```public void join()```: waits for a thread to die.
- ```public void join(long miliseconds)```: waits for a thread to die for the specified miliseconds.
- ```public int getPriority()```: returns the priority of the thread.
- ```public int setPriority(int priority)```: changes the priority of the thread.
- ```public String getName()```: returns the name of the thread.
- ```public void setName(String name)```: changes the name of the thread.
- ```public Thread currentThread()```: returns the reference of currently executing thread.
- ```public int getId()```: returns the id of the thread.
- ```public Thread.State getState()```: returns the state of the thread.
- ```public boolean isAlive()```: tests if the thread is alive.
- ```public void yield()```: causes the currently executing thread object to temporarily pause and allow other threads to execute.
- ```public void suspend()```: is used to suspend the thread(depricated).
- ```public void resume()```: is used to resume the suspended thread(depricated).
- ```public void stop()```: is used to stop the thread(depricated).
- ```public boolean isDaemon()```: tests if the thread is a daemon thread.
- ```public void setDaemon(boolean b)```: marks the thread as daemon or user thread.
- ```public void interrupt()```: interrupts the thread.
- ```public boolean isInterrupted()```: tests if the thread has been interrupted.
- ```public static boolean interrupted()```: tests if the current thread has been interrupted.

### Runnable interface:
The Runnable interface should be implemented by any class whose instances are intended to be executed by a thread. Runnable interface have only one method named run().

**public void run()**: is used to perform action for a thread.

### Starting a thread:
The ```start()``` method of Thread class is used to start a newly created thread. It performs the following tasks:

A new thread starts(with new callstack).
The thread moves from New state to the Runnable state.
When the thread gets a chance to execute, its target ```run()``` method will run.

## Thread Scheduler in Java

A component of Java that decides which thread to run or execute and which thread to wait is called a thread scheduler in Java. In Java, a thread is only chosen by a thread scheduler if it is in the runnable state. However, if there is more than one thread in the runnable state, it is up to the thread scheduler to pick one of the threads and ignore the other ones. There are some criteria that decide which thread will execute first. There are two factors for scheduling a thread i.e. **Priority** and **Time of arrival**.

**Priority**: Priority of each thread lies between 1 to 10. If a thread has a higher priority, it means that thread has got a better chance of getting picked up by the thread scheduler.

**Time of Arrival**: Suppose two threads of the same priority enter the runnable state, then priority cannot be the factor to pick a thread from these two threads. In such a case, arrival time of thread is considered by the thread scheduler. A thread that arrived first gets the preference over the other threads.

### Thread Scheduler Algorithms
On the basis of the above-mentioned factors, the scheduling algorithm is followed by a Java thread scheduler.

1. **First Come First Serve Scheduling**:
In this scheduling algorithm, the scheduler picks the threads thar arrive first in the runnable queue.

2. **Time-slicing scheduling**:
Usually, the First Come First Serve algorithm is non-preemptive, which is bad as it may lead to infinite blocking (also known as starvation). To avoid that, some time-slices are provided to the threads so that after some time, the running thread has to give up the CPU. Thus, the other waiting threads also get time to run their job.

3. **Preemptive-Priority Scheduling**:
Suppose there are multiple threads available in the runnable state. The thread scheduler picks that thread that has the highest priority. Since the algorithm is also preemptive, therefore, time slices are also provided to the threads to avoid starvation. Thus, after some time, even if the highest priority thread has not completed its job, it has to release the CPU because of preemption.

## Thread.sleep()

```java
public static void sleep(long mls) throws InterruptedException   
public static void sleep(long mls, int n) throws InterruptedException   
```

### Points:
- The following are the parameters used in the sleep() method : 
    - mls: The time in milliseconds is represented by the parameter mls. The duration for which the thread will sleep is given by the method sleep().  
    - n: It shows the additional time up to which the programmer or developer wants the thread to be in the sleeping state. The range of n is from 0 to 999999.
- The method does not return anything.
- Whenever another thread does interruption while the current thread is already in the sleep mode, then the InterruptedException is thrown.
- Throws the exception IllegalArguementException when the time for sleeping is negative.

## Can we start a thread twice?
**No**. After starting a thread, it can never be started again. If you does so, an IllegalThreadStateException is thrown. In such case, thread will run once but for second time, it will throw exception.

## What if we call Java run() method directly instead start() method?
- Each thread starts in a separate call stack.
- Invoking the run() method from the main thread, the run() method goes onto the current call stack rather than at the beginning of a new call stack.

## Java join() method
[Example ↗](./ThreadJoin.java)

The ```join()``` method in Java is provided by the ```java.lang.Thread``` class that permits one thread to wait until the other thread to finish its execution. Suppose ```th``` be the object the class Thread whose thread is doing its execution currently, then the ```th.join();``` statement ensures that ```th``` is finished before the program does the execution of the next statement. When there are more than one thread invoking the ```join()``` method, then it leads to overloading on the ```join()``` method that permits the developer or programmer to mention the waiting period. However, similar to the ```sleep()``` method in Java, the ```join()``` method is also dependent on the operating system for the timing, so we should not assume that the ```join()``` method waits equal to the time we mention in the parameters. The following are the three overloaded ```join()``` methods.

### Description of The Overloaded join() Method
**join()**: When the ```join()``` method is invoked, the current thread stops its execution and the thread goes into the wait state. The current thread remains in the wait state until the thread on which the ```join()``` method is invoked has achieved its dead state. If interruption of the thread occurs, then it throws the InterruptedException.  

*Syntax*:
```java
public final void join() throws InterruptedException  
```

**join(long mls)**: When the ```join()``` method is invoked, the current thread stops its execution and the thread goes into the wait state. The current thread remains in the wait state until the thread on which the ```join()``` method is invoked called is dead or the wait for the specified time frame(in milliseconds) is over.  

*Syntax*:
```java
public final synchronized void join(long mls) throws InterruptedException // where mls is in milliseconds  
```

**join(long mls, int nanos)**: When the ```join()``` method is invoked, the current thread stops its execution and go into the wait state. The current thread remains in the wait state until the thread on which the join() method is invoked called is dead or the wait for the specified time frame(in milliseconds + nanos) is over.

*Syntax*:
```java
public final synchronized void join(long mls, int nanos) throws InterruptedException //where mls is in milliseconds.  
```

## Priority

3 constants defined in Thread class:
```java
public static int MIN_PRIORITY
public static int NORM_PRIORITY
public static int MAX_PRIORITY
```
> Default priority of a thread is 5 (NORM_PRIORITY). The value of MIN_PRIORITY is 1 and the value of MAX_PRIORITY is 10.

> If the value of the parameter newPriority of the method getPriority() goes out of the range (1 to 10), then we get the IllegalArgumentException.

## Daemon Thread in Java 
[Example ↗︎](./DaemonThread.java)

- It provides services to user threads for background supporting tasks. It has no role in life than to serve user threads.
- It is a low priority thread.
- There are many java daemon threads running automatically e.g. gc, finalizer etc.
- Its life depend on the mercy of user threads i.e. when all the user threads dies, JVM terminates this thread automatically.

#### Method provided by Thread class:
```java
public void setDaemon(boolean status)
```
This method is used to mark the current thread as daemon thread or user thread.

```java
public boolean isDaemon()
```
This method is used to check that current thread is daemon.

## Java Thread Pool 
[Example ↗︎](./ThreadPool.java)

Java Thread pool represents a group of worker threads that are waiting for the job and reused many times.

In the case of a thread pool, a group of fixed-size threads is created. A thread from the thread pool is pulled out and assigned a job by the service provider. After completion of the job, the thread is contained in the thread pool again.

```ExecutorService``` interface provides way to create Thread Pool. ``` Executors``` excute threads in thread pool.

### Thread Pool Methods
```newFixedThreadPool(int s)```: The method creates a thread pool of the fixed size s.

```newCachedThreadPool()```: The method creates a new thread pool that creates the new threads when needed but will still use the previously created thread whenever they are available to use.

```newSingleThreadExecutor()```: The method creates a new thread.

### Advantage of Java Thread Pool
Better performance It saves time because there is no need to create a new thread.

### Real time usage
It is used in Servlet and JSP where the container creates a thread pool to process the request.

> When one wants to execute 50 tasks but is not willing to create 50 threads. In such a case, one can create a pool of 10 threads. Thus, 10 out of 50 tasks are assigned, and the rest are put in the queue. Whenever any thread out of 10 threads becomes idle, it picks up the 11th task. The other pending tasks are treated the same way.

### Risks involved in Thread Pools
The following are the risk involved in the thread pools.

**Deadlock**: It is a known fact that deadlock can come in any program that involves multithreading, and a thread pool introduces another scenario of deadlock. Consider a scenario where all the threads that are executing are waiting for the results from the threads that are blocked and waiting in the queue because of the non-availability of threads for the execution.

**Thread Leakage**: Leakage of threads occurs when a thread is being removed from the pool to execute a task but is not returning to it after the completion of the task. For example, when a thread throws the exception and the pool class is not able to catch this exception, then the thread exits and reduces the thread pool size by 1. If the same thing repeats a number of times, then there are fair chances that the pool will become empty, and hence, there are no threads available in the pool for executing other requests.

**Resource Thrashing**: A lot of time is wasted in context switching among threads when the size of the thread pool is very large. Whenever there are more threads than the optimal number may cause the starvation problem, and it leads to resource thrashing.

> [!NOTE]
> In the end, the thread pool has to be ended explicitly. If it does not happen, then the program continues to execute, and it never ends. Invoke the shutdown() method on the thread pool to terminate the executor. Note that if someone tries to send another task to the executor after shutdown, it will throw a RejectedExecutionException.

### Tuning the Thread Pool
The accurate size of a thread pool is decided by the number of available processors and the type of tasks the threads have to execute. If a system has the P processors that have only got the computation type processes, then the maximum size of the thread pool of P or P + 1 achieves the maximum efficiency. However, the tasks may have to wait for I/O, and in such a scenario, one has to take into consideration the ratio of the waiting time (W) and the service time (S) for the request; resulting in the maximum size of the pool P * (1 + W / S) for the maximum efficiency.

## ThreadGroup in Java
[Example ↗︎](./ThreadGroupDemo.java)

Java provides a convenient way to group multiple threads in a single object. In such a way, we can suspend, resume or interrupt a group of threads by a single method call.

> Now suspend(), resume() and stop() methods are deprecated.

Java thread group is implemented by ```java.lang.ThreadGroup``` class.

A ThreadGroup represents a set of threads. A thread group can also include the other thread group. The thread group creates a tree in which every thread group except the initial thread group has a parent.

A thread is allowed to access information about its own thread group, but it cannot access the information about its thread group's parent thread group or any other thread groups.

### Constructors of Thread Group

There are only two constructors of ThreadGroup class.

- ```ThreadGroup(String name)``` : creates a thread group with given name.
- ```ThreadGroup(ThreadGroup parent, String name)``` : creates a thread group with a given parent group and name.

More at [External Source](https://www.javatpoint.com/threadgroup-in-java).

## Shutdown Hook
[Example ↗︎](./ShutDownHook.java)

A special construct that facilitates the developers to add some code that has to be run when the Java Virtual Machine (JVM) is shutting down is known as the Java shutdown hook. The Java shutdown hook comes in very handy in the cases where one needs to perform some special cleanup work when the JVM is shutting down. Note that handling an operation such as invoking a special method before the JVM terminates does not work using a general construct when the JVM is shutting down due to some external factors. For example, whenever a kill request is generated by the operating system or due to resource is not allocated because of the lack of free memory, then in such a case, it is not possible to invoke the procedure. The shutdown hook solves this problem comfortably by providing an arbitrary block of code.

All one has to do is to derive a class using the java.lang.Thread class, and then provide the code for the task one wants to do in the ```run()``` method when the JVM is going to shut down. For registering the instance of the derived class as the shutdown hook, one has to invoke the method ```Runtime.getRuntime().addShutdownHook(Thread)```, whereas for removing the already registered shutdown hook, one has to invoke the ```removeShutdownHook(Thread)``` method.

In nutshell, the shutdown hook can be used to perform cleanup resources or save the state when JVM shuts down normally or abruptly. Performing clean resources means closing log files, sending some alerts, or something else. So if you want to execute some code before JVM shuts down, use the shutdown hook.

### When does the JVM shut down?
The JVM shuts down when:
- user presses *ctrl+c* on the command prompt
- *System.exit(int)* method is invoked
- user logoff
- user shutdown etc.

### Factory method
The method that returns the instance of a class is known as factory method.

## Java Garbage Collection
[Example ↗︎](./GarbageCollection.java)  
In java, garbage means unreferenced objects.

Garbage Collection is process of reclaiming the runtime unused memory automatically. In other words, it is a way to destroy the unused objects.

To do so, we were using free() function in C language and delete() in C++. But, in java it is performed automatically. So, java provides better memory management.

### How can an object be unreferenced?
There are many ways:

- By nulling the reference
    ```java
    Employee e=new Employee();  
    e=null;  
    ```
- By assigning a reference to another
    ```java
    Employee e1=new Employee();  
    Employee e2=new Employee();  
    e1=e2;//now the first object referred by e1 is available for garbage collection  
    ```
- By anonymous object etc.
    ```java
    new Employee();  
    ```

### finalize() method
The finalize() method is invoked each time before the object is garbage collected. This method can be used to perform cleanup processing. This method is defined in Object class as:

```java
protected void finalize(){}  
```

> Note: The Garbage collector of JVM collects only those objects that are created by new keyword. So if you have created any object without new, you can use finalize method to perform cleanup processing (destroying remaining objects).

### gc() method
The gc() method is used to invoke the garbage collector to perform cleanup processing. The gc() is found in System and Runtime classes.

```java
public static void gc(){}  
```

> Garbage collection is performed by a daemon thread called Garbage Collector(GC). This thread calls the finalize() method before object is garbage collected.

## Java Runtime class
[Example ↗︎](./RuntimeDemo.java)  
Java Runtime class is used to interact with *java runtime environment*. Java Runtime class provides methods to execute a process, invoke GC, get total and free memory etc. There is only one instance of java.lang.Runtime class is available for one java application.

The **Runtime.getRuntime()** method returns the singleton instance of Runtime class.

## !Additional Important

### Using Future and Callable for Return Values

In Java, ```Callable``` and ```Future``` are part of the ```java.util.concurrent``` package and are used together to create concurrent tasks that can return a result or throw an exception. This makes them more powerful than ```Runnable```, which can’t return a value or throw a checked exception.

If you want a thread to return a result after completing its task, you can use ```Callable``` instead of ```Runnable```, and submit it to an ```ExecutorService```. It allows tasks to return a value.  

### Callable:  
Callable Interface ```Callable<V>``` is a functional interface that is similar to Runnable, but with two main differences:
- It returns a result (V).
- It can throw a checked exception.

**Key Features of Callable:**  
- Has a single method ```V call()``` where V is the result type.
- Used in conjunction with ```ExecutorService``` to submit tasks that return results.


### Futures: 
```Future<V>``` represents the result of an asynchronous computation. It provides methods to:  
- Retrieve the result of the computation (via ```get()``` method).
- Cancel the computation (via ```cancel()``` method).
- Check if the computation is done (via ```isDone()``` method).
When a task is submitted, the Future object allows us to retrieve the result once the task completes. The ```future.get()``` call blocks the main thread until the task is done.

[Example ↗︎](./ThreadPoolUsingCallable.java)

### Synchronizing Threads
[Example ↗︎](./Synchronization.java)  
When multiple threads access shared resources (like variables or files), you may need to synchronize access to prevent race conditions.

## Mini Example

**Scenario**: Order Processing System  
**Problem**:
When a customer places an order, several independent tasks must be processed:

- Validate Payment: Verify that the payment method is valid and funds are available.
- Update Inventory: Deduct the ordered products from the inventory.
- Send Confirmation Email: Notify the customer that their order has been placed successfully.

These tasks can be done in parallel to speed up the overall process. This is where multithreading can be used.  
[**Code**](./OrderProcessing/OrderProcessing.java) : Example to make use of Multithreading
