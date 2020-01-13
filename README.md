<b>1. Generate test SSL certificates</b><br>
Generate key and certificate:<br>
Windows:<br>
```
openssl req -x509 -sha256 -nodes -newkey rsa:2048 -days 365 -out self-signed-localhost.crt -keyout localhost.key -subj "//CN=localhost\O=My Company Name LTD.\C=RU"
```
<br>Linux:<br>
```
openssl req -x509 -sha256 -nodes -newkey rsa:2048 -days 365 -out self-signed-localhost.crt -keyout localhost.key -subj "/CN=localhost/O=My Company Name LTD./C=RU"
```

<b>2. Generate Key Store from key and certificate with password "cert-pass" and alias "cert-alias":</b><br>
```
openssl pkcs12 -export -in self-signed-localhost.crt -inkey localhost.key -out self-signed-keystore.p12 -passout pass:cert-pass -name cert-alias
```

<b>Run app</b><br>
Run app with the command<br>
Https notifications:
```
java -Dconfig.resource=client/conf/https-client.conf ... com.epam.example.akkahttp.client.multiple.Client
``` 
Email notifications:
```
java -Dconfig.resource=client/conf/smtp-client.conf ... com.epam.example.akkahttp.client.multiple.Client
```
application.conf is used by default
