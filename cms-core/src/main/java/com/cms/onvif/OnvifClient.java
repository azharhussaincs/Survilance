package com.cms.onvif;

import com.cms.model.Camera;
import com.cms.model.NvrDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * ONVIF protocol client for device communication and camera discovery.
 * Implements WS-Discovery and ONVIF SOAP calls.
 */
public class OnvifClient {
    private static final Logger logger = LoggerFactory.getLogger(OnvifClient.class);

    private static final String ONVIF_DEVICE_SERVICE = "/onvif/device_service";
    private static final String ONVIF_MEDIA_SERVICE = "/onvif/media_service";
    private static final int DEFAULT_ONVIF_PORT = 80;
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 15000;

    static {
        disableSslVerification();
    }

    private static void disableSslVerification() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            HostnameVerifier allHostsValid = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (Exception e) {
            logger.error("Failed to disable SSL verification", e);
        }
    }

    /**
     * Discovers cameras/profiles from an ONVIF-compatible NVR.
     */
    public List<Camera> discoverProfiles(NvrDevice nvr) throws Exception {
        logger.info("Starting ONVIF discovery for {}", nvr.getIpAddress());
        List<Camera> cameras = new ArrayList<>();

        String deviceUrl = buildDeviceUrl(nvr);
        // Step 1: Get device capabilities (media service URL)
        String mediaServiceUrl = getMediaServiceUrl(nvr, deviceUrl);
        if (mediaServiceUrl == null) {
            mediaServiceUrl = buildMediaUrl(nvr);
        }

        // Step 2: Get media profiles
        List<OnvifProfile> profiles = getMediaProfiles(nvr, mediaServiceUrl);

        // Step 3: Get stream URIs for each profile
        int channelNum = 1;
        for (OnvifProfile profile : profiles) {
            String streamUri = getStreamUri(nvr, mediaServiceUrl, profile.token());
            Camera camera = new Camera();
            camera.setNvrId(nvr.getId());
            camera.setCameraName(profile.name().isBlank() ? "Camera " + channelNum : profile.name());
            camera.setChannelNumber(channelNum++);
            camera.setOnvifProfileToken(profile.token());
            camera.setStreamType(Camera.StreamType.RTSP);
            camera.setStatus(Camera.CameraStatus.UNKNOWN);
            if (streamUri != null) {
                // Decode XML entities like &amp; to &
                streamUri = streamUri.replace("&amp;", "&");
                // Inject credentials into RTSP URL
                camera.setStreamUrl(injectCredentials(streamUri, nvr.getUsername(), nvr.getPassword()));
            }
            // Build snapshot URL
            String snapshotUri = getSnapshotUri(nvr, mediaServiceUrl, profile.token());
            if (snapshotUri != null) {
                snapshotUri = snapshotUri.replace("&amp;", "&");
                camera.setSnapshotUrl(snapshotUri);
            }
            cameras.add(camera);
        }

        logger.info("Discovered {} ONVIF profiles on {}", cameras.size(), nvr.getIpAddress());
        return cameras;
    }

    private String getMediaServiceUrl(NvrDevice nvr, String deviceUrl) {
        try {
            String soap = buildGetCapabilitiesRequest(nvr);
            String response = sendSoapRequest(deviceUrl, soap, "http://www.onvif.org/ver10/device/wsdl/GetCapabilities");
            if (response != null) {
                return extractXmlValue(response, "XAddr");
            }
        } catch (Exception e) {
            logger.warn("Could not get capabilities from {}: {}", nvr.getIpAddress(), e.getMessage());
        }
        return null;
    }

    private List<OnvifProfile> getMediaProfiles(NvrDevice nvr, String mediaUrl) {
        List<OnvifProfile> profiles = new ArrayList<>();
        try {
            String soap = buildGetProfilesRequest(nvr);
            String response = sendSoapRequest(mediaUrl, soap, "http://www.onvif.org/ver10/media/wsdl/GetProfiles");
            if (response != null) {
                profiles = parseProfiles(response);
            }
        } catch (Exception e) {
            logger.warn("Could not get profiles: {}", e.getMessage());
        }
        return profiles;
    }

    private String getStreamUri(NvrDevice nvr, String mediaUrl, String profileToken) {
        try {
            String soap = buildGetStreamUriRequest(nvr, profileToken);
            String response = sendSoapRequest(mediaUrl, soap, "http://www.onvif.org/ver10/media/wsdl/GetStreamUri");
            if (response != null) {
                return extractXmlValue(response, "Uri");
            }
        } catch (Exception e) {
            logger.warn("Could not get stream URI for profile {}: {}", profileToken, e.getMessage());
        }
        return null;
    }

    private String getSnapshotUri(NvrDevice nvr, String mediaUrl, String profileToken) {
        try {
            String soap = buildGetSnapshotUriRequest(nvr, profileToken);
            String response = sendSoapRequest(mediaUrl, soap, "http://www.onvif.org/ver10/media/wsdl/GetSnapshotUri");
            if (response != null) {
                return extractXmlValue(response, "Uri");
            }
        } catch (Exception e) {
            logger.debug("Could not get snapshot URI for profile {}: {}", profileToken, e.getMessage());
        }
        return null;
    }

    private String sendSoapRequest(String url, String soapBody, String action) throws Exception {
        URL requestUrl = new URL(url);
        URLConnection connection = requestUrl.openConnection();
        HttpURLConnection conn;

        if (connection instanceof HttpsURLConnection httpsConn) {
            conn = httpsConn;
        } else {
            conn = (HttpURLConnection) connection;
        }

        conn.setRequestMethod("POST");
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/soap+xml; charset=utf-8");
        conn.setRequestProperty("SOAPAction", action);

        byte[] body = soapBody.getBytes(StandardCharsets.UTF_8);
        conn.setRequestProperty("Content-Length", String.valueOf(body.length));

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == 301 || responseCode == 302) {
            String newUrl = conn.getHeaderField("Location");
            if (newUrl != null) {
                logger.info("Redirecting SOAP request from {} to {}", url, newUrl);
                return sendSoapRequest(newUrl, soapBody, action);
            }
        }
        if (responseCode == 200 || responseCode == 400) {
            InputStream is = responseCode == 200 ? conn.getInputStream() : conn.getErrorStream();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                return sb.toString();
            }
        }
        logger.warn("SOAP request to {} returned HTTP {}", url, responseCode);
        return null;
    }

    private String buildGetCapabilitiesRequest(NvrDevice nvr) {
        return buildSoapEnvelope(nvr, """
            <tds:GetCapabilities>
                <tds:Category>Media</tds:Category>
            </tds:GetCapabilities>
            """, "http://www.onvif.org/ver10/device/wsdl");
    }

    private String buildGetProfilesRequest(NvrDevice nvr) {
        return buildSoapEnvelope(nvr, "<trt:GetProfiles/>", "http://www.onvif.org/ver10/media/wsdl");
    }

    private String buildGetStreamUriRequest(NvrDevice nvr, String profileToken) {
        return buildSoapEnvelope(nvr, String.format("""
            <trt:GetStreamUri>
                <trt:StreamSetup>
                    <tt:Stream>RTP-Unicast</tt:Stream>
                    <tt:Transport><tt:Protocol>RTSP</tt:Protocol></tt:Transport>
                </trt:StreamSetup>
                <trt:ProfileToken>%s</trt:ProfileToken>
            </trt:GetStreamUri>
            """, profileToken), "http://www.onvif.org/ver10/media/wsdl");
    }

    private String buildGetSnapshotUriRequest(NvrDevice nvr, String profileToken) {
        return buildSoapEnvelope(nvr, String.format("""
            <trt:GetSnapshotUri>
                <trt:ProfileToken>%s</trt:ProfileToken>
            </trt:GetSnapshotUri>
            """, profileToken), "http://www.onvif.org/ver10/media/wsdl");
    }

    private String buildSoapEnvelope(NvrDevice nvr, String body, String namespace) {
        String wsseHeader = buildWsseHeader(nvr.getUsername(), nvr.getPassword());
        return String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope"
                        xmlns:tds="http://www.onvif.org/ver10/device/wsdl"
                        xmlns:trt="http://www.onvif.org/ver10/media/wsdl"
                        xmlns:tt="http://www.onvif.org/ver10/schema">
                <s:Header>%s</s:Header>
                <s:Body>%s</s:Body>
            </s:Envelope>
            """, wsseHeader, body);
    }

    private String buildWsseHeader(String username, String password) {
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String created = Instant.now().toString();
        String digest = generatePasswordDigest(nonce, created, password);

        return String.format("""
            <wsse:Security xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
                <wsse:UsernameToken>
                    <wsse:Username>%s</wsse:Username>
                    <wsse:Password Type="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordDigest">%s</wsse:Password>
                    <wsse:Nonce EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary">%s</wsse:Nonce>
                    <wsu:Created xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd">%s</wsu:Created>
                </wsse:UsernameToken>
            </wsse:Security>
            """, username, digest, Base64.getEncoder().encodeToString(nonce.getBytes()), created);
    }

    private String generatePasswordDigest(String nonce, String created, String password) {
        try {
            byte[] nonceBytes = nonce.getBytes(StandardCharsets.UTF_8);
            byte[] createdBytes = created.getBytes(StandardCharsets.UTF_8);
            byte[] passwordBytes = (password != null ? password : "").getBytes(StandardCharsets.UTF_8);
            byte[] combined = new byte[nonceBytes.length + createdBytes.length + passwordBytes.length];
            System.arraycopy(nonceBytes, 0, combined, 0, nonceBytes.length);
            System.arraycopy(createdBytes, 0, combined, nonceBytes.length, createdBytes.length);
            System.arraycopy(passwordBytes, 0, combined, nonceBytes.length + createdBytes.length, passwordBytes.length);
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(combined);
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return Base64.getEncoder().encodeToString(password != null ? password.getBytes() : new byte[0]);
        }
    }

    private List<OnvifProfile> parseProfiles(String xml) {
        List<OnvifProfile> profiles = new ArrayList<>();
        int idx = 0;
        while (true) {
            int start = xml.indexOf("<trt:Profiles ", idx);
            if (start == -1) start = xml.indexOf("<Profiles ", idx);
            if (start == -1) break;
            int end = xml.indexOf("</trt:Profiles>", start);
            if (end == -1) end = xml.indexOf("</Profiles>", start);
            if (end == -1) break;
            String segment = xml.substring(start, end + 20);
            String token = extractAttribute(segment, "token");
            String name = extractXmlValue(segment, "Name");
            if (token != null) {
                profiles.add(new OnvifProfile(token, name != null ? name : "Profile " + (profiles.size() + 1)));
            }
            idx = end + 1;
        }
        return profiles;
    }

    private String extractXmlValue(String xml, String tagName) {
        String[] variants = {tagName, "tt:" + tagName, "trt:" + tagName, "tds:" + tagName};
        for (String tag : variants) {
            String open = "<" + tag + ">";
            String close = "</" + tag + ">";
            int start = xml.indexOf(open);
            if (start != -1) {
                int end = xml.indexOf(close, start);
                if (end != -1) {
                    return xml.substring(start + open.length(), end).trim();
                }
            }
        }
        return null;
    }

    private String extractAttribute(String xml, String attrName) {
        String search = attrName + "=\"";
        int start = xml.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        int end = xml.indexOf("\"", start);
        return end != -1 ? xml.substring(start, end) : null;
    }

    private String buildDeviceUrl(NvrDevice nvr) {
        String ip = nvr.getIpAddress();
        if (ip.startsWith("http://") || ip.startsWith("https://")) {
            try {
                URL url = new URL(ip);
                String protocol = url.getProtocol();
                String host = url.getHost();
                int urlPort = url.getPort() != -1 ? url.getPort() : url.getDefaultPort();
                return protocol + "://" + host + ":" + urlPort + ONVIF_DEVICE_SERVICE;
            } catch (Exception e) {
                // fallback
            }
        }
        int port = nvr.getPort() > 0 ? nvr.getPort() : DEFAULT_ONVIF_PORT;
        return "http://" + nvr.getIpAddress() + ":" + port + ONVIF_DEVICE_SERVICE;
    }

    private String buildMediaUrl(NvrDevice nvr) {
        String ip = nvr.getIpAddress();
        if (ip.startsWith("http://") || ip.startsWith("https://")) {
            try {
                URL url = new URL(ip);
                String protocol = url.getProtocol();
                String host = url.getHost();
                int urlPort = url.getPort() != -1 ? url.getPort() : url.getDefaultPort();
                return protocol + "://" + host + ":" + urlPort + ONVIF_MEDIA_SERVICE;
            } catch (Exception e) {
                // fallback
            }
        }
        int port = nvr.getPort() > 0 ? nvr.getPort() : DEFAULT_ONVIF_PORT;
        return "http://" + nvr.getIpAddress() + ":" + port + ONVIF_MEDIA_SERVICE;
    }

    private String injectCredentials(String rtspUrl, String username, String password) {
        if (rtspUrl == null) return null;
        if (username == null || username.isBlank()) return rtspUrl;
        
        String cleanUrl = rtspUrl;
        if (rtspUrl.contains("://http://")) {
             cleanUrl = rtspUrl.replace("://http://", "://").replace("://https://", "://");
             try {
                 URL url = new URL(rtspUrl.substring(rtspUrl.indexOf("://") + 3));
                 cleanUrl = "rtsp://" + url.getHost() + (url.getPort() != -1 ? ":" + url.getPort() : "") + url.getFile();
             } catch (Exception e) {
                 // ignore
             }
        }

        if (cleanUrl.startsWith("rtsp://") && !cleanUrl.contains("@")) {
            String encoded = cleanUrl.substring("rtsp://".length());
            return "rtsp://" + URLEncoder.encode(username, StandardCharsets.UTF_8) + ":" +
                   URLEncoder.encode(password != null ? password : "", StandardCharsets.UTF_8) + "@" + encoded;
        }
        return cleanUrl;
    }

    private record OnvifProfile(String token, String name) {}
}
