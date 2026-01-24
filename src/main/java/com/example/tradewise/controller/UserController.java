package com.example.tradewise.controller;

import com.example.tradewise.entity.User;
import com.example.tradewise.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/list")
    public ResponseEntity<List<User>> listUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addUser(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        String role = request.get("role");

        if (userService.findByUsername(username) != null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "用户名已存在");
            return ResponseEntity.badRequest().body(error);
        }

        User user = userService.createUser(username, password, role);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", user);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }
}