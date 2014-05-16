#!/bin/bash
/usr/lib/jvm/java-6-openjdk-i386/bin/keytool -importcert -v -trustcacerts -file "mock-server-cert.pem" -alias ca -keystore "truststore.bks" -provider org.bouncycastle.jce.provider.BouncyCastleProvider -providerpath "./bcprov-jdk15on-146.jar" -storetype BKS
