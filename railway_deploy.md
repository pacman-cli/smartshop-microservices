# Railway Deployment Guide

This guide provides instructions for deploying the SmartShop Microservices Platform to [Railway](https://railway.app).

## Approach

Railway is excellent for microservices. You can deploy each service individually while sharing a common PostgreSQL and Redis instance provided by Railway.

## Step 1: Database & Infrastructure

1. **Provision PostgreSQL**: In your Railway project, click **New** -> **Database** -> **Add PostgreSQL**.
2. **Provision Redis**: Click **New** -> **Database** -> **Add Redis**.
3. **Kafka**: Railway doesn't have a native Kafka service. You can either:
   - Deploy a Kafka Docker image (using a custom Dockerfile).
   - Use a managed provider like [Upstash](https://upstash.com) or [Confluent Cloud] and provide the connection strings via environment variables.

## Step 2: Deploy Platform Services

For each service (Discovery, Config, Gateway):

1. Click **New** -> **GitHub Repo** -> Select this repository.
2. In **Settings** -> **Root Directory**, set it to the specific service folder (e.g., `/discovery-server`).
3. Railway will automatically detect the `Dockerfile` and build it.

### Discovery Server (Eureka)
- No special environment variables needed for basic setup.

### Config Server
- Set `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`: `https://discovery-server-production.up.railway.app/eureka/` (Replace with your actual Discovery Server URL).

### API Gateway
- Set `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`.
- Set `SPRING_CONFIG_IMPORT`: `optional:configserver:https://config-server-production.up.railway.app`.
- Set `SPRING_DATA_REDIS_HOST`: Use the variable provided by the Railway Redis service.
- Set `JWT_SECRET`: Your secure secret.

## Step 3: Deploy Business Services

Follow the same steps as above, setting the **Root Directory** to `/user-service`, `/product-service`, etc.

### Required Environment Variables
For most services, you will need to map Railway's provided database variables to Spring Boot properties:

- `SPRING_DATASOURCE_URL`: `jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}` (You may need to manually create the separate databases `user_db`, `product_db`, etc., or use one database with different schemas).
- `SPRING_DATASOURCE_USERNAME`: `${{Postgres.PGUSER}}`
- `SPRING_DATASOURCE_PASSWORD`: `${{Postgres.PGPASSWORD}}`
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`: Your Kafka connection string.
- `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`.
- `SPRING_CONFIG_IMPORT`.

## Step 4: Networking & Public Access

1. Only the **API Gateway** needs a public domain.
2. Go to the **API Gateway** service -> **Settings** -> **Public Networking** -> **Generate Domain**.
3. All other services can remain private, communicating via Railway's internal network or Eureka.

## Common Issues

- **Memory Limits**: JVM services can be memory-heavy. Ensure you assign at least 512MB to 1GB of RAM per service in Railway settings.
- **Service Order**: Since services depend on Eureka and Config Server, the first deployments might fail. Railway will automatically restart them until they can successfully connect.
