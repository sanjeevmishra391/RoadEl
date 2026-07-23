# Sorting

## Quick Reference — All Algorithms

| Algorithm | Best | Average | Worst | Space | Stable | Notes |
|---|---|---|---|---|---|---|
| Bubble Sort | O(n) | O(n²) | O(n²) | O(1) | Yes | Only if optimized with early exit |
| Selection Sort | O(n²) | O(n²) | O(n²) | O(1) | No | Minimum swaps |
| Insertion Sort | O(n) | O(n²) | O(n²) | O(1) | Yes | Best for small/nearly sorted |
| Merge Sort | O(n log n) | O(n log n) | O(n log n) | O(n) | Yes | Linked lists, external sort |
| Quick Sort | O(n log n) | O(n log n) | O(n²) | O(log n) | No | Cache-friendly, fastest in practice |
| Heap Sort | O(n log n) | O(n log n) | O(n log n) | O(1) | No | Guaranteed O(n log n) in-place |
| Counting Sort | O(n+k) | O(n+k) | O(n+k) | O(k) | Yes | Only for integers in small range |
| Radix Sort | O(nk) | O(nk) | O(nk) | O(n+k) | Yes | Integers or fixed-length strings |

## Interview Decision Guide
```
Small array (< 20 elements)       → Insertion Sort
Nearly sorted                     → Insertion Sort
Need stable sort                  → Merge Sort
Need guaranteed O(n log n)        → Merge Sort or Heap Sort
Best average performance          → Quick Sort
Sorting linked list               → Merge Sort (no random access needed)
Integers in known range           → Counting Sort
Memory is critical                → Heap Sort (in-place, O(1) space)
```

## Custom Comparator in Java (Interview Favorite)
```java
// Sort by string length, then alphabetically
Arrays.sort(arr, (a, b) -> a.length() != b.length()
    ? a.length() - b.length()
    : a.compareTo(b));

// Sort intervals by start time
Arrays.sort(intervals, (a, b) -> a[0] - b[0]);

// Largest number from array of integers
Arrays.sort(nums, (a, b) -> (b + a).compareTo(a + b));
```

## Key Interview Problems

| Problem | Approach | Key Insight |
|---|---|---|
| Sort Colors (Dutch Flag) | 3-way partition | Maintain low, mid, high pointers |
| Kth Largest Element | Quick Select | Partial quick sort — O(n) avg |
| Merge Intervals | Sort by start | Sort + check overlap with last interval |
| Meeting Rooms | Sort by start | Check if consecutive intervals overlap |
| Largest Number | Custom comparator | Compare `b+a` vs `a+b` as strings |

## Common Mistakes
- Forgetting Quick Sort worst case is O(n²) on sorted arrays → always use random pivot
- Assuming stable sort when using Arrays.sort on primitives (it's not stable for primitives)
- `Arrays.sort` on objects IS stable (uses TimSort); on primitives it's dual-pivot QuickSort

## How Each Algorithm Works — With Code

### Selection Sort [Code](./SelectionSort.java)

Selection Sort is a comparison-based sorting algorithm. It sorts an array by repeatedly selecting the smallest (or largest) element from the unsorted portion and swapping it with the first unsorted element. This process continues until the entire array is sorted.

1. First we find the smallest element and swap it with the first element. This way we get the smallest element at its correct position.
2. Then we find the smallest among remaining elements (or second smallest) and swap it with the second element.
3. We keep doing this until we get all elements moved to correct position.

```java
// Time: O(n²)  Space: O(1)  Stable: No
void selectionSort(int[] arr) {
    int n = arr.length;
    for (int i = 0; i < n - 1; i++) {
        int minIdx = i;
        for (int j = i + 1; j < n; j++)
            if (arr[j] < arr[minIdx]) minIdx = j;
        int temp = arr[minIdx]; arr[minIdx] = arr[i]; arr[i] = temp;
    }
}
```

> Use when: **minimum number of swaps** is critical — it does at most n-1 swaps.

### Complexity Analysis of Selection Sort
Time Complexity: O(n<sup>2</sup>) ,as there are two nested loops:

One loop to select an element of Array one by one = O(n)
Another loop to compare that element with every other Array element = O(n)
Therefore overall complexity = O(n) * O(n) = O(n*n) = O(n<sup>2</sup>)

> Selection Sort is not stable as it may change the relative order of equal elements.

> Selection Sort is an in-place sorting algorithm and requires only O(1) additional space.

## Bubble Sort [Code](./BubbleSort.java)

Repeatedly swaps adjacent elements if they are in the wrong order. After each pass, the largest unsorted element bubbles to its correct position at the end.

```java
// Time: O(n²)  Space: O(1)  Stable: Yes
void bubbleSort(int[] arr) {
    int n = arr.length;
    for (int i = 0; i < n - 1; i++) {
        boolean swapped = false;                  // early exit optimisation
        for (int j = 0; j < n - i - 1; j++) {
            if (arr[j] > arr[j + 1]) {
                int temp = arr[j]; arr[j] = arr[j + 1]; arr[j + 1] = temp;
                swapped = true;
            }
        }
        if (!swapped) break;                      // already sorted — O(n) best case
    }
}
```

**Complexity:** Best O(n) · Average O(n²) · Worst O(n²) · Space O(1)

> Use when: almost never in practice — only for teaching. The early-exit trick makes it O(n) on sorted input.

---

## Insertion Sort [Code](./InsertionSort.java)

Builds a sorted portion one element at a time. Takes the next element and inserts it into its correct position in the already-sorted left portion.

```java
// Time: O(n²) worst, O(n) best  Space: O(1)  Stable: Yes
void insertionSort(int[] arr) {
    for (int i = 1; i < arr.length; i++) {
        int key = arr[i];
        int j = i - 1;
        while (j >= 0 && arr[j] > key) {
            arr[j + 1] = arr[j];    // shift right to make room
            j--;
        }
        arr[j + 1] = key;           // insert in correct position
    }
}
```

**Complexity:** Best O(n) · Average O(n²) · Worst O(n²) · Space O(1)

> Use when: array is **small** (< 20 elements) or **nearly sorted**. Java's TimSort uses insertion sort for small subarrays.

---

## Merge Sort [Code](./MergeSort.java)

Divide and conquer. Recursively splits array in half, sorts each half, then merges them back together. The merge step is where the real work happens.

```java
// Time: O(n log n) all cases  Space: O(n)  Stable: Yes
void mergeSort(int[] arr, int left, int right) {
    if (left >= right) return;
    int mid = left + (right - left) / 2;
    mergeSort(arr, left, mid);
    mergeSort(arr, mid + 1, right);
    merge(arr, left, mid, right);
}

void merge(int[] arr, int left, int mid, int right) {
    int[] temp = new int[right - left + 1];
    int i = left, j = mid + 1, k = 0;
    while (i <= mid && j <= right)
        temp[k++] = (arr[i] <= arr[j]) ? arr[i++] : arr[j++];
    while (i <= mid)  temp[k++] = arr[i++];
    while (j <= right) temp[k++] = arr[j++];
    for (int l = 0; l < temp.length; l++) arr[left + l] = temp[l];
}
```

**Complexity:** Best O(n log n) · Average O(n log n) · Worst O(n log n) · Space O(n)

> Use when: **stable sort required**, sorting **linked lists** (no random access needed), or **external sort** (data too large for memory).

---

## Quick Sort [Code](./QuickSort.java)

Divide and conquer. Picks a pivot, partitions the array so all elements < pivot are left, > pivot are right. Pivot is now in its final position. Recursively sort both halves.

```java
// Time: O(n log n) avg, O(n²) worst  Space: O(log n)  Stable: No
void quickSort(int[] arr, int low, int high) {
    if (low < high) {
        int pivotIdx = partition(arr, low, high);
        quickSort(arr, low, pivotIdx - 1);
        quickSort(arr, pivotIdx + 1, high);
    }
}

int partition(int[] arr, int low, int high) {
    // Randomise pivot to avoid O(n²) on sorted input
    int randIdx = low + (int)(Math.random() * (high - low + 1));
    swap(arr, randIdx, high);

    int pivot = arr[high];
    int i = low - 1;                          // i = last position of "less than" zone
    for (int j = low; j < high; j++) {
        if (arr[j] <= pivot) {
            i++;
            swap(arr, i, j);
        }
    }
    swap(arr, i + 1, high);                   // place pivot in correct position
    return i + 1;
}
```

**Complexity:** Best O(n log n) · Average O(n log n) · Worst O(n²) · Space O(log n)

> Use when: **best average performance** in practice. Cache-friendly because it works in-place.
> Always randomise the pivot — fixes the O(n²) worst case on sorted/reverse-sorted input.

---

## Heap Sort

Uses a max-heap. Build heap from array, then repeatedly extract the max and place it at the end.

```java
// Time: O(n log n) all cases  Space: O(1)  Stable: No
void heapSort(int[] arr) {
    int n = arr.length;
    // Build max heap (heapify from last non-leaf node down to root)
    for (int i = n / 2 - 1; i >= 0; i--)
        heapify(arr, n, i);
    // Extract max one by one — place at end of array
    for (int i = n - 1; i > 0; i--) {
        swap(arr, 0, i);          // move current max to end
        heapify(arr, i, 0);      // restore heap property on reduced heap
    }
}

void heapify(int[] arr, int n, int i) {
    int largest = i, left = 2 * i + 1, right = 2 * i + 2;
    if (left  < n && arr[left]  > arr[largest]) largest = left;
    if (right < n && arr[right] > arr[largest]) largest = right;
    if (largest != i) {
        swap(arr, i, largest);
        heapify(arr, n, largest);
    }
}
```

**Complexity:** Best O(n log n) · Average O(n log n) · Worst O(n log n) · Space O(1)

> Use when: **guaranteed O(n log n) with O(1) space** is required. Not cache-friendly — use Merge Sort if stability matters.

---

## Counting Sort

Not comparison-based. Works by counting occurrences of each value. Only works on non-negative integers within a known range.

```java
// Time: O(n + k)  Space: O(k)  Stable: Yes  (k = range of values)
void countingSort(int[] arr, int maxVal) {
    int[] count = new int[maxVal + 1];
    for (int n : arr) count[n]++;                 // count each value
    for (int i = 1; i <= maxVal; i++) count[i] += count[i - 1];  // prefix sum → positions
    int[] output = new int[arr.length];
    for (int i = arr.length - 1; i >= 0; i--)     // fill from right → stable
        output[--count[arr[i]]] = arr[i];
    System.arraycopy(output, 0, arr, 0, arr.length);
}
```

**Complexity:** O(n + k) time · O(k) space

> Use when: integers in a small known range (e.g., ages 0–120, scores 0–100). Linear time but only for integers.

---

## Quick Select — Kth Largest/Smallest in O(n) Average

Not a full sort — finds the kth element without sorting the entire array. Uses the partition step from QuickSort.

```java
// Time: O(n) average, O(n²) worst  Space: O(log n)
// Find kth LARGEST element (k=1 means largest)
public int findKthLargest(int[] nums, int k) {
    return quickSelect(nums, 0, nums.length - 1, nums.length - k);
    // kth largest = (n-k)th smallest → target index in sorted order
}

int quickSelect(int[] arr, int low, int high, int k) {
    int pivotIdx = partition(arr, low, high);   // same partition as QuickSort
    if (pivotIdx == k)      return arr[pivotIdx];
    else if (pivotIdx < k)  return quickSelect(arr, pivotIdx + 1, high, k);
    else                    return quickSelect(arr, low, pivotIdx - 1, k);
}
```

> **Interview key point:** QuickSelect is O(n) average — faster than sorting (O(n log n)) when you only need the kth element. Randomise pivot to avoid O(n²) worst case.