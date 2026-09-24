package com.minimysql.protocol;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;

/**
 * 完成「读完 Handshake 之后」的登录流程：
 * 发 Handshake Response → 处理 OK / ERR / AuthSwitch / caching_sha2 续聊。
 */
public final class Authenticator {

    private Authenticator() {
    }

    public static void authenticate(PacketIO io,
                                    Handshake handshake,
                                    String username,
                                    String password,
                                    String database) throws IOException {
        byte[] response = HandshakeResponse.build(handshake, username, password, database);
        io.writePacketPayload(response);

        String plugin = handshake.authPluginName;
        byte[] scramble = handshake.authPluginData;

        while (true) {
            byte[] payload = io.readPacketPayload();
            AuthResult result = AuthResult.parse(payload);

            switch (result.kind) {
                case OK:
                    return;
                case ERR:
                    throw new IOException("认证失败 [" + result.errorCode + "] "
                            + result.sqlState + " " + result.message);
                case AUTH_SWITCH:
                    plugin = result.switchPlugin;
                    scramble = result.switchScramble;
                    byte[] switched = PasswordEncryption.scramble(plugin, password, scramble);
                    io.writePacketPayload(switched);
                    break;
                case AUTH_MORE_DATA:
                    handleCachingSha2MoreData(io, result.moreData, password, scramble);
                    break;
                default:
                    throw new IllegalStateException("未处理的认证结果: " + result.kind);
            }
        }
    }

    /**
     * caching_sha2_password 在非 SSL 下可能还要走「完整认证」：
     *   0x03 = fast auth 成功，接着应收到 OK
     *   0x04 = 需要完整认证 → 向服务器要 RSA 公钥 → 加密密码再发
     */
    private static void handleCachingSha2MoreData(PacketIO io,
                                                  byte[] moreData,
                                                  String password,
                                                  byte[] scramble) throws IOException {
        if (moreData.length == 0) {
            throw new IOException("AuthMoreData 为空");
        }

        int status = moreData[0] & 0xFF;
        if (status == 0x03) {
            // fast auth success，下一包应是 OK，回到主循环继续读
            return;
        }
        if (status == 0x04) {
            // 请求公钥：发一个字节 0x02
            io.writePacketPayload(new byte[]{0x02});
            byte[] keyPacket = io.readPacketPayload();
            AuthResult keyResult = AuthResult.parse(keyPacket);
            if (keyResult.kind != AuthResult.Kind.AUTH_MORE_DATA) {
                throw new IOException("期望公钥 AuthMoreData，实际: " + keyResult);
            }
            PublicKey publicKey = parsePublicKey(keyResult.moreData);
            byte[] encrypted = rsaEncryptPassword(password, scramble, publicKey);
            io.writePacketPayload(encrypted);
            return;
        }

        // 有的实现直接把 PEM 公钥放在 moreData 里（少见），这里也兼容一下
        if (moreData.length > 1 && moreData[0] == '-') {
            PublicKey publicKey = parsePublicKey(moreData);
            byte[] encrypted = rsaEncryptPassword(password, scramble, publicKey);
            io.writePacketPayload(encrypted);
            return;
        }

        throw new IOException("未知 AuthMoreData 状态: 0x" + Integer.toHexString(status));
    }

    private static PublicKey parsePublicKey(byte[] pemBytes) throws IOException {
        String pem = new String(pemBytes, StandardCharsets.US_ASCII);
        String body = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        try {
            byte[] der = Base64.getDecoder().decode(body);
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IOException("解析 RSA 公钥失败", e);
        }
    }

    /**
     * 密码（含末尾 \\0）与 scramble 循环异或，再用 RSA/ECB/OAEPWithSHA-1AndMGF1Padding 加密。
     */
    private static byte[] rsaEncryptPassword(String password, byte[] scramble, PublicKey key)
            throws IOException {
        byte[] pass = (password == null ? "" : password).getBytes(StandardCharsets.UTF_8);
        byte[] plain = new byte[pass.length + 1];
        System.arraycopy(pass, 0, plain, 0, pass.length);
        // plain[last] = 0

        for (int i = 0; i < plain.length; i++) {
            plain[i] ^= scramble[i % scramble.length];
        }

        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher.doFinal(plain);
        } catch (Exception e) {
            throw new IOException("RSA 加密密码失败", e);
        }
    }
}
