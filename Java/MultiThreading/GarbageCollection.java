package Java.MultiThreading;

public class GarbageCollection {
    public void finalize() { 
        System.out.println("object is garbage collected");
    }
    public static void main(String[] args) {
        GarbageCollection s1=new GarbageCollection();  
        GarbageCollection s2=new GarbageCollection();  
        System.out.println(s1.hashCode() + "\n" + s2.getClass());
        s1=null;  
        s2=null;  
        System.gc();  
    }
}
