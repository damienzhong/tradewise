package com.example.tradewise.controller;

import com.example.tradewise.entity.SymbolConfig;
import com.example.tradewise.service.SymbolConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/symbol-config")
public class SymbolConfigController {

    @Autowired
    private SymbolConfigService symbolConfigService;

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getAllSymbols() {
        List<SymbolConfig> symbols = symbolConfigService.getAllSymbols();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", symbols);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/enabled")
    public ResponseEntity<Map<String, Object>> getEnabledSymbols() {
        List<String> symbols = symbolConfigService.getEnabledSymbolNames();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", symbols);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addSymbol(@RequestBody SymbolConfig symbolConfig) {
        Map<String, Object> response = new HashMap<>();
        boolean success = symbolConfigService.addSymbol(symbolConfig);
        response.put("success", success);
        response.put("message", success ? "添加成功" : "添加失败，币对可能已存在");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Map<String, Object>> updateSymbol(@PathVariable Long id, @RequestBody SymbolConfig symbolConfig) {
        symbolConfig.setId(id);
        Map<String, Object> response = new HashMap<>();
        boolean success = symbolConfigService.updateSymbol(symbolConfig);
        response.put("success", success);
        response.put("message", success ? "更新成功" : "更新失败");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, Object>> deleteSymbol(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        boolean success = symbolConfigService.deleteSymbol(id);
        response.put("success", success);
        response.put("message", success ? "删除成功" : "删除失败");
        return ResponseEntity.ok(response);
    }
}
