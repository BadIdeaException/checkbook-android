#!/bin/bash
openssl req -x509 -nodes -newkey rsa:1024 -keyout mock-server-key.pem -out mock-server-cert.pem -config mock-server-openssl.cnf
