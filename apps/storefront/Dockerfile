FROM node:26-alpine AS build
WORKDIR /workspace
COPY package.json package-lock.json ./
COPY apps/storefront/package.json apps/storefront/package.json
COPY apps/admin-web/package.json apps/admin-web/package.json
RUN npm ci
COPY apps/storefront apps/storefront
RUN npm run build:storefront

FROM nginx:1.27-alpine
COPY deployment/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/apps/storefront/dist /usr/share/nginx/html
EXPOSE 80
