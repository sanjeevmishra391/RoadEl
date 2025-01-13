## Selection Sorting

[Code](./SelectionSort.java)

Selection Sort is a comparison-based sorting algorithm. It sorts an array by repeatedly selecting the smallest (or largest) element from the unsorted portion and swapping it with the first unsorted element. This process continues until the entire array is sorted.

1. First we find the smallest element and swap it with the first element. This way we get the smallest element at its correct position.
2. Then we find the smallest among remaining elements (or second smallest) and swap it with the second element.
3. We keep doing this until we get all elements moved to correct position.

### Complexity Analysis of Selection Sort
Time Complexity: O(n<sup>2</sup>) ,as there are two nested loops:

One loop to select an element of Array one by one = O(n)
Another loop to compare that element with every other Array element = O(n)
Therefore overall complexity = O(n) * O(n) = O(n*n) = O(n<sup>2</sup>)

> Selection Sort is not stable as it may change the relative order of equal elements.

> Selection Sort is an in-place sorting algorithm and requires only O(1) additional space.

## Bubble Sorting

Bubble Sort is the simplest sorting algorithm that works by repeatedly swapping the adjacent elements if they are in the wrong order. This algorithm is not suitable for large data sets as its average and worst-case time complexity are quite high.

1. We sort the array using multiple passes. After the first pass, the maximum element goes to end (its correct position). Same way, after second pass, the second largest element goes to second last position and so on.
2. In every pass, we process only those elements that have already not moved to correct position. After k passes, the largest k elements must have been moved to the last k positions.
3. In a pass, we consider remaining elements and compare all adjacent and swap if larger element is before a smaller element. If we keep doing this, we get the largest (among the remaining elements) at its correct position.

### Complexity Analysis of Bubble Sort:
Time Complexity: O(n<sup>2</sup>)  
Auxiliary Space: O(1)

> Bubble sort takes minimum time (Order of n) when elements are already sorted. Hence it is best to check if the array is already sorted or not beforehand, to avoid O(n<sup>2</sup>) time complexity.

> In-place algorithm, no need of extra space

> The bubble sort algorithm is stable.

## Insertion Sort

Insertion sort is a simple sorting algorithm that works by iteratively inserting each element of an unsorted list into its correct position in a sorted portion of the list. It is like sorting playing cards in your hands. You split the cards into two groups: the sorted cards and the unsorted cards. Then, you pick a card from the unsorted group and put it in the right place in the sorted group.

1. We start with second element of the array as first element in the array is assumed to be sorted.
2. Compare second element with the first element and check if the second element is smaller then swap them.
3. Move to the third element and compare it with the first two elements and put at its correct position
4. Repeat until the entire array is sorted.

### Time Complexity of Insertion Sort
Best case: O(n) , If the list is already sorted, where n is the number of elements in the list.  
Average case: O(n<sup>2</sup> ) , If the list is randomly ordered  
Worst case: O(n<sup>2</sup> ) , If the list is in reverse order

> Stable and In-place

>  Insertion sort is used when number of elements is small. It can also be useful when the input array is almost sorted, and only a few elements are misplaced in a complete big array. 

## Merge Sort

Merge sort is a sorting algorithm that follows the divide-and-conquer approach. It works by recursively dividing the input array into smaller subarrays and sorting those subarrays then merging them back together to obtain the sorted array.

### Complexity Analysis of Merge Sort:
Time Complexity:
Best Case: O(n log n), When the array is already sorted or nearly sorted.  
Average Case: O(n log n), When the array is randomly ordered.  
Worst Case: O(n log n), When the array is sorted in reverse order.  

Auxiliary Space: O(n), Additional space is required for the temporary array used during merging.

> External Sorting, when dataset is too large to fit in memory

> It is a preferred algorithm for sorting Linked lists.

> Stable sorting but not in-place

> QuickSort is more cache friendly because it works in-place.

## Quick Sort

QuickSort is a sorting algorithm based on the Divide and Conquer that picks an element as a pivot and partitions the given array around the picked pivot by placing the pivot in its correct position in the sorted array.

There are mainly three steps in the algorithm:

1. Choose a Pivot: Select an element from the array as the pivot. The choice of pivot can vary (e.g., first element, last element, random element, or median).
2. Partition the Array: Rearrange the array around the pivot. After partitioning, all elements smaller than the pivot will be on its left, and all elements greater than the pivot will be on its right. The pivot is then in its correct position, and we obtain the index of the pivot.
3. Recursively Call: Recursively apply the same process to the two partitioned sub-arrays (left and right of the pivot).
4. Base Case: The recursion stops when there is only one element left in the sub-array, as a single element is already sorted.

### Choice of Pivot
There are many different choices for picking pivots.

1. Always pick the first (or last) element as a pivot. The problem with this approach is it ends up in the worst case when array is already sorted.
2. Pick a random element as a pivot. This is a preferred approach because it does not have a pattern for which the worst case happens.
3. Pick the median element is pivot. This is an ideal approach in terms of time complexity as we can find median in linear time and the partition function will always divide the input array into two halves. But it takes more time on average as median finding has high constants

### Complexity Analysis of Quick Sort
Time Complexity:
- Best Case: (Ω(n log n)), Occurs when the pivot element divides the array into two equal halves.
- Average Case (θ(n log n)), On average, the pivot divides the array into two parts, but not necessarily equal.
- Worst Case: (O(n²)), Occurs when the smallest or largest element is always chosen as the pivot (e.g., sorted arrays).  

Auxiliary Space: O(n), due to recursive call stack

> Cache Friendly  

> It is not a good choice for small data sets.