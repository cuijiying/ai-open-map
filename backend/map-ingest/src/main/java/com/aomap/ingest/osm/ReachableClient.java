package com.aomap.ingest.osm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 先按系统 DNS 下载。连不上时改问阿里公共 DNS，再按解析出的 IPv4 建立 TLS，
 * 这样域名被污染时仍然能下到境外镜像。
 */
final class ReachableClient {

    private static final Logger log = LoggerFactory.getLogger(ReachableClient.class);
    private static final Pattern IPV4 = Pattern.compile("\"data\"\\s*:\\s*\"((?:\\d{1,3}\\.){3}\\d{1,3})\"");

    private final HttpClient httpClient;
    private final SSLSocketFactory tls;
    private volatile boolean preferPinned;

    ReachableClient(HttpClient httpClient) {
        this.httpClient = httpClient;
        this.tls = (SSLSocketFactory) SSLSocketFactory.getDefault();
    }

    Opened open(String url, String userAgent, Duration requestTimeout) throws IOException, InterruptedException {
        if (preferPinned) {
            return openPinned(URI.create(url), userAgent, 0);
        }
        try {
            return openWithClient(url, userAgent, requestTimeout);
        } catch (IOException ex) {
            if (!connectFailed(ex)) {
                throw ex;
            }
            log.warn("直连失败，改用阿里 DNS 后重试 {}: {}", url, ex.getMessage());
            preferPinned = true;
            try {
                return openPinned(URI.create(url), userAgent, 0);
            } catch (IOException pinned) {
                pinned.addSuppressed(ex);
                throw pinned;
            }
        }
    }

    static String ipv4FromDoh(String json) {
        if (json == null) {
            return null;
        }
        Matcher matcher = IPV4.matcher(json);
        String found = null;
        while (matcher.find()) {
            found = matcher.group(1);
        }
        return found;
    }

    private Opened openWithClient(String url, String userAgent, Duration requestTimeout) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", userAgent)
                .GET();
        if (requestTimeout != null) {
            builder.timeout(requestTimeout);
        }
        HttpRequest request = builder.build();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        long length = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
        return new Opened(response.statusCode(), length, response.body());
    }

    private Opened openPinned(URI uri, String userAgent, int redirects) throws IOException, InterruptedException {
        if (redirects > 3) {
            throw new IOException("重定向次数过多");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IOException("只支持 HTTPS：" + uri);
        }
        InetAddress address = resolvePublicV4(uri.getHost());
        Socket socket = new Socket();
        SSLSocket ssl = null;
        try {
            socket.connect(new InetSocketAddress(address, uri.getPort() == -1 ? 443 : uri.getPort()), 10_000);
            socket.setSoTimeout(120_000);
            ssl = (SSLSocket) tls.createSocket(socket, uri.getHost(), 443, true);
            SSLParameters parameters = ssl.getSSLParameters();
            parameters.setServerNames(java.util.List.of(new SNIHostName(uri.getHost())));
            parameters.setEndpointIdentificationAlgorithm("HTTPS");
            ssl.setSSLParameters(parameters);
            ssl.startHandshake();
            String path = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
            if (uri.getRawQuery() != null) {
                path = path + "?" + uri.getRawQuery();
            }
            String request = "GET " + path + " HTTP/1.1\r\n"
                    + "Host: " + uri.getHost() + "\r\n"
                    + "User-Agent: " + userAgent + "\r\n"
                    + "Accept: */*\r\n"
                    + "Connection: close\r\n\r\n";
            OutputStream out = ssl.getOutputStream();
            out.write(request.getBytes(StandardCharsets.US_ASCII));
            out.flush();
            InputStream in = ssl.getInputStream();
            ByteArrayOutputStream headerBytes = new ByteArrayOutputStream();
            int matched = 0;
            while (matched < 4) {
                int value = in.read();
                if (value < 0) {
                    throw new IOException("响应头不完整");
                }
                headerBytes.write(value);
                int expected = switch (matched) {
                    case 0, 2 -> '\r';
                    default -> '\n';
                };
                matched = value == expected ? matched + 1 : (value == '\r' ? 1 : 0);
            }
            String header = headerBytes.toString(StandardCharsets.ISO_8859_1);
            int lineEnd = header.indexOf("\r\n");
            String statusLine = lineEnd < 0 ? header : header.substring(0, lineEnd);
            String[] parts = statusLine.split(" ");
            if (parts.length < 2) {
                throw new IOException("无法识别响应：" + statusLine);
            }
            int status = Integer.parseInt(parts[1]);
            String location = headerValue(header, "Location");
            if (status >= 300 && status < 400 && location != null) {
                ssl.close();
                ssl = null;
                socket = null;
                return openPinned(uri.resolve(location), userAgent, redirects + 1);
            }
            long length = -1L;
            String contentLength = headerValue(header, "Content-Length");
            if (contentLength != null) {
                length = Long.parseLong(contentLength.trim());
            }
            SSLSocket toClose = ssl;
            ssl = null;
            socket = null;
            InputStream body = new InputStream() {
                @Override
                public int read() throws IOException {
                    return in.read();
                }

                @Override
                public int read(byte[] buffer, int off, int len) throws IOException {
                    return in.read(buffer, off, len);
                }

                @Override
                public void close() throws IOException {
                    toClose.close();
                }
            };
            return new Opened(status, length, body);
        } catch (RuntimeException | IOException ex) {
            if (ssl != null) {
                ssl.close();
            } else {
                socket.close();
            }
            throw ex instanceof IOException ? (IOException) ex : new IOException(ex);
        }
    }

    private InetAddress resolvePublicV4(String host) throws IOException, InterruptedException {
        String query = "https://dns.alidns.com/resolve?name="
                + URLEncoder.encode(host, StandardCharsets.UTF_8)
                + "&type=A";
        HttpRequest request = HttpRequest.newBuilder(URI.create(query))
                .timeout(Duration.ofSeconds(8))
                .header("Accept", "application/dns-json")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("阿里 DNS 返回 HTTP " + response.statusCode());
        }
        String ip = ipv4FromDoh(response.body());
        if (ip == null) {
            throw new UnknownHostException("阿里 DNS 没有返回 " + host + " 的 IPv4");
        }
        log.info("阿里 DNS 将 {} 解析为 {}", host, ip);
        return InetAddress.getByName(ip);
    }

    private static String headerValue(String header, String name) {
        String[] lines = header.split("\r\n");
        for (int i = 1; i < lines.length; i++) {
            int colon = lines[i].indexOf(':');
            if (colon > 0 && lines[i].substring(0, colon).equalsIgnoreCase(name)) {
                return lines[i].substring(colon + 1).trim();
            }
        }
        return null;
    }

    private static boolean connectFailed(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof java.net.ConnectException
                    || current instanceof UnknownHostException
                    || current instanceof HttpTimeoutException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains("timed out")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    static final class Opened implements AutoCloseable {
        private final int status;
        private final long length;
        private final InputStream body;

        private Opened(int status, long length, InputStream body) {
            this.status = status;
            this.length = length;
            this.body = body;
        }

        int status() {
            return status;
        }

        long length() {
            return length;
        }

        InputStream body() {
            return body;
        }

        @Override
        public void close() throws IOException {
            body.close();
        }
    }
}
