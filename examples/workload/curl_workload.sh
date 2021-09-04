#!/usr/bin/env bash
# curl workload
# Localhost is the default configuration
WEB_SERVICE=localhost
WEB_PORT=80

# Run curl against the microservice architecture
# 1. GET /api/web/index
# 2. GET /api/web/login
# 3. POST /api/web/logioaction?username=userXY&password=Z (returns /api/web/profile, get cookie-1)
# 4. GET /api/web/category?id=3 (use cookie-1)
# 5. GET /api/web/product?id=110 (use cookie-1)
# 6. GET /api/web/cartaction/addtocart?productid=110 (use cookie-1, returns /api/web/cart, get cookie-2)
# 7. GET /api/web/cartaction/addtocart?productid=63 (use cookie-2, returns /api/web/cart, get cookie-3)
# 8. GET /api/web/cartaction/proceedtocheckout (use cookie-3)
# 9. POST /api/web/cartaction/confirm (use order json and cookie-3, returns /api/web/profile, get cookie-4)
# 10. POST /api/web/logioaction (use cookie-4, returns /api/web/index)

# You can add -v for more details
curl http://$WEB_SERVICE:$WEB_PORT/api/web/index
curl http://$WEB_SERVICE:$WEB_PORT/api/web/login
curl -X POST "http://$WEB_SERVICE:$WEB_PORT/api/web/logioaction?username=user97&password=password"
curl -H 'Cookie: SessionData=%7B%22userId%22%3A507%2C%22sessionId%22%3A%22481153396%22%2C%22token%22%3A%225379fa050a3215cb1e631a5adaa012562997ca5806694f134b8da3ba2d48a4608ae423df2aceba3bf49e4b5ca1b9b7fc504399852355a62c984f2762e315d689%22%2C%22order%22%3Anull%2C%22orderItems%22%3A%5B%5D%2C%22message%22%3Anull%7D, domain=gateway, path=/api' http://$WEB_SERVICE:$WEB_PORT/api/web/category?id=3
curl -H 'Cookie: SessionData=%7B%22userId%22%3A507%2C%22sessionId%22%3A%22481153396%22%2C%22token%22%3A%225379fa050a3215cb1e631a5adaa012562997ca5806694f134b8da3ba2d48a4608ae423df2aceba3bf49e4b5ca1b9b7fc504399852355a62c984f2762e315d689%22%2C%22order%22%3Anull%2C%22orderItems%22%3A%5B%5D%2C%22message%22%3Anull%7D, domain=gateway, path=/api' http://$WEB_SERVICE:$WEB_PORT/api/web/product?id=110
curl -H 'Cookie: SessionData=%7B%22userId%22%3A507%2C%22sessionId%22%3A%22481153396%22%2C%22token%22%3A%225379fa050a3215cb1e631a5adaa012562997ca5806694f134b8da3ba2d48a4608ae423df2aceba3bf49e4b5ca1b9b7fc504399852355a62c984f2762e315d689%22%2C%22order%22%3Anull%2C%22orderItems%22%3A%5B%5D%2C%22message%22%3Anull%7D, domain=gateway, path=/api' http://$WEB_SERVICE:$WEB_PORT/api/web/cartaction/addtocart?productid=110
curl -H 'Cookie: SessionData=%7B%22userId%22%3A507%2C%22sessionId%22%3A%22481153396%22%2C%22token%22%3A%22962945126c06c48a9f2d76c0048bcea3a6418b9b19f9af4664207587015503b3faa04ff2e7a0fdd1719f5ea50cc15d09c39ad8b90a8beba7fc35e0fcdef8ed96%22%2C%22order%22%3Anull%2C%22orderItems%22%3A%5B%7B%22id%22%3Anull%2C%22productId%22%3A110%2C%22orderId%22%3Anull%2C%22quantity%22%3A1%2C%22unitPriceInCents%22%3A8063%7D%5D%2C%22message%22%3Anull%7D, domain=gateway, path=/api' http://$WEB_SERVICE:$WEB_PORT/api/web/cartaction/addtocart?productid=63
curl -H 'Cookie: SessionData=%7B%22userId%22%3A507%2C%22sessionId%22%3A%22481153396%22%2C%22token%22%3A%224a85148218c0de18f6521ffa839d353f510a6c9d4ea97798a6c9b24cc4a6363a0c9fa2fc2fa4e7482e20e4b2d30e23031c7bc061bb3db6e346403aadec97e805%22%2C%22order%22%3Anull%2C%22orderItems%22%3A%5B%7B%22id%22%3Anull%2C%22productId%22%3A110%2C%22orderId%22%3Anull%2C%22quantity%22%3A1%2C%22unitPriceInCents%22%3A8063%7D%2C%7B%22id%22%3Anull%2C%22productId%22%3A63%2C%22orderId%22%3Anull%2C%22quantity%22%3A1%2C%22unitPriceInCents%22%3A9154%7D%5D%2C%22message%22%3Anull%7D, domain=gateway, path=/api' http://$WEB_SERVICE:$WEB_PORT/api/web/cartaction/proceedtocheckout
curl -H 'Cookie: SessionData=%7B%22userId%22%3A507%2C%22sessionId%22%3A%22481153396%22%2C%22token%22%3A%224a85148218c0de18f6521ffa839d353f510a6c9d4ea97798a6c9b24cc4a6363a0c9fa2fc2fa4e7482e20e4b2d30e23031c7bc061bb3db6e346403aadec97e805%22%2C%22order%22%3Anull%2C%22orderItems%22%3A%5B%7B%22id%22%3Anull%2C%22productId%22%3A110%2C%22orderId%22%3Anull%2C%22quantity%22%3A1%2C%22unitPriceInCents%22%3A8063%7D%2C%7B%22id%22%3Anull%2C%22productId%22%3A63%2C%22orderId%22%3Anull%2C%22quantity%22%3A1%2C%22unitPriceInCents%22%3A9154%7D%5D%2C%22message%22%3Anull%7D, domain=gateway, path=/api; Content-Length: 324; Content-Type: application/json' -d '{"id": null, "userId": null, "time": null, "totalPriceInCents": null, "addressName": "Uni Ulm", "address1": "Helmholtzstraße 16", "address2": "89081 Ulm", "creditCardCompany": "MasterCard", "creditCardNumber": "5331 4672 1441 9827", "creditCardExpiryDate": "2031-08-06"}' http://$WEB_SERVICE:$WEB_PORT/api/web/cartaction/confirm
curl -X POST -H 'Cookie: SessionData=%7B%22userId%22%3A507%2C%22sessionId%22%3A%22481153396%22%2C%22token%22%3A%22f1692ed6017f3eb37cfc55a5b7f5edf466fdb358aaf26265b8a4cf49431fdc3494427a63384f9af2d84e33183d610122676371f4605c6da73404ce9d78a804b9%22%2C%22order%22%3A%7B%22id%22%3Anull%2C%22userId%22%3Anull%2C%22time%22%3Anull%2C%22totalPriceInCents%22%3Anull%2C%22addressName%22%3Anull%2C%22address1%22%3Anull%2C%22address2%22%3Anull%2C%22creditCardCompany%22%3Anull%2C%22creditCardNumber%22%3Anull%2C%22creditCardExpiryDate%22%3Anull%7D%2C%22orderItems%22%3A%5B%5D%2C%22message%22%3Anull%7D, domain=gateway, path=/api' http://$WEB_SERVICE:$WEB_PORT/api/web/logioaction
