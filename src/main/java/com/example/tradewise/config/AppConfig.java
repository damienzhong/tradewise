package com.example.tradewise.config;

import com.example.tradewise.service.*;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.client.RestTemplate;

import java.util.Properties;

@Configuration
public class AppConfig {
    
    @Value("${spring.mail.host}")
    private String mailHost;
    
    @Value("${spring.mail.port}")
    private int mailPort;
    
    @Value("${spring.mail.username}")
    private String mailUsername;
    
    @Value("${spring.mail.password}")
    private String mailPassword;
    
    // 代理配置（可选）
    @Value("${http.proxyHost:#{null}}")
    private String proxyHost;
    
    @Value("${http.proxyPort:#{null}}")
    private String proxyPort;
    
    @Value("${https.proxyHost:#{null}}")
    private String httpsProxyHost;
    
    @Value("${https.proxyPort:#{null}}")
    private String httpsProxyPort;
    
    @Value("${http.proxyUser:#{null}}")
    private String proxyUser;
    
    @Value("${http.proxyPassword:#{null}}")
    private String proxyPassword;
    
    @Bean
    public RestTemplate restTemplate() {
        // 配置连接池
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(100); // 最大连接数
        connectionManager.setDefaultMaxPerRoute(20); // 每个路由的最大连接数
        
        CloseableHttpClient httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .build();
        
        // 优先使用HTTPS代理设置，如果没有则使用HTTP代理设置
        String actualProxyHost = httpsProxyHost != null ? httpsProxyHost : proxyHost;
        String actualProxyPort = httpsProxyPort != null ? httpsProxyPort : proxyPort;
        
        CloseableHttpClient httpClientWithProxy;
        if (actualProxyHost != null && actualProxyPort != null) {
            // 配置代理
            org.apache.http.HttpHost proxy = new org.apache.http.HttpHost(actualProxyHost, Integer.parseInt(actualProxyPort));
            org.apache.http.impl.client.HttpClientBuilder builder = HttpClients.custom()
                .setConnectionManager(connectionManager);
            
            if (proxyUser != null && proxyPassword != null) {
                // 带认证的代理
                org.apache.http.auth.UsernamePasswordCredentials credentials = 
                    new org.apache.http.auth.UsernamePasswordCredentials(proxyUser, proxyPassword);
                org.apache.http.impl.client.BasicCredentialsProvider credentialsProvider = 
                    new org.apache.http.impl.client.BasicCredentialsProvider();
                credentialsProvider.setCredentials(
                    org.apache.http.auth.AuthScope.ANY, 
                    credentials
                );
                builder.setDefaultCredentialsProvider(credentialsProvider);
            }
            
            httpClientWithProxy = builder
                .setProxy(proxy)
                .build();
        } else {
            httpClientWithProxy = httpClient; // 使用没有代理的客户端
        }
        
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClientWithProxy);
        
        // 为了能够读取响应头和内容进行调试，使用缓冲包装
        BufferingClientHttpRequestFactory bufferingFactory = new BufferingClientHttpRequestFactory(factory);
        
        factory.setConnectTimeout(15000); // 15秒连接超时
        factory.setReadTimeout(45000);    // 45秒读取超时
        
        return new RestTemplate(bufferingFactory);
    }
    
    @Bean
    public JavaMailSender mailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mailHost);
        mailSender.setPort(mailPort);
        mailSender.setUsername(mailUsername);
        mailSender.setPassword(mailPassword);
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "false"); // 关闭调试日志
        
        return mailSender;
    }

    @Bean
    public SmartDataEngine smartDataEngine() {
        return new SmartDataEngine();
    }

    @Bean
    public TrendMomentumResonanceModel trendMomentumResonanceModel() {
        return new TrendMomentumResonanceModel();
    }

    @Bean
    public InstitutionalFlowModel institutionalFlowModel() {
        return new InstitutionalFlowModel();
    }

    @Bean
    public VolatilityBreakoutModel volatilityBreakoutModel() {
        return new VolatilityBreakoutModel();
    }

    @Bean
    public KeyLevelBattlefieldModel keyLevelBattlefieldModel() {
        return new KeyLevelBattlefieldModel();
    }

    @Bean
    public SentimentExtremesModel sentimentExtremesModel() {
        return new SentimentExtremesModel();
    }

    @Bean
    public CorrelationArbitrageModel correlationArbitrageModel() {
        return new CorrelationArbitrageModel();
    }

    @Bean
    public SignalFusionEngine signalFusionEngine() {
        return new SignalFusionEngine();
    }

    @Bean
    public SmartExecutionOptimizer smartExecutionOptimizer() {
        return new SmartExecutionOptimizer();
    }
}