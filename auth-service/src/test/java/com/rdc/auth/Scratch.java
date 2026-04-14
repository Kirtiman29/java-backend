import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class Scratch {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println("Admin@Rdc2020: " + encoder.encode("Admin@Rdc2020"));
        System.out.println("Admin@Rdc2026: " + encoder.encode("Admin@Rdc2026"));
        System.out.println("admin: " + encoder.encode("admin"));
        System.out.println("password: " + encoder.encode("password"));
    }
}
