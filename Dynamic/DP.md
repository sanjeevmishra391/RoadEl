## About
The main idea of dynamic programming is to consider a significant problem and break it into smaller, individualized components. When it comes to implementation, optimal techniques rely on data storage and reuse to increase algorithm efficiency.  
  
Used for problems that have overlapping subproblems and optimal substructure property.

If any problem can be divided into subproblems, which in turn are divided into smaller subproblems, and if there are overlapping among these subproblems, then the solutions to these subproblems can be saved for future reference. In this way, efficiency of the CPU can be enhanced. This method of solving a solution is referred to as dynamic programming.

## When to Use Dynamic Programming (DP)?

1. **Optimal Substructure:**  
Optimal substructure means that we combine the optimal results of subproblems to achieve the optimal result of the bigger problem.

2. **Overlapping Subproblems:**  
The same subproblems are solved repeatedly in different parts of the problem.

## Approaches of Dynamic Programming (DP)
Dynamic programming can be achieved using two approaches:

1. **Top-Down Approach (Memoization):**  
In the top-down approach, also known as memoization, we start with the final solution and recursively break it down into smaller subproblems. To avoid redundant calculations, we store the results of solved subproblems in a memoization table.  
  
    Let’s breakdown Top down approach:

- Starts with the final solution and recursively breaks it down into smaller subproblems.
- Stores the solutions to subproblems in a table to avoid redundant calculations.
- Suitable when the number of subproblems is large and many of them are reused.

    > Slow due to a lot of recursive calls and return statements

2. **Bottom-Up Approach (Tabulation):**  
In the bottom-up approach, also known as tabulation, we start with the smallest subproblems and gradually build up to the final solution. We store the results of solved subproblems in a table to avoid redundant calculations.  
    
    Let’s breakdown Bottom-up approach:

- Starts with the smallest subproblems and gradually builds up to the final solution.
- Fills a table with solutions to subproblems in a bottom-up manner.
- Suitable when the number of subproblems is small and the optimal solution can be directly computed from the solutions to smaller subproblems.

    > Fast, as we directly access previous states from the table


<details>
<summary><span style="font-size:1.4em;"><b>Example</b></span></summary>

> Fibonacci sequence: 0, 1, 1, 2, 3, 5, 8, 13, 21, 34, …
> - Zero element is 0, First element is 1
> - nth element = (n-1)th element + (n-2)the element
> - Fib(n) = Fib(n-1) + Fib(n-2)

**Brute Force Approach:**  

To find the nth Fibonacci number using a brute force approach, you would simply add the (n-1)th and (n-2)th Fibonacci numbers. This would work, but it would be inefficient for large values of n, as it would require calculating all the previous Fibonacci numbers.

**Dynamic Programming Approach:**  

Fibonacci Series using Dynamic Programming:
- Subproblems: F(0), F(1), F(2), F(3), …
- Store Solutions: Create a table to store the values of F(n) as they are calculated.
- Build Up Solutions: For F(n), look up F(n-1) and F(n-2) in the table and add them.
- Avoid Redundancy: The table ensures that each subproblem (e.g., F(2)) is solved only once

**Code:**

1. **Recursive Solution :**  
    ```java
    fib(n) {
        // base case
        if(n == 0 || n == 1)
            return n;

        return fib(n-1) + fib(n-2);
    }
    ```

2. **Dynamic Programming Solution :**
    ```java
    fib(n) {
        if(n == 0)
            return 0;

        int dp[] = new int[n+1];
        dp[1] = 1;

        for(int i=2; i<=n; i++) {
            dp[i] = dp[i-1] + dp[i-2];
        }

        return dp[n];
    }
    ```

3. **Optimised Solution :**  
    ```java
    fib(n) {
        int a, b, c;
        a = 0;
        b = 1;

        for(int i=2; i<=n; i++) {
            c = a + b;
            a = b;
            b = c;
        }

    }
    ```

</details>

## Patterns

Few patterns that can be found in different problems

**Minimum (Maximum) Path to Reach a Target**  
***
Problem list: [https://leetcode.com/list/55ac4kuc](https://leetcode.com/list/55ac4kuc)

Generate problem statement for this pattern

**Statement** 
> Given a target find minimum (maximum) cost / path / sum to reach the target.

**Approach**  
> Choose minimum (maximum) path among all possible paths before the current state, then add value for the current state.

```
routes[i] = min(routes[i-1], routes[i-2], ... , routes[i-k]) + cost[i]
```
Generate optimal solutions for all values in the target and return the value for the target.

- Top-Down
    ```java
    for (int j = 0; j < ways.size(); ++j) {
        result = min(result, topDown(target - ways[j]) + cost/ path / sum);
    }
    return memo[/*state parameters*/] = result;
    ```

- Bottom-Up
    ```java
    for (int i = 1; i <= target; ++i) {
        for (int j = 0; j < ways.size(); ++j) {
            if (ways[j] <= i) {
                dp[i] = min(dp[i], dp[i - ways[j]] + cost / path / sum) ;
            }
        }
    }

    return dp[target]
    ```

## List of problems:

[Utility](../roadel/Utility.java) : Have created a utility class which is helpful for printing input and output along with labels. It can take multiple inputs and outputs along with the labels of each argument.

1. [Fibonacci](./Fibonacci.java)
2. [ThreeNumberProblem](./ThreeNumberProblem.java)
3. [MinCostClimbingStairs](./MinCostClimbingStairs.java) : [Link](https://leetcode.com/problems/min-cost-climbing-stairs/description/?envType=list&envId=m1emb7zs)
4. [MinimumPathSum](./MinimumPathSum.java) : [Link](https://leetcode.com/problems/minimum-path-sum/description/)
5. [Triangle](./Triangle.java) : [Link](https://leetcode.com/problems/triangle/description/?envType=list&envId=55ac4kuc)
6. [DungeonGame](./DungeonGame.java) : [Link](https://leetcode.com/problems/dungeon-game/description/)
7. [MinimumCostForTickets](./MinimumCostForTickets.java) : [Link](https://leetcode.com/problems/minimum-cost-for-tickets/description/)
8. [MinimumFallPathSum](./MinimumFallPathSum.java) : [Link](https://leetcode.com/problems/minimum-falling-path-sum/description/)
9. [TwoKeysKeyboard](./TwoKeysKeyboard.java) : [Link](https://leetcode.com/problems/2-keys-keyboard/description/)

### Referrences

* https://leetcode.com/discuss/general-discussion/458695/dynamic-programming-patterns