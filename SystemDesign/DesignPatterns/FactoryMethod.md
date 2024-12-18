## Problem That Can Be Solved Using the Factory Method Design Pattern:
Scenario: You are developing a document management system where different types of documents need to be created (e.g., Word documents, PDFs, and Spreadsheets). Each type of document has its own set of rules and operations, but the system should allow for easy addition of new document types without modifying the core logic.

## Problem Details:

1. Context:
The system needs to support multiple document types (e.g., Word, PDF, Spreadsheet).
The creation process for each document type might involve different steps (e.g., setting headers, footers, metadata).
New document types may be introduced in the future.

2. Challenges:
Avoid tightly coupling the system to specific document types.
Simplify the process of adding support for new document types without modifying existing code (adhering to the Open-Closed Principle).

3. Requirements:
Create a system where the client code works with a general interface for documents.
Delegate the instantiation logic to subclasses that specialize in creating specific document types.

## Solution: Apply the Factory Method Pattern
The Factory Method Pattern allows the client to rely on an abstract creator class to instantiate objects, while subclasses handle the creation of specific document types.

## Implementation Steps:
1. Define a Common Product Interface: Create a common interface or abstract class for all document types.
2. Abstract Creator Class: Define a base class with a factory method that returns objects of the product type.
3. Concrete Creators: Implement subclasses of the abstract creator to produce specific document types.

## Example Implementation:
1. Common Document Interface (Product)
```java
interface Document {
    void open();
    void save();
    void close();
}
```

2. Concrete Document Types (Concrete Products)
```java
class WordDocument implements Document {
    @Override
    public void open() {
        System.out.println("Opening Word Document...");
    }

    @Override
    public void save() {
        System.out.println("Saving Word Document...");
    }

    @Override
    public void close() {
        System.out.println("Closing Word Document...");
    }
}

class PDFDocument implements Document {
    @Override
    public void open() {
        System.out.println("Opening PDF Document...");
    }

    @Override
    public void save() {
        System.out.println("Saving PDF Document...");
    }

    @Override
    public void close() {
        System.out.println("Closing PDF Document...");
    }
}

class SpreadsheetDocument implements Document {
    @Override
    public void open() {
        System.out.println("Opening Spreadsheet Document...");
    }

    @Override
    public void save() {
        System.out.println("Saving Spreadsheet Document...");
    }

    @Override
    public void close() {
        System.out.println("Closing Spreadsheet Document...");
    }
}
```

3. Abstract Creator
```java
abstract class DocumentCreator {
    // Factory method
    public abstract Document createDocument();

    // Common logic for using a document
    public void handleDocument() {
        Document document = createDocument();
        document.open();
        document.save();
        document.close();
    }
}
```

4. Concrete Creators
```java
class WordDocumentCreator extends DocumentCreator {
    @Override
    public Document createDocument() {
        return new WordDocument();
    }
}

class PDFDocumentCreator extends DocumentCreator {
    @Override
    public Document createDocument() {
        return new PDFDocument();
    }
}

class SpreadsheetDocumentCreator extends DocumentCreator {
    @Override
    public Document createDocument() {
        return new SpreadsheetDocument();
    }
}
```

5. Client Code

```java
public class Main {
    public static void main(String[] args) {
        DocumentCreator wordCreator = new WordDocumentCreator();
        DocumentCreator pdfCreator = new PDFDocumentCreator();
        DocumentCreator spreadsheetCreator = new SpreadsheetDocumentCreator();

        System.out.println("Using Word Creator:");
        wordCreator.handleDocument();

        System.out.println("\nUsing PDF Creator:");
        pdfCreator.handleDocument();

        System.out.println("\nUsing Spreadsheet Creator:");
        spreadsheetCreator.handleDocument();
    }
}

```

### Output

```java
Using Word Creator:
Opening Word Document...
Saving Word Document...
Closing Word Document...

Using PDF Creator:
Opening PDF Document...
Saving PDF Document...
Closing PDF Document...

Using Spreadsheet Creator:
Opening Spreadsheet Document...
Saving Spreadsheet Document...
Closing Spreadsheet Document...

```

## Benefits of Using Factory Method Here:
1. **Open-Closed Principle**: Adding support for a new document type requires only a new concrete product and creator class.
2. **Single Responsibility Principle**: Document creation logic is encapsulated in the respective creator classes.
3. **Scalability**: You can easily extend the system to support more document types without modifying existing code.