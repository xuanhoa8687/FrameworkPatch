package com.vnrom.framework;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.security.keystore.KeyProperties;
import android.text.TextUtils;
import android.util.Log;

import org.lsposed.lsparanoid.Obfuscate;
import org.spongycastle.asn1.ASN1Boolean;
import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.ASN1EncodableVector;
import org.spongycastle.asn1.ASN1Enumerated;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.ASN1OctetString;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.ASN1TaggedObject;
import org.spongycastle.asn1.DEROctetString;
import org.spongycastle.asn1.DERSequence;
import org.spongycastle.asn1.DERTaggedObject;
import org.spongycastle.asn1.x509.Extension;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.cert.X509v3CertificateBuilder;
import org.spongycastle.cert.jcajce.JcaX509CertificateConverter;
import org.spongycastle.openssl.PEMKeyPair;
import org.spongycastle.openssl.PEMParser;
import org.spongycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;
import org.spongycastle.util.io.pem.PemReader;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Obfuscate
public final class Android {
    private static final String TAG = "chiteroman";
    private static final PEMKeyPair EC, RSA;
    private static final ASN1ObjectIdentifier OID = new ASN1ObjectIdentifier("1.3.6.1.4.1.11129.2.1.17");
    private static final List<Certificate> EC_CERTS = new ArrayList<>();
    private static final List<Certificate> RSA_CERTS = new ArrayList<>();
    private static final Map<String, String> map = new HashMap<>();
    private static final CertificateFactory certificateFactory;

    static {
        map.put("MANUFACTURER", "Google");
        map.put("MODEL", "Pixel");
        map.put("FINGERPRINT", "google/sailfish/sailfish:8.1.0/OPM1.171019.011/4448085:user/release-keys");
        map.put("BRAND", "google");
        map.put("PRODUCT", "sailfish");
        map.put("DEVICE", "sailfish");
        map.put("RELEASE", "8.1.0");
        map.put("ID", "OPM1.171019.011");
        map.put("INCREMENTAL", "4448085");
        map.put("SECURITY_PATCH", "2017-12-05");
        map.put("TYPE", "user");
        map.put("TAGS", "release-keys");
        // add
        map.put("HARDWARE", "");
        map.put("BOARD", "");
        map.put("DEVICE_INITIAL_SDK_INT", "24");
        try {
            certificateFactory = CertificateFactory.getInstance("X.509");

            EC = parseKeyPair(Keybox.EC.PRIVATE_KEY);
            EC_CERTS.add(parseCert(Keybox.EC.CERTIFICATE_1));
            EC_CERTS.add(parseCert(Keybox.EC.CERTIFICATE_2));

            RSA = parseKeyPair(Keybox.RSA.PRIVATE_KEY);
            RSA_CERTS.add(parseCert(Keybox.RSA.CERTIFICATE_1));
            RSA_CERTS.add(parseCert(Keybox.RSA.CERTIFICATE_2));
        } catch (Throwable t) {
            Log.e(TAG, t.toString());
            throw new RuntimeException(t);
        }
    }

    private static PEMKeyPair parseKeyPair(String key) throws Throwable {
        try (PEMParser parser = new PEMParser(new StringReader(key))) {
            return (PEMKeyPair) parser.readObject();
        }
    }

    private static Certificate parseCert(String cert) throws Throwable {
        try (PemReader reader = new PemReader(new StringReader(cert))) {
            return certificateFactory.generateCertificate(new ByteArrayInputStream(reader.readPemObject().getContent()));
        }
    }

    private static Field getField(String fieldName) {
        Field field = null;
        try {
            field = Build.class.getDeclaredField(fieldName);
        } catch (Throwable ignored) {
            try {
                field = Build.VERSION.class.getDeclaredField(fieldName);
            } catch (Throwable t) {
                Log.e(TAG, "Couldn't find field " + fieldName);
            }
        }
        return field;
    }

    public static boolean hasSystemFeature(boolean ret, String name) {
        if (PackageManager.FEATURE_KEYSTORE_APP_ATTEST_KEY.equals(name) || PackageManager.FEATURE_STRONGBOX_KEYSTORE.equals(name)) {
            return false;
        }
        return ret;
    }

    public static void newApplication(Context context) {
        if (context == null) return;

        String packageName = context.getPackageName();
        String processName = Application.getProcessName();

        if (TextUtils.isEmpty(packageName) || TextUtils.isEmpty(processName)) return;

        if (!"com.google.android.gms".equals(packageName)) return;

        if (!"com.google.android.gms.unstable".equals(processName)) return;

        map.forEach((fieldName, value) -> {
            Field field = getField(fieldName);
            if (field == null) return;
            field.setAccessible(true);
            try {
                field.set(null, value);
            } catch (Throwable t) {
                Log.e(TAG, t.toString());
            }
            field.setAccessible(false);
        });
    }

    public static Certificate[] engineGetCertificateChain(Certificate[] caList) {
        if (caList == null) throw new UnsupportedOperationException();
        try {
            X509Certificate leaf = (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(caList[0].getEncoded()));

            byte[] bytes = leaf.getExtensionValue(OID.getId());

            if (bytes == null) return caList;

            X509CertificateHolder holder = new X509CertificateHolder(leaf.getEncoded());

            Extension ext = holder.getExtension(OID);

            ASN1Sequence sequence = ASN1Sequence.getInstance(ext.getExtnValue().getOctets());

            ASN1Encodable[] encodables = sequence.toArray();

            ASN1Sequence teeEnforced = (ASN1Sequence) encodables[7];

            ASN1EncodableVector vector = new ASN1EncodableVector();

            for (ASN1Encodable asn1Encodable : teeEnforced) {
                ASN1TaggedObject taggedObject = (ASN1TaggedObject) asn1Encodable;
                if (taggedObject.getTagNo() == 704) continue;
                vector.add(taggedObject);
            }

            LinkedList<Certificate> certificates;

            X509v3CertificateBuilder builder;
            ContentSigner signer;

            if (KeyProperties.KEY_ALGORITHM_EC.equals(leaf.getPublicKey().getAlgorithm())) {
                certificates = new LinkedList<>(EC_CERTS);
                builder = new X509v3CertificateBuilder(new X509CertificateHolder(EC_CERTS.get(0).getEncoded()).getSubject(), holder.getSerialNumber(), holder.getNotBefore(), holder.getNotAfter(), holder.getSubject(), EC.getPublicKeyInfo());
                signer = new JcaContentSignerBuilder(leaf.getSigAlgName()).build(new JcaPEMKeyConverter().getPrivateKey(EC.getPrivateKeyInfo()));
            } else {
                certificates = new LinkedList<>(RSA_CERTS);
                builder = new X509v3CertificateBuilder(new X509CertificateHolder(RSA_CERTS.get(0).getEncoded()).getSubject(), holder.getSerialNumber(), holder.getNotBefore(), holder.getNotAfter(), holder.getSubject(), RSA.getPublicKeyInfo());
                signer = new JcaContentSignerBuilder(leaf.getSigAlgName()).build(new JcaPEMKeyConverter().getPrivateKey(RSA.getPrivateKeyInfo()));
            }

            byte[] verifiedBootKey = new byte[32];
            byte[] verifiedBootHash = new byte[32];

            ThreadLocalRandom.current().nextBytes(verifiedBootKey);
            ThreadLocalRandom.current().nextBytes(verifiedBootHash);

            ASN1Encodable[] rootOfTrustEnc = {new DEROctetString(verifiedBootKey), ASN1Boolean.TRUE, new ASN1Enumerated(0), new DEROctetString(verifiedBootHash)};

            ASN1Sequence rootOfTrustSeq = new DERSequence(rootOfTrustEnc);

            ASN1TaggedObject rootOfTrustTagObj = new DERTaggedObject(704, rootOfTrustSeq);

            vector.add(rootOfTrustTagObj);

            ASN1Sequence hackEnforced = new DERSequence(vector);

            encodables[7] = hackEnforced;

            ASN1Sequence hackedSeq = new DERSequence(encodables);

            ASN1OctetString hackedSeqOctets = new DEROctetString(hackedSeq);

            Extension hackedExt = new Extension(OID, false, hackedSeqOctets);

            builder.addExtension(hackedExt);

            for (ASN1ObjectIdentifier extensionOID : holder.getExtensions().getExtensionOIDs()) {
                if (OID.getId().equals(extensionOID.getId())) continue;
                builder.addExtension(holder.getExtension(extensionOID));
            }

            certificates.addFirst(new JcaX509CertificateConverter().getCertificate(builder.build(signer)));

            return certificates.toArray(new Certificate[0]);

        } catch (Throwable t) {
            Log.e(TAG, t.toString());
        }
        return caList;
    }
}