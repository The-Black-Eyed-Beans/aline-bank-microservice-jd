#!/bin/bash

if [ -f /run/secrets/MYSQL_USER ]; then
  export DB_USERNAME=$(cat /run/secrets/MYSQL_USER)
fi

if [ -f /run/secrets/MYSQL_PASSWORD ]; then
  export DB_PASSWORD=$(cat /run/secrets/MYSQL_PASSWORD)
fi

if [ -f /run/secrets/MYSQL_DATABASE ]; then
  export DB_NAME=$(cat /run/secrets/MYSQL_DATABASE)
fi

if [ -f /run/secrets/MYSQL_HOST ]; then
  export DB_HOST=$(cat /run/secrets/MYSQL_HOST)
fi

if [ -f /run/secrets/MYSQL_PORT ]; then
  export DB_PORT=$(cat /run/secrets/MYSQL_PORT)
fi

if [ -f /run/secrets/ENCRYPT_SECRET_KEY ]; then
  export ENCRYPT_SECRET_KEY=$(cat /run/secrets/ENCRYPT_SECRET_KEY)
fi

if [ -f /run/secrets/JWT_SECRET_KEY ]; then
  export JWT_SECRET_KEY=$(cat /run/secrets/JWT_SECRET_KEY)
fi

exec "$@"