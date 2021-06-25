FROM mariadb:focal

COPY setup.sql /docker-entrypoint-initdb.d/setup_0.sql
COPY initialData.sql /docker-entrypoint-initdb.d/setup_1.sql
RUN sed -i "s/max_connections.*/max_connections = 2048/g" /etc/mysql/my.cnf

ENV MYSQL_ROOT_PASSWORD SECRET-PASSWORD!