package com.mock;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/restconf/data/ietf-interfaces:interfaces")
public class InterfaceController {

    private Map<String, Object> db = new HashMap<>();

    @PostMapping
    public Object create(@RequestBody Map<String, Object> body) {
        db.put("interface", body);
        return Map.of("message", "Interface created", "data", body);
    }

    @GetMapping("/interface={name}")
    public Object get(@PathVariable String name) {
        return db.getOrDefault("interface", Map.of("error", "Not found"));
    }

    @PutMapping("/interface={name}")
    public Object update(@PathVariable String name,
                         @RequestBody Map<String, Object> body) {
        db.put("interface", body);
        return Map.of("message", "Interface updated", "data", body);
    }

    @DeleteMapping("/interface={name}")
    public Object delete(@PathVariable String name) {
        db.remove("interface");
        return Map.of("message", "Interface deleted");
    }
}
