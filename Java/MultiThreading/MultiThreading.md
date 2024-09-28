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

1. By extending Thread class
2. By implementing Runnable interface.

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

#### Java Thread Example by extending Thread class
FileName: [Using Thread Class](./UsingThreadClass.java)

#### Java Thread Example by implementing Runnable interface
FileName: [Using Runnable Interface](./UsingRunnableInterface.java)

## Thread Scheduler in Java

A component of Java that decides which thread to run or execute and which thread to wait is called a thread scheduler in Java. In Java, a thread is only chosen by a thread scheduler if it is in the runnable state. However, if there is more than one thread in the runnable state, it is up to the thread scheduler to pick one of the threads and ignore the other ones. There are some criteria that decide which thread will execute first. There are two factors for scheduling a thread i.e. **Priority** and **Time of arrival**.

**Priority**: Priority of each thread lies between 1 to 10. If a thread has a higher priority, it means that thread has got a better chance of getting picked up by the thread scheduler.

**Time of Arrival**: Suppose two threads of the same priority enter the runnable state, then priority cannot be the factor to pick a thread from these two threads. In such a case, arrival time of thread is considered by the thread scheduler. A thread that arrived first gets the preference over the other threads.

### Thread Scheduler Algorithms
On the basis of the above-mentioned factors, the scheduling algorithm is followed by a Java thread scheduler.

#### First Come First Serve Scheduling:
In this scheduling algorithm, the scheduler picks the threads thar arrive first in the runnable queue.

#### Time-slicing scheduling:
Usually, the First Come First Serve algorithm is non-preemptive, which is bad as it may lead to infinite blocking (also known as starvation). To avoid that, some time-slices are provided to the threads so that after some time, the running thread has to give up the CPU. Thus, the other waiting threads also get time to run their job.

#### Preemptive-Priority Scheduling:
Suppose there are multiple threads available in the runnable state. The thread scheduler picks that thread that has the highest priority. Since the algorithm is also preemptive, therefore, time slices are also provided to the threads to avoid starvation. Thus, after some time, even if the highest priority thread has not completed its job, it has to release the CPU because of preemption.

## Thread.sleep()

```java
public static void sleep(long mls) throws InterruptedException   
public static void sleep(long mls, int n) throws InterruptedException   
```

### Points:
The following are the parameters used in the sleep() method : 
- mls: The time in milliseconds is represented by the parameter mls. The duration for which the thread will sleep is given by the method sleep().  
- n: It shows the additional time up to which the programmer or developer wants the thread to be in the sleeping state. The range of n is from 0 to 999999.
- The method does not return anything.
- Whenever another thread does interruption while the current thread is already in the sleep mode, then the InterruptedException is thrown.
- Throws the exception IllegalArguementException when the time for sleeping is negative.

## Can we start a thread twice
No. After starting a thread, it can never be started again. If you does so, an IllegalThreadStateException is thrown. In such case, thread will run once but for second time, it will throw exception.

## What if we call Java run() method directly instead start() method?
- Each thread starts in a separate call stack.
- Invoking the run() method from the main thread, the run() method goes onto the current call stack rather than at the beginning of a new call stack.

## Java join() method
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
public final synchronized void join(long mls) throws InterruptedException, where mls is in milliseconds  
```

**join(long mls, int nanos)**: When the ```join()``` method is invoked, the current thread stops its execution and go into the wait state. The current thread remains in the wait state until the thread on which the join() method is invoked called is dead or the wait for the specified time frame(in milliseconds + nanos) is over.

*Syntax*:
```java
public final synchronized void join(long mls, int nanos) throws InterruptedException, where mls is in milliseconds.  
```

Example: [Join](./ThreadJoin.java)