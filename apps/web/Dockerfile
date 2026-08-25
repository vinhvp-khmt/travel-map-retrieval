FROM node:24-alpine AS build
WORKDIR /app
COPY apps/web/package.json apps/web/package-lock.json ./
RUN npm ci
COPY apps/web .
ARG VITE_API_BASE_URL=http://localhost:8080
ARG VITE_MAP_TILE_URL
ARG VITE_MAP_TILE_ATTRIBUTION
ARG VITE_GEOAPIFY_API_KEY
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_MAP_TILE_URL=$VITE_MAP_TILE_URL
ENV VITE_MAP_TILE_ATTRIBUTION=$VITE_MAP_TILE_ATTRIBUTION
ENV VITE_GEOAPIFY_API_KEY=$VITE_GEOAPIFY_API_KEY
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY apps/web/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 8080
