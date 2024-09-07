# Database Management System

Database Management System, commonly known as DBMS is a system that houses data so that it can be accessed and altered easily by those who are permitted access. DBMS facilitates the management of data in a highly-organized manner.

## Index
- [Relational Model](#relational-model)
- [Normalization](#normalization)
- [Indexing](#indexing-in-dbms)
- [Trasactions In DBMS](#transactions-in-dbms)
- [Deadlock](#deadlock)
- [Relational Database](#what-is-a-relational-database)
- [Non-relational Database](#what-is-a-non-relational-database)
- [Resources](#resources)

## Relational Model

The data is organized into a collection of two-dimensional inter-related tables, also known as relations. Each relation is a collection of columns and rows, where the column represents the attributes of an entity and the rows (or tuples) represent the records.

### Relational Keys
Keys in DBMS are introduced to avoid redundancy in data. A table represents a big box with unique records, and keys help to identify those records efficiently.  Keys in DBMS can be single or a group of attributes that uniquely identify the records.

- **Super Key** : A super key is a set of all the keys (with single or multiple attributes) which can uniquely identify the records of the table.
- **Candidate Key** : From all the super keys available, the candidate key is the one whose proper subset is not a super key.
- **Primary Key** : The primary key is nothing but a candidate key which has given the right to be called the primary key. **It should be unique and can not be null.**
- **Alternate Key** : Alternate keys are nothing but candidate keys that don’t get rights of the primary key.
- **Foreign Key** : Key that links to primary key of other table
- **Composite key** : Key that contains more than one attribute and is subset of super key.

### Cardinality

Cardinality helps us identify the uniqueness of the data in database columns. Here Low cardinality denotes that there are a lot of redundancies(repeated values) in columns. High cardinality denotes that there are a lot of unique values in columns.

Types of cardinalities between tables are:
- One-to-One
- One-to-Many
- Many-to-One
- Many-to-Many 

### Joins in DBMS
Joins are used to Join two or more tables in the Database.

There can be more than one way to join the database tables. So different types of Joins are:-

- **Inner Join** : It selects the values present in both the Table performing Inner join.
    - *Theta Inner Join* : Theta Join is used to join two tables based on some conditions. The condition can be on any attributes of the tables performing Theta join. Any comparison operator can be used in the condition.
    - *Natural Inner Join* : Natural join is also considered a type of inner join but it does not use any comparison operator for join condition. It joins the table only when the two tables have at least one common attribute with same name and domain.
- **Natural Join** : Natural Join is performed only when there is at least one matching attribute in both the tables.
- **Outer Join** : Outer Join in Relational algebra returns all the attributes of both the table depending on the condition. If some attribute value is not present for any one of the tables it returns NULL in the respective row of the table attribute.
    - *Left Outer Join* : It returns all the rows of the left table even if there is no matching row for it in the right table performing Left Outer Join.
    - *Right Outer Join* : It returns all the rows of the second table even if there is no matching row for it in the first table performing Right Outer Join.
    - *Full Outer Join* : It returns all the rows of the first and second Table.

### SQL Commands: DDL, DML, DCL, TCL, DQL

- **DDL(Data Definition Language)**: 

    To make/perform changes to the physical structure of any table residing inside a database, DDL is used. These commands when executed are auto-commit in nature and all the changes in the table are reflected and saved immediately. **CREATE, DROP, ALTER, TRUNCATE, and RENAME**

- **DML(Data Manipulation Language)**: 

    Once the tables are created and the database is generated using DDL commands, manipulation inside those tables and databases is done using DML commands. The advantage of using DML commands is, that if in case any wrong changes or values are made, they can be changed and rolled back easily. **INSERT, UPDATE, and DELETE**

- **DQL(Data Query Language)**: 

    Data query language consists of only one command upon which data selection in SQL relies. The SELECT command in combination with other SQL clauses is used to retrieve and fetch data from databases/tables based on certain conditions applied by the user. **SELECT**

- **DCL(Data Control Language)**: 

    DCL commands as the name suggests manage the matters and issues related to the data controller in any database. DCL includes commands such as GRANT and REVOKE which mainly deal with the rights, permissions, and other controls of the database system.

- **TCL(Transaction Control Language)**: 

    Transaction Control Language as the name suggests manages the issues and matters related to the transactions in any database. They are used to roll back or commit the changes in the database.

### Integrity Constraints in DBMS

There are four types of integrity constraints in DBMS:

1. **Domain Constraint**

    Domain integrity constraint contains a certain set of rules or conditions to restrict the kind of attributes or values a column can hold in the database table. The data type of a domain can be string, integer, character, DateTime, currency, etc.
2. **Entity Constraint**

    Entity Integrity Constraint is used to ensure that the **primary key cannot be null**. A primary key is used to identify individual records in a table and if the primary key has a null value, then we can't identify those records. There can be null values anywhere in the table except the primary key column.
3. **Referential Integrity Constraint**

    Referential Integrity Constraint ensures that there must always exist a valid relationship between two relational database tables. This valid relationship between the two tables confirms that a foreign key exists in a table. It should always reference a corresponding value or attribute in the other table or be null.
4. **Key Constraint**

    Keys are the set of entities that are used to identify an entity within its entity set uniquely. There could be multiple keys in a single entity set, but out of these multiple keys, only one key will be the primary key. A primary key can only contain unique and not null values in the relational database table.

## Normalization

### Functional Dependency in DBMS

Functional Dependency is the relationship between attributes(characteristics) of a table related to each other. The functional dependency of A on B is represented by A → B, where A and B are the attributes of the relation.

> **Functional Dependency**  
> In general, if we say, A, B, C, D, E are attributes of a table, and we’ve given AB -> CDE.  
> This means that given values of A and B, we can find values of C, D, and E.

### Normalization 
It is the process of organizing the data and the attributes of a database. It is performed to **reduce the data redundancy in a database and to ensure that data is stored logically (avoiding data anamolies).** Data redundancy in DBMS means having the same data but at multiple places. It is necessary to remove data redundancy because it causes anomalies in a database which makes it very hard for a database administrator to maintain it.

### Why Do We Need Normalization?
Normalization is used to reduce data redundancy. It provides a method to remove the following anomalies from the database and bring it to a more consistent state:

A database anomaly is a flaw in the database that occurs because of poor planning and redundancy.

- **Insertion anomalies** : This occurs when we are not able to insert data into a database because some attributes may be missing at the time of insertion.

- **Updation anomalies**: This occurs when the same data items are repeated with the same values and are not linked to each other.

- **Deletion anomalies**: This occurs when deleting one part of the data deletes the other necessary information from the database.

### Normal Forms
There are four types of normal forms that are usually used in relational databases.

1. 1NF: A relation is in 1NF if all its attributes have an atomic value.

2. 2NF: A relation is in 2NF if it is in 1NF and all non-key attributes are fully functional dependent on the candidate key in DBMS.

3. 3NF: A relation is in 3NF if it is in 2NF and there is no transitive dependency.

4. BCNF: A relation is in BCNF if it is in 3NF and for every Functional Dependency, LHS is the super key.

## Indexing in DBMS
[⤴︎](https://www.scaler.com/topics/dbms/indexing-in-dbms/)

```sql
CREATE INDEX index_name ON table-name (column-name_1, column-name_2 DESC);
```

Indexing in DBMS is a technique that uses data structures to optimize the searching time of a database query.   

It uses data structures to optimize the searching time of a database query in DBMS.   

Indexing reduces the number of disks required to access a particular data by internally creating an index table.  

Indexing is achieved by creating Index-table or Index.   


- Index usually consists of two columns which are a key-value pair. The two columns of the index table(i.e., the key-value pair) contain copies of selected columns of the tabular data of the database.
- Search Key contains the copy of the Primary Key or the Candidate Key of the database table. Generally, we store the selected Primary or Candidate keys in a sorted manner so that we can reduce the overall query time or search time(from linear to binary).
- Data Reference contains a set of pointers that holds the address of the disk block. The pointed disk block contains the actual data referred to by the Search Key. Data Reference is also called Block Pointer because it uses block-based addressing.

Either B-tree or Bitmap is used for indexing

### Types of Indexes

- **Single Level Indexing**

    It is much similar like index found in books.

    Single Level Indexing is further divided into three categories:

    1. **Primary Indexing**:

        The indexing or the index table created using Primary keys is known as Primary Indexing. It is **defined on ordered data**. As the index is comprised of primary keys, they are unique, not null, and possess one to one relationship with the data blocks. Fast and Efficient Searching.
    
    2. **Secondary Indexing**:

        It is a two-level indexing technique used to reduce the mapping size of the primary index. The secondary index points to a certain location where the data is to be found but the actual data is not sorted like in the primary indexing. Secondary Indexing is also known as non-clustered Indexing.

        - Search Keys are Candidate Keys.
        - Search Keys are sorted but actual data may or may not be sorted.
        - Search Keys cannot be null.

    3. **Cluster Indexing**:

        Clustered Indexing is used when there are multiple related records found at one place. It is defined on ordered data. The important thing to note here is that the index table of clustered indexing is created using non-key values which may or may not be unique. To achieve faster retrieval, we group columns having similar characteristics. The indexes are created using these groups and this process is known as Clustering Index.

        - Search Keys are non-key values.
        - Search Keys are sorted.
        - Search Keys cannot be null.
        - Search Keys may or may not be unique.
        - Requires extra work to create indexing.

    - **Ordered Indexing**:

        The indices are stored in a sorted manner hence it is also known as ordered indices.

        Ordered Indexing is further divided into two categories:

        1. **Dense Indexing**:

        In dense indexing, the index table contains records for every search key value of the database. This makes searching faster but requires a lot more space. It is like primary indexing but contains a record for every search key.

        2. **Sparse Indexing**:

        Sparse indexing consumes lesser space than dense indexing, but it is a bit slower as well. We do not include a search key for every record despite that we store a Search key that points to a block. The pointed block further contains a group of data. Sometimes we have to perform double searching this makes sparse indexing a bit slower.

- **Multi-level Indexing**:

    Since the index table is stored in the main memory, single-level indexing for a huge amount of data requires a lot of memory space. Hence, multilevel indexing was introduced in which we divide the main data block into smaller blocks. This makes the outer block of the index table small enough to be stored in the main memory.

    We use the **B+ Tree data structure for multilevel indexing**. The leaf nodes of the B+ tree contain the actual data pointers. The leaf nodes are themselves in the form of a linked list. This linked list representation helps in both sequential and random access.


## Transactions in DBMS
[⤴︎](https://www.scaler.com/topics/dbms/transaction-in-dbms/)

Transactions in Database Management Systems (DBMS) are sets of operations performed to modify data, including insertion, updates, or deletions. They ensure data consistency even during system failures, demonstrating a key advantage of DBMS.

Operations done is transaction : 1. Read/Access Data 2. Write/Change Data 3. Commit

Transaction can be in many transaction states : 
1. Active State
2. Partially Committed
3. Failed State
4. Aborted State
5. Committed State
6. Terminated State

### Properties of Transaction (ACID) [⤴︎](https://www.scaler.com/topics/dbms/acid-properties-in-dbms/)

These are used to maintain state consistency in the database, both before and after the transaction. These are called ACID properties.

1. **Atomicity** (all or nothing):
    
    <span style="color:#E19898">This property means that either the transaction takes place completely at once or doesn’t happen at all</span>. There is no middle option, i.e., transactions do not occur partially. Each transaction is considered as one single step which either runs completely or is not executed at all. If any of the operations aren’t completed fully, the transaction gets aborted.

    > Example: When booking a train seat, seat selection and payment should both be completed. Or none should happen.

2. **Consistency**: 

    This property means that the integrity constraints of a database are maintained so that the <span style="color:#E19898">database is consistent before and after the transaction</span>. It refers to the correctness of a database.

    > Example: Number of seats left + Total seats booked = Total seats in train. Consistency ensures that this formula is valid each time.

3. **Isolation**: 

    This property means that multiple transactions can occur concurrently without causing any inconsistency to the database state. These transactions occur independently without any external interference. <span style="color:#E19898">Changes that occur in a particular transaction are not visible/ accessible to any other transaction until that particular change in that transaction has been committed</span>.

    > Example: Suppose two people try to book the same seat simultaneously. Transactions are serialized to maintain data consistency. The first person's transaction succeeds, and they receive a ticket. The second person's transaction fails as the seat is already booked. They receive an error message indicating no available seats.

4. **Durability**:

    <span style="color:#E19898">This property ensures that once the transaction has completed execution, the updates and modifications to the database are stored in and written to disk and they remain intact even if a system failure occurs</span>. These updates become permanent and are stored in the non-volatile memory. Every time we make a change, we must use the COMMIT command to commit the values.

    > Example: Suppose that there is a system failure in the railway management system resulted in the loss of all booked train details. Millions of users who had paid for their seats are now unable to board the train, causing significant financial losses and eroding trust in the company. The situation is particularly critical as these trains are needed for important reasons, causing widespread panic and inconvenience.

### Disadvantages of ACID properties in DBMS

- **Performance Overhead**: ACID properties can impact system performance and throughput due to additional processing and resource utilization.
- **Complexity**: Implementing ACID properties adds complexity to database systems, increasing design and maintenance challenges.
- **Scalability Challenges**: ACID properties can pose difficulties in highly distributed or scalable systems, limiting scalability.
- **Potential for Deadlocks**: ACID transactions using locking mechanisms can lead to deadlocks and system halts.
- **Limited Concurrency**: ACID properties may restrict concurrency, impacting overall system throughput.
- **Recovery Complexity**: ACID properties introduce complexities in recovery and backup strategies.
- **Trade-off with Availability**: Strict adherence to ACID properties may affect system availability in certain situations.

## Deadlock
[⤴︎](https://www.scaler.com/topics/dbms/deadlock-in-dbms/)

Deadlock in a database management system (DBMS) is a problematic scenario where two or more transactions are stuck in an indefinite wait for each other to finish, yet neither relinquishes the necessary CPU and memory resources. This impasse halts the system, as tasks remain uncompleted and perpetually waiting.

### Coffman Conditions

Regarding deadlock in DBMS, there were four conditions stated by Coffman. A deadlock might occur if all of these four Coffman conditions hold true at any point in time.

- **Mutual Exclusion**: There should be at least one resource that cannot be utilized by more than one transaction at a time.
- **Hold and wait condition**: When any transaction is holding a resource, requests for some more additional resources which are already being held by some other transactions in the system.
- **No preemption condition**: Access to a particular resource can never be forcibly taken from a running transaction. Only that running transaction can release a resource that is being held by it.
- **Circular wait condition**: In this condition, a transaction is kept waiting for a resource that is at the same time is held by some other transaction and which is further waiting for a third transaction so on and the last transaction is waiting for the very first one. Thus, giving rise to a circular chain of waiting transactions.

### Deadlock Handling

- **Ostrich Algorithm**

    Ostrich Algorithm is an approach of handling deadlock that involves ignoring the deadlock and pretending that it would never occur.

    Ignore deadlock because deadlock is a very rare case and the cost of handling a deadlock is very high.
    
    Windows and UNIX-based systems use this approach of handling a deadlock.

- **Deadlock Avoidance**

    Deadlock avoidance is a technique of detecting any deadlock in advance. Methods like Wait-For graph can be used in smaller databases to detect deadlocks, but in the case of larger databases deadlock prevention measures have to be used.

    When a database gets stuck in a state of deadlock, it is preferred to avoid using that database instead of aborting or rebooting the database server as it wastes of both time and resources.

### Deadlock Detection

A resource scheduler can detect deadlock by keeping track of resources allocated to a specific transaction and requested by another transaction.

- **Wait-For Graph**: This method of detecting deadlocks involves creating a graph based on a transaction and its acquired lock (a lock is a way of preventing multiple transactions from accessing any resource simultaneously). If the created graph contains a cycle/loop, it means a deadlock exists.

## What is a relational database? 

A relational database, or relational database management system (RDMS), stores information in tables. Often, these tables have shared information between them, causing a relationship to form between tables. 

A table uses columns to define the information being stored and rows for the actual data. Each table will have a column that must have unique values—known as the primary key.

When one table’s primary key is used in another table, this column in the second table is known as the foreign key.

```SQL
SELECT PRODUCT_NAME, PRICE FROM PRODUCT WHERE PRODUCT _ID = 23;
```

### Advantages of relational databases
- They work with structured data.
- **ACID compliance** : Atomicity, Consistency, Isolation, and Durability (ACID) is a standard that guarantees the reliability of database transactions.
- **Data accuracy** : Using primary and foreign keys allows you to ensure there is no duplicate information. This helps enforce data accuracy because there will never be repeated information.
- **Normalization** : The process of normalization involves ensuring the data is organized in such a way that data anomalies are reduced or eliminated. This, in turn, reduces storage costs.
- **Tools and Simplicity** : RDMS, or SQL databases, have been around for so long that a wide variety of tools and resources have been developed to help get started and interact with relational databases
- Relationships in the system have constraints, which promotes a high level of data integrity.
- There are limitless indexing capabilities, which results in faster query response times.
- They are excellent at keeping data transactions secure.
- They provide the ability to write complex SQL queries for data analysis and reporting.
- Their models can ensure and enforce business rules at the data layer adding a level of data integrity not found in a non-relational database.
- They are table and row oriented.
- They Use SQL (structured query language) for shaping and manipulating data, which is very powerful.
- SQL database examples: MySql, Oracle, Sqlite, Postgres and MS-SQL. NoSQL database examples: MongoDB, BigTable, Redis, RavenDb, Cassandra, Hbase, Neo4j and CouchDb.

### Disadvantages of relational databases
- **Scalability** : RDMSs are historically intended to be run on a single machine. This means that if the requirements of the machine are insufficient, due to data size or an increase in the frequency of access, you will have to improve the hardware in the machine, also known as vertical scaling. Machine with better hardware comes at higher cost.
- **Flexibility** : In relational databases, the schema is rigid. You define the columns and data types for those columns, including any restraints such as format or length.  Although this means you can interpret the data more easily and identify the relationships between tables, it means that making changes to the structure of the data is very complex. You have to decide at the start what the data will look like, which isn’t always possible. If you want to make changes later, you have to change all the data, which involves the database being offline temporarily.
- **Performance** : The performance of the database is tightly linked to the complexity of the tables—the number of them, as well as the amount of data in each table. As this increases, the time taken to perform queries increases too.

## What is a non-relational database?

A non-relational database, sometimes called NoSQL (Not Only SQL), is any kind of database that doesn’t use the tables, fields, and columns structured data concept from relational databases. Non-relational databases have been designed with the cloud in mind, making them **great at horizontal scaling**.

#### Different groups of database types that store the data in different ways:

- **Document databases** : Document databases store data in documents, which are usually JSON-like structures that support a variety of data types. Highly scalable, self-healing means highly available, JSON is easy to read.

- **Key-value database** : The key is used to retrieve the information from the database. Reading and writing will always be fast. More complex data requirements can’t be supported.

- **Graph databases** : They use a structure of elements called nodes that store data, and edges between them contain attributes about the relationship. They are not very good for querying the whole database, where relationships aren’t as well—or at all—defined.

- **Wide-column databases** : Wide-column databases, similar to relational databases, store data in tables, columns, and rows. However, the names and formatting of the columns don’t have to match in each row. The columns can even be stored across multiple servers. They are considered two-dimensional key-value stores because they use multi-dimensional mapping to reference data by row and column.

## Additional Terms

### View
A view is a virtual table based on the result-set of an SQL statement. We can create it using create view syntax. 
```sql
CREATE VIEW view_name AS
SELECT column_name(s)
FROM table_name
WHERE condition
```

1. Views can represent a subset of the data contained in a table; consequently, a view can limit the degree of exposure of the underlying tables to the outer world: a given user may have permission to query the view, while denied access to the rest of the base table. 
2. Views can join and simplify multiple tables into a single virtual table.
3. Views can act as aggregated tables, where the database engine aggregates data (sum, average, etc.) and presents the calculated results as part of the data.
4. Views can hide the complexity of data.
5. Views take very little space to store; the database contains only the definition of a view, not a copy of all the data which it presents. 
6. Depending on the SQL engine used, views can provide extra security.

### Trigger
A Trigger is a code associated with insert, update or delete operations. The code is executed automatically whenever the associated query is executed on a table. Triggers can be useful to maintain integrity in the database. 

### Stored Procedure
A stored procedure is like a function that contains a set of operations compiled together. It contains a set of operations that are commonly used in an application to do some common database tasks. 

Unlike Stored Procedures, Triggers cannot be called directly. They can only be associated with queries. 

### Cursors
A cursor is an object used to store the output of a query for row-by-row processing by the application programs. The cursors are used to navigate through a set of rows returned by an embedded SQL SELECT statement. A cursor can be compared to a pointer.

### Difference between UNION and UNION ALL?

UNION and UNION ALL are used to join the data from 2 or more tables but **UNION removes duplicate rows and picks the rows which are distinct after combining the data from the tables** whereas **UNION ALL does not remove the duplicate rows, it just picks all the data from the tables.**

### RAID

RAID stands for Redundant Array of Inexpensive (or sometimes “Independent”)Disks.

RAID is a method of combining several hard disk drives into one logical unit (two or more disks grouped together to appear as a single device to the host system). RAID technology was developed to address the fault-tolerance and performance limitations of conventional disk storage. It can offer fault tolerance and higher throughput levels than a single hard drive or group of independent hard drives. While arrays were once considered complex and relatively specialized storage solutions, today they are easy to use and essential for a broad spectrum of client/server applications.

## PL/SQL

```sql
DECLARE 
   -- variable declaration 
   message  varchar2(20):= 'Hello, World!'; 
BEGIN 
   /* 
   *  PL/SQL executable statement(s) 
   */ 
   dbms_output.put_line(message); 
END; 
/
```

## Resources

- https://www.scaler.com/topics/dbms/
- https://www.mongodb.com/resources/compare/relational-vs-non-relational-databases
- https://ksat.me/a-plain-english-introduction-to-cap-theorem