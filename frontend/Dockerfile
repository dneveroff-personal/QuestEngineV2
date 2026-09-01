# syntax=docker/dockerfile:1

# ---- Build stage ----
FROM node:22-alpine AS build
WORKDIR /app

# Кэшируется отдельно от исходников — npm ci перезапускается только когда
# меняется package*.json, а не на каждое изменение src/.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ---- Serve stage ----
FROM nginx:1.27-alpine AS serve

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
