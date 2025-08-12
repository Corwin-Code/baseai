package com.cloud.baseai.infrastructure.utils;

import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

/**
 * <h1>RSA密钥对生成工具</h1>
 *
 * <p>这个工具类用于生成JWT签名所需的RSA密钥对。在生产环境部署时，
 * 应该使用这个工具预先生成密钥对，然后将Base64编码的密钥存储在配置中。</p>
 *
 * <p><b>安全最佳实践：</b></p>
 * <ul>
 * <li><b>预生成密钥：</b>在部署前生成，避免运行时生成导致重启后令牌失效</li>
 * <li><b>密钥轮换：</b>定期更换密钥对，增强安全性</li>
 * <li><b>安全存储：</b>密钥应存储在安全的密钥管理服务中</li>
 * <li><b>环境隔离：</b>不同环境使用不同的密钥对</li>
 * </ul>
 *
 * <p><b>使用方法：</b></p>
 * <pre>
 * // 生成新的密钥对
 * RSAKeyPair keyPair = RSAKeyGenerator.generateKeyPair();
 *
 * // 获取Base64编码的密钥（用于配置文件）
 * String privateKey = keyPair.getPrivateKeyBase64();
 * String publicKey = keyPair.getPublicKeyBase64();
 *
 * // 将密钥配置到application.yml中
 * baseai.security.jwt.rsa-private-key: {privateKey}
 * baseai.security.jwt.rsa-public-key: {publicKey}
 * </pre>
 */
@Slf4j
public class RSAKeyGenerator {

    /**
     * RSA密钥长度（位）
     * <p>2048位是目前推荐的最小长度，4096位提供更高安全性但性能较低</p>
     */
    private static final int KEY_SIZE_2048 = 2048;
    private static final int KEY_SIZE_4096 = 4096;

    /**
     * 私有构造函数，防止实例化
     */
    private RSAKeyGenerator() {
        throw new UnsupportedOperationException("这是一个工具类，不应该被实例化");
    }

    /**
     * 生成RSA密钥对（默认2048位）
     *
     * @return RSA密钥对封装对象
     * @throws RuntimeException 当密钥生成失败时抛出
     */
    public static RSAKeyPair generateKeyPair() {
        return generateKeyPair(KEY_SIZE_2048);
    }

    /**
     * 生成指定长度的RSA密钥对
     *
     * @param keySize 密钥长度（位），推荐2048或4096
     * @return RSA密钥对封装对象
     * @throws RuntimeException 当密钥生成失败时抛出
     */
    public static RSAKeyPair generateKeyPair(int keySize) {
        if (keySize < 2048) {
            throw new IllegalArgumentException("RSA密钥长度不能小于2048位");
        }

        try {
            log.info("开始生成{}位RSA密钥对...", keySize);
            long startTime = System.currentTimeMillis();

            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(keySize);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

            long duration = System.currentTimeMillis() - startTime;
            log.info("RSA密钥对生成完成，耗时: {}ms", duration);

            return new RSAKeyPair(privateKey, publicKey);

        } catch (NoSuchAlgorithmException e) {
            log.error("RSA算法不可用", e);
            throw new RuntimeException("无法生成RSA密钥对：RSA算法不可用", e);
        } catch (Exception e) {
            log.error("生成RSA密钥对失败", e);
            throw new RuntimeException("生成RSA密钥对失败", e);
        }
    }

    /**
     * 从Base64字符串加载RSA私钥
     *
     * @param base64PrivateKey Base64编码的私钥字符串
     * @return RSA私钥对象
     * @throws RuntimeException 当密钥格式错误或加载失败时抛出
     */
    public static RSAPrivateKey loadPrivateKeyFromBase64(String base64PrivateKey) {
        if (base64PrivateKey == null || base64PrivateKey.trim().isEmpty()) {
            throw new IllegalArgumentException("私钥字符串不能为空");
        }

        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64PrivateKey);
            java.security.spec.PKCS8EncodedKeySpec spec = new java.security.spec.PKCS8EncodedKeySpec(keyBytes);
            java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) keyFactory.generatePrivate(spec);

        } catch (Exception e) {
            log.error("加载RSA私钥失败", e);
            throw new RuntimeException("无法从Base64字符串加载RSA私钥", e);
        }
    }

    /**
     * 从Base64字符串加载RSA公钥
     *
     * @param base64PublicKey Base64编码的公钥字符串
     * @return RSA公钥对象
     * @throws RuntimeException 当密钥格式错误或加载失败时抛出
     */
    public static RSAPublicKey loadPublicKeyFromBase64(String base64PublicKey) {
        if (base64PublicKey == null || base64PublicKey.trim().isEmpty()) {
            throw new IllegalArgumentException("公钥字符串不能为空");
        }

        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64PublicKey);
            java.security.spec.X509EncodedKeySpec spec = new java.security.spec.X509EncodedKeySpec(keyBytes);
            java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
            return (RSAPublicKey) keyFactory.generatePublic(spec);

        } catch (Exception e) {
            log.error("加载RSA公钥失败", e);
            throw new RuntimeException("无法从Base64字符串加载RSA公钥", e);
        }
    }

    /**
     * 验证RSA密钥对是否匹配
     *
     * @param privateKey RSA私钥
     * @param publicKey  RSA公钥
     * @return 如果密钥对匹配返回true，否则返回false
     */
    public static boolean validateKeyPair(RSAPrivateKey privateKey, RSAPublicKey publicKey) {
        try {
            // 使用私钥签名测试数据
            java.security.Signature signature = java.security.Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);

            String testData = "test-data-for-key-validation";
            signature.update(testData.getBytes());
            byte[] signedData = signature.sign();

            // 使用公钥验证签名
            signature.initVerify(publicKey);
            signature.update(testData.getBytes());
            boolean isValid = signature.verify(signedData);

            log.debug("RSA密钥对验证结果: {}", isValid ? "有效" : "无效");
            return isValid;

        } catch (Exception e) {
            log.error("验证RSA密钥对失败", e);
            return false;
        }
    }

    /**
     * 生成密钥配置示例
     *
     * @param keyPair RSA密钥对
     * @return YAML格式的配置示例
     */
    public static String generateConfigExample(RSAKeyPair keyPair) {
        StringBuilder config = new StringBuilder();
        config.append("# RSA密钥配置示例\n");
        config.append("# 生成时间: ").append(new java.util.Date()).append("\n");
        config.append("# 密钥长度: ").append(keyPair.getKeySize()).append("位\n");
        config.append("\n");
        config.append("baseai:\n");
        config.append("  security:\n");
        config.append("    jwt:\n");
        config.append("      use-rsa: true\n");
        config.append("      rsa-private-key: >\n");
        config.append("        ").append(keyPair.getPrivateKeyBase64()).append("\n");
        config.append("      rsa-public-key: >\n");
        config.append("        ").append(keyPair.getPublicKeyBase64()).append("\n");

        return config.toString();
    }

    /**
     * RSA密钥对封装类
     */
    public static class RSAKeyPair {
        private final RSAPrivateKey privateKey;
        private final RSAPublicKey publicKey;

        public RSAKeyPair(RSAPrivateKey privateKey, RSAPublicKey publicKey) {
            this.privateKey = privateKey;
            this.publicKey = publicKey;
        }

        /**
         * 获取RSA私钥
         */
        public RSAPrivateKey getPrivateKey() {
            return privateKey;
        }

        /**
         * 获取RSA公钥
         */
        public RSAPublicKey getPublicKey() {
            return publicKey;
        }

        /**
         * 获取Base64编码的私钥字符串
         */
        public String getPrivateKeyBase64() {
            return Base64.getEncoder().encodeToString(privateKey.getEncoded());
        }

        /**
         * 获取Base64编码的公钥字符串
         */
        public String getPublicKeyBase64() {
            return Base64.getEncoder().encodeToString(publicKey.getEncoded());
        }

        /**
         * 获取密钥长度（位）
         */
        public int getKeySize() {
            return privateKey.getModulus().bitLength();
        }

        /**
         * 验证密钥对是否有效
         */
        public boolean isValid() {
            return validateKeyPair(privateKey, publicKey);
        }

        /**
         * 获取密钥对的指纹（用于识别）
         */
        public String getFingerprint() {
            try {
                java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(publicKey.getEncoded());
                return Base64.getEncoder().encodeToString(hash).substring(0, 16);
            } catch (Exception e) {
                return "unknown";
            }
        }

        @Override
        public String toString() {
            return String.format("RSAKeyPair{keySize=%d, fingerprint=%s, valid=%s}",
                    getKeySize(), getFingerprint(), isValid());
        }
    }

    /**
     * 主方法，用于命令行生成密钥对
     */
    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("Base AI Cloud - RSA密钥对生成工具");
        System.out.println("=".repeat(60));

        try {
            // 解析命令行参数
            int keySize = KEY_SIZE_2048;
            if (args.length > 0) {
                try {
                    keySize = Integer.parseInt(args[0]);
                    if (keySize != 2048 && keySize != 4096) {
                        System.err.println("警告: 推荐使用2048或4096位密钥长度");
                    }
                } catch (NumberFormatException e) {
                    System.err.println("无效的密钥长度参数，使用默认值: " + KEY_SIZE_2048);
                }
            }

            // 生成密钥对
            System.out.printf("正在生成%d位RSA密钥对...\n", keySize);
            RSAKeyPair keyPair = generateKeyPair(keySize);

            // 验证密钥对
            if (!keyPair.isValid()) {
                System.err.println("错误: 生成的密钥对验证失败！");
                System.exit(1);
            }

            // 输出结果
            System.out.println("\n✓ 密钥对生成成功！");
            System.out.println("密钥信息: " + keyPair);
            System.out.println("\n" + "=".repeat(60));
            System.out.println("配置文件示例:");
            System.out.println("=".repeat(60));
            System.out.println(generateConfigExample(keyPair));

            System.out.println("=".repeat(60));
            System.out.println("安全提示:");
            System.out.println("1. 请将私钥安全存储，不要泄露给他人");
            System.out.println("2. 建议在生产环境中使用环境变量或密钥管理服务");
            System.out.println("3. 定期轮换密钥对以增强安全性");
            System.out.println("4. 不同环境应使用不同的密钥对");
            System.out.println("=".repeat(60));

        } catch (Exception e) {
            System.err.println("密钥生成失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}