package com.minimysql.protocol;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 把明文密码变成「认证响应字节」。
 *
 * 不会把明文密码直接发到网上，而是用 scramble（握手里的随机串）做一次混淆。
 */
public final class PasswordEncryption {

    private PasswordEncryption() {
    }

    /**
     * mysql_native_password:
     * SHA1(password) XOR SHA1(scramble + SHA1(SHA1(password)))
     */
    public static byte[] nativePassword(String password, byte[] scramble) {
        if (password == null || password.isEmpty()) {
            return new byte[0];
        }
        byte[] passBytes = password.getBytes(StandardCharsets.UTF_8);
        byte[] stage1 = sha1(passBytes);
        byte[] stage2 = sha1(stage1);

        byte[] scrambleAndStage2 = concat(scramble, stage2);
        byte[] stage3 = sha1(scrambleAndStage2);

        return xor(stage1, stage3);
    }

    /**
     * caching_sha2_password（快速认证路径）:
     * SHA256(password) XOR SHA256(SHA256(SHA256(password)) + scramble)
     */
    public static byte[] cachingSha2Password(String password, byte[] scramble) {
        if (password == null || password.isEmpty()) {
            return new byte[0];
        }
        byte[] passBytes = password.getBytes(StandardCharsets.UTF_8);
        byte[] dig1 = sha256(passBytes);
        byte[] dig2 = sha256(dig1);
        byte[] dig3 = sha256(concat(dig2, scramble));
        return xor(dig1, dig3);
    }

    public static byte[] scramble(String plugin, String password, byte[] scramble) {
        if ("mysql_native_password".equals(plugin)) {
            return nativePassword(password, scramble);
        }
        if ("caching_sha2_password".equals(plugin)) {
            return cachingSha2Password(password, scramble);
        }
        throw new IllegalArgumentException("暂不支持的认证插件: " + plugin);
    }

    private static byte[] sha1(byte[] data) {
        return digest("SHA-1", data);
    }

    private static byte[] sha256(byte[] data) {
        return digest("SHA-256", data);
    }

    private static byte[] digest(String algo, byte[] data) {
        try {
            return MessageDigest.getInstance(algo).digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(algo + " 不可用", e);
        }
    }

    private static byte[] xor(byte[] a, byte[] b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("xor 参数不能为 null");
        }
        if (a.length != b.length) {
            throw new IllegalArgumentException(
                    "xor 两侧长度必须相同: a=" + a.length + ", b=" + b.length);
        }
        byte[] out = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            out[i] = (byte) (a[i] ^ b[i]);
        }
        return out;
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
