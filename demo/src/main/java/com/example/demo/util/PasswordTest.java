package com.example.demo.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码测试工具
 */
public class PasswordTest {
    
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // 数据库中的密码
        String dbPassword = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi";
        
        // 测试密码
        String inputPassword = "123456";
        
        // 验证
        boolean matches = encoder.matches(inputPassword, dbPassword);
        
        System.out.println("数据库密码: " + dbPassword);
        System.out.println("输入密码: " + inputPassword);
        System.out.println("验证结果: " + matches);
        
        // 生成新密码
        String newEncoded = encoder.encode("123456");
        System.out.println("新生成的密码: " + newEncoded);
        System.out.println("新密码验证: " + encoder.matches("123456", newEncoded));
    }
}
