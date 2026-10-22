FROM node:22-alpine AS build
WORKDIR /workspace
COPY package.json package-lock.json ./
COPY apps/storefront/package.json apps/storefront/package.json
COPY apps/admin-web/package.json apps/admin-web/package.json
RUN npm ci
COPY apps/admin-web apps/admin-web
RUN npm run build:admin

FROM nginx:1.31-alpine
COPY deployment/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/apps/admin-web/dist /usr/share/nginx/html
EXPOSE 80
