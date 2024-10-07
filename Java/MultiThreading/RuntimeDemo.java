package Java.MultiThreading;

import java.io.BufferedReader; 
import java.io.InputStreamReader;
import java.io.File;
import java.io.IOException;

public class RuntimeDemo {
    public static void main(String[] args) throws Exception {
        // number of processors
        System.out.println(Runtime.getRuntime().availableProcessors());  

        // free memory and available memory
        Runtime r=Runtime.getRuntime();  
        System.out.println("Total Memory: "+r.totalMemory());  
        System.out.println("Free Memory: "+r.freeMemory());  

        // opens calculator app
        Runtime.getRuntime().exec("open -a Calculator");

        // listing files
        File workingDir = new File("./Java/MultiThreading"); // Absolute path
        try {
            // Check if the directory exists and is a directory
            if (workingDir.exists() && workingDir.isDirectory()) {
                // Execute the 'ls -l' command in the specified directory
                Process process = Runtime.getRuntime().exec(new String[]{"bash", "-c", "ls -l"}, null, workingDir);
                
                // Reading output (if needed)
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }

                process.waitFor();  // Wait for process to complete
            } else {
                System.out.println("Directory does not exist or is not a directory.");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        // Ping 
        Process process = Runtime.getRuntime().exec("ping google.com");
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line); // Prints output from the ping command
        }
    }
}
