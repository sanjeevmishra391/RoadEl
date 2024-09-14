### Difference between final, finally and finalize


| Sr. no. | 	Key |	final |	finally	| finalize |
| ------- | ------- | ------- | ------- | -------- |
| 1. |	Definition |	final is the keyword and access modifier which is used to apply restrictions on a class, method or variable. |	finally is the block in Java Exception Handling to execute the important code whether the exception occurs or not.	|finalize is the method in Java which is used to perform clean up processing just before object is garbage collected. |
| 2. |	Applicable to |	Final keyword is used with the classes, methods and variables. |	Finally block is always related to the try and catch block in exception handling. |	finalize() method is used with the objects. |
| 3. |	Functionality |	<span style="color:#E19898">(1) Once declared, final variable becomes constant and cannot be modified. (2) final method cannot be overridden by sub class. (3) final class cannot be inherited.<./span> | <span style="color:#E19898">(1) finally block runs the important code even if exception occurs or not. (2) finally block cleans up all the resources used in try block. </span> | <span style="color:#E19898">finalize method performs the cleaning activities with respect to the object before its destruction.</span>
| 4. |	Execution |	Final method is executed only when we call it. | Finally block is executed as soon as the try-catch block is executed. It's execution is not dependant on the exception. | finalize method is executed just before the object is destroyed.