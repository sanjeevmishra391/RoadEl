### Infix to Postfix

1. Scan the infix expression from left to right. 
2. If the scanned character is an operand, put it in the postfix expression. 
3. Otherwise, do the following
    - If the precedence of the current scanned operator is higher than the precedence of the operator on top of the stack, or if the stack is empty, or if the stack contains a '(', then push the current operator onto the stack.
    - Else, pop all operators from the stack that have precedence higher than or equal to that of the current operator. After that push the current operator onto the stack.
4. If the scanned character is a ‘(‘, push it to the stack. 
5. If the scanned character is a ‘)’, pop the stack and output it until a ‘(‘ is encountered, and discard both the parenthesis. 
6. Repeat steps 2-5 until the infix expression is scanned. 
7. Once the scanning is over, Pop the stack and add the operators in the postfix expression until it is not empty.
8. Finally, print the postfix expression.

### Prefix to Infix

1. Read the Prefix expression in reverse order (from right to left)
2. If the symbol is an operand, then push it onto the Stack
3. If the symbol is an operator, then pop two operands from the Stack 
    Create a string by concatenating the two operands and the operator between them. 
    string = (operand1 + operator + operand2) 
    And push the resultant string back to Stack
4. Repeat the above steps until the end of Prefix expression.
5. At the end stack will have only 1 string i.e resultant string