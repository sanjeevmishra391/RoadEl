# Stack

## Pattern Recognition Triggers
- "Next greater / smaller element" → **Monotonic Stack**
- "Valid parentheses / balanced brackets" → **Stack**
- "Evaluate expression" / "infix to postfix" → **Stack**
- "Largest rectangle / max area" → **Monotonic Stack**
- "Undo/redo operations" → **Stack**

## Complexity
- Push / Pop / Peek: **O(1)**
- Space: **O(n)**

## Monotonic Stack — The Most Important Stack Pattern

A stack that maintains elements in increasing or decreasing order.

### Monotonic Decreasing Stack (Next Greater Element)
```java
// For each element, find the next element greater than it
int[] result = new int[n];
Arrays.fill(result, -1);
Deque<Integer> stack = new ArrayDeque<>();  // stores indices
for (int i = 0; i < n; i++) {
    while (!stack.isEmpty() && nums[stack.peek()] < nums[i]) {
        result[stack.pop()] = nums[i];   // nums[i] is the next greater
    }
    stack.push(i);
}
```

### Monotonic Increasing Stack (Next Smaller Element)
Same pattern — just flip the comparison: `nums[stack.peek()] > nums[i]`

### Key Insight
When you pop an element from the monotonic stack, you've found its answer.
- Popping because `nums[i] > top` → `nums[i]` is the **next greater**
- Popping because `nums[i] < top` → `nums[i]` is the **next smaller**

## Key Interview Problems

| Problem | Technique | Key Insight |
|---|---|---|
| Valid Parentheses | Stack | Push open, pop+match on close |
| Min Stack | Two stacks | Second stack tracks current min |
| Next Greater Element | Monotonic decreasing | Pop when current > top |
| Daily Temperatures | Monotonic decreasing | Store indices, answer = i - popped_index |
| Largest Rectangle in Histogram | Monotonic increasing | Pop when current < top; width = i - stack.peek() - 1 |
| Trapping Rain Water | Monotonic | Max left + max right - height at each bar |
| Decode String | Stack | Push count + current string on `[` |

## Common Mistakes
- Using `Stack<>` class in Java — prefer `Deque<Integer> stack = new ArrayDeque<>()` (faster)
- In histogram problems: forgetting to handle remaining elements in the stack after the loop
- Off-by-one in width calculation: `width = i - stack.peek() - 1` (not just `i - stack.peek()`)

---

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