import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StreamAPI {
  public static void main(String[] args) {
    // Create a stream of integers
    List<Integer> number = List.of(1, 2, 3, 4, 5, 6);
    number.stream()
        .filter(n -> n%2 ==0)
        .collect(Collectors.toList())
        .forEach(System.out::println);

    int nums[] = { 1, 2, 1, 2, 6, 7, 5, -1 };
    System.out.println(String.join(":", Arrays.stream(nums).mapToObj(String::valueOf).toArray(String[]::new)));
  }
}