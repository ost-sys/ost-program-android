package com.ost.application.core.share

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Security
import java.security.cert.X509Certificate
import java.util.Date

object LocalSendCertificateHelper {

    init {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    private var cachedKeyStore: KeyStore? = null
    private var cachedFingerprint: String? = null

    fun getOrGenerateKeyStore(): Pair<KeyStore, String> {
        cachedKeyStore?.let { ks ->
            cachedFingerprint?.let { fp ->
                return ks to fp
            }
        }

        val keyPair = generateKeyPair()
        val certificate = generateSelfSignedCertificate(keyPair)
        val fingerprint = generateFingerprint(certificate)

        val keyStore = KeyStore.getInstance("PKCS12").apply {
            load(null, null)
            setKeyEntry("localsend", keyPair.private, "localsend".toCharArray(), arrayOf(certificate))
        }

        cachedKeyStore = keyStore
        cachedFingerprint = fingerprint

        return keyStore to fingerprint
    }

    private fun generateKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048, SecureRandom())
        return keyPairGenerator.generateKeyPair()
    }

    private fun generateSelfSignedCertificate(keyPair: KeyPair): X509Certificate {
        val notBefore = Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30)
        val notAfter = Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 365 * 10)

        val subject = X500Name("CN=LocalSend")
        val serial = BigInteger(160, SecureRandom())

        val certBuilder = JcaX509v3CertificateBuilder(
            subject,
            serial,
            notBefore,
            notAfter,
            subject,
            keyPair.public
        )

        val signer = JcaContentSignerBuilder("SHA256withRSA")
            .build(keyPair.private)

        return JcaX509CertificateConverter()
            .getCertificate(certBuilder.build(signer))
    }

    private fun generateFingerprint(certificate: X509Certificate): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(certificate.encoded)
        return digest.joinToString("") { "%02X".format(it) }
    }
}
