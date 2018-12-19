package click.dummer.notify_to_jabber;

import android.net.SSLCertificateSocketFactory;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;

public class SslHelper {

    public static class Basics {
        public boolean isHttps;
        public String fingerprint;
        public String domain;
        public Certificate certificate;
    }

    public static Basics CertCheck(URL destinationURL) {
        // START get cert and data with ignoring all ssl errors

        Basics back = new Basics();
        back.isHttps = destinationURL.toString().startsWith("https://");
        back.fingerprint = null;
        back.domain = null;
        back.certificate = null;

        if (!back.isHttps) return back;

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) destinationURL.openConnection();
            if (conn instanceof HttpsURLConnection) {
                HttpsURLConnection httpsConn = (HttpsURLConnection) conn;
                httpsConn.setSSLSocketFactory(SSLCertificateSocketFactory.getInsecure(0, null));
                httpsConn.setHostnameVerifier(new MyHostnameVerifier(destinationURL));
            }
            conn.connect();
        } catch (IOException e) {
            return back;
        }


        Certificate[] certs;
        try {
            certs = ( (HttpsURLConnection) conn).getServerCertificates();
            back.domain = destinationURL.getHost();
        } catch (SSLPeerUnverifiedException e) {
            return back;
        }

        for (Certificate cert : certs) {

            if (cert instanceof X509Certificate) {
                back.certificate = cert;
                break;
            }
        }

        // END cert with ignoring all ssl errors: now we have to compare the signature as alternative way

        if (back.certificate == null) return back;

        StringBuilder sb = new StringBuilder();
        for (byte b : ((X509Certificate) back.certificate).getSignature()) {
            sb.append(String.format("%02X:", b));
        }
        back.fingerprint = sb.toString();
        back.fingerprint = back.fingerprint.substring(0, back.fingerprint.length()-1);

        return back;
    }

    public static OkHttpClient createOkClient(URL destinationURL, Certificate certificate) {
        OkHttpClient client = null;

        String keyStoreType = KeyStore.getDefaultType();
        KeyStore keyStore = null;
        try {
            keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", certificate);

            // Create a TrustManager that trusts the CAs in our KeyStore.
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(tmfAlgorithm);
            trustManagerFactory.init(keyStore);

            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            X509TrustManager x509TrustManager = (X509TrustManager) trustManagers[0];

            // Create an SSLSocketFactory that uses our TrustManager
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[]{x509TrustManager}, null);
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            //create Okhttp client
            client = new OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory,x509TrustManager)
                    .hostnameVerifier(new MyHostnameVerifier(destinationURL))
                    .build();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

        return client;
    }

    private static class MyHostnameVerifier implements HostnameVerifier {
        private URL shouldHost;

        public MyHostnameVerifier(URL shouldHost) {
            this.shouldHost = shouldHost;
        }

        @Override
        public boolean verify(String hostname, SSLSession sslSession) {
            return shouldHost.getHost().equals(hostname);
        }
    }
}
