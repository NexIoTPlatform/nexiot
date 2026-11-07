FROM postgres:18.4-alpine
COPY database/postgres/ /nexiot-seed/
COPY postgres-init.sh /docker-entrypoint-initdb.d/10-nexiot.sh
RUN chmod 0644 /docker-entrypoint-initdb.d/10-nexiot.sh \
    && chmod -R a+rX /nexiot-seed
