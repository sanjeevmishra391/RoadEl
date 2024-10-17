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

1. Threads enter to acquire lock.
2. Lock is acquired by on thread.
3. Now thread goes to waiting state if you call wait() method on the object. Otherwise it releases the lock and exits.
4. If you call notify() or notifyAll() method, thread moves to the notified state (runnable state).
5. Now thread is available to acquire lock.
6. After completion of the task, thread releases the lock and exits the monitor state of the object.

## Why ```wait()```, ```notify()``` and ```notifyAll()``` methods are defined in ```Object``` class not ```Thread``` class?
It is because they are related to lock and object has a lock.

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


