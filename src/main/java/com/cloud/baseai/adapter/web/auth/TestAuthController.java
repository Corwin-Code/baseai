package com.cloud.baseai.adapter.web.auth;

import com.cloud.baseai.application.kb.dto.DocumentDTO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
public class TestAuthController {

    // 基于角色的权限控制
    @PreAuthorize("hasRole('USER')")
    @GetMapping
    public List<Object> listDocuments() {
        return new ArrayList<>();
    }

    // 基于资源的权限控制
    @PreAuthorize("hasPermission(#documentId, 'DOCUMENT', 'READ')")
    @GetMapping("/{documentId}")
    public Object getDocument(@PathVariable Long documentId) {
        return new Object();
    }

    // 基于租户的权限控制
    @PreAuthorize("hasPermission(#tenantId, 'TENANT', 'ADMIN')")
    @PostMapping("/batch-import")
    public void batchImport(@RequestParam Long tenantId,
                            @RequestBody List<DocumentDTO> documents) {
    }

    // 复合权限控制
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and @securityService.isOwner(#documentId, authentication.principal.id))")
    @DeleteMapping("/{documentId}")
    public void deleteDocument(@PathVariable Long documentId) {
    }
}