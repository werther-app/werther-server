FROM mongo:latest

RUN mkdir /home/mongodb
RUN mkdir /home/mongodb/data
RUN mkdir /home/mongodb/logs
RUN chown -R mongodb:mongodb /home/mongodb

COPY assets/mongodb.conf /home/mongodb
COPY assets/index.js /home/mongodb

RUN mongod \
    --fork \
    --config /home/mongodb/mongodb.conf \
    && mongo \
    werther \
    --host localhost \
    /home/mongodb/index.js \
    && mongod --dbpath /home/mongodb/data --shutdown \
    && chown -R mongodb /home/mongodb

CMD ["mongod", "--config", "/home/mongodb/mongodb.conf"]