package net.houdaifa.accountservice.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AccountMcpConfig {

    @Bean
    public ToolCallbackProvider accountToolCallbackProvider(AccountMcpTools accountMcpTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(accountMcpTools)
                .build();
    }
}
