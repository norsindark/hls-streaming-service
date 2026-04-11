package com.hls.streaming.security.utils;

import com.hls.streaming.exception.AbstractHLSException;
import lombok.experimental.UtilityClass;

import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@UtilityClass
public class ECDSAUtils {

    public static final String ENCRYPT_ALGORITHM = "EC";

    public static ECPublicKey getPublicKey(byte[] bytes) {
        bytes = Base64.getDecoder().decode(bytes);
        var spec = new X509EncodedKeySpec(bytes);

        try {
            var factory = KeyFactory.getInstance(ENCRYPT_ALGORITHM);
            return (ECPublicKey) factory.generatePublic(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new AbstractHLSException(e);
        }
    }

    public static ECPrivateKey getPrivateKey(byte[] bytes) {
        bytes = Base64.getDecoder().decode(bytes);
        var spec = new PKCS8EncodedKeySpec(bytes);
        try {
            var factory = KeyFactory.getInstance(ENCRYPT_ALGORITHM);
            return (ECPrivateKey) factory.generatePrivate(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new AbstractHLSException(e);
        }
    }

    public static void main(String[] args) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        var g = KeyPairGenerator.getInstance("EC");
        var spec = new ECGenParameterSpec("secp256r1");
        g.initialize(spec);
        var keyPair = g.generateKeyPair();

        byte[] bytes = keyPair.getPublic().getEncoded();
        var xspec = new X509EncodedKeySpec(bytes);
        try {
            var factory = KeyFactory.getInstance("EC");
            var ecPublicKey = (ECPublicKey) factory.generatePublic(xspec);
            System.out.println(ecPublicKey.getAlgorithm());
            System.out.println("private key: " + Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
            System.out.println("public key: " + Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
