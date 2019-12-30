1. Generated key and certificate:<br>
Windows:<br>
```openssl req -x509 -sha256 -nodes -newkey rsa:2048 -days 365 -out self-signed-localhost.crt -keyout localhost.key -subj "//CN=localhost\O=My Company Name LTD.\C=RU"```
<br>Linux:<br>
```openssl req -x509 -sha256 -nodes -newkey rsa:2048 -days 365 -out self-signed-localhost.crt -keyout localhost.key -subj "/CN=localhost/O=My Company Name LTD./C=RU"```

2. Generated Key Store from key and certificate with password "cert-pass" and alias "cert-alias":<br>
```openssl pkcs12 -export -in self-signed-localhost.crt -inkey localhost.key -out self-signed-keystore.p12 -passout pass:cert-pass -name cert-alias```